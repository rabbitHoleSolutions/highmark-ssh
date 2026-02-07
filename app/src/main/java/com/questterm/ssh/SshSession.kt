package com.questterm.ssh

import android.util.Log
import com.trilead.ssh2.ChannelCondition
import com.trilead.ssh2.Connection
import com.trilead.ssh2.ServerHostKeyVerifier
import com.trilead.ssh2.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SSH session using ConnectBot's sshlib (Trilead SSH2 fork).
 * This library is battle-tested on Android and should be more reliable than SSHJ.
 */
class SshSession(
    private val host: String,
    private val port: Int,
    private val username: String,
    private val password: String,
) {
    private var _connection: Connection? = null
    val connection: Connection? get() = _connection

    private var session: Session? = null
    private val running = AtomicBoolean(false)

    // Conditions to wait for when reading
    private val readConditions = ChannelCondition.STDOUT_DATA or
            ChannelCondition.STDERR_DATA or
            ChannelCondition.EOF or
            ChannelCondition.CLOSED

    val isConnected: Boolean
        get() = running.get() && _connection != null && session != null

    /**
     * Read data from the SSH session using waitForCondition (ConnectBot pattern).
     * Returns the number of bytes read, or -1 if the channel is closed/EOF.
     */
    fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val sess = session ?: return -1
        val stdout = sess.stdout ?: return -1

        // Wait for data or channel close (with 100ms timeout to allow checking for resize/disconnect)
        val conditions = sess.waitForCondition(readConditions, 100)

        // Check for timeout (no conditions met)
        if ((conditions and ChannelCondition.TIMEOUT) != 0) {
            return 0 // No data, but keep polling
        }

        // Check if channel is closed or EOF
        if ((conditions and ChannelCondition.CLOSED) != 0) {
            Log.d("SshSession", "Channel closed - exit status: ${sess.exitStatus}")
            return -1
        }
        if ((conditions and ChannelCondition.EOF) != 0) {
            // EOF but might still have data to read
            if ((conditions and ChannelCondition.STDOUT_DATA) == 0) {
                Log.d("SshSession", "EOF with no more data")
                return -1
            }
        }

        // Read available stdout data
        if ((conditions and ChannelCondition.STDOUT_DATA) != 0) {
            return stdout.read(buffer, offset, length)
        }

        // Handle stderr (just discard for now, like ConnectBot does)
        if ((conditions and ChannelCondition.STDERR_DATA) != 0) {
            val stderr = sess.stderr
            val discard = ByteArray(256)
            while ((stderr?.available() ?: 0) > 0) {
                stderr?.read(discard)
            }
        }

        return 0 // No data read this time, but channel still open
    }

    fun writeBytes(data: ByteArray) {
        val out = session?.stdin ?: throw IllegalStateException("Not connected")
        try {
            out.write(data)
            out.flush()
        } catch (e: Exception) {
            Log.e("SshSession", "Write failed: ${e.message}")
        }
    }

    suspend fun connect(
        cols: Int = 80,
        rows: Int = 24,
        hostKeyVerifier: ServerHostKeyVerifier? = null,
    ) = withContext(Dispatchers.IO) {
        Log.d("SshSession", "Connecting to $host:$port...")

        val conn = Connection(host, port)

        conn.connect(hostKeyVerifier, 30000, 30000)
        Log.d("SshSession", "Connected, authenticating...")

        val authResult = conn.authenticateWithPassword(username, password)
        if (!authResult) {
            conn.close()
            throw Exception("Authentication failed")
        }
        Log.d("SshSession", "Authenticated successfully")

        val sess = conn.openSession()
        Log.d("SshSession", "Session opened, requesting PTY...")

        // Request PTY with xterm-256color terminal type
        sess.requestPTY("xterm-256color", cols, rows, 0, 0, null)
        Log.d("SshSession", "PTY allocated, starting shell...")

        sess.startShell()
        Log.d("SshSession", "Shell started successfully")

        _connection = conn
        session = sess
        running.set(true)
    }

    fun resize(cols: Int, rows: Int) {
        // MUST run on separate thread - NOT on writeExecutor
        // If writeExecutor is blocked waiting for server, and server is waiting
        // for resize signal, we'd have a deadlock
        Thread {
            try {
                session?.resizePTY(cols, rows, 0, 0)
            } catch (_: Exception) {
            }
        }.start()
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        running.set(false)

        try { session?.close() } catch (_: Exception) {}
        try { _connection?.close() } catch (_: Exception) {}

        session = null
        _connection = null
        Log.d("SshSession", "Disconnected")
    }
}
