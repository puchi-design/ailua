package com.example.ui.themeengine.external.iconpack

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidIconPackParserTest {
    @Test
    fun parsesStaticCalendarAndClockEntries() {
        val xml = """
            <resources>
              <item component="ComponentInfo{com.example.mail/.MainActivity}" drawable="mail_icon"/>
              <item component="ComponentInfo{com.example.photos/com.example.photos.Viewer}" drawable="photo_icon"/>
              <calendar component="ComponentInfo{com.example.calendar/.Start}" prefix="cal_"/>
              <dynamic-clock drawable="clock_icon" hourLayerIndex="0"/>
              <item component="broken" drawable="bad"/>
            </resources>
        """.trimIndent()
        val parsed = AndroidIconPackParser.parseXml(ByteArrayInputStream(xml.toByteArray()))
        assertEquals(
            AndroidIconMapping("com.example.mail", "com.example.mail.MainActivity", "mail_icon"),
            parsed.mappings.first()
        )
        assertEquals(2, parsed.mappings.size)
        assertEquals("cal_", parsed.calendars.single().drawableName)
        assertEquals(setOf("clock_icon"), parsed.dynamicClockDrawables)
    }

    @Test
    fun acceptsPackageOnlyMappingAndRejectsMalformedComponent() {
        assertEquals(
            AndroidIconMapping("com.example.app", null, "icon"),
            AndroidIconPackParser.parseComponent("com.example.app", "icon")
        )
        assertNull(AndroidIconPackParser.parseComponent("not-a-package/.Main", "icon"))
    }
}
