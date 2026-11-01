package com.questterm.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WindowCloseConfirmDialogTest {

    @Test
    fun `formatSessionWarning formats singular active session with grammatical precision`() {
        val message = WindowCloseConfirmDialogDefaults.formatSessionWarning(1)
        assertEquals(
            "This window has 1 active SSH session. Closing will disconnect the connection and terminate remote processes.",
            message,
        )
    }

    @Test
    fun `formatSessionWarning formats plural active sessions with grammatical precision`() {
        val message = WindowCloseConfirmDialogDefaults.formatSessionWarning(3)
        assertEquals(
            "This window has 3 active SSH sessions. Closing will disconnect all connections and terminate remote processes.",
            message,
        )
    }

    @Test
    fun `formatSessionWarning formats zero or negative session defensively as singular`() {
        val message = WindowCloseConfirmDialogDefaults.formatSessionWarning(0)
        assertEquals(
            "This window has 1 active SSH session. Closing will disconnect the connection and terminate remote processes.",
            message,
        )
    }

    @Test
    fun `shouldShowCloseConfirmOnBack returns true when active sessions exist and no dialogs are visible`() {
        val shouldShow = WindowCloseConfirmDialogDefaults.shouldShowCloseConfirmOnBack(
            showConnectDialog = false,
            showCloseConfirmDialog = false,
            hasActiveTabs = true,
        )
        assertTrue(shouldShow)
    }

    @Test
    fun `shouldShowCloseConfirmOnBack returns false when connect dialog is open`() {
        val shouldShow = WindowCloseConfirmDialogDefaults.shouldShowCloseConfirmOnBack(
            showConnectDialog = true,
            showCloseConfirmDialog = false,
            hasActiveTabs = true,
        )
        assertFalse(shouldShow)
    }

    @Test
    fun `shouldShowCloseConfirmOnBack returns false when close confirm dialog is already active`() {
        val shouldShow = WindowCloseConfirmDialogDefaults.shouldShowCloseConfirmOnBack(
            showConnectDialog = false,
            showCloseConfirmDialog = true,
            hasActiveTabs = true,
        )
        assertFalse(shouldShow)
    }

    @Test
    fun `shouldShowCloseConfirmOnBack returns false when window has no active tabs`() {
        val shouldShow = WindowCloseConfirmDialogDefaults.shouldShowCloseConfirmOnBack(
            showConnectDialog = false,
            showCloseConfirmDialog = false,
            hasActiveTabs = false,
        )
        assertFalse(shouldShow)
    }
}
