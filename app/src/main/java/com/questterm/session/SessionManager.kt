package com.questterm.session

import android.util.Log
import com.questterm.data.TerminalSettingsStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val settingsStore: TerminalSettingsStore,
) {
    companion object {
        const val DEFAULT_WINDOW_ID = "default"
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _tabs = MutableStateFlow<List<TabSession>>(emptyList())
    val tabs: StateFlow<List<TabSession>> = _tabs.asStateFlow()

    private val _tabWindowMap = MutableStateFlow<Map<String, String>>(emptyMap())
    private val _windowActiveTab = MutableStateFlow<Map<String, String?>>(emptyMap())

    private val windowTabsFlows = ConcurrentHashMap<String, StateFlow<List<TabSession>>>()
    private val windowActiveTabFlows = ConcurrentHashMap<String, StateFlow<TabSession?>>()

    /** Current terminal font size (in pixels), persisted across sessions. */
    val fontSize: StateFlow<Int> = settingsStore.fontSize

    fun increaseFontSize() = settingsStore.increaseFontSize()

    fun decreaseFontSize() = settingsStore.decreaseFontSize()

    private val _activeTabId = MutableStateFlow<String?>(null)
    val activeTabId: StateFlow<String?> = _activeTabId.asStateFlow()

    val activeTab: StateFlow<TabSession?> = combine(_tabs, _activeTabId) { tabs, activeId ->
        tabs.find { it.id == activeId }
    }.stateIn(
        scope = scope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null,
    )

    fun getTabsForWindow(windowId: String): StateFlow<List<TabSession>> =
        windowTabsFlows.getOrPut(windowId) {
            val initialTabs = _tabs.value.filter { (_tabWindowMap.value[it.id] ?: DEFAULT_WINDOW_ID) == windowId }
            combine(_tabs, _tabWindowMap) { tabs, windowMap ->
                tabs.filter { (windowMap[it.id] ?: DEFAULT_WINDOW_ID) == windowId }
            }.stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = initialTabs,
            )
        }

    fun getActiveTabForWindow(windowId: String): StateFlow<TabSession?> =
        windowActiveTabFlows.getOrPut(windowId) {
            val initialTabs = _tabs.value.filter { (_tabWindowMap.value[it.id] ?: DEFAULT_WINDOW_ID) == windowId }
            val initialActiveId = _windowActiveTab.value[windowId]
            val initialActiveTab = initialTabs.find { it.id == initialActiveId } ?: initialTabs.firstOrNull()
            combine(_tabs, _tabWindowMap, _windowActiveTab) { tabs, windowMap, activeMap ->
                val windowTabs = tabs.filter { (windowMap[it.id] ?: DEFAULT_WINDOW_ID) == windowId }
                val activeId = activeMap[windowId]
                windowTabs.find { it.id == activeId } ?: windowTabs.firstOrNull()
            }.stateIn(
                scope = scope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = initialActiveTab,
            )
        }

    fun addTab(
        label: String,
        sshSession: com.questterm.ssh.SshSession,
        terminalSession: com.questterm.terminal.termux.SshTerminalSession,
        viewClient: com.questterm.terminal.termux.QuestTermViewClient,
        windowId: String = DEFAULT_WINDOW_ID,
    ): String {
        val tab = TabSession(
            label = label,
            sshSession = sshSession,
            terminalSession = terminalSession,
            viewClient = viewClient,
        )
        _tabWindowMap.update { it + (tab.id to windowId) }
        _windowActiveTab.update { it + (windowId to tab.id) }
        _tabs.value = _tabs.value + tab
        _activeTabId.value = tab.id
        Log.d("SessionManager", "Added tab: $label (id=${tab.id}, window=$windowId), total tabs=${_tabs.value.size}")
        return tab.id
    }

    suspend fun removeTab(tabId: String) {
        val tab = _tabs.value.find { it.id == tabId } ?: return

        // Disconnect and cleanup
        tab.terminalSession.disconnect()

        val windowId = _tabWindowMap.value[tabId] ?: DEFAULT_WINDOW_ID

        // Remove from list
        val newTabs = _tabs.value.filter { it.id != tabId }
        _tabs.value = newTabs

        // Remove from window mapping
        _tabWindowMap.update { it - tabId }

        // Switch to adjacent tab in this window only if the active tab was removed
        val currentWindowActive = _windowActiveTab.value[windowId]
        if (currentWindowActive == tabId) {
            val windowTabs = newTabs.filter { (_tabWindowMap.value[it.id] ?: DEFAULT_WINDOW_ID) == windowId }
            val newWindowActiveId = windowTabs.firstOrNull()?.id
            _windowActiveTab.update { it + (windowId to newWindowActiveId) }
        }

        if (_activeTabId.value == tabId) {
            _activeTabId.value = newTabs.firstOrNull()?.id
        }
    }

    fun switchTab(tabId: String, windowId: String? = null) {
        if (_tabs.value.any { it.id == tabId }) {
            _activeTabId.value = tabId
            val targetWindow = windowId ?: _tabWindowMap.value[tabId] ?: DEFAULT_WINDOW_ID
            _windowActiveTab.update { it + (targetWindow to tabId) }
        }
    }

    fun transferTabToWindow(tabId: String, targetWindowId: String): Boolean {
        val tab = _tabs.value.find { it.id == tabId } ?: return false
        val sourceWindowId = _tabWindowMap.value[tabId] ?: DEFAULT_WINDOW_ID

        if (sourceWindowId == targetWindowId) {
            _windowActiveTab.update { it + (targetWindowId to tabId) }
            return true
        }

        _tabWindowMap.update { it + (tabId to targetWindowId) }
        _windowActiveTab.update { it + (targetWindowId to tabId) }

        // If this tab was active in source window, update source window to another tab
        if (_windowActiveTab.value[sourceWindowId] == tabId) {
            val remainingSourceTabs = _tabs.value.filter {
                it.id != tabId && (_tabWindowMap.value[it.id] ?: DEFAULT_WINDOW_ID) == sourceWindowId
            }
            _windowActiveTab.update { it + (sourceWindowId to remainingSourceTabs.firstOrNull()?.id) }
        }

        Log.d("SessionManager", "Transferred tab $tabId from window $sourceWindowId to $targetWindowId")
        return true
    }

    suspend fun cleanupWindow(windowId: String) {
        val windowTabs = _tabs.value.filter { (_tabWindowMap.value[it.id] ?: DEFAULT_WINDOW_ID) == windowId }
        windowTabs.forEach { it.terminalSession.disconnect() }

        val remainingTabs = _tabs.value.filter { (_tabWindowMap.value[it.id] ?: DEFAULT_WINDOW_ID) != windowId }
        _tabs.value = remainingTabs
        _tabWindowMap.update { current ->
            current.filterKeys { tabId -> remainingTabs.any { it.id == tabId } }
        }
        _windowActiveTab.update { it - windowId }

        windowTabsFlows.remove(windowId)
        windowActiveTabFlows.remove(windowId)

        if (windowTabs.any { it.id == _activeTabId.value }) {
            _activeTabId.value = remainingTabs.firstOrNull()?.id
        }
        Log.d("SessionManager", "Cleaned up window $windowId (removed ${windowTabs.size} tabs, ${remainingTabs.size} remaining)")
    }

    suspend fun cleanup() {
        _tabs.value.forEach { it.terminalSession.disconnect() }
        _tabs.value = emptyList()
        _tabWindowMap.value = emptyMap()
        _windowActiveTab.value = emptyMap()
        windowTabsFlows.clear()
        windowActiveTabFlows.clear()
        _activeTabId.value = null
    }
}
