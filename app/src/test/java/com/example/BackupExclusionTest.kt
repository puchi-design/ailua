package com.example

import com.example.data.ai.security.ApiSecretStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * BackupExclusionTest — spec §11 #6: the API secret store must never leave the device.
 *
 * Verifies the actual resource files on disk (not just constants):
 * - backup_rules.xml (Android 11 and lower / Auto Backup) excludes the prefs file
 * - data_extraction_rules.xml (Android 12+) excludes it from BOTH
 *   <cloud-backup> and <device-transfer>
 * - AndroidManifest.xml references both rule files
 */
class BackupExclusionTest {

    private val excludeTag =
        """<exclude domain="sharedpref" path="${ApiSecretStore.PREFS_FILE}.xml"/>"""

    private fun repoRoot(): File {
        var dir = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (true) {
            if (File(dir, "app/src/main/res/xml/backup_rules.xml").isFile) return dir
            dir = dir.parentFile
                ?: error("repository root not found from user.dir=${System.getProperty("user.dir")}")
        }
    }

    private fun read(relative: String): String =
        File(repoRoot(), relative).readText(Charsets.UTF_8)

    @Test
    fun legacyAutoBackupExcludesSecretPrefs() {
        val rules = read("app/src/main/res/xml/backup_rules.xml")

        assertTrue(rules.contains("<full-backup-content>"))
        assertTrue(rules.contains(excludeTag))
    }

    @Test
    fun cloudBackupExcludesSecretPrefs() {
        val rules = read("app/src/main/res/xml/data_extraction_rules.xml")

        val cloudStart = rules.indexOf("<cloud-backup")
        val cloudEnd = rules.indexOf("</cloud-backup>")
        val cloudExclude = rules.indexOf(excludeTag)

        assertTrue("missing <cloud-backup> section", cloudStart >= 0)
        assertTrue("missing </cloud-backup>", cloudEnd > cloudStart)
        assertTrue(
            "exclude not inside <cloud-backup>",
            cloudExclude > cloudStart && cloudExclude < cloudEnd,
        )
    }

    @Test
    fun deviceTransferExcludesSecretPrefs() {
        val rules = read("app/src/main/res/xml/data_extraction_rules.xml")

        val transferStart = rules.indexOf("<device-transfer>")
        val transferEnd = rules.indexOf("</device-transfer>")
        val transferExclude = rules.indexOf(excludeTag, transferStart.coerceAtLeast(0))

        assertTrue("missing <device-transfer> section", transferStart >= 0)
        assertTrue("missing </device-transfer>", transferEnd > transferStart)
        assertTrue("exclude not inside <device-transfer>", transferExclude > transferStart)
    }

    @Test
    fun manifestReferencesBothRuleFiles() {
        val manifest = read("app/src/main/AndroidManifest.xml")

        assertTrue(manifest.contains("""android:fullBackupContent="@xml/backup_rules""""))
        assertTrue(manifest.contains("""android:dataExtractionRules="@xml/data_extraction_rules""""))
    }

    @Test
    fun exclusionTargetMatchesSecretStoreFileName() {
        assertEquals("ailua_ai_secrets", ApiSecretStore.PREFS_FILE)
        val rules = read("app/src/main/res/xml/backup_rules.xml")
        assertTrue(rules.contains("""path="${ApiSecretStore.PREFS_FILE}.xml""""))
    }
}
