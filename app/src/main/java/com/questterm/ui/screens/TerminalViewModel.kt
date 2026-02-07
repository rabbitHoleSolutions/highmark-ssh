package com.questterm.ui.screens

import androidx.lifecycle.ViewModel
import com.questterm.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class TerminalViewModel @Inject constructor(
    val sessionManager: SessionManager,
) : ViewModel()
