package com.questterm.data

import com.google.gson.Gson
import com.questterm.ssh.CredentialEncryption
import com.questterm.ssh.SshKeyPairs
import com.trilead.ssh2.crypto.keys.Ed25519PrivateKey
import com.trilead.ssh2.crypto.keys.Ed25519PublicKey
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.security.KeyPair

class ConnectionProfileTest {

    private val gson = Gson()

    @Before
    fun setUp() {
        mockkObject(CredentialEncryption)
        mockkObject(SshKeyPairs)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun displayLabelFormatsCorrectly() {
        val profile = ConnectionProfile(
            host = "192.168.1.50",
            port = 2222,
            username = "admin",
        )
        assertEquals("admin@192.168.1.50:2222", profile.displayLabel)
    }

    @Test
    fun gsonSerializationAndDeserializationPreservesFields() {
        val profile = ConnectionProfile(
            id = "test-uuid-1234",
            host = "example.com",
            port = 22,
            username = "developer",
            encryptedPassword = "encrypted-secret-blob",
            authMethod = AuthMethod.PASSWORD,
            encryptedPrivateKey = null,
            publicKeyRaw = null,
            isFavorite = true,
            lastUsedTimestamp = 1700000000000L,
        )

        val json = gson.toJson(profile)
        val deserialized = gson.fromJson(json, ConnectionProfile::class.java)

        assertEquals(profile.id, deserialized.id)
        assertEquals("example.com", deserialized.host)
        assertEquals(22, deserialized.port)
        assertEquals("developer", deserialized.username)
        assertEquals("encrypted-secret-blob", deserialized.encryptedPassword)
        assertEquals(AuthMethod.PASSWORD, deserialized.authMethod)
        assertTrue(deserialized.isFavorite)
        assertEquals(1700000000000L, deserialized.lastUsedTimestamp)
    }

    @Test
    fun gsonKeyProfileSerializationRoundTrip() {
        val keyProfile = ConnectionProfile(
            id = "key-profile-1",
            host = "keyserver.net",
            port = 2200,
            username = "deployer",
            authMethod = AuthMethod.KEY,
            encryptedPrivateKey = "encrypted-pkcs8-key",
            publicKeyRaw = "raw-ed25519-public-key-base64",
        )

        val json = gson.toJson(keyProfile)
        val deserialized = gson.fromJson(json, ConnectionProfile::class.java)

        assertEquals(AuthMethod.KEY, deserialized.authMethod)
        assertEquals("encrypted-pkcs8-key", deserialized.encryptedPrivateKey)
        assertEquals("raw-ed25519-public-key-base64", deserialized.publicKeyRaw)
        assertNull(deserialized.encryptedPassword)
    }

    @Test
    fun gsonDeserializesLegacyObfuscatedKeysCorrectly() {
        // Release builds <= 1.0.2 serialized with R8 obfuscated names: a, b, c, d, e, f, g
        val legacyJson = """
            {
                "a": "legacy-id-9999",
                "b": "legacy.host.org",
                "c": 2200,
                "d": "legacyuser",
                "e": "legacy-encrypted-pwd",
                "f": true,
                "g": 1690000000000
            }
        """.trimIndent()

        val deserialized = gson.fromJson(legacyJson, ConnectionProfile::class.java)

        assertEquals("legacy-id-9999", deserialized.id)
        assertEquals("legacy.host.org", deserialized.host)
        assertEquals(2200, deserialized.port)
        assertEquals("legacyuser", deserialized.username)
        assertEquals("legacy-encrypted-pwd", deserialized.encryptedPassword)
        assertTrue(deserialized.isFavorite)
        assertEquals(1690000000000L, deserialized.lastUsedTimestamp)
        assertEquals(AuthMethod.PASSWORD, deserialized.authMethod)
    }

    @Test
    fun withPasswordSetsEncryptedPasswordAndAuthMethod() {
        every { CredentialEncryption.encrypt("my-secret-password") } returns "mocked-encrypted-password"

        val profile = ConnectionProfile(
            host = "myserver.com",
            port = 22,
            username = "user1",
        ).withPassword("my-secret-password")

        assertEquals(AuthMethod.PASSWORD, profile.authMethod)
        assertEquals("mocked-encrypted-password", profile.encryptedPassword)
    }

    @Test
    fun withGeneratedKeySetsKeyAuthAndEncryptedKeys() {
        val mockPublic = mockk<Ed25519PublicKey>()
        val mockPrivate = mockk<Ed25519PrivateKey>()
        val keyPair = KeyPair(mockPublic, mockPrivate)

        every { SshKeyPairs.encodePrivateKey(keyPair) } returns "raw-priv-b64"
        every { SshKeyPairs.encodePublicKeyRaw(mockPublic) } returns "raw-pub-b64"
        every { CredentialEncryption.encrypt("raw-priv-b64") } returns "enc-priv-b64"

        val profile = ConnectionProfile(
            host = "myserver.com",
            port = 22,
            username = "user1",
        ).withGeneratedKey(keyPair)

        assertEquals(AuthMethod.KEY, profile.authMethod)
        assertEquals("enc-priv-b64", profile.encryptedPrivateKey)
        assertEquals("raw-pub-b64", profile.publicKeyRaw)
    }

    @Test
    fun getKeyPairReturnsNullWhenKeysMissingOrEmpty() {
        val noKeys = ConnectionProfile(host = "h", port = 22, username = "u")
        assertNull(noKeys.getKeyPair())

        val emptyPriv = ConnectionProfile(
            host = "h", port = 22, username = "u",
            encryptedPrivateKey = "",
            publicKeyRaw = "pub",
        )
        assertNull(emptyPriv.getKeyPair())

        val emptyPub = ConnectionProfile(
            host = "h", port = 22, username = "u",
            encryptedPrivateKey = "priv",
            publicKeyRaw = "",
        )
        assertNull(emptyPub.getKeyPair())
    }

    @Test
    fun getKeyPairDecodesSuccessfullyWhenValid() {
        val mockPub = mockk<Ed25519PublicKey>()
        val mockPriv = mockk<Ed25519PrivateKey>()

        every { CredentialEncryption.decrypt("enc-priv") } returns "dec-priv"
        every { SshKeyPairs.decodePublicKeyRaw("raw-pub") } returns mockPub
        every { SshKeyPairs.decodePrivateKey("dec-priv") } returns mockPriv

        val profile = ConnectionProfile(
            host = "h", port = 22, username = "u",
            encryptedPrivateKey = "enc-priv",
            publicKeyRaw = "raw-pub",
        )

        val keyPair = profile.getKeyPair()
        assertNotNull(keyPair)
        assertEquals(mockPub, keyPair?.public)
        assertEquals(mockPriv, keyPair?.private)
    }

    @Test
    fun getDecryptedPasswordReturnsDecryptedValue() {
        every { CredentialEncryption.decrypt("stored-ciphertext") } returns "original-cleartext"

        val profile = ConnectionProfile(
            host = "myserver.com",
            port = 22,
            username = "user1",
            encryptedPassword = "stored-ciphertext",
        )

        assertEquals("original-cleartext", profile.getDecryptedPassword())
    }

    @Test
    fun getDecryptedPasswordReturnsNullWhenNoPasswordStored() {
        val profile = ConnectionProfile(
            host = "myserver.com",
            port = 22,
            username = "user1",
            encryptedPassword = null,
        )

        assertNull(profile.getDecryptedPassword())
    }

    @Test
    fun updateLastUsedUpdatesTimestamp() {
        val before = System.currentTimeMillis()
        val profile = ConnectionProfile(
            host = "host.test",
            port = 22,
            username = "tester",
            lastUsedTimestamp = 1000L,
        )
        val updated = profile.updateLastUsed()
        assertTrue(updated.lastUsedTimestamp >= before)
    }
}
