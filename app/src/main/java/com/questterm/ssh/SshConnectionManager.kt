package com.questterm.ssh

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SshConnectionManager @Inject constructor(
    val knownHostsStore: KnownHostsStore,
) {

    fun createSession(
        host: String,
        port: Int,
        username: String,
        password: String,
    ): SshSession {
        return SshSession(host, port, username, password)
    }
}
