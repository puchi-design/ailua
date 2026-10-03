package com.example.ui.themeengine.icon

/** Virtual app keys used by the launcher, ordered by the most common Android equivalent. */
object AiluaIconAliasRegistry {
    private val aliases = mapOf(
        "chat" to listOf("com.google.android.apps.messaging", "com.android.mms"),
        "messages" to listOf("com.google.android.apps.messaging", "com.android.mms"),
        "contacts" to listOf("com.google.android.contacts", "com.android.contacts"),
        "gallery" to listOf("com.google.android.apps.photos", "com.android.gallery3d"),
        "call" to listOf("com.google.android.dialer", "com.android.dialer", "com.android.phone"),
        "call_history" to listOf("com.google.android.dialer", "com.android.dialer", "com.android.phone"),
        "mailbox" to listOf("com.google.android.gm", "com.android.email"),
        "mail" to listOf("com.google.android.gm", "com.android.email"),
        "letter" to listOf("com.google.android.gm", "com.android.email"),
        "world" to listOf("com.android.chrome", "org.mozilla.firefox"),
        "games" to listOf("com.google.android.play.games"),
        "apps" to listOf("com.google.android.googlequicksearchbox"),
        "moments" to listOf("com.instagram.android", "com.google.android.apps.photos"),
        "market" to listOf("com.android.vending"),
        "diary" to listOf("com.google.android.keep", "com.samsung.android.app.notes"),
        "creator" to listOf("com.android.camera2", "com.sec.android.app.camera"),
        "theater" to listOf("com.google.android.youtube"),
        "assistant" to listOf("com.google.android.googlequicksearchbox")
    )

    /** Unknown AILUA-specific apps deliberately fall back to their built-in identity. */
    fun aliasesFor(iconKey: String): List<String> =
        aliases[BundledIconCatalog.canonicalIconKey(iconKey) ?: iconKey].orEmpty()
}
