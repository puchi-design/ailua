package com.example

import com.example.data.mock.MockData
import com.example.navigation.AiluaDestinations
import com.example.navigation.AppRouter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * AppRouterTest
 *
 * P3B-4 app routing contract:
 * - one routing table for Home grid / app library / widget dispatch
 * - id aliases resolve consistently (chat, creator, lore_books, ...)
 * - every routed AiluaApp in the library resolves to its declared route
 * - unknown ids fall back to the app library, never to a wrong screen
 * - character-scoped routes are not resolved here (no hardcoded companion ids)
 */
class AppRouterTest {

    @Test
    fun coreAppIdsResolveToExpectedDestinations() {
        val expectations = mapOf(
            "messages" to AiluaDestinations.MESSAGES,
            "chat" to AiluaDestinations.MESSAGES,
            "group_chat" to AiluaDestinations.GROUP_CHAT,
            "contacts" to AiluaDestinations.CONTACTS,
            "relations" to AiluaDestinations.RELATIONS,
            "check_phone" to AiluaDestinations.CHECK_PHONE,
            "diary" to AiluaDestinations.DIARY,
            "moments" to AiluaDestinations.MOMENTS,
            "living" to AiluaDestinations.LIVING,
            "memories" to AiluaDestinations.MEMORIES,
            "apps" to AiluaDestinations.APPS,
            "mailbox" to AiluaDestinations.MAILBOX,
            "call_history" to AiluaDestinations.CALL_HISTORY,
            "gallery" to AiluaDestinations.GALLERY,
            "world_map" to AiluaDestinations.WORLD_MAP,
            "theater" to AiluaDestinations.THEATER
        )
        expectations.forEach { (appId, expected) ->
            assertEquals("app id '$appId'", expected, AppRouter.resolve(appId))
        }
    }

    @Test
    fun aliasesResolveConsistently() {
        assertEquals(AiluaDestinations.CHARACTER_CREATOR, AppRouter.resolve("creator"))
        assertEquals(AiluaDestinations.CHARACTER_CREATOR, AppRouter.resolve("character_creation"))
        assertEquals(AiluaDestinations.CHARACTER_CREATOR, AppRouter.resolve("character_creator"))
        assertEquals(AiluaDestinations.WORLD_BOOK, AppRouter.resolve("world_book"))
        assertEquals(AiluaDestinations.WORLD_BOOK, AppRouter.resolve("lore_books"))
        assertEquals(AiluaDestinations.MESSAGES, AppRouter.resolve("chat"))
    }

    @Test
    fun everyRoutedLibraryAppResolvesToItsDeclaredRoute() {
        val routed = MockData.appLibraryList.filter { it.route != null }
        check(routed.isNotEmpty()) { "library fixture must contain routed apps" }
        routed.forEach { app ->
            assertEquals("app '${app.id}'", app.route, AppRouter.resolve(app.id))
        }
    }

    @Test
    fun libraryRouteStringsResolveToThemselves() {
        // AiluaApp.route values are destination constants — resolving the route
        // string itself must be idempotent (used by data-driven navigation)
        MockData.appLibraryList.mapNotNull { it.route }.forEach { route ->
            assertEquals(route, AppRouter.resolve(route))
        }
    }

    @Test
    fun unknownAppIdFallsBackToAppLibrary() {
        assertEquals(AiluaDestinations.APPS, AppRouter.resolve("space_invaders"))
        assertEquals(AiluaDestinations.APPS, AppRouter.resolve(""))
        assertNull(AppRouter.destinationOrNull("space_invaders"))
    }

    @Test
    fun characterScopedRoutesAreNeverResolvedHere() {
        // chat/profile/call need a character id built by the caller — the router
        // must not return template routes or hardcoded companion destinations
        assertNull(AppRouter.destinationOrNull(AiluaDestinations.CHAT))
        assertNull(AppRouter.destinationOrNull(AiluaDestinations.PROFILE))
        assertNull(AppRouter.destinationOrNull(AiluaDestinations.CALL))
    }
}
