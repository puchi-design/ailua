package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.model.CharacterCard
import com.example.data.model.TheaterBookmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * AiluaLocalStore
 *
 * Lightweight durable local persistence inspired by Now in Android's
 * NiaPreferencesDataSource, using SharedPreferences and kotlinx.serialization.
 * Persists:
 * - Custom Character Cards
 * - Theater Bookmark & Progress
 * - Virtual Clock Time & Date
 * - Delivered / Opened Letter States
 * - Virtual Companion Call History
 * - User Imported Gallery Assets
 * - Chat Message Bookmarks
 */
object AiluaLocalStore {

    private const val PREFS_NAME = "ailua_os_store"
    private const val KEY_CUSTOM_CARDS = "custom_character_cards"
    private const val KEY_THEATER_BOOKMARK = "theater_bookmark"
    private const val KEY_VIRTUAL_MINUTES = "virtual_minutes_of_day"
    private const val KEY_VIRTUAL_DATE = "virtual_date_label"
    private const val KEY_DELIVERED_LETTERS = "delivered_letter_ids"
    private const val KEY_OPENED_LETTERS = "opened_letter_ids"
    private const val KEY_CALL_HISTORY = "call_history_json"
    private const val KEY_GALLERY_ASSETS = "gallery_imported_assets"
    private const val KEY_BOOKMARKED_MSGS = "bookmarked_message_ids"

    private var sharedPrefs: SharedPreferences? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    // In-memory reactive state
    private val _customCards = MutableStateFlow<List<CharacterCard>>(emptyList())
    val customCards: StateFlow<List<CharacterCard>> = _customCards.asStateFlow()

    private val _theaterBookmark = MutableStateFlow<TheaterBookmark?>(null)
    val theaterBookmark: StateFlow<TheaterBookmark?> = _theaterBookmark.asStateFlow()

    private val _deliveredLetterIds = MutableStateFlow<Set<String>>(emptySet())
    val deliveredLetterIds: StateFlow<Set<String>> = _deliveredLetterIds.asStateFlow()

    private val _openedLetterIds = MutableStateFlow<Set<String>>(emptySet())
    val openedLetterIds: StateFlow<Set<String>> = _openedLetterIds.asStateFlow()

    private val _bookmarkedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedMessageIds: StateFlow<Set<String>> = _bookmarkedMessageIds.asStateFlow()

    fun init(context: Context) {
        if (sharedPrefs != null) return
        sharedPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromDisk()
    }

    private fun loadFromDisk() {
        val prefs = sharedPrefs ?: return

        // 1. Custom cards
        val cardsJson = prefs.getString(KEY_CUSTOM_CARDS, null)
        if (!cardsJson.isNullOrBlank()) {
            try {
                val list = json.decodeFromString(ListSerializer(CharacterCard.serializer()), cardsJson)
                _customCards.value = list
            } catch (_: Exception) {}
        }

        // 2. Theater bookmark
        val bookmarkJson = prefs.getString(KEY_THEATER_BOOKMARK, null)
        if (!bookmarkJson.isNullOrBlank()) {
            try {
                _theaterBookmark.value = json.decodeFromString(TheaterBookmark.serializer(), bookmarkJson)
            } catch (_: Exception) {}
        }

        // 3. Letters
        _deliveredLetterIds.value = prefs.getStringSet(KEY_DELIVERED_LETTERS, emptySet()) ?: emptySet()
        _openedLetterIds.value = prefs.getStringSet(KEY_OPENED_LETTERS, emptySet()) ?: emptySet()

        // 4. Message bookmarks
        _bookmarkedMessageIds.value = prefs.getStringSet(KEY_BOOKMARKED_MSGS, emptySet()) ?: emptySet()
    }

    // === Custom Cards ===

    fun saveCustomCard(card: CharacterCard) {
        val current = _customCards.value.toMutableList()
        val index = current.indexOfFirst { it.data.id == card.data.id }
        if (index >= 0) {
            current[index] = card
        } else {
            current.add(card)
        }
        _customCards.value = current
        sharedPrefs?.edit()?.putString(
            KEY_CUSTOM_CARDS,
            json.encodeToString(ListSerializer(CharacterCard.serializer()), current)
        )?.apply()
    }

    fun deleteCustomCard(id: String) {
        val updated = _customCards.value.filterNot { it.data.id == id }
        _customCards.value = updated
        sharedPrefs?.edit()?.putString(
            KEY_CUSTOM_CARDS,
            json.encodeToString(ListSerializer(CharacterCard.serializer()), updated)
        )?.apply()
    }

    // === Theater Bookmark ===

    fun saveTheaterBookmark(bookmark: TheaterBookmark) {
        _theaterBookmark.value = bookmark
        sharedPrefs?.edit()?.putString(
            KEY_THEATER_BOOKMARK,
            json.encodeToString(TheaterBookmark.serializer(), bookmark)
        )?.apply()
    }

    fun clearTheaterBookmark() {
        _theaterBookmark.value = null
        sharedPrefs?.edit()?.remove(KEY_THEATER_BOOKMARK)?.apply()
    }

    // === Letters ===

    fun markLetterDelivered(letterId: String) {
        val updated = _deliveredLetterIds.value + letterId
        _deliveredLetterIds.value = updated
        sharedPrefs?.edit()?.putStringSet(KEY_DELIVERED_LETTERS, updated)?.apply()
    }

    fun markLetterOpened(letterId: String) {
        val updatedDelivered = _deliveredLetterIds.value + letterId
        val updatedOpened = _openedLetterIds.value + letterId
        _deliveredLetterIds.value = updatedDelivered
        _openedLetterIds.value = updatedOpened
        sharedPrefs?.edit()
            ?.putStringSet(KEY_DELIVERED_LETTERS, updatedDelivered)
            ?.putStringSet(KEY_OPENED_LETTERS, updatedOpened)
            ?.apply()
    }

    // === Virtual Clock ===

    fun getVirtualMinutes(defaultVal: Int = 21 * 60 + 30): Int {
        return sharedPrefs?.getInt(KEY_VIRTUAL_MINUTES, defaultVal) ?: defaultVal
    }

    fun saveVirtualMinutes(minutes: Int) {
        sharedPrefs?.edit()?.putInt(KEY_VIRTUAL_MINUTES, minutes)?.apply()
    }

    fun getVirtualDate(defaultVal: String = "9月25日"): String {
        return sharedPrefs?.getString(KEY_VIRTUAL_DATE, defaultVal) ?: defaultVal
    }

    fun saveVirtualDate(date: String) {
        sharedPrefs?.edit()?.putString(KEY_VIRTUAL_DATE, date)?.apply()
    }

    // === Message Bookmarks ===

    fun toggleMessageBookmark(messageId: String): Boolean {
        val set = _bookmarkedMessageIds.value.toMutableSet()
        val isNowBookmarked = if (set.contains(messageId)) {
            set.remove(messageId)
            false
        } else {
            set.add(messageId)
            true
        }
        _bookmarkedMessageIds.value = set
        sharedPrefs?.edit()?.putStringSet(KEY_BOOKMARKED_MSGS, set)?.apply()
        return isNowBookmarked
    }
}
