package com.questterm.session

import android.util.Log
import com.questterm.data.TerminalSettingsStore
import com.questterm.ssh.SshSession
import com.questterm.terminal.termux.QuestTermViewClient
import com.questterm.terminal.termux.SshTerminalSession
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionManagerTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsStore: TerminalSettingsStore
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0

        settingsStore = mockk(relaxed = true) {
            every { fontSize } returns MutableStateFlow(28)
        }
        sessionManager = SessionManager(settingsStore)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun initialStateIsEmpty() {
        assertTrue(sessionManager.allTabs.isEmpty())
        assertNull(activeTabId())
    }

    @Test
    fun addTabAddsTabAndSetsItActive() {
        val sshSession = mockk<SshSession>(relaxed = true)
        val terminalSession = mockk<SshTerminalSession>(relaxed = true)
        val viewClient = mockk<QuestTermViewClient>(relaxed = true)

        val tabId = sessionManager.addTab(
            label = "dev@server:22",
            sshSession = sshSession,
            terminalSession = terminalSession,
            viewClient = viewClient,
        )

        assertNotNull(tabId)
        assertEquals(1, sessionManager.allTabs.size)
        assertEquals(tabId, sessionManager.allTabs.first().id)
        assertEquals("dev@server:22", sessionManager.allTabs.first().label)
        assertEquals(tabId, activeTabId())
    }

    @Test
    fun addMultipleTabsUpdatesActiveToLatest() {
        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        val tab2 = sessionManager.addTab("tab2", mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        assertEquals(2, sessionManager.allTabs.size)
        assertEquals(tab2, activeTabId())

        sessionManager.switchTab(tab1)
        assertEquals(tab1, activeTabId())
    }

    @Test
    fun switchTabIgnoresNonExistentId() {
        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        sessionManager.switchTab("non-existent-id")
        assertEquals(tab1, activeTabId())
    }

    @Test
    fun removeActiveTabSwitchesToRemainingTab() = runTest {
        val termSession1 = mockk<SshTerminalSession>(relaxed = true)
        val termSession2 = mockk<SshTerminalSession>(relaxed = true)

        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), termSession1, mockk(relaxed = true))
        val tab2 = sessionManager.addTab("tab2", mockk(relaxed = true), termSession2, mockk(relaxed = true))

        assertEquals(tab2, activeTabId())

        // Removing tab2 should disconnect it and switch active to tab1
        sessionManager.removeTab(tab2)

        coVerify { termSession2.disconnect() }
        assertEquals(1, sessionManager.allTabs.size)
        assertEquals(tab1, sessionManager.allTabs.first().id)
        assertEquals(tab1, activeTabId())
    }

    @Test
    fun removeNonActiveTabRetainsCurrentActiveTab() = runTest {
        val termSession1 = mockk<SshTerminalSession>(relaxed = true)
        val termSession2 = mockk<SshTerminalSession>(relaxed = true)

        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), termSession1, mockk(relaxed = true))
        val tab2 = sessionManager.addTab("tab2", mockk(relaxed = true), termSession2, mockk(relaxed = true))

        assertEquals(tab2, activeTabId())

        // Removing tab1 (inactive) should disconnect tab1 but keep tab2 active
        sessionManager.removeTab(tab1)

        coVerify { termSession1.disconnect() }
        assertEquals(1, sessionManager.allTabs.size)
        assertEquals(tab2, sessionManager.allTabs.first().id)
        assertEquals(tab2, activeTabId())
    }

    @Test
    fun removeNonExistentTabDoesNothing() = runTest {
        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        sessionManager.removeTab("unknown-id")

        assertEquals(1, sessionManager.allTabs.size)
        assertEquals(tab1, activeTabId())
    }

    @Test
    fun removeOnlyTabLeavesActiveTabIdNull() = runTest {
        val termSession = mockk<SshTerminalSession>(relaxed = true)
        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), termSession, mockk(relaxed = true))

        sessionManager.removeTab(tab1)

        coVerify { termSession.disconnect() }
        assertTrue(sessionManager.allTabs.isEmpty())
        assertNull(activeTabId())
    }

    @Test
    fun closeWindowDisconnectsAllItsSessionsAndClearsState() = runTest {
        val termSession1 = mockk<SshTerminalSession>(relaxed = true)
        val termSession2 = mockk<SshTerminalSession>(relaxed = true)

        sessionManager.addTab("tab1", mockk(relaxed = true), termSession1, mockk(relaxed = true))
        sessionManager.addTab("tab2", mockk(relaxed = true), termSession2, mockk(relaxed = true))

        sessionManager.closeWindow(SessionManager.DEFAULT_WINDOW_ID)
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { termSession1.disconnect() }
        coVerify { termSession2.disconnect() }
        assertTrue(sessionManager.allTabs.isEmpty())
        assertNull(activeTabId())
    }

    @Test
    fun fontSizeControlsDelegateToSettingsStore() {
        sessionManager.increaseFontSize()
        verify { settingsStore.increaseFontSize() }

        sessionManager.decreaseFontSize()
        verify { settingsStore.decreaseFontSize() }
    }

    @Test
    fun tabSessionDisplayLabelTruncatesLongLabels() {
        val shortTab = TabSession(
            label = "short-label",
            sshSession = mockk(relaxed = true),
            terminalSession = mockk(relaxed = true),
            viewClient = mockk(relaxed = true),
        )
        assertEquals("short-label", shortTab.displayLabel)

        val longTab = TabSession(
            label = "a-very-long-server-hostname-that-exceeds-twenty-chars",
            sshSession = mockk(relaxed = true),
            terminalSession = mockk(relaxed = true),
            viewClient = mockk(relaxed = true),
        )
        assertEquals("a-very-long-serve...", longTab.displayLabel)
        assertEquals(20, longTab.displayLabel.length)
    }

    private fun activeTabId(windowId: String = SessionManager.DEFAULT_WINDOW_ID): String? =
        sessionManager.activeTabIn(windowId)?.id

    private fun addTabIn(windowId: String, terminalSession: SshTerminalSession = mockk(relaxed = true)): String =
        sessionManager.addTab("tab", mockk(relaxed = true), terminalSession, mockk(relaxed = true), windowId = windowId)

    @Test
    fun tabsInFiltersTabsByWindow() {
        val tabA = addTabIn("window-a")
        val tabB = addTabIn("window-b")

        assertEquals(listOf(tabA), sessionManager.tabsIn("window-a").map { it.id })
        assertEquals(listOf(tabB), sessionManager.tabsIn("window-b").map { it.id })
        assertTrue(sessionManager.tabsIn(SessionManager.DEFAULT_WINDOW_ID).isEmpty())
        assertEquals(tabA, activeTabId("window-a"))
        assertEquals(tabB, activeTabId("window-b"))
    }

    @Test
    fun switchTabOnlyAffectsTheTabsOwnWindow() {
        val a1 = addTabIn("window-a")
        val a2 = addTabIn("window-a")
        val b1 = addTabIn("window-b")

        sessionManager.switchTab(a1)

        assertEquals(a1, activeTabId("window-a"))
        assertEquals(b1, activeTabId("window-b"))
        assertTrue(a2 in sessionManager.tabsIn("window-a").map { it.id })
    }

    @Test
    fun moveTabToWindowMovesLiveTabWithoutDisconnecting() {
        val termSession = mockk<SshTerminalSession>(relaxed = true)
        val keep = addTabIn("window-1")
        val moving = addTabIn("window-1", termSession)
        assertEquals(moving, activeTabId("window-1"))

        assertTrue(sessionManager.moveTabToWindow(moving, "window-2"))

        assertEquals(listOf(keep), sessionManager.tabsIn("window-1").map { it.id })
        assertEquals(listOf(moving), sessionManager.tabsIn("window-2").map { it.id })
        // Source window falls back to its remaining tab; target shows the moved one.
        assertEquals(keep, activeTabId("window-1"))
        assertEquals(moving, activeTabId("window-2"))
        coVerify(exactly = 0) { termSession.disconnect() }
    }

    @Test
    fun moveTabToWindowReturnsFalseForUnknownTab() {
        assertFalse(sessionManager.moveTabToWindow("gone", "window-2"))
        assertTrue(sessionManager.tabsIn("window-2").isEmpty())
    }

    @Test
    fun closeWindowDisconnectsOnlyThatWindowsTabs() = runTest {
        val termSession1 = mockk<SshTerminalSession>(relaxed = true)
        val termSession2 = mockk<SshTerminalSession>(relaxed = true)
        addTabIn("win-1", termSession1)
        val tab2 = addTabIn("win-2", termSession2)

        sessionManager.closeWindow("win-1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { termSession1.disconnect() }
        coVerify(exactly = 0) { termSession2.disconnect() }
        assertTrue(sessionManager.tabsIn("win-1").isEmpty())
        assertEquals(listOf(tab2), sessionManager.tabsIn("win-2").map { it.id })
    }

    @Test
    fun tabsFlowEmitsWindowChanges() = runTest {
        val tab = addTabIn("window-1")
        assertEquals(listOf(tab), sessionManager.tabsFlow("window-1").first().map { it.id })

        sessionManager.moveTabToWindow(tab, "window-2")

        assertTrue(sessionManager.tabsFlow("window-1").first().isEmpty())
        assertEquals(tab, sessionManager.activeTabFlow("window-2").first()?.id)
    }
}
