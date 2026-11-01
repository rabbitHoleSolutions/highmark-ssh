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
    sessionManager: SessionManager = hiltViewModel<TerminalViewModel>().sessionManager,
) {
    val windowTabsState = remember(sessionManager, windowId) {
        sessionManager.getTabsForWindow(windowId)
    }
    val tabs by windowTabsState.collectAsState()

    val activeTabState = remember(sessionManager, windowId) {
        sessionManager.getActiveTabForWindow(windowId)
    }
    val activeTab by activeTabState.collectAsState()
    val activeTabId = activeTab?.id

    LaunchedEffect(tabs.size) {
        Log.d("TerminalHostScreen", "Tabs updated for window $windowId: size=${tabs.size}, tabs=$tabs")
    }
    val scope = rememberCoroutineScope()

    var showConnectDialog by remember { mutableStateOf(tabs.isEmpty()) }

    // Auto-show dialog when no tabs exist, hide when tabs become available
    LaunchedEffect(tabs.isEmpty()) {
        showConnectDialog = tabs.isEmpty()
    }

    // Back button closes dialog if it's open (and tabs exist)
    BackHandler(enabled = showConnectDialog && tabs.isNotEmpty()) {
        showConnectDialog = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if (tabs.isNotEmpty()) {
            TabBar(
                tabs = tabs,
                activeTabId = activeTabId,
                onTabClick = { sessionManager.switchTab(it, windowId) },
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
            windowId = windowId,
            modifier = Modifier.weight(1f),
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
