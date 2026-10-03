package com.example.data.context

import android.content.Context
import com.example.data.model.CharacterProfile
import com.example.data.registry.CharacterRegistry
import com.example.data.chat.local.ChatDriverFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CharacterContext
 *
 * The app-wide active companion selection. Every screen that is scoped to one
 * companion (Home, Living, Diary, CheckPhone, Memories, navigation targets)
 * reads the selection here instead of hardcoding Mira.
 *
 * Contract:
 * - new installs start with Yan; existing choices and legacy Mira saves are preserved
 * - profiles are always resolved through CharacterRegistry; an unknown ID yields
 *   a profile derived from that ID and NEVER falls back to Mira
 * - selection is programmatic (tests and future companion-switching UI);
 *   opening a chat or profile does not silently change the active companion
 */
object CharacterContext {

    const val DEFAULT_CHARACTER_ID = CharacterSelectionPolicy.DEFAULT_CHARACTER_ID

    private val _selectedId = MutableStateFlow(DEFAULT_CHARACTER_ID)
    val selectedId: StateFlow<String> = _selectedId.asStateFlow()
    private var context: Context? = null

    fun init(appContext: Context) {
        context = appContext.applicationContext
        val app = checkNotNull(context)
        val prefs = app.getSharedPreferences("ailua_character_context", Context.MODE_PRIVATE)
        val savedId = prefs.getString("selected_id", null)
        // MainActivity calls this before MemoryGraph creates a fresh database.
        val hasLegacyData = app.getDatabasePath(ChatDriverFactory.DEFAULT_DATABASE_NAME).exists() ||
            app.getSharedPreferences("ailua_first_session", Context.MODE_PRIVATE).getBoolean("onboarding", false) ||
            app.getSharedPreferences("ailua_os_store", Context.MODE_PRIVATE).all.isNotEmpty()
        val resolved = CharacterSelectionPolicy.resolve(savedId, hasLegacyData)
        _selectedId.value = resolved
        // Persist the first decision immediately: an unfinished Welcome can create a DB
        // later in this launch, which must not change the default on the next launch.
        if (savedId.isNullOrBlank()) prefs.edit().putString("selected_id", resolved).apply()
    }

    fun select(characterId: String) {
        if (characterId.isBlank()) return
        _selectedId.value = characterId
        context?.getSharedPreferences("ailua_character_context", Context.MODE_PRIVATE)
            ?.edit()?.putString("selected_id", characterId)?.apply()
    }

    fun currentId(): String = _selectedId.value

    fun currentCharacter(): CharacterProfile = CharacterRegistry.getCharacter(_selectedId.value)
}
