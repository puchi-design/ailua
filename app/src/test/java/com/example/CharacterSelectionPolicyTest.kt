package com.example

import com.example.data.context.CharacterSelectionPolicy
import org.junit.Assert.assertEquals
import org.junit.Test

class CharacterSelectionPolicyTest {
    @Test fun firstInstallUsesHeWenchuanButLegacyWithoutExplicitSelectionKeepsMira() {
        assertEquals("hewenchuan", CharacterSelectionPolicy.resolve(null, hasLegacyData = false))
        assertEquals("mira", CharacterSelectionPolicy.resolve(null, hasLegacyData = true))
        assertEquals("mira", CharacterSelectionPolicy.resolve(" ", hasLegacyData = true))
    }

    @Test fun savedFemaleCustomAndUnknownIdentitiesAreNeverReplaced() {
        listOf("mira", "yuna", "noa", "yan", "yeo", "custom_rose", "not_in_registry").forEach { id ->
            assertEquals(id, CharacterSelectionPolicy.resolve(id, hasLegacyData = true))
        }
        // A previously saved archive choice keeps its original ID after a DB exists.
        assertEquals("yan", CharacterSelectionPolicy.resolve("yan", hasLegacyData = true))
    }
}
