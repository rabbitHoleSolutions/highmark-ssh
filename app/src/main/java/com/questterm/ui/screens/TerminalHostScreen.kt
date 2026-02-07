package com.questterm.ui.screens

import android.app.Activity
import android.util.Log
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.questterm.session.SessionManager
import com.questterm.ui.components.QuickConnectDialog
import com.questterm.ui.components.TabBar
import com.questterm.ui.components.TerminalContent
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding

@Composable
fun TerminalHostScreen(
    sessionManager: SessionManager = hiltViewModel<TerminalViewModel>().sessionManager,
) {
    val tabs by sessionManager.tabs.collectAsState()
    val activeTabId by sessionManager.activeTabId.collectAsState()

    LaunchedEffect(tabs.size) {
        Log.d("TerminalHostScreen", "Tabs updated: size=${tabs.size}, tabs=$tabs")
    }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    var showConnectDialog by remember { mutableStateOf(tabs.isEmpty()) }
    var keyboardShowing by remember { mutableStateOf(true) }

    // Auto-show dialog when no tabs exist
    LaunchedEffect(tabs.isEmpty()) {
        if (tabs.isEmpty()) {
            showConnectDialog = true
        }
    }

    // Only register BackHandler when there's actual back functionality
    // (dialog is showing with tabs) - this makes the back button appear/disappear dynamically
    BackHandler(enabled = showConnectDialog && tabs.isNotEmpty()) {
        showConnectDialog = false
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            scope.launch {
                sessionManager.cleanup()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
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
                onNewTabClick = { showConnectDialog = true },
                onKeyboardToggle = {
                    val activity = context as? Activity ?: return@TabBar
                    val imm = activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
                    val focusedView = activity.currentFocus
                    if (focusedView != null) {
                        if (keyboardShowing) {
                            imm.hideSoftInputFromWindow(focusedView.windowToken, 0)
                        } else {
                            imm.showSoftInput(focusedView, InputMethodManager.SHOW_IMPLICIT)
                        }
                        keyboardShowing = !keyboardShowing
                    }
                },
            )
        }

        TerminalContent(
            sessionManager = sessionManager,
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
        )
    }
}
