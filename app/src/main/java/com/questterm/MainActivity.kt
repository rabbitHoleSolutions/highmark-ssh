package com.questterm

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import java.util.UUID
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
import com.questterm.ui.screens.TerminalHostScreen
import com.questterm.ui.theme.QuestTermTheme
import com.termux.view.TerminalView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var scrollAccumulator = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fullscreen - hide all system bars to maximize terminal space
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        setContent {
            QuestTermTheme {
                TerminalHostScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
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
        const val EXTRA_SESSION_ID = "com.questterm.extra.SESSION_ID"
        const val EXTRA_AUTOCONNECT_PROFILE_ID = "com.questterm.extra.AUTOCONNECT_PROFILE_ID"

        fun createMultiWindowIntent(
            context: Context,
            sessionId: String? = null,
            profileId: String? = null,
        ): Intent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            val uriPath = when {
                sessionId != null -> "session/$sessionId"
                profileId != null -> "profile/$profileId"
                else -> "new/${UUID.randomUUID()}"
            }
            data = Uri.parse("questterm://window/$uriPath")
            if (sessionId != null) putExtra(EXTRA_SESSION_ID, sessionId)
            if (profileId != null) putExtra(EXTRA_AUTOCONNECT_PROFILE_ID, profileId)
        }
    }
}
