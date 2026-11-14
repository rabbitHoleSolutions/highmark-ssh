package com.questterm.ui.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.questterm.session.SessionManager
import com.questterm.ui.components.QuickConnectDialog
import com.questterm.ui.components.TabBar
import com.questterm.ui.components.TerminalContent
import kotlinx.coroutines.launch

@Composable
fun TerminalHostScreen(
    windowId: String = SessionManager.DEFAULT_WINDOW_ID,
    onPopOutTab: ((String) -> Unit)? = null,
    onLastTabClosed: () -> Unit = {},
    sessionManager: SessionManager = hiltViewModel<TerminalViewModel>().sessionManager,
) {
    val tabs by remember(sessionManager, windowId) { sessionManager.tabsFlow(windowId) }
        .collectAsState(initial = sessionManager.tabsIn(windowId))
    val activeTab by remember(sessionManager, windowId) { sessionManager.activeTabFlow(windowId) }
        .collectAsState(initial = sessionManager.activeTabIn(windowId))
    val activeTabId = activeTab?.id

    LaunchedEffect(tabs.size) {
        Log.d("TerminalHostScreen", "Tabs updated for window $windowId: size=${tabs.size}, tabs=$tabs")
    }
    val scope = rememberCoroutineScope()

    var showConnectDialog by remember { mutableStateOf(tabs.isEmpty()) }

    // Auto-show dialog when no tabs exist. A window that had tabs and lost them
    // all reports it, so a popped-out window can close itself.
    var hadTabs by remember { mutableStateOf(tabs.isNotEmpty()) }
    LaunchedEffect(tabs.isEmpty()) {
        if (tabs.isEmpty()) {
            showConnectDialog = true
            if (hadTabs) onLastTabClosed()
        }
        hadTabs = tabs.isNotEmpty()
    }

    // Back button closes dialog if it's open (and tabs exist), or opens it if closed
    BackHandler(enabled = showConnectDialog && tabs.isNotEmpty()) {
        showConnectDialog = false
    }
    BackHandler(enabled = !showConnectDialog && tabs.isNotEmpty()) {
        showConnectDialog = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (tabs.isNotEmpty()) {
            TabBar(
                tabs = tabs,
                activeTabId = activeTabId,
                onTabClick = { sessionManager.switchTab(it) },
                onTabClose = { tabId ->
                    scope.launch {
                        sessionManager.removeTab(tabId)
                    }
                },
                onTabPopOut = onPopOutTab,
                onNewTabClick = { showConnectDialog = true },
                onKeyboardToggle = { activeTab?.viewClient?.toggleKeyboard() },
                onFontDecrease = { sessionManager.decreaseFontSize() },
                onFontIncrease = { sessionManager.increaseFontSize() },
            )
        }

        TerminalContent(
            sessionManager = sessionManager,
            modifier = Modifier.weight(1f),
            windowId = windowId,
        )
    }

    if (showConnectDialog) {
        QuickConnectDialog(
            onDismissRequest = {
                // Only allow dismiss if there's at least one tab
                if (tabs.isNotEmpty()) {
                    showConnectDialog = false
                }
            },
            onConnected = {
                showConnectDialog = false
            },
            windowId = windowId,
        )
    }
}
