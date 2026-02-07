package com.questterm

import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
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

        // Fullscreen - hide status bar to maximize terminal space
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        }

        setContent {
            QuestTermTheme {
                TerminalHostScreen()
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
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
}
