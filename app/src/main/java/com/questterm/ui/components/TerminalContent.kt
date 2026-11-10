package com.questterm.ui.components

import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
    windowId: String = SessionManager.DEFAULT_WINDOW_ID,
) {
    val activeTabState = remember(sessionManager, windowId) {
        sessionManager.getActiveTabForWindow(windowId)
    }
    val activeTab by activeTabState.collectAsState()
    val fontSize by sessionManager.fontSize.collectAsState()
    val session = activeTab?.terminalSession
    val viewClient = activeTab?.viewClient

    // Tracks the size last pushed into the view so we only rebuild the renderer
    // when the user actually changes the font size.
    var appliedFontSize by remember { mutableIntStateOf(fontSize) }

    Box(modifier = modifier.fillMaxWidth().clipToBounds()) {
        if (session != null && viewClient != null) {
            AndroidView(
                factory = { ctx ->
                    TerminalView(ctx, null).apply {
                        mRenderer = TerminalRenderer(fontSize, Typeface.MONOSPACE)
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

                    // Apply font-size changes by rebuilding the renderer, which
                    // also reflows the terminal grid and pushes the new size to
                    // the SSH session.
                    if (fontSize != appliedFontSize) {
                        view.setTextSize(fontSize)
                        appliedFontSize = fontSize
                    }

                    // Request focus so physical keyboards work immediately.
                    // Don't force-show the soft keyboard — virtual keyboard users
                    // can tap the terminal or use the toggle button to summon it.
                    view.requestFocus()
                },
                onRelease = { view ->
                    ((view.getCurrentSession() as? SshTerminalSession)?.client as? QuestTermViewClient)?.detachView()
                },
                modifier = Modifier.fillMaxSize().clipToBounds(),
            )
        }
    }
}
