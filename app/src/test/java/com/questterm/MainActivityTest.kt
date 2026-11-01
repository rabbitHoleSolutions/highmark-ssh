package com.questterm

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MainActivityTest {

    @Before
    fun setUp() {
        mockkConstructor(Intent::class)
        mockkStatic(Uri::class)
        val dummyUri = mockk<Uri>(relaxed = true)
        every { Uri.parse(any()) } returns dummyUri
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `companion object constants match expected intent extra keys`() {
        assertEquals("com.questterm.extra.SESSION_ID", MainActivity.EXTRA_SESSION_ID)
        assertEquals("com.questterm.extra.AUTOCONNECT_PROFILE_ID", MainActivity.EXTRA_AUTOCONNECT_PROFILE_ID)
    }

    @Test
    fun `createMultiWindowIntent sets required multi-window document flags and both extras`() {
        val context = mockk<Context>()
        val uriSlot = slot<String>()
        every { Uri.parse(capture(uriSlot)) } returns mockk(relaxed = true)
        every { anyConstructed<Intent>().addFlags(any()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<String>()) } answers { self as Intent }
        every { anyConstructed<Intent>().data = any() } answers { }

        val intent = MainActivity.createMultiWindowIntent(
            context = context,
            sessionId = "session-123",
            profileId = "profile-456",
        )

        assertNotNull(intent)
        verify {
            anyConstructed<Intent>().addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            anyConstructed<Intent>().putExtra(MainActivity.EXTRA_SESSION_ID, "session-123")
            anyConstructed<Intent>().putExtra(MainActivity.EXTRA_AUTOCONNECT_PROFILE_ID, "profile-456")
        }
        assertEquals("questterm://window/session/session-123", uriSlot.captured)
    }

    @Test
    fun `createMultiWindowIntent with only sessionId sets session extra and session URI`() {
        val context = mockk<Context>()
        val uriSlot = slot<String>()
        every { Uri.parse(capture(uriSlot)) } returns mockk(relaxed = true)
        every { anyConstructed<Intent>().addFlags(any()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<String>()) } answers { self as Intent }
        every { anyConstructed<Intent>().data = any() } answers { }

        val intent = MainActivity.createMultiWindowIntent(
            context = context,
            sessionId = "session-abc",
            profileId = null,
        )

        assertNotNull(intent)
        verify {
            anyConstructed<Intent>().addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            anyConstructed<Intent>().putExtra(MainActivity.EXTRA_SESSION_ID, "session-abc")
        }
        verify(exactly = 0) {
            anyConstructed<Intent>().putExtra(MainActivity.EXTRA_AUTOCONNECT_PROFILE_ID, any<String>())
        }
        assertEquals("questterm://window/session/session-abc", uriSlot.captured)
    }

    @Test
    fun `createMultiWindowIntent with only profileId sets profile extra and profile URI`() {
        val context = mockk<Context>()
        val uriSlot = slot<String>()
        every { Uri.parse(capture(uriSlot)) } returns mockk(relaxed = true)
        every { anyConstructed<Intent>().addFlags(any()) } answers { self as Intent }
        every { anyConstructed<Intent>().putExtra(any<String>(), any<String>()) } answers { self as Intent }
        every { anyConstructed<Intent>().data = any() } answers { }

        val intent = MainActivity.createMultiWindowIntent(
            context = context,
            sessionId = null,
            profileId = "profile-xyz",
        )

        assertNotNull(intent)
        verify {
            anyConstructed<Intent>().addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            anyConstructed<Intent>().putExtra(MainActivity.EXTRA_AUTOCONNECT_PROFILE_ID, "profile-xyz")
        }
        verify(exactly = 0) {
            anyConstructed<Intent>().putExtra(MainActivity.EXTRA_SESSION_ID, any<String>())
        }
        assertEquals("questterm://window/profile/profile-xyz", uriSlot.captured)
    }

    @Test
    fun `createMultiWindowIntent without optional parameters generates unique new URI and omits extras`() {
        val context = mockk<Context>()
        val uriSlot = slot<String>()
        every { Uri.parse(capture(uriSlot)) } returns mockk(relaxed = true)
        every { anyConstructed<Intent>().addFlags(any()) } answers { self as Intent }
        every { anyConstructed<Intent>().data = any() } answers { }

        val intent = MainActivity.createMultiWindowIntent(context = context)

        assertNotNull(intent)
        verify {
            anyConstructed<Intent>().addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        verify(exactly = 0) {
            anyConstructed<Intent>().putExtra(any<String>(), any<String>())
        }
        assertTrue(uriSlot.captured.startsWith("questterm://window/new/"))
    }
}
