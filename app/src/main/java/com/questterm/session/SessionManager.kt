package com.questterm.session

import android.util.Log
import com.questterm.data.TerminalSettingsStore
import com.questterm.ssh.SshSession
import com.questterm.terminal.termux.QuestTermViewClient
import com.questterm.terminal.termux.SshTerminalSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns every open SSH tab, across all windows.
 *
 * Each tab belongs to exactly one window (an activity instance, identified by a
 * window id). The main window uses [DEFAULT_WINDOW_ID]; tabs popped out into their
 * own window get a fresh id. Tab list, window ownership and per-window active tab
 * live in a single [State] so a window never sees a half-applied update.
 */
@Singleton
class SessionManager @Inject constructor(
    private val settingsStore: TerminalSettingsStore,
) {
    companion object {
        const val DEFAULT_WINDOW_ID = "default"
        private const val TAG = "SessionManager"
    }

    private data class State(
        val tabs: List<TabSession> = emptyList(),
        /** tabId -> windowId. Every open tab has an entry. */
        val windowOf: Map<String, String> = emptyMap(),
        /** windowId -> the tab the user last selected in that window. */
        val activeIn: Map<String, String> = emptyMap(),
    ) {
        fun tabsIn(windowId: String): List<TabSession> =
            tabs.filter { windowOf[it.id] == windowId }

        /** The selected tab, or the window's first tab if the selection was closed or moved. */
        fun activeTabIn(windowId: String): TabSession? {
            val windowTabs = tabsIn(windowId)
            return windowTabs.find { it.id == activeIn[windowId] } ?: windowTabs.firstOrNull()
        }

        fun without(tabIds: Set<String>): State = copy(
            tabs = tabs.filter { it.id !in tabIds },
            windowOf = windowOf - tabIds,
            activeIn = activeIn.filterValues { it !in tabIds },
        )
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val state = MutableStateFlow(State())

    /** Current terminal font size (in pixels), persisted across sessions. */
    val fontSize: StateFlow<Int> = settingsStore.fontSize

    fun increaseFontSize() = settingsStore.increaseFontSize()

    fun decreaseFontSize() = settingsStore.decreaseFontSize()

    /** Every open tab, across all windows. */
    val allTabs: List<TabSession> get() = state.value.tabs

    fun tabsIn(windowId: String): List<TabSession> = state.value.tabsIn(windowId)

    fun activeTabIn(windowId: String): TabSession? = state.value.activeTabIn(windowId)

    fun tabsFlow(windowId: String): Flow<List<TabSession>> =
        state.map { it.tabsIn(windowId) }.distinctUntilChanged()

    fun activeTabFlow(windowId: String): Flow<TabSession?> =
        state.map { it.activeTabIn(windowId) }.distinctUntilChanged()

    fun addTab(
        label: String,
        sshSession: SshSession,
        terminalSession: SshTerminalSession,
        viewClient: QuestTermViewClient,
        windowId: String = DEFAULT_WINDOW_ID,
    ): String {
        val tab = TabSession(
            label = label,
            sshSession = sshSession,
            terminalSession = terminalSession,
            viewClient = viewClient,
        )
        state.update {
            it.copy(
                tabs = it.tabs + tab,
                windowOf = it.windowOf + (tab.id to windowId),
                activeIn = it.activeIn + (windowId to tab.id),
            )
        }
        Log.d(TAG, "Added tab: $label (id=${tab.id}, window=$windowId), total tabs=${allTabs.size}")
        return tab.id
    }

    suspend fun removeTab(tabId: String) {
        val tab = allTabs.find { it.id == tabId } ?: return
        state.update { it.without(setOf(tabId)) }
        tab.terminalSession.disconnect()
    }

    fun switchTab(tabId: String) {
        state.update { current ->
            val windowId = current.windowOf[tabId] ?: return@update current
            current.copy(activeIn = current.activeIn + (windowId to tabId))
        }
    }

    /**
     * Moves a live tab into another window without disconnecting it. The tab
     * becomes the active one in [targetWindowId]. Returns false if the tab is gone.
     */
    fun moveTabToWindow(tabId: String, targetWindowId: String): Boolean {
        var moved = false
        state.update { current ->
            if (tabId !in current.windowOf) return@update current
            moved = true
            current.copy(
                windowOf = current.windowOf + (tabId to targetWindowId),
                activeIn = current.activeIn.filterValues { it != tabId } + (targetWindowId to tabId),
            )
        }
        if (moved) Log.d(TAG, "Moved tab $tabId to window $targetWindowId")
        return moved
    }

    /**
     * Closes a window's tabs and disconnects their sessions. Tabs in other windows
     * are untouched. The tabs disappear immediately; disconnects finish in the
     * background so this is safe to call from onDestroy.
     */
    fun closeWindow(windowId: String) {
        val closing = tabsIn(windowId)
        if (closing.isEmpty()) return
        state.update { it.without(closing.map(TabSession::id).toSet()) }
        Log.d(TAG, "Closing window $windowId: disconnecting ${closing.size} tab(s), ${allTabs.size} remaining")
        scope.launch {
            closing.forEach { tab ->
                runCatching { tab.terminalSession.disconnect() }
                    .onFailure { Log.w(TAG, "Disconnect failed for tab ${tab.id}", it) }
            }
        }
    }
}
