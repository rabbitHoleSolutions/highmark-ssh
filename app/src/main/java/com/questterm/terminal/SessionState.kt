package com.questterm.terminal

sealed class SessionState {
    data object Idle : SessionState()
    data class Connecting(val message: String = "Connecting...") : SessionState()
    data object Connected : SessionState()
    data class Error(val message: String, val cause: Throwable? = null) : SessionState()
    data object Disconnected : SessionState()
}
