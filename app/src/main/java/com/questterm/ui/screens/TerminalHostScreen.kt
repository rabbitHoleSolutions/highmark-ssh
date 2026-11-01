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
import com.questterm.ui.components.WindowCloseConfirmDialog
import kotlinx.coroutines.launch

@Composable
fun TerminalHostScreen(
    windowId: String = SessionManager.DEFAULT_WINDOW_ID,
    onPopOutTab: ((String) -> Unit)? = null,
    onCloseWindow: () -> Unit = {},
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
    var showCloseConfirmDialog by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }

    // Auto-show dialog when no tabs exist, hide when tabs become available
    LaunchedEffect(tabs.isEmpty()) {
        showConnectDialog = tabs.isEmpty()
        if (tabs.isEmpty()) {
            showCloseConfirmDialog = false
        }
    }

    // Back button behavior:
    // 1. If QuickConnectDialog is visible, back closes it (if other tabs exist).
    BackHandler(enabled = showConnectDialog && tabs.isNotEmpty()) {
        showConnectDialog = false
    }
    // 2. If WindowCloseConfirmDialog is visible, back dismisses the dialog.
    BackHandler(enabled = showCloseConfirmDialog) {
        showCloseConfirmDialog = false
    }
    // 3. If no dialogs are visible and tabs exist, back opens the close confirmation dialog.
    BackHandler(
        enabled = com.questterm.ui.components.WindowCloseConfirmDialogDefaults.shouldShowCloseConfirmOnBack(
            showConnectDialog = showConnectDialog,
            showCloseConfirmDialog = showCloseConfirmDialog,
            hasActiveTabs = tabs.isNotEmpty(),
        )
    ) {
        showCloseConfirmDialog = true
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

    if (showCloseConfirmDialog) {
        WindowCloseConfirmDialog(
            sessionCount = tabs.size,
            onConfirm = {
                showCloseConfirmDialog = false
                onCloseWindow()
            },
            onDismiss = {
                showCloseConfirmDialog = false
            },
        )
    }
}
