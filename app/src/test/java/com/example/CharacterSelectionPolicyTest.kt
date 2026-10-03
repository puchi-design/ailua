package com.example

import com.example.data.context.CharacterSelectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterSelectionPolicyTest {
    @Test fun firstInstallUsesYanButLegacyWithoutExplicitSelectionKeepsMira() {
        assertEquals("yan", CharacterSelectionPolicy.resolve(null, hasLegacyData = false))
        assertEquals("mira", CharacterSelectionPolicy.resolve(null, hasLegacyData = true))
        assertEquals("mira", CharacterSelectionPolicy.resolve(" ", hasLegacyData = true))
    }

    @Test fun savedFemaleCustomAndUnknownIdentitiesAreNeverReplaced() {
        listOf("mira", "yuna", "custom_rose", "not_in_registry").forEach { id ->
            assertEquals(id, CharacterSelectionPolicy.resolve(id, hasLegacyData = true))
        }
        // The persisted first-install choice stays Yan after this launch creates a DB.
        assertEquals("yan", CharacterSelectionPolicy.resolve("yan", hasLegacyData = true))
    }
}
