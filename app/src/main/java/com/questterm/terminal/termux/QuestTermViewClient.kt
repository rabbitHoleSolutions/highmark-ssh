package com.questterm.terminal.termux

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.ITerminalSessionHost
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

/**
 * Implements both TerminalViewClient (for TerminalView UI callbacks) and
 * TerminalSessionClient (for TerminalEmulator callbacks).
 */
class QuestTermViewClient(
    private val context: Context,
    private val onScreenUpdate: () -> Unit = {},
) : TerminalViewClient, TerminalSessionClient {

    private var terminalView: TerminalView? = null

    fun attachView(view: TerminalView) {
        terminalView = view
    }

    fun detachView() {
        terminalView = null
    }

    // === TerminalViewClient ===

    override fun onScale(scale: Float): Float = 1.0f

    override fun onSingleTapUp(e: MotionEvent) {
        terminalView?.let { view ->
            view.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            view.post { imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT) }
        }
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = true

    override fun shouldEnforceCharBasedInput(): Boolean = false

    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false

    override fun isTerminalViewSelected(): Boolean = true

    override fun copyModeChanged(copyMode: Boolean) {}

    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: ITerminalSessionHost): Boolean = false

    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false

    override fun onLongPress(event: MotionEvent): Boolean = false

    override fun readControlKey(): Boolean = false

    override fun readAltKey(): Boolean = false

    override fun readShiftKey(): Boolean = false

    override fun readFnKey(): Boolean = false

    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: ITerminalSessionHost): Boolean = false

    override fun onEmulatorSet() {
        terminalView?.setTerminalCursorBlinkerRate(500)
        terminalView?.setTerminalCursorBlinkerState(true, true)
    }

    // === TerminalSessionClient ===

    override fun onTextChanged(changedSession: TerminalSession?) {
        terminalView?.onScreenUpdated()
        onScreenUpdate()
    }

    override fun onTitleChanged(changedSession: TerminalSession?) {}

    override fun onSessionFinished(finishedSession: TerminalSession?) {}

    override fun onCopyTextToClipboard(session: TerminalSession?, text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Highmark SSH", text))
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {}

    override fun onBell(session: TerminalSession?) {}

    override fun onColorsChanged(session: TerminalSession?) {
        terminalView?.invalidate()
    }

    override fun onTerminalCursorStateChange(state: Boolean) {
        terminalView?.setTerminalCursorBlinkerState(state, true)
    }

    override fun getTerminalCursorStyle(): Int = 0

    // Log methods satisfy both TerminalViewClient and TerminalSessionClient
    override fun logError(tag: String?, message: String?) {
        if (tag != null && message != null) Log.e(tag, message)
    }

    override fun logWarn(tag: String?, message: String?) {
        if (tag != null && message != null) Log.w(tag, message)
    }

    override fun logInfo(tag: String?, message: String?) {
        if (tag != null && message != null) Log.i(tag, message)
    }

    override fun logDebug(tag: String?, message: String?) {
        if (tag != null && message != null) Log.d(tag, message)
    }

    override fun logVerbose(tag: String?, message: String?) {
        if (tag != null && message != null) Log.v(tag, message)
    }

    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {
        if (tag != null) Log.e(tag, message, e)
    }

    override fun logStackTrace(tag: String?, e: Exception?) {
        if (tag != null) Log.e(tag, "Error", e)
    }
}
