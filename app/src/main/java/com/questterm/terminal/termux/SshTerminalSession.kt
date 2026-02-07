package com.questterm.terminal.termux

import android.os.Handler
import android.os.Looper
import com.questterm.ssh.SshSession
import com.questterm.terminal.SessionState
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalOutput
import com.termux.terminal.TerminalSessionClient
import com.termux.view.ITerminalSessionHost
import com.trilead.ssh2.ServerHostKeyVerifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

/**
 * Bridge between SSH streams (via ConnectBot's sshlib) and Termux TerminalEmulator.
 *
 * This extends TerminalOutput (which TerminalEmulator calls when it needs to
 * write data back, e.g. terminal query responses) and implements
 * ITerminalSessionHost (which our forked TerminalView uses for rendering and input).
 */
class SshTerminalSession(
    private val sshSession: SshSession,
    val client: TerminalSessionClient,
    private val transcriptRows: Int = 10000,
) : TerminalOutput(), ITerminalSessionHost {

    private var emulator: TerminalEmulator? = null
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val writeExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val utf8InputBuffer = ByteArray(5)

    // Batch data from the IO read loop and flush to emulator on the main thread
    private val pendingData = java.io.ByteArrayOutputStream()
    private val dataLock = Object()
    @Volatile private var flushScheduled = false

    private val flushRunnable = Runnable {
        val dataToProcess: ByteArray
        synchronized(dataLock) {
            dataToProcess = pendingData.toByteArray()
            pendingData.reset()
            flushScheduled = false
        }
        if (dataToProcess.isNotEmpty()) {
            emulator?.append(dataToProcess, dataToProcess.size)
            client.onTextChanged(null)
        }
    }

    private val _state = MutableStateFlow<SessionState>(SessionState.Idle)
    val state: StateFlow<SessionState> = _state

    /**
     * Connect the SSH session and start the I/O loop.
     * Must be called from a coroutine (suspending).
     */
    suspend fun connect(
        cols: Int = 80,
        rows: Int = 24,
        hostKeyVerifier: ServerHostKeyVerifier? = null,
    ) {
        _state.value = SessionState.Connecting()
        try {
            sshSession.connect(cols, rows, hostKeyVerifier)

            // Create emulator on main thread since TerminalView reads it there
            // (unless updateSize() already created one with proper view dimensions)
            mainHandler.post {
                if (emulator == null) {
                    emulator = TerminalEmulator(this, cols, rows, transcriptRows, client)
                }
                _state.value = SessionState.Connected
                client.onTextChanged(null)
            }

            startReadLoop()
        } catch (e: Exception) {
            _state.value = SessionState.Error(e.message ?: "Connection failed", e)
            throw e
        }
    }

    private fun startReadLoop() {
        ioScope.launch {
            try {
                val buffer = ByteArray(8192)
                while (sshSession.isConnected) {
                    val bytesRead = sshSession.read(buffer, 0, buffer.size)

                    if (bytesRead == -1) break

                    if (bytesRead > 0) {
                        synchronized(dataLock) {
                            pendingData.write(buffer, 0, bytesRead)
                            if (!flushScheduled) {
                                flushScheduled = true
                                mainHandler.postDelayed(flushRunnable, 8)
                            }
                        }
                    }
                }
                // Final flush for any remaining data
                mainHandler.post(flushRunnable)
            } catch (e: Exception) {
                if (_state.value is SessionState.Connected) {
                    _state.value = SessionState.Disconnected
                    val detail = e.message ?: e.javaClass.simpleName
                    mainHandler.post {
                        val msg = "\r\n[Connection closed: $detail]"
                        val bytes = msg.toByteArray(StandardCharsets.UTF_8)
                        emulator?.append(bytes, bytes.size)
                        client.onTextChanged(null)
                        client.onSessionFinished(null)
                    }
                }
            }
        }
    }

    suspend fun disconnect() {
        ioScope.cancel()
        writeExecutor.shutdown()
        mainHandler.removeCallbacks(flushRunnable)
        sshSession.disconnect()
        _state.value = SessionState.Disconnected
    }

    // === TerminalOutput methods (called by TerminalEmulator for responses) ===

    override fun write(data: ByteArray, offset: Int, count: Int) {
        if (!sshSession.isConnected) return
        val copy = data.copyOfRange(offset, offset + count)
        try {
            writeExecutor.execute {
                if (!sshSession.isConnected) return@execute
                try {
                    sshSession.writeBytes(copy)
                } catch (_: Exception) {}
            }
        } catch (_: java.util.concurrent.RejectedExecutionException) {}
    }

    override fun titleChanged(oldTitle: String?, newTitle: String?) {
        client.onTitleChanged(null)
    }

    override fun onCopyTextToClipboard(text: String) {
        client.onCopyTextToClipboard(null, text)
    }

    override fun onPasteTextFromClipboard() {
        client.onPasteTextFromClipboard(null)
    }

    override fun onBell() {
        client.onBell(null)
    }

    override fun onColorsChanged() {
        client.onColorsChanged(null)
    }

    // === ITerminalSessionHost methods (called by TerminalView) ===

    override fun getEmulator(): TerminalEmulator? = emulator

    // write(data: String) is inherited from TerminalOutput (final) and satisfies
    // ITerminalSessionHost.write(String) - it delegates to write(byte[], int, int) above.

    override fun writeCodePoint(prependEscape: Boolean, codePoint: Int) {
        if (codePoint > 1114111 || (codePoint in 0xD800..0xDFFF)) {
            throw IllegalArgumentException("Invalid code point: $codePoint")
        }

        var bufferPosition = 0
        if (prependEscape) utf8InputBuffer[bufferPosition++] = 27

        if (codePoint <= 0b1111111) {
            utf8InputBuffer[bufferPosition++] = codePoint.toByte()
        } else if (codePoint <= 0b11111111111) {
            utf8InputBuffer[bufferPosition++] = (0b11000000 or (codePoint shr 6)).toByte()
            utf8InputBuffer[bufferPosition++] = (0b10000000 or (codePoint and 0b111111)).toByte()
        } else if (codePoint <= 0b1111111111111111) {
            utf8InputBuffer[bufferPosition++] = (0b11100000 or (codePoint shr 12)).toByte()
            utf8InputBuffer[bufferPosition++] = (0b10000000 or ((codePoint shr 6) and 0b111111)).toByte()
            utf8InputBuffer[bufferPosition++] = (0b10000000 or (codePoint and 0b111111)).toByte()
        } else {
            utf8InputBuffer[bufferPosition++] = (0b11110000 or (codePoint shr 18)).toByte()
            utf8InputBuffer[bufferPosition++] = (0b10000000 or ((codePoint shr 12) and 0b111111)).toByte()
            utf8InputBuffer[bufferPosition++] = (0b10000000 or ((codePoint shr 6) and 0b111111)).toByte()
            utf8InputBuffer[bufferPosition++] = (0b10000000 or (codePoint and 0b111111)).toByte()
        }
        write(utf8InputBuffer, 0, bufferPosition)
    }

    override fun updateSize(columns: Int, rows: Int) {
        // Flush any pending data before resize to avoid corruption
        synchronized(dataLock) {
            if (pendingData.size() > 0) {
                val data = pendingData.toByteArray()
                pendingData.reset()
                emulator?.append(data, data.size)
            }
            mainHandler.removeCallbacks(flushRunnable)
            flushScheduled = false
        }

        // Resize emulator locally
        val emu = emulator
        if (emu == null) {
            emulator = TerminalEmulator(this, columns, rows, transcriptRows, client)
        } else {
            emu.resize(columns, rows)
        }

        // Send resize to SSH server
        sshSession.resize(columns, rows)

        client.onTextChanged(null)
    }

    override fun getTitle(): String? = emulator?.title

    override fun reset() {
        emulator?.reset()
        client.onTextChanged(null)
    }
}
