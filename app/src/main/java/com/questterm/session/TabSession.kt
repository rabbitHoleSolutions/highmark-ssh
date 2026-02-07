package com.questterm.session

import com.questterm.ssh.SshSession
import com.questterm.terminal.termux.QuestTermViewClient
import com.questterm.terminal.termux.SshTerminalSession
import java.util.UUID

data class TabSession(
    val id: String = UUID.randomUUID().toString(),
    val label: String,  // "user@host"
    val sshSession: SshSession,
    val terminalSession: SshTerminalSession,
    val viewClient: QuestTermViewClient,
) {
    val displayLabel: String
        get() = if (label.length > 20) label.take(17) + "..." else label
}
