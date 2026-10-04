package com.example.navigation

/**
 * AppRouter
 *
 * Single source of truth for app-id → navigation destination resolution.
 * Home app grid, app library, and widget dispatch all reach destinations through
 * this table instead of local `when(appId)` chains, so aliases (chat/messages,
 * creator/character_creation, world_book/lore_books) always resolve consistently.
 *
 * Contract:
 * - returns only parameterless global destinations; character-scoped destinations
 *   (chat/profile/call) are built by callers with the active character id from
 *   CharacterContext — this router never hardcodes a companion id
 * - unknown or unrouted app ids resolve to the app library (APPS), never to a
 *   wrong screen
 * - every AiluaApp.route value in the library must resolve to itself (tested)
 */
object AppRouter {

    private val destinationByAppId = mapOf(
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
        "creator" to AiluaDestinations.CHARACTER_CREATOR,
        "character_creation" to AiluaDestinations.CHARACTER_CREATOR,
        "character_creator" to AiluaDestinations.CHARACTER_CREATOR,
        "world_book" to AiluaDestinations.WORLD_BOOK,
        "lore_books" to AiluaDestinations.WORLD_BOOK,
        "world_map" to AiluaDestinations.WORLD_MAP,
        "theater" to AiluaDestinations.THEATER,
        "mailbox" to AiluaDestinations.MAILBOX,
        "call_history" to AiluaDestinations.CALL_HISTORY,
        // The library declares call_history for this app. Launching the library
        // entry itself opens a companion call through launchRouteOrNull below.
        "call" to AiluaDestinations.CALL_HISTORY,
        "companion_call" to AiluaDestinations.CALL_HISTORY,
        "gallery" to AiluaDestinations.GALLERY,
        "notes" to AiluaDestinations.NOTES,
        "reality" to AiluaDestinations.REALITY,
        "reality_bridge" to AiluaDestinations.REALITY,
        "settings" to AiluaDestinations.SETTINGS,
    )

    fun destinationOrNull(appId: String): String? = destinationByAppId[appId]

    /** Launch behavior shared by drawer and folder entries. */
    fun launchRouteOrNull(appId: String, activeCharacterId: String): String? =
        if (appId == "companion_call") AiluaDestinations.callRoute(activeCharacterId)
        else destinationOrNull(appId)

    fun resolve(appId: String): String =
        destinationByAppId[appId] ?: AiluaDestinations.APPS
}
