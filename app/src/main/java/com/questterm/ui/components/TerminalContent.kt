package com.questterm.ui.components

import android.content.Context
import android.graphics.Typeface
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.questterm.session.SessionManager
import com.questterm.terminal.termux.QuestTermViewClient
import com.questterm.terminal.termux.SshTerminalSession
import com.termux.view.TerminalRenderer
import com.termux.view.TerminalView

@Composable
fun TerminalContent(
    sessionManager: SessionManager,
    modifier: Modifier = Modifier,
) {
    val activeTab by sessionManager.activeTab.collectAsState()
    val session = activeTab?.terminalSession
    val viewClient = activeTab?.viewClient

    Box(modifier = modifier.fillMaxWidth().clipToBounds()) {
        if (session != null && viewClient != null) {
            AndroidView(
                factory = { ctx ->
                    TerminalView(ctx, null).apply {
                        mRenderer = TerminalRenderer(28, Typeface.MONOSPACE)
                        setTerminalViewClient(viewClient)
                        viewClient.attachView(this)
                        attachSession(session)
                        isFocusable = true
                        isFocusableInTouchMode = true
                    }
                },
                update = { view ->
                    val currentSession = view.getCurrentSession() as? SshTerminalSession

                    if (currentSession !== session) {
                        // Detach old viewClient
                        currentSession?.client?.let {
                            (it as? QuestTermViewClient)?.detachView()
                        }

                        // Attach new session
                        view.setTerminalViewClient(viewClient)
                        viewClient.attachView(view)
                        view.attachSession(session)
                    }

                    // Request focus and show keyboard after update
                    view.post {
                        view.requestFocus()
                        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
                    }
                },
                modifier = Modifier.fillMaxSize().clipToBounds(),
            )
        }
    }
}
