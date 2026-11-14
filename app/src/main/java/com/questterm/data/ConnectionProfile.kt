package com.questterm.data

import com.google.gson.annotations.SerializedName
import com.questterm.ssh.CredentialEncryption
import com.questterm.ssh.SshKeyPairs
import com.trilead.ssh2.crypto.keys.Ed25519PublicKey
import java.security.KeyPair
import java.util.UUID

enum class AuthMethod {
    @SerializedName("PASSWORD") PASSWORD,
    @SerializedName("KEY") KEY,
}

/**
 * Persisted as JSON by [ConnectionHistoryStore]. Every field has a fixed
 * @SerializedName so R8 renaming can't change the stored format. The `alternate`
 * names are the R8-obfuscated keys that release builds up to 1.0.2 wrote, so
 * existing saved connections still load; never reuse or reorder them.
 */
data class ConnectionProfile(
    @SerializedName(value = "id", alternate = ["a"])
    val id: String = UUID.randomUUID().toString(),
    @SerializedName(value = "host", alternate = ["b"])
    val host: String,
    @SerializedName(value = "port", alternate = ["c"])
    val port: Int,
    @SerializedName(value = "username", alternate = ["d"])
    val username: String,
    @SerializedName(value = "encryptedPassword", alternate = ["e"])
    val encryptedPassword: String? = null,  // null if not remembered
    @SerializedName("authMethod")
    val authMethod: AuthMethod = AuthMethod.PASSWORD,
    @SerializedName("encryptedPrivateKey")
    val encryptedPrivateKey: String? = null,  // PKCS8, encrypted; kept even when the last login used a password
    @SerializedName("publicKeyRaw")
    val publicKeyRaw: String? = null,  // raw 32-byte ed25519 point, base64 (not sensitive)
    @SerializedName(value = "isFavorite", alternate = ["f"])
    val isFavorite: Boolean = false,
    @SerializedName(value = "lastUsedTimestamp", alternate = ["g"])
    val lastUsedTimestamp: Long = System.currentTimeMillis(),
) {
    val displayLabel: String
        get() = "$username@$host:$port"

    fun withPassword(password: String): ConnectionProfile =
        copy(
            authMethod = AuthMethod.PASSWORD,
            encryptedPassword = CredentialEncryption.encrypt(password),
        )

    fun getDecryptedPassword(): String? =
        encryptedPassword?.let { CredentialEncryption.decrypt(it) }

    /** Generates a new on-device ed25519 keypair and attaches it to this profile. */
    fun withGeneratedKey(keyPair: KeyPair): ConnectionProfile =
        copy(
            authMethod = AuthMethod.KEY,
            encryptedPrivateKey = CredentialEncryption.encrypt(SshKeyPairs.encodePrivateKey(keyPair)),
            publicKeyRaw = SshKeyPairs.encodePublicKeyRaw(keyPair.public as Ed25519PublicKey),
        )

    fun getKeyPair(): KeyPair? {
        val encryptedPrivate = encryptedPrivateKey?.takeIf { it.isNotEmpty() } ?: return null
        val rawPublic = publicKeyRaw?.takeIf { it.isNotEmpty() } ?: return null
        val decryptedPrivate = CredentialEncryption.decrypt(encryptedPrivate).takeIf { it.isNotEmpty() } ?: return null
        return try {
            KeyPair(SshKeyPairs.decodePublicKeyRaw(rawPublic), SshKeyPairs.decodePrivateKey(decryptedPrivate))
        } catch (_: Exception) {
            null
        }
    }

    /** The public key line to add to the server's ~/.ssh/authorized_keys, if this profile uses key auth. */
    fun getOpenSshPublicKey(): String? =
        publicKeyRaw?.let { SshKeyPairs.formatOpenSsh(SshKeyPairs.decodePublicKeyRaw(it), displayLabel) }

    fun updateLastUsed(): ConnectionProfile =
        copy(lastUsedTimestamp = System.currentTimeMillis())
}
