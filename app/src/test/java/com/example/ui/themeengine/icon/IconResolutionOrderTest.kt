package com.example.ui.themeengine.icon

import com.example.ui.themeengine.ThemeSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconResolutionOrderTest {
    @Test
    fun manualOverrideWinsBeforeExternalAndBundledForBothSourceTypes() {
        for (source in listOf("android:my.pack", "theme:my-theme")) {
            val calls = mutableListOf<String>()
            val selection = ThemeSelection("soft_home", iconSourceOverrideId = source,
                manualIconOverrides = mapOf("chat" to "my-chat"))
            val result = resolveIconWithFallback("chat", selection,
                loadManual = { actualSource, _, manual ->
                    assertEquals(source, actualSource)
                    assertEquals("my-chat", manual)
                    calls += "manual"
                    "manual bitmap"
                },
                loadExternal = { _, _ -> calls += "external"; "external bitmap" },
                loadBundled = { calls += it.drawableName; "bundled bitmap" })
            assertEquals("manual bitmap", result)
            assertEquals(listOf("manual"), calls)
        }
    }

    @Test
    fun damagedManualOverrideFallsBackToSelectedExternalPack() {
        val calls = mutableListOf<String>()
        val result = resolveIconWithFallback("chat", ThemeSelection("midnight_glass",
            iconSourceOverrideId = "theme:imported", manualIconOverrides = mapOf("chat" to "damaged")),
            loadManual = { _, _, _ -> calls += "manual"; null },
            loadExternal = { _, _ -> calls += "external"; "external bitmap" },
            loadBundled = { calls += it.drawableName; "bundled bitmap" })
        assertEquals("external bitmap", result)
        assertEquals(listOf("manual", "external"), calls)
    }

    @Test
    fun automaticExternalOverrideStaysAheadOfOfficialPackAfterThemeChange() {
        for (source in listOf("android:chosen.pack", "theme:chosen-theme")) {
            val selected = ThemeSelection("default", iconSourceOverrideId = source)
            for (preset in listOf("default", "soft_home", "midnight_glass")) {
                val candidate = selected.copy(themePresetId = preset)
                val result = resolveIconWithFallback("chat", candidate,
                    loadManual = { _, _, _ -> error("No manual choice") },
                    loadExternal = { actualSource, _ ->
                        assertEquals(source, actualSource)
                        "external bitmap"
                    },
                    loadBundled = { error("External source must remain ahead of the official pack") })
                assertEquals("external bitmap", result)
                assertEquals(source, candidate.iconSourceOverrideId)
            }
        }
    }

    @Test
    fun missingExternalAndSelectedPackContinueToDefaultThenVector() {
        for (defaultAvailable in listOf(true, false)) {
            val calls = mutableListOf<String>()
            val selection = ThemeSelection("soft_home", iconSourceOverrideId = "android:uninstalled.pack")
            val result = resolveIconWithFallback("chat", selection,
                loadManual = { _, _, _ -> error("No manual override was selected") },
                loadExternal = { _, _ -> calls += "external"; null },
                loadBundled = { resource ->
                    calls += resource.drawableName
                    "default bitmap".takeIf { defaultAvailable && resource.pack.iconStyleId == "default_icons" }
                })
            assertEquals(listOf("external", "ti_soft_chat", "ti_default_chat"), calls)
            if (defaultAvailable) assertEquals("default bitmap", result) else assertNull(result)
            assertEquals("android:uninstalled.pack", selection.iconSourceOverrideId)
        }
    }

    @Test
    fun currentOfficialPackWinsAndDoesNotLoadDefault() {
        val calls = mutableListOf<String>()
        val result = resolveIconWithFallback("gallery", ThemeSelection("midnight_glass"),
            loadManual = { _, _, _ -> error("No external source was selected") },
            loadExternal = { _, _ -> error("No external source was selected") },
            loadBundled = { calls += it.drawableName; "midnight bitmap" })
        assertEquals("midnight bitmap", result)
        assertEquals(listOf("ti_midnight_gallery"), calls)
    }

    @Test
    fun unknownAppKeepsIdentityWhileExternalOverrideStillWorks() {
        val noSource = ThemeSelection("default")
        assertNull(resolveIconWithFallback<String>("dream", noSource,
            loadManual = { _, _, _ -> error("No manual lookup expected") },
            loadExternal = { _, _ -> error("No external lookup expected") },
            loadBundled = { error("Unsupported apps must keep their vector identity") }))
        val external = noSource.copy(iconSourceOverrideId = "theme:custom")
        assertEquals("custom dream", resolveIconWithFallback("dream", external,
            loadManual = { _, _, _ -> null },
            loadExternal = { _, key -> "custom dream".takeIf { key == "dream" } },
            loadBundled = { error("Unsupported app must not use an unrelated official asset") }))
    }

    @Test
    fun externalManualChoiceSurvivesThemeChangeAndCanonicalAliases() {
        val source = ThemeSelection("default", iconSourceOverrideId = "android:chosen.pack",
            manualIconOverrides = mapOf("chat" to "chosen_message"))
        for (preset in listOf("default", "soft_home", "midnight_glass", "milk")) {
            val candidate = source.copy(themePresetId = preset)
            assertEquals("chosen_message", resolveIconWithFallback("messages", candidate,
                loadManual = { actualSource, _, manual ->
                    assertEquals("android:chosen.pack", actualSource)
                    manual
                },
                loadExternal = { _, _ -> error("Manual choice wins") },
                loadBundled = { error("Theme changes must keep explicit icon choices") }))
            assertEquals(source.manualIconOverrides, candidate.manualIconOverrides)
        }
    }

    @Test
    fun candidatePreviewUsesItsSelectionIndependentlyOfActiveSelection() {
        val active = ThemeSelection("midnight_glass", iconSourceOverrideId = "android:active.pack")
        val candidate = ThemeSelection("soft_home", iconStyleOverrideId = "y2k_icons")
        val preview = resolveIconWithFallback("chat", candidate,
            loadManual = { _, _, _ -> error("Candidate has no external source") },
            loadExternal = { _, _ -> error("Active external source must not affect candidate") },
            loadBundled = { it.drawableName })
        assertEquals("ti_y2k_chat", preview)
        assertEquals("android:active.pack", active.iconSourceOverrideId)
    }
}
