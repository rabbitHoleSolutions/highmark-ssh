package com.questterm.ui.screens

import android.content.Context
import com.questterm.data.AuthMethod
import com.questterm.data.ConnectionHistoryStore
import com.questterm.data.ConnectionProfile
import com.questterm.session.SessionManager
import com.questterm.ssh.CachedConnection
import com.questterm.ssh.ConnectionCache
import com.questterm.ssh.CredentialEncryption
import com.questterm.ssh.SshConnectionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickConnectViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var sessionManager: SessionManager
    private lateinit var sshConnectionManager: SshConnectionManager
    private lateinit var connectionCache: ConnectionCache
    private lateinit var connectionHistory: ConnectionHistoryStore
    private lateinit var appContext: Context

    private lateinit var viewModel: QuickConnectViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        sessionManager = mockk(relaxed = true)
        sshConnectionManager = mockk(relaxed = true)
        connectionCache = mockk(relaxed = true) {
            every { load() } returns null
        }
        connectionHistory = mockk(relaxed = true) {
            every { getAllProfiles() } returns emptyList()
        }
        appContext = mockk(relaxed = true)

        viewModel = createViewModel()
    }

    private fun createViewModel(): QuickConnectViewModel {
        return QuickConnectViewModel(
            sessionManager = sessionManager,
            sshConnectionManager = sshConnectionManager,
            connectionCache = connectionCache,
            connectionHistory = connectionHistory,
            appContext = appContext,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun defaultStateHasStandardDefaults() {
        val state = viewModel.uiState.value
        assertEquals("", state.host)
        assertEquals("22", state.port)
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertEquals(AuthMethod.PASSWORD, state.authMethod)
        assertFalse(state.rememberPassword)
    }

    @Test
    fun initWithCachedConnectionPreFillsFields() {
        every { connectionCache.load() } returns CachedConnection(
            host = "cached.server.net",
            port = "2202",
            username = "cacheduser",
            password = "cachedpassword",
        )

        val vm = createViewModel()
        val state = vm.uiState.value
        assertEquals("cached.server.net", state.host)
        assertEquals("2202", state.port)
        assertEquals("cacheduser", state.username)
        assertEquals("cachedpassword", state.password)
    }

    @Test
    fun firstLaunchDemoPrefillKeepsSavedPasswordOnConnect() {
        mockkObject(CredentialEncryption)
        every { CredentialEncryption.decrypt("enc-bandit0") } returns "bandit0"
        val demo = ConnectionProfile(
            host = "bandit.labs.overthewire.org",
            port = 2220,
            username = "bandit0",
            authMethod = AuthMethod.PASSWORD,
            encryptedPassword = "enc-bandit0",
        )
        every { connectionHistory.getAllProfiles() } returns listOf(demo)

        val state = createViewModel().uiState.value

        assertEquals("bandit0", state.password)
        // Otherwise the first Connect saves the demo profile without its password.
        assertTrue(state.rememberPassword)
    }

    @Test
    fun updateHostWithPlainHostnameSetsHost() {
        viewModel.updateHost("myhost.example.com")
        val state = viewModel.uiState.value
        assertEquals("myhost.example.com", state.host)
        assertEquals("22", state.port)
        assertEquals("", state.username)
    }

    @Test
    fun updateHostWithUserAndHostParsesUsernameAndHost() {
        viewModel.updateHost("admin@myserver.net")
        val state = viewModel.uiState.value
        assertEquals("myserver.net", state.host)
        assertEquals("admin", state.username)
        assertEquals("22", state.port)
    }

    @Test
    fun updateHostWithHostAndPortParsesHostAndPort() {
        viewModel.updateHost("myserver.net:2222")
        val state = viewModel.uiState.value
        assertEquals("myserver.net", state.host)
        assertEquals("2222", state.port)
    }

    @Test
    fun updateHostWithFullConnectionStringParsesAllParts() {
        viewModel.updateHost("ubuntu@bastion.company.com:8022")
        val state = viewModel.uiState.value
        assertEquals("bastion.company.com", state.host)
        assertEquals("ubuntu", state.username)
        assertEquals("8022", state.port)
    }

    @Test
    fun updateUsernameAndPortAndPasswordUpdateStateDirectly() {
        viewModel.updateUsername("root")
        viewModel.updatePort("2200")
        viewModel.updatePassword("supersecret")

        val state = viewModel.uiState.value
        assertEquals("root", state.username)
        assertEquals("2200", state.port)
        assertEquals("supersecret", state.password)
    }

    @Test
    fun toggleRememberPasswordTogglesBoolean() {
        assertFalse(viewModel.uiState.value.rememberPassword)

        viewModel.toggleRememberPassword()
        assertTrue(viewModel.uiState.value.rememberPassword)

        viewModel.toggleRememberPassword()
        assertFalse(viewModel.uiState.value.rememberPassword)
    }

    @Test
    fun selectAuthMethodSwitchesBetweenPasswordAndKey() {
        assertEquals(AuthMethod.PASSWORD, viewModel.uiState.value.authMethod)

        viewModel.selectAuthMethod(AuthMethod.KEY)
        assertEquals(AuthMethod.KEY, viewModel.uiState.value.authMethod)

        viewModel.selectAuthMethod(AuthMethod.PASSWORD)
        assertEquals(AuthMethod.PASSWORD, viewModel.uiState.value.authMethod)
    }

    @Test
    fun selectProfilePopulatesFormFields() {
        val profile = ConnectionProfile(
            host = "prod.server.io",
            port = 2222,
            username = "deploy",
            encryptedPassword = null,
            authMethod = AuthMethod.PASSWORD,
        )

        viewModel.selectProfile(profile)

        val state = viewModel.uiState.value
        assertEquals("prod.server.io", state.host)
        assertEquals("2222", state.port)
        assertEquals("deploy", state.username)
        assertEquals(AuthMethod.PASSWORD, state.authMethod)
        assertFalse(state.rememberPassword)
    }

    @Test
    fun toggleFavoriteDelegatesToHistoryStoreAndReloads() {
        viewModel.toggleFavorite("profile-42")
        verify { connectionHistory.toggleFavorite("profile-42") }
        verify(atLeast = 2) { connectionHistory.getAllProfiles() }
    }

    @Test
    fun deleteProfileDelegatesToHistoryStoreAndReloads() {
        viewModel.deleteProfile("profile-42")
        verify { connectionHistory.deleteProfile("profile-42") }
        verify(atLeast = 2) { connectionHistory.getAllProfiles() }
    }

    @Test
    fun connectWithBlankHostOrUsernameShowsError() {
        viewModel.updateHost("")
        viewModel.updateUsername("")

        var onSuccessCalled = false
        viewModel.connect { onSuccessCalled = true }

        assertFalse(onSuccessCalled)
        assertEquals("Host and username are required", viewModel.uiState.value.error)
    }

    @Test
    fun acceptAndRejectHostKeyClearPrompt() {
        viewModel.acceptHostKey()
        assertNull(viewModel.uiState.value.hostKeyPrompt)

        viewModel.rejectHostKey()
        assertNull(viewModel.uiState.value.hostKeyPrompt)
    }
}
