package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.registry.CharacterRegistry
import com.example.ui.home.CharacterSwitcherSheet
import com.example.ui.home.VirtualHomeScreen
import com.example.ui.home.special.LifeBentoPage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The current character must be switchable from the Home hero itself. */
@RunWith(RobolectricTestRunner::class)
class HomeCharacterSwitcherTest {
    @get:Rule val compose = createComposeRule()

    @Test fun homeKeepsSwitchEntryWhenCharacterWidgetIsAbsent() {
        compose.setContent { VirtualHomeScreen(character = CharacterRegistry.getCharacter("mira")) }

        compose.onNodeWithTag("home_character_switcher").assertIsDisplayed()
        compose.onNodeWithTag("home_character_switcher").performClick()
        compose.onNodeWithTag("character_switcher_sheet").assertIsDisplayed()
    }

    @Test fun lifeHeroExposesCharacterSwitchAction() {
        var switchRequests = 0
        compose.setContent {
            LifeBentoPage(
                onNavigateToGroupChat = {},
                onNavigateToCheckPhone = {},
                onNavigateToDiary = {},
                onNavigateToRelations = {},
                onNavigateToLiving = {},
                onSwitchCharacter = { switchRequests++ },
                onAppClick = {},
            )
        }

        compose.onNodeWithTag("life_bento_switch_character").assertIsDisplayed()
        compose.onNodeWithTag("life_bento_switch_character").performClick()
        compose.runOnIdle { assertEquals(1, switchRequests) }
    }

    @Test fun sheetGroupsOfficialCharactersAndSelectsFromSavedLegacyChoice() {
        var chosenId: String? = null
        compose.setContent {
            CharacterSwitcherSheet(selectedId = "mira", onSelect = { chosenId = it }, onDismiss = {})
        }

        compose.onNodeWithText("心动对象").assertIsDisplayed()
        compose.onNodeWithText("我的朋友").assertExists()
        compose.onNodeWithTag("character_switcher_option_mira").assertIsSelected()
        compose.onNodeWithTag("character_switcher_option_hewenchuan").performClick()
        compose.runOnIdle { assertEquals("hewenchuan", chosenId) }
    }
}
