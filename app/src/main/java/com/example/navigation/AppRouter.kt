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
        // AiluaApp "companion_call" declares route = call_history; the app library
        // intercepts "call"/"companion_call" itself to open an active call, so this
        // mapping only applies to Home grid / widget dispatch
        "call" to AiluaDestinations.CALL_HISTORY,
        "companion_call" to AiluaDestinations.CALL_HISTORY,
        "gallery" to AiluaDestinations.GALLERY,
        "reality" to AiluaDestinations.REALITY
    )

    fun destinationOrNull(appId: String): String? = destinationByAppId[appId]

    fun resolve(appId: String): String =
        destinationByAppId[appId] ?: AiluaDestinations.APPS
}
