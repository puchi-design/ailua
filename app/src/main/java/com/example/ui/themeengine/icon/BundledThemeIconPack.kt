package com.example.ui.themeengine.icon

import com.example.ui.themeengine.ThemeResolver
import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.external.ThemeAssetRef

/** Official icon styles are assets; legacy styles keep their existing vector treatment. */
data class BundledThemeIconPack internal constructor(
    val iconStyleId: String,
    val resourcePackId: String,
)

data class BundledThemeIconResource internal constructor(
    val pack: BundledThemeIconPack,
    val iconKey: String,
) {
    // Resource naming belongs to this catalog, never to launcher or preview callers.
    val drawableName: String get() = "ti_${pack.resourcePackId}_$iconKey"
    val asset: ThemeAssetRef.BuiltIn get() = ThemeAssetRef.BuiltIn(drawableName)
    val isPixelArt: Boolean get() = pack.iconStyleId == "y2k_icons"
}

/** The same registry supplies Home, Dock, folders, the drawer, and candidate previews. */
object BundledIconCatalog {
    const val DEFAULT_STYLE_ID = "default_icons"

    val packs = listOf(
        BundledThemeIconPack(DEFAULT_STYLE_ID, "default"),
        BundledThemeIconPack("soft_home_icons", "soft"),
        BundledThemeIconPack("rainy_study_icons", "rain"),
        BundledThemeIconPack("sakura_icons", "sakura"),
        BundledThemeIconPack("midnight_icons", "midnight"),
        BundledThemeIconPack("y2k_icons", "y2k"),
    )

    val iconKeys = listOf(
        "chat", "living", "moments", "gallery", "contacts", "mailbox", "call",
        "memories", "relations", "diary", "check_phone", "theater", "world",
        "lore", "creator", "assistant", "apps", "settings",
    )

    private val aliases = mapOf(
        "messages" to "chat",
        "group_chat" to "chat",
        "call_history" to "call",
        "companion_call" to "call",
        "phone" to "call",
        "mail" to "mailbox",
        "letter" to "mailbox",
        "world_map" to "world",
        "world_3d" to "world",
        "lore_books" to "lore",
        "world_book" to "lore",
        "character_creation" to "creator",
        "character_creator" to "creator",
        "agent" to "assistant",
        "agent_tasks" to "assistant",
    )

    private val resourcesByName = packs.flatMap { pack ->
        iconKeys.map { BundledThemeIconResource(pack, it) }
    }.associateBy { it.drawableName }

    fun canonicalIconKey(iconKey: String): String? =
        (aliases[iconKey] ?: iconKey).takeIf { it in iconKeys }

    /** Preserve exact imported mappings/manual choices before consulting equivalent app keys. */
    fun lookupKeys(iconKey: String): List<String> {
        val canonical = canonicalIconKey(iconKey) ?: return listOf(iconKey)
        return (listOf(iconKey, canonical) + aliases.filterValues { it == canonical }.keys).distinct()
    }

    fun resource(iconStyleId: String, iconKey: String): BundledThemeIconResource? {
        val pack = packs.firstOrNull { it.iconStyleId == iconStyleId } ?: return null
        val canonical = canonicalIconKey(iconKey) ?: return null
        return BundledThemeIconResource(pack, canonical)
    }

    /** A missing official icon falls back to Default, then the caller's vector identity. */
    fun candidates(iconKey: String, selection: ThemeSelection): List<BundledThemeIconResource> {
        val selected = resource(ThemeResolver.resolveIconStyleId(selection), iconKey)
            ?: return emptyList()
        val default = resource(DEFAULT_STYLE_ID, iconKey) ?: return listOf(selected)
        return listOf(selected, default).distinct()
    }

    /** Only registered built-in keys may be loaded; arbitrary drawable names are not accepted. */
    fun resourceForAssetKey(key: String): BundledThemeIconResource? = resourcesByName[key]
}
