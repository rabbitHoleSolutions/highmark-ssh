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
        assertTrue(sessionManager.tabs.value.isEmpty())
        assertNull(sessionManager.activeTabId.value)
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
        assertEquals(1, sessionManager.tabs.value.size)
        assertEquals(tabId, sessionManager.tabs.value.first().id)
        assertEquals("dev@server:22", sessionManager.tabs.value.first().label)
        assertEquals(tabId, sessionManager.activeTabId.value)
    }

    @Test
    fun addMultipleTabsUpdatesActiveToLatest() {
        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        val tab2 = sessionManager.addTab("tab2", mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))

        assertEquals(2, sessionManager.tabs.value.size)
        assertEquals(tab2, sessionManager.activeTabId.value)

        sessionManager.switchTab(tab1)
        assertEquals(tab1, sessionManager.activeTabId.value)
    }

    @Test
    fun switchTabIgnoresNonExistentId() {
        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        sessionManager.switchTab("non-existent-id")
        assertEquals(tab1, sessionManager.activeTabId.value)
    }

    @Test
    fun removeActiveTabSwitchesToRemainingTab() = runTest {
        val termSession1 = mockk<SshTerminalSession>(relaxed = true)
        val termSession2 = mockk<SshTerminalSession>(relaxed = true)

        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), termSession1, mockk(relaxed = true))
        val tab2 = sessionManager.addTab("tab2", mockk(relaxed = true), termSession2, mockk(relaxed = true))

        assertEquals(tab2, sessionManager.activeTabId.value)

        // Removing tab2 should disconnect it and switch active to tab1
        sessionManager.removeTab(tab2)

        coVerify { termSession2.disconnect() }
        assertEquals(1, sessionManager.tabs.value.size)
        assertEquals(tab1, sessionManager.tabs.value.first().id)
        assertEquals(tab1, sessionManager.activeTabId.value)
    }

    @Test
    fun removeNonActiveTabRetainsCurrentActiveTab() = runTest {
        val termSession1 = mockk<SshTerminalSession>(relaxed = true)
        val termSession2 = mockk<SshTerminalSession>(relaxed = true)

        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), termSession1, mockk(relaxed = true))
        val tab2 = sessionManager.addTab("tab2", mockk(relaxed = true), termSession2, mockk(relaxed = true))

        assertEquals(tab2, sessionManager.activeTabId.value)

        // Removing tab1 (inactive) should disconnect tab1 but keep tab2 active
        sessionManager.removeTab(tab1)

        coVerify { termSession1.disconnect() }
        assertEquals(1, sessionManager.tabs.value.size)
        assertEquals(tab2, sessionManager.tabs.value.first().id)
        assertEquals(tab2, sessionManager.activeTabId.value)
    }

    @Test
    fun removeNonExistentTabDoesNothing() = runTest {
        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true))
        sessionManager.removeTab("unknown-id")

        assertEquals(1, sessionManager.tabs.value.size)
        assertEquals(tab1, sessionManager.activeTabId.value)
    }

    @Test
    fun removeOnlyTabLeavesActiveTabIdNull() = runTest {
        val termSession = mockk<SshTerminalSession>(relaxed = true)
        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), termSession, mockk(relaxed = true))

        sessionManager.removeTab(tab1)

        coVerify { termSession.disconnect() }
        assertTrue(sessionManager.tabs.value.isEmpty())
        assertNull(sessionManager.activeTabId.value)
    }

    @Test
    fun cleanupDisconnectsAllSessionsAndClearsState() = runTest {
        val termSession1 = mockk<SshTerminalSession>(relaxed = true)
        val termSession2 = mockk<SshTerminalSession>(relaxed = true)

        sessionManager.addTab("tab1", mockk(relaxed = true), termSession1, mockk(relaxed = true))
        sessionManager.addTab("tab2", mockk(relaxed = true), termSession2, mockk(relaxed = true))

        sessionManager.cleanup()

        coVerify { termSession1.disconnect() }
        coVerify { termSession2.disconnect() }
        assertTrue(sessionManager.tabs.value.isEmpty())
        assertNull(sessionManager.activeTabId.value)
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

    @Test
    fun getTabsForWindowFiltersTabsByWindowId() {
        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), windowId = "window-a")
        val tab2 = sessionManager.addTab("tab2", mockk(relaxed = true), mockk(relaxed = true), mockk(relaxed = true), windowId = "window-b")

        testDispatcher.scheduler.advanceUntilIdle()

        val windowATabs = sessionManager.getTabsForWindow("window-a").value
        val windowBTabs = sessionManager.getTabsForWindow("window-b").value

        assertEquals(1, windowATabs.size)
        assertEquals(tab1, windowATabs.first().id)

        assertEquals(1, windowBTabs.size)
        assertEquals(tab2, windowBTabs.first().id)
    }

    @Test
    fun transferTabToWindowMovesTabBetweenWindowsWithoutDisconnectingSession() {
        val termSession = mockk<SshTerminalSession>(relaxed = true)
        val tab = sessionManager.addTab("tab1", mockk(relaxed = true), termSession, mockk(relaxed = true), windowId = "window-1")

        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, sessionManager.getTabsForWindow("window-1").value.size)

        val success = sessionManager.transferTabToWindow(tab, "window-2")
        assertTrue(success)

        testDispatcher.scheduler.advanceUntilIdle()

        // Tab moved from window-1 to window-2
        assertTrue(sessionManager.getTabsForWindow("window-1").value.isEmpty())
        val window2Tabs = sessionManager.getTabsForWindow("window-2").value
        assertEquals(1, window2Tabs.size)
        assertEquals(tab, window2Tabs.first().id)

        // Session was preserved without disconnect
        coVerify(exactly = 0) { termSession.disconnect() }
    }

    @Test
    fun cleanupWindowDisconnectsOnlyTargetWindowTabs() = runTest {
        val termSession1 = mockk<SshTerminalSession>(relaxed = true)
        val termSession2 = mockk<SshTerminalSession>(relaxed = true)

        val tab1 = sessionManager.addTab("tab1", mockk(relaxed = true), termSession1, mockk(relaxed = true), windowId = "win-1")
        val tab2 = sessionManager.addTab("tab2", mockk(relaxed = true), termSession2, mockk(relaxed = true), windowId = "win-2")

        testDispatcher.scheduler.advanceUntilIdle()

        sessionManager.cleanupWindow("win-1")
        testDispatcher.scheduler.advanceUntilIdle()

        coVerify { termSession1.disconnect() }
        coVerify(exactly = 0) { termSession2.disconnect() }

        assertTrue(sessionManager.getTabsForWindow("win-1").value.isEmpty())
        val win2Tabs = sessionManager.getTabsForWindow("win-2").value
        assertEquals(1, win2Tabs.size)
        assertEquals(tab2, win2Tabs.first().id)
    }
}
