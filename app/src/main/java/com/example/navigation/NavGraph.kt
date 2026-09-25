package com.example.navigation

object AiluaDestinations {
    const val HOME = "home"
    const val MESSAGES = "messages"
    const val CHAT = "chat/{characterId}"
    const val GROUP_CHAT = "group_chat"
    const val CONTACTS = "contacts"
    const val RELATIONS = "relations"
    const val CHECK_PHONE = "check_phone"
    const val DIARY = "diary"
    const val MOMENTS = "moments"
    const val LIVING = "living"
    const val APPS = "apps"
    const val PROFILE = "profile/{characterId}"
    const val MEMORIES = "memories"

    fun chatRoute(characterId: String = "mira"): String = "chat/$characterId"
    fun profileRoute(characterId: String = "mira"): String = "profile/$characterId"
}
