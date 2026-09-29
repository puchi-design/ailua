package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.codec.CharacterCardJsonCodec
import com.example.data.model.CallSession
import com.example.data.model.CharacterCard
import com.example.data.model.LifeEvent
import com.example.data.model.WorldPlan
import com.example.data.relationship.model.RelationshipState
import com.example.data.model.ProactiveSettings
import com.example.data.model.ProactiveState
import com.example.data.model.TheaterBookmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * AiluaLocalStore
 *
 * Durable lightweight persistence inspired by Now in Android's
 * PreferencesDataSource patterns, using SharedPreferences and kotlinx.serialization.
 *
 * Persists:
 * - Fired World Action IDs (KEY_FIRED_WORLD_ACTION_IDS)
 * - Generated World Life Events (KEY_WORLD_EVENTS)
 * - Call Session History (KEY_CALL_HISTORY)
 * - Virtual Clock Time & Date (KEY_VIRTUAL_MINUTES, KEY_VIRTUAL_DATE)
 * - Delivered / Opened Letter States (KEY_DELIVERED_LETTERS, KEY_OPENED_LETTERS)
 * - Custom Character Cards (KEY_CUSTOM_CARDS)
 * - Theater Bookmark & Progress (KEY_THEATER_BOOKMARK)
 * - Chat Message Bookmarks (KEY_BOOKMARKED_MSGS)
 */
object AiluaLocalStore {
    private const val PREFS_NAME = "ailua_os_store"

    const val KEY_CUSTOM_CARDS = "custom_character_cards"
    const val KEY_THEATER_BOOKMARK = "theater_bookmark"
    const val KEY_VIRTUAL_MINUTES = "virtual_minutes_of_day"
    const val KEY_VIRTUAL_DATE = "virtual_date_label"
    const val KEY_DELIVERED_LETTERS = "delivered_letter_ids"
    const val KEY_OPENED_LETTERS = "opened_letter_ids"
    const val KEY_CALL_HISTORY = "call_history_json"
    const val KEY_FIRED_WORLD_ACTION_IDS = "fired_world_action_ids"
    const val KEY_WORLD_EVENTS = "world_events_json"
    const val KEY_WORLD_PLAN = "world_plan_json"
    const val KEY_RELATIONSHIPS = "relationships_json"
    const val KEY_BOOKMARKED_MSGS = "bookmarked_message_ids"
    const val KEY_HOME_APP_ORDER = "home_app_order"
    const val KEY_PROACTIVE_ENABLED = "proactive_message_enabled"
    const val KEY_PROACTIVE_INTERVAL_HOURS = "proactive_interval_hours"
    const val KEY_PROACTIVE_QUIET_START = "proactive_quiet_start_minute"
    const val KEY_PROACTIVE_QUIET_END = "proactive_quiet_end_minute"
    const val KEY_PROACTIVE_DAILY_LIMIT = "proactive_daily_limit"
    const val KEY_PROACTIVE_LAST_SUCCESS = "proactive_last_success_at"
    const val KEY_PROACTIVE_SENT_DATE = "proactive_sent_date"
    const val KEY_PROACTIVE_SENT_COUNT = "proactive_sent_count"

    private var sharedPrefs: SharedPreferences? = null

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = false
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

    private val _firedWorldActionIds = MutableStateFlow<Set<String>>(emptySet())
    val firedWorldActionIds: StateFlow<Set<String>> = _firedWorldActionIds.asStateFlow()

    private val _savedWorldEvents = MutableStateFlow<List<LifeEvent>>(emptyList())
    val savedWorldEvents: StateFlow<List<LifeEvent>> = _savedWorldEvents.asStateFlow()
    private val _savedWorldPlan = MutableStateFlow<WorldPlan?>(null)
    val savedWorldPlan: StateFlow<WorldPlan?> = _savedWorldPlan.asStateFlow()
    private val _savedRelationships = MutableStateFlow<List<RelationshipState>>(emptyList())
    val savedRelationships: StateFlow<List<RelationshipState>> = _savedRelationships.asStateFlow()

    private val _savedCallHistory = MutableStateFlow<List<CallSession>>(emptyList())
    val savedCallHistory: StateFlow<List<CallSession>> = _savedCallHistory.asStateFlow()

    private val _bookmarkedMessageIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedMessageIds: StateFlow<Set<String>> = _bookmarkedMessageIds.asStateFlow()

    private val _homeAppOrder = MutableStateFlow<List<String>>(emptyList())
    val homeAppOrder: StateFlow<List<String>> = _homeAppOrder.asStateFlow()

    private val _proactiveSettings = MutableStateFlow(ProactiveSettings())
    val proactiveSettings: StateFlow<ProactiveSettings> = _proactiveSettings.asStateFlow()

    fun init(context: Context) {
        if (sharedPrefs != null) return
        sharedPrefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromDisk()
    }

    fun loadFromDisk() {
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

        // 4. Fired scheduled world action IDs
        _firedWorldActionIds.value = prefs.getStringSet(KEY_FIRED_WORLD_ACTION_IDS, emptySet()) ?: emptySet()

        // 5. World Life Events
        val eventsJson = prefs.getString(KEY_WORLD_EVENTS, null)
        if (!eventsJson.isNullOrBlank()) {
            try {
                val list = json.decodeFromString(ListSerializer(LifeEvent.serializer()), eventsJson)
                _savedWorldEvents.value = list
            } catch (_: Exception) {}
        }
        prefs.getString(KEY_WORLD_PLAN, null)?.let { value ->
            _savedWorldPlan.value = runCatching { json.decodeFromString(WorldPlan.serializer(), value) }.getOrNull()
        }
        prefs.getString(KEY_RELATIONSHIPS, null)?.let { value ->
            _savedRelationships.value = runCatching { json.decodeFromString(ListSerializer(RelationshipState.serializer()), value) }.getOrDefault(emptyList())
        }

        // 6. Call History
        val callJson = prefs.getString(KEY_CALL_HISTORY, null)
        if (!callJson.isNullOrBlank()) {
            try {
                val list = json.decodeFromString(ListSerializer(CallSession.serializer()), callJson)
                _savedCallHistory.value = list
            } catch (_: Exception) {}
        }

        // 7. Message bookmarks
        _bookmarkedMessageIds.value = prefs.getStringSet(KEY_BOOKMARKED_MSGS, emptySet()) ?: emptySet()

        // 8. Home app order (comma separated stable ids)
        _homeAppOrder.value = prefs.getString(KEY_HOME_APP_ORDER, null)
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        // 9. Proactive message settings (P3D-2)
        _proactiveSettings.value = ProactiveSettings(
            enabled = prefs.getBoolean(KEY_PROACTIVE_ENABLED, false),
            intervalHours = prefs.getInt(KEY_PROACTIVE_INTERVAL_HOURS, 6),
            quietStartMinute = prefs.getInt(KEY_PROACTIVE_QUIET_START, 23 * 60),
            quietEndMinute = prefs.getInt(KEY_PROACTIVE_QUIET_END, 8 * 60),
            dailyLimit = prefs.getInt(KEY_PROACTIVE_DAILY_LIMIT, 3),
        )
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

    // === Fired Scheduled Action IDs ===
    fun getFiredWorldActionIds(): Set<String> {
        return _firedWorldActionIds.value
    }

    fun markWorldActionFired(actionId: String) {
        val updated = _firedWorldActionIds.value + actionId
        _firedWorldActionIds.value = updated
        sharedPrefs?.edit()?.putStringSet(KEY_FIRED_WORLD_ACTION_IDS, updated)?.apply()
    }

    // === World Life Events ===
    fun saveWorldEvents(events: List<LifeEvent>) {
        _savedWorldEvents.value = events
        try {
            sharedPrefs?.edit()?.putString(
                KEY_WORLD_EVENTS,
                json.encodeToString(ListSerializer(LifeEvent.serializer()), events)
            )?.apply()
        } catch (_: Exception) {}
    }

    fun appendWorldEvent(event: LifeEvent) {
        val current = _savedWorldEvents.value
        if (current.none { it.id == event.id }) {
            val updated = listOf(event) + current
            saveWorldEvents(updated)
        }
    }

    fun saveWorldPlan(plan: WorldPlan): Boolean {
        val encoded = json.encodeToString(WorldPlan.serializer(), plan)
        val prefs = sharedPrefs
        if (prefs != null && !prefs.edit().putString(KEY_WORLD_PLAN, encoded).commit()) return false
        _savedWorldPlan.value = plan
        return true
    }

    fun saveRelationships(states: List<RelationshipState>) {
        val encoded = json.encodeToString(ListSerializer(RelationshipState.serializer()), states)
        sharedPrefs?.edit()?.putString(KEY_RELATIONSHIPS, encoded)?.apply()
        _savedRelationships.value = states
    }

    // === Call History ===
    fun saveCallHistory(history: List<CallSession>) {
        _savedCallHistory.value = history
        try {
            sharedPrefs?.edit()?.putString(
                KEY_CALL_HISTORY,
                json.encodeToString(ListSerializer(CallSession.serializer()), history)
            )?.apply()
        } catch (_: Exception) {}
    }

    fun appendCallHistory(session: CallSession) {
        val current = _savedCallHistory.value
        if (current.none { it.id == session.id }) {
            val updated = listOf(session) + current
            saveCallHistory(updated)
        }
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

    // === Home App Order ===
    fun getHomeAppOrder(): List<String> {
        return _homeAppOrder.value
    }

    fun saveHomeAppOrder(ids: List<String>) {
        _homeAppOrder.value = ids
        sharedPrefs?.edit()?.putString(KEY_HOME_APP_ORDER, ids.joinToString(","))?.apply()
    }

    // === Proactive Message Settings (P3D-2) ===
    fun getProactiveSettings(): ProactiveSettings = _proactiveSettings.value

    fun saveProactiveSettings(settings: ProactiveSettings) {
        _proactiveSettings.value = settings
        sharedPrefs?.edit()
            ?.putBoolean(KEY_PROACTIVE_ENABLED, settings.enabled)
            ?.putInt(KEY_PROACTIVE_INTERVAL_HOURS, settings.intervalHours)
            ?.putInt(KEY_PROACTIVE_QUIET_START, settings.quietStartMinute)
            ?.putInt(KEY_PROACTIVE_QUIET_END, settings.quietEndMinute)
            ?.putInt(KEY_PROACTIVE_DAILY_LIMIT, settings.dailyLimit)
            ?.apply()
    }

    fun getProactiveState(): ProactiveState = ProactiveState(
        lastSuccessAtEpochMs = sharedPrefs?.getLong(KEY_PROACTIVE_LAST_SUCCESS, 0L) ?: 0L,
        sentDate = sharedPrefs?.getString(KEY_PROACTIVE_SENT_DATE, "") ?: "",
        sentCount = sharedPrefs?.getInt(KEY_PROACTIVE_SENT_COUNT, 0) ?: 0,
    )

    fun saveProactiveState(state: ProactiveState) {
        sharedPrefs?.edit()
            ?.putLong(KEY_PROACTIVE_LAST_SUCCESS, state.lastSuccessAtEpochMs)
            ?.putString(KEY_PROACTIVE_SENT_DATE, state.sentDate)
            ?.putInt(KEY_PROACTIVE_SENT_COUNT, state.sentCount)
            ?.apply()
    }
}
