package com.questterm

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.questterm.session.SessionManager
import com.questterm.ui.screens.TerminalHostScreen
import com.questterm.ui.theme.QuestTermTheme
import com.termux.view.TerminalView
import dagger.hilt.android.AndroidEntryPoint
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sessionManager: SessionManager

    private var scrollAccumulator = 0f

    /**
     * Which window's tabs this activity shows. The launcher-started window is
     * [SessionManager.DEFAULT_WINDOW_ID]; each popped-out tab gets its own window.
     */
    private lateinit var windowId: String

    private val isPopOutWindow: Boolean
        get() = windowId != SessionManager.DEFAULT_WINDOW_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        windowId = savedInstanceState?.getString(KEY_WINDOW_ID)
            ?: intent.getStringExtra(EXTRA_WINDOW_ID)
            ?: SessionManager.DEFAULT_WINDOW_ID

        // Claim the popped-out tab only on first launch. The move happens here,
        // not in the window it came from, so if this window never opens the tab
        // simply stays where it was.
        if (savedInstanceState == null) {
            intent.getStringExtra(EXTRA_SESSION_ID)?.let { sessionManager.moveTabToWindow(it, windowId) }
        }

        // Fullscreen - hide all system bars to maximize terminal space
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        setContent {
            QuestTermTheme {
                TerminalHostScreen(
                    windowId = windowId,
                    onPopOutTab = ::popOutTab,
                    onLastTabClosed = { if (isPopOutWindow) finishAndRemoveTask() },
                    sessionManager = sessionManager,
                )
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_WINDOW_ID, windowId)
    }

    override fun onDestroy() {
        // Going away for good (user closed the window, or the system dropped it):
        // disconnect this window's sessions. Other windows keep theirs. A config
        // change recreates the activity with the same window id, so keep them then.
        if (!isChangingConfigurations) {
            sessionManager.closeWindow(windowId)
        }
        super.onDestroy()
    }

    private fun popOutTab(tabId: String) {
        try {
            startActivity(createPopOutIntent(this, tabId, UUID.randomUUID().toString()))
        } catch (e: ActivityNotFoundException) {
            Log.w(TAG, "Could not open a new window for tab $tabId", e)
        }
    }

    override fun onResume() {
        super.onResume()
        refocusTerminal()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
            refocusTerminal()
        }
    }

    /**
     * Re-focus the terminal view and rebuild the IME InputConnection.
     * Called from both onResume (app foregrounded) and onWindowFocusChanged
     * (Quest system overlay like dimmer dismissed).
     */
    private fun refocusTerminal() {
        findTerminalView(window.decorView)?.let { tv ->
            tv.requestFocus()
            tv.post {
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.restartInput(tv)
            }
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_SCROLL) {
            val vscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL)
            if (vscroll != 0f) {
                findTerminalView(window.decorView)?.let { tv ->
                    scrollAccumulator += vscroll
                    val rows = scrollAccumulator.toInt()
                    if (rows != 0) {
                        // Positive VSCROLL = scroll up (negative rows), negative = scroll down
                        tv.scroll(-rows)
                        scrollAccumulator -= rows
                    }
                    return true
                }
            }
        }
        return super.dispatchGenericMotionEvent(event)
    }

    private fun findTerminalView(view: View): TerminalView? {
        if (view is TerminalView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                findTerminalView(view.getChildAt(i))?.let { return it }
            }
        }
        return null
    }

    companion object {
        private const val TAG = "MainActivity"
        const val EXTRA_SESSION_ID = "com.questterm.extra.SESSION_ID"
        const val EXTRA_WINDOW_ID = "com.questterm.extra.WINDOW_ID"
        private const val KEY_WINDOW_ID = "com.questterm.state.WINDOW_ID"

        /** Opens [tabId] in a new window (its own task, so Horizon OS gives it its own panel). */
        fun createPopOutIntent(context: Context, tabId: String, windowId: String): Intent =
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                // A distinct data URI keeps each window's task separate in recents.
                data = Uri.parse("questterm://window/$windowId")
                putExtra(EXTRA_SESSION_ID, tabId)
                putExtra(EXTRA_WINDOW_ID, windowId)
            }
    }
}
