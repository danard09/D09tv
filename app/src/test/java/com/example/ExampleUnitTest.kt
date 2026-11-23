package com.example

import com.example.data.model.ChannelEntity
import com.example.i18n.AppLanguage
import com.example.i18n.stringsForLanguage
import com.example.security.AccessControlManager
import com.example.security.AuthVerificationResult
import com.example.security.UserRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun allFourLanguagesConfigureRtlAndTranslationsCorrectly() {
        assertTrue(AppLanguage.SORANI.isRtl)
        assertTrue(AppLanguage.BADINI.isRtl)
        assertTrue(AppLanguage.ARABIC.isRtl)
        assertFalse(AppLanguage.ENGLISH.isRtl)

        AppLanguage.entries.forEach { lang ->
            val strings = stringsForLanguage(lang)
            assertEquals("D09 TV", strings.appName)
            assertEquals("danar", strings.settingsDeveloperName)
            assertTrue(strings.translateCategory("KURD TV").isNotBlank())
            assertTrue(strings.translateCategory("KURD SPORTS").isNotBlank())
            assertTrue(strings.translateCategory("QURAN").isNotBlank())
            // Ensure UI strings never leak access codes
            assertFalse(strings.userLoginSubtitle.contains("0909"))
            assertFalse(strings.adminLoginSubtitle.contains("2009"))
            assertFalse(strings.adminSecurityNoticeBody.contains("2009"))
        }
    }

    @Test
    fun userLoginEnforcesCodeAndThrottling() {
        val ac = AccessControlManager()

        // Valid user code succeeds with USER role
        val ok = ac.verifyUserCode("0909", customCredential = null, nowMs = 1000L)
        assertEquals(AuthVerificationResult.Success(UserRole.USER), ok)

        // Admin code cannot be used on user login
        val wrongAdminOnUser = ac.verifyUserCode("2009", customCredential = null, nowMs = 2000L)
        assertTrue(wrongAdminOnUser is AuthVerificationResult.InvalidCode)

        // Five consecutive failures trigger cooldown
        ac.resetThrottle()
        var lastResult: AuthVerificationResult? = null
        for (attempt in 1..5) {
            lastResult = ac.verifyUserCode("1111", customCredential = null, nowMs = 3000L)
        }
        assertTrue(lastResult is AuthVerificationResult.RateLimited)

        // During cooldown, even valid code is blocked until cooldown expires
        val blocked = ac.verifyUserCode("0909", customCredential = null, nowMs = 5000L)
        assertTrue(blocked is AuthVerificationResult.RateLimited)

        // After cooldown expires, valid code succeeds
        val recovered = ac.verifyUserCode("0909", customCredential = null, nowMs = 20_000L)
        assertEquals(AuthVerificationResult.Success(UserRole.USER), recovered)
    }

    @Test
    fun adminLoginEnforcesRoleIsolationAndCustomCredentialUpdate() {
        val ac = AccessControlManager()

        // User code cannot access admin dashboard
        val userOnAdmin = ac.verifyAdminCode("0909", customCredential = null, nowMs = 1000L)
        assertTrue(userOnAdmin is AuthVerificationResult.InvalidCode)

        // Valid admin code succeeds with ADMIN role
        val ok = ac.verifyAdminCode("2009", customCredential = null, nowMs = 2000L)
        assertEquals(AuthVerificationResult.Success(UserRole.ADMIN), ok)

        // Updating custom admin credential invalidates old code and accepts new code
        val updated = ac.createCredential("8844")
        assertTrue(ac.verifyAdminCode("2009", updated, nowMs = 3000L) is AuthVerificationResult.InvalidCode)
        assertEquals(
            AuthVerificationResult.Success(UserRole.ADMIN),
            ac.verifyAdminCode("8844", updated, nowMs = 4000L),
        )
    }

    @Test
    fun channelAvailableStreamsPreservesBackupOrderAndIgnoresEmptyUrls() {
        val channel = ChannelEntity(
            id = "rudaw",
            name = "RUDAW",
            groupName = "KURD TV",
            url = "https://example.com/primary.m3u8",
            url2 = "",
            url3 = "https://example.com/backup3.m3u8",
            referer = "https://rudaw.net",
            userAgent = "CustomUA",
            url3Name = "Mirror 3",
        )
        val streams = channel.availableStreams()
        assertEquals(2, streams.size)
        assertEquals(1, streams[0].index)
        assertEquals("https://example.com/primary.m3u8", streams[0].rawUrl)
        assertEquals(3, streams[1].index)
        assertEquals("Mirror 3", streams[1].label)
        assertEquals("https://rudaw.net", streams[1].referer)
        assertEquals("CustomUA", streams[1].userAgent)
    }
}
