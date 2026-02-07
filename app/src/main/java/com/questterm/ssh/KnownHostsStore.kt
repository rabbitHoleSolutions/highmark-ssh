package com.questterm.ssh

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists known SSH host keys for trust-on-first-use (TOFU) verification.
 * Keys are stored as "algorithm:base64(key)" keyed by "host:port".
 */
@Singleton
class KnownHostsStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences("questterm_known_hosts", Context.MODE_PRIVATE)
    }

    enum class VerifyResult {
        /** Host not seen before — should prompt user to accept. */
        UNKNOWN,
        /** Host key matches stored key. */
        MATCH,
        /** Host key differs from stored key — possible MITM. */
        CHANGED,
    }

    fun verify(host: String, port: Int, algorithm: String, hostKey: ByteArray): VerifyResult {
        val key = hostKeyId(host, port)
        val stored = prefs.getString(key, null) ?: return VerifyResult.UNKNOWN
        val current = encodeHostKey(algorithm, hostKey)
        return if (stored == current) VerifyResult.MATCH else VerifyResult.CHANGED
    }

    fun accept(host: String, port: Int, algorithm: String, hostKey: ByteArray) {
        prefs.edit()
            .putString(hostKeyId(host, port), encodeHostKey(algorithm, hostKey))
            .apply()
    }

    fun remove(host: String, port: Int) {
        prefs.edit().remove(hostKeyId(host, port)).apply()
    }

    companion object {
        private fun hostKeyId(host: String, port: Int): String = "$host:$port"

        private fun encodeHostKey(algorithm: String, hostKey: ByteArray): String =
            "$algorithm:${Base64.encodeToString(hostKey, Base64.NO_WRAP)}"

        /** SHA-256 fingerprint in the standard colon-separated hex format. */
        fun fingerprint(hostKey: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256").digest(hostKey)
            return digest.joinToString(":") { "%02x".format(it) }
        }
    }
}
