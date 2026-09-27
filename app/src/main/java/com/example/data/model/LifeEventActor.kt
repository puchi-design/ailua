package com.example.data.model

/**
 * P3D-3 cross-app continuity marker.
 *
 * LifeEvents carry BOTH the character's own life and the user's app actions
 * (photo import / chat message / place visit). Character-side projections
 * (presence, living, private phone, moments) must only ever show the
 * character's own facts, so user-originated events are tagged with
 * [LIFE_EVENT_ACTOR_USER] in metadata and filtered out there — they still
 * reach the chat prompt via `eventsForCharacter`.
 */
const val LIFE_EVENT_ACTOR_USER = "user"

/** True when this event records a USER action, not the character's own life. */
fun LifeEvent.isUserActivity(): Boolean = metadata["actor"] == LIFE_EVENT_ACTOR_USER
