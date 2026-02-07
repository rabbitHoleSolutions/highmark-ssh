package com.questterm.ssh

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class CachedConnection(
    val host: String,
    val port: String,
    val username: String,
    val password: String,
)

@Singleton
class ConnectionCache @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("questterm_connections", Context.MODE_PRIVATE)
    }

    fun save(host: String, port: String, username: String, password: String) {
        prefs.edit()
            .putString(KEY_HOST, host)
            .putString(KEY_PORT, port)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, CredentialEncryption.encrypt(password))
            .putBoolean(KEY_ENCRYPTED, true)
            .apply()
    }

    fun load(): CachedConnection? {
        val host = prefs.getString(KEY_HOST, null) ?: return null
        val rawPassword = prefs.getString(KEY_PASSWORD, "") ?: ""
        val isEncrypted = prefs.getBoolean(KEY_ENCRYPTED, false)

        // Decrypt password, or migrate legacy plaintext entries
        val password = if (isEncrypted) {
            CredentialEncryption.decrypt(rawPassword)
        } else {
            // Legacy plaintext - re-save encrypted
            val plain = rawPassword
            val port = prefs.getString(KEY_PORT, "22") ?: "22"
            val username = prefs.getString(KEY_USERNAME, "") ?: ""
            save(host, port, username, plain)
            plain
        }

        return CachedConnection(
            host = host,
            port = prefs.getString(KEY_PORT, "22") ?: "22",
            username = prefs.getString(KEY_USERNAME, "") ?: "",
            password = password,
        )
    }

    private companion object {
        const val KEY_HOST = "host"
        const val KEY_PORT = "port"
        const val KEY_USERNAME = "username"
        const val KEY_PASSWORD = "password"
        const val KEY_ENCRYPTED = "encrypted"
    }
}
