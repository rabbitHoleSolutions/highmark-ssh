package com.questterm.data

import com.questterm.ssh.CredentialEncryption
import java.util.UUID

data class ConnectionProfile(
    val id: String = UUID.randomUUID().toString(),
    val host: String,
    val port: Int,
    val username: String,
    val encryptedPassword: String? = null,  // null if not remembered
    val isFavorite: Boolean = false,
    val lastUsedTimestamp: Long = System.currentTimeMillis(),
) {
    val displayLabel: String
        get() = "$username@$host:$port"

    fun withPassword(password: String): ConnectionProfile =
        copy(encryptedPassword = CredentialEncryption.encrypt(password))

    fun getDecryptedPassword(): String? =
        encryptedPassword?.let { CredentialEncryption.decrypt(it) }

    fun updateLastUsed(): ConnectionProfile =
        copy(lastUsedTimestamp = System.currentTimeMillis())
}
