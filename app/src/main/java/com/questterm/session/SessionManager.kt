package com.questterm.session

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor() {

    private val _tabs = MutableStateFlow<List<TabSession>>(emptyList())
    val tabs: StateFlow<List<TabSession>> = _tabs.asStateFlow()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    val activeTab: StateFlow<TabSession?> = combine(_tabs, _activeTabId) { tabs, activeId ->
        tabs.find { it.id == activeId }
    }.stateIn(
        scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Main + kotlinx.coroutines.SupervisorJob()),
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun addTab(
        label: String,
        sshSession: com.questterm.ssh.SshSession,
        terminalSession: com.questterm.terminal.termux.SshTerminalSession,
        viewClient: com.questterm.terminal.termux.QuestTermViewClient,
    ): String {
        val tab = TabSession(
            label = label,
            sshSession = sshSession,
            terminalSession = terminalSession,
            viewClient = viewClient,
        )
        _tabs.value = _tabs.value + tab
        _activeTabId.value = tab.id
        Log.d("SessionManager", "Added tab: $label (id=${tab.id}), total tabs=${_tabs.value.size}")
        return tab.id
    }

    suspend fun removeTab(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return

        // Disconnect and cleanup
        tab.terminalSession.disconnect()

        // Remove from list
        val newTabs = _tabs.value.filter { it.id != tabId }
        _tabs.value = newTabs

        // Switch to adjacent tab if we removed the active one
        if (_activeTabId.value == tabId) {
            _activeTabId.value = newTabs.firstOrNull()?.id
        }
    }

    fun switchTab(tabId: String) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
        }
    }

    suspend fun cleanup() {
        _tabs.value.forEach { it.terminalSession.disconnect() }
        _tabs.value = emptyList()
        _activeTabId.value = null
    }
}
