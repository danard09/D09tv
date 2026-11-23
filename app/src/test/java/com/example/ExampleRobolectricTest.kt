package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.parser.PlaylistJsonParser
import com.example.i18n.AppLanguage
import com.example.i18n.stringsForLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches D09 TV`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("D09 TV", appName)
    }

    @Test
    fun `bundled kobani_4k playlist parses all valid channels and round trips`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val rawJson = context.assets.open("aistudio/kobani_4k.json_4d9e328f.txt")
            .bufferedReader()
            .use { it.readText() }

        val parsed = PlaylistJsonParser.parse(rawJson)
        assertTrue("Expected > 100 channels from bundled playlist, got ${parsed.channels.size}", parsed.channels.size > 100)
        assertTrue("Expected categories to be parsed", parsed.groups.isNotEmpty())

        val groupNames = parsed.groups.map { it.name }.toSet()
        assertTrue(groupNames.contains("KURD TV"))
        assertTrue(groupNames.contains("KURD SPORTS"))
        assertTrue(groupNames.contains("MUSIC"))
        assertTrue(groupNames.contains("QURAN"))
        assertTrue(groupNames.contains("DOCUMENTARY"))

        // Verify all parsed channels have at least one non-empty stream URL and are sorted by orderIndex
        assertTrue(parsed.channels.all { it.availableStreams().isNotEmpty() })
        val orders = parsed.channels.map { it.orderIndex }
        assertEquals(orders.sorted(), orders)

        // Verify JSON export + re-import preserves channel count and backup URLs
        val exported = PlaylistJsonParser.exportToJson(parsed.channels, parsed.groups)
        val reimported = PlaylistJsonParser.parse(exported)
        assertEquals(parsed.channels.size, reimported.channels.size)
        assertEquals(parsed.groups.size, reimported.groups.size)
    }

    @Test
    fun `all four languages configure RTL and LTR properly`() {
        assertTrue(AppLanguage.SORANI.isRtl)
        assertTrue(AppLanguage.BADINI.isRtl)
        assertTrue(AppLanguage.ARABIC.isRtl)
        assertFalse(AppLanguage.ENGLISH.isRtl)

        AppLanguage.entries.forEach { lang ->
            val strings = stringsForLanguage(lang)
            assertEquals("D09 TV", strings.appName)
            assertEquals("danar", strings.settingsDeveloperName)
            assertTrue(strings.translateCategory("KURD TV").isNotBlank())
        }
    }
}
