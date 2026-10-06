package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.context.CharacterContext
import com.example.data.firstsession.FirstSessionStore
import com.example.ui.systemui.VirtualSystemUiSession
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Production Home flow, using the saved user install and restoring its original selection. */
@RunWith(AndroidJUnit4::class)
class HomeCharacterSwitcherDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun homeSwitchUpdatesAndPersistsCurrentCharacter() {
        assumeTrue(FirstSessionStore.state.value.onboardingComplete)
        compose.runOnIdle { VirtualSystemUiSession.controller.unlock() }
        compose.waitUntil(10_000) {
            compose.onAllNodesWithTag("home_character_switcher").fetchSemanticsNodes().isNotEmpty()
        }
        val original = CharacterContext.currentId()
        val target = if (original == "hewenchuan") "zhoujianye" else "hewenchuan"
        try {
            compose.onNodeWithTag("home_character_switcher").performClick()
            compose.onNodeWithTag("character_switcher_sheet").assertIsDisplayed()
            compose.onNodeWithTag("character_switcher_option_$target").performClick()
            compose.waitUntil(5_000) { CharacterContext.currentId() == target }
            compose.onNodeWithTag("character_switcher_sheet").assertDoesNotExist()

            CharacterContext.init(InstrumentationRegistry.getInstrumentation().targetContext)
            assertEquals(target, CharacterContext.currentId())
        } finally {
            CharacterContext.select(original)
        }
    }
}
