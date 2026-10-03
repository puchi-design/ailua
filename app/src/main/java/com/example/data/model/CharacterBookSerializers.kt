package com.example.data.model

import com.example.data.codec.LosslessJsonObjectSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/** Keeps the existing runtime lore model while reading both V2 and pre-CHAR AILUA saves. */
object WorldBookSerializer : KSerializer<WorldBook> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CharacterBookV2")

    override fun deserialize(decoder: Decoder): WorldBook = CharacterBookWire.readBook(decoder.objectValue())

    override fun serialize(encoder: Encoder, value: WorldBook) = encoder.objectValue(CharacterBookWire.writeBook(value))
}

object LoreEntrySerializer : KSerializer<LoreEntry> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CharacterBookEntryV2")

    override fun deserialize(decoder: Decoder): LoreEntry = CharacterBookWire.readEntry(decoder.objectValue())

    override fun serialize(encoder: Encoder, value: LoreEntry) = encoder.objectValue(CharacterBookWire.writeEntry(value))
}

private fun Decoder.objectValue(): JsonObject =
    ((this as? JsonDecoder)?.decodeJsonElement() as? JsonObject)
        ?: throw SerializationException("Character books must be JSON objects")

private fun Encoder.objectValue(value: JsonObject) {
    LosslessJsonObjectSerializer.serialize(this, value)
}

/**
 * An unedited standard book is emitted with its original values, optional-field presence, and
 * unknown keys. A copied/edited model updates only changed projections. Legacy internal names
 * are migrated to V2; runtime-only metadata is namespaced inside extensions.ailua.
 */
private object CharacterBookWire {
    private val emptyObject = JsonObject(emptyMap())
    private val legacyBookKeys = setOf("id", "scanDepth", "tokenBudget", "recursiveScanning")
    private val legacyEntryKeys = setOf(
        "title", "keywords", "secondaryKeywords", "characterIds", "locationIds", "activationMode", "category", "notes",
    )

    fun readBook(raw: JsonObject): WorldBook {
        val extensions = raw["extensions"] as? JsonObject ?: emptyObject
        val privateData = extensions["ailua"] as? JsonObject
        return WorldBook(
            id = privateData.text("id") ?: raw.text("id") ?: stableId("book", raw),
            name = raw.text("name").orEmpty(),
            description = raw.text("description").orEmpty(),
            scanDepth = raw.integer("scan_depth") ?: raw.integer("scanDepth") ?: 2,
            tokenBudget = raw.integer("token_budget") ?: raw.integer("tokenBudget") ?: 500,
            recursiveScanning = raw.boolean("recursive_scanning") ?: raw.boolean("recursiveScanning") ?: false,
            entries = (raw["entries"] as? JsonArray).orEmpty().map { entry ->
                readEntry(entry as? JsonObject ?: throw SerializationException("Character book entries must be objects"))
            },
            extensions = extensions,
            sourceJson = raw,
        )
    }

    fun readEntry(raw: JsonObject): LoreEntry {
        val extensions = raw["extensions"] as? JsonObject ?: emptyObject
        val privateData = extensions["ailua"] as? JsonObject
        val activation = privateData.text("activation_mode") ?: raw.text("activationMode")
        return LoreEntry(
            id = privateData.text("id") ?: (raw["id"] as? JsonPrimitive)
                ?.takeUnless { it == JsonNull }?.content ?: stableId("entry", raw),
            title = raw.text("name") ?: raw.text("title") ?: raw.text("comment").orEmpty(),
            content = raw.text("content").orEmpty(),
            keywords = raw.strings("keys") ?: raw.strings("keywords") ?: emptyList(),
            secondaryKeywords = raw.strings("secondary_keys") ?: raw.strings("secondaryKeywords") ?: emptyList(),
            enabled = raw.boolean("enabled") ?: true,
            priority = raw.integer("priority") ?: 10,
            characterIds = privateData.strings("character_ids") ?: raw.strings("characterIds") ?: emptyList(),
            locationIds = privateData.strings("location_ids") ?: raw.strings("locationIds") ?: emptyList(),
            activationMode = LoreActivationMode.entries.firstOrNull { it.name == activation }
                ?: if (raw.boolean("constant") == true) LoreActivationMode.ALWAYS else LoreActivationMode.KEYWORD,
            category = privateData.text("category") ?: raw.text("category") ?: "世界设定",
            notes = privateData.text("notes") ?: raw.text("notes") ?: raw.text("comment").orEmpty(),
            extensions = extensions,
            sourceJson = raw,
        )
    }

    fun writeBook(book: WorldBook): JsonObject {
        val source = book.sourceJson
        val previous = source?.let(::readBook)
        val migrate = source == null || isLegacyBook(source)
        val result = source.orEmpty().toMutableMap()
        if (migrate) legacyBookKeys.forEach(result::remove)
        fun update(key: String, changed: Boolean, value: JsonElement) {
            if (migrate || changed) result[key] = value
        }
        update("name", book.name != previous?.name, JsonPrimitive(book.name))
        update("description", book.description != previous?.description, JsonPrimitive(book.description))
        update("scan_depth", book.scanDepth != previous?.scanDepth, JsonPrimitive(book.scanDepth))
        update("token_budget", book.tokenBudget != previous?.tokenBudget, JsonPrimitive(book.tokenBudget))
        update("recursive_scanning", book.recursiveScanning != previous?.recursiveScanning, JsonPrimitive(book.recursiveScanning))
        if (migrate || book.entries != previous?.entries || "entries" !in result) {
            result["entries"] = JsonArray(book.entries.map(::writeEntry))
        }
        var extensions = book.extensions
        if (migrate || book.id != previous?.id) {
            extensions = extensions.withPrivate(mapOf("id" to JsonPrimitive(book.id)))
        }
        if (migrate || extensions != previous?.extensions || "extensions" !in result) result["extensions"] = extensions
        return JsonObject(result)
    }

    fun writeEntry(entry: LoreEntry): JsonObject {
        val source = entry.sourceJson
        val previous = source?.let(::readEntry)
        val migrate = source == null || isLegacyEntry(source)
        val result = source.orEmpty().toMutableMap()
        if (migrate) legacyEntryKeys.forEach(result::remove)
        fun update(key: String, changed: Boolean, value: JsonElement) {
            if (migrate || changed) result[key] = value
        }
        update("name", entry.title != previous?.title, JsonPrimitive(entry.title))
        update("content", entry.content != previous?.content, JsonPrimitive(entry.content))
        update("keys", entry.keywords != previous?.keywords, entry.keywords.jsonArray())
        update("secondary_keys", entry.secondaryKeywords != previous?.secondaryKeywords, entry.secondaryKeywords.jsonArray())
        update("enabled", entry.enabled != previous?.enabled, JsonPrimitive(entry.enabled))
        update("priority", entry.priority != previous?.priority, JsonPrimitive(entry.priority))
        update("comment", entry.notes != previous?.notes, JsonPrimitive(entry.notes))
        update("constant", entry.activationMode != previous?.activationMode, JsonPrimitive(entry.activationMode == LoreActivationMode.ALWAYS))
        // Required by V2, even when an old internal entry never had an insertion order.
        if ("insertion_order" !in result) result["insertion_order"] = JsonPrimitive(0)

        val privateUpdates = mutableMapOf<String, JsonElement>()
        if (migrate || entry.id != previous?.id) {
            val numericId = entry.id.toLongOrNull()
            if (numericId == null) result.remove("id") else result["id"] = JsonPrimitive(numericId)
            privateUpdates["id"] = JsonPrimitive(entry.id)
        }
        if (migrate || entry.characterIds != previous?.characterIds) privateUpdates["character_ids"] = entry.characterIds.jsonArray()
        if (migrate || entry.locationIds != previous?.locationIds) privateUpdates["location_ids"] = entry.locationIds.jsonArray()
        if (migrate || entry.activationMode != previous?.activationMode) privateUpdates["activation_mode"] = JsonPrimitive(entry.activationMode.name)
        if (migrate || entry.category != previous?.category) privateUpdates["category"] = JsonPrimitive(entry.category)
        // A legacy notes key can also be present in private extensions. Keep it in sync on edits.
        if ((entry.extensions["ailua"] as? JsonObject)?.containsKey("notes") == true && entry.notes != previous?.notes) {
            privateUpdates["notes"] = JsonPrimitive(entry.notes)
        }
        val extensions = if (privateUpdates.isEmpty()) entry.extensions else entry.extensions.withPrivate(privateUpdates)
        if (migrate || extensions != previous?.extensions || "extensions" !in result) result["extensions"] = extensions
        return JsonObject(result)
    }

    private fun isLegacyBook(raw: JsonObject): Boolean =
        ("extensions" !in raw && legacyBookKeys.any { it in raw }) ||
            (raw["entries"] as? JsonArray).orEmpty().any { it is JsonObject && isLegacyEntry(it) }

    private fun isLegacyEntry(raw: JsonObject): Boolean =
        "keys" !in raw && legacyEntryKeys.any { it in raw }

    private fun JsonObject.withPrivate(updates: Map<String, JsonElement>): JsonObject = JsonObject(
        this + ("ailua" to JsonObject((this["ailua"] as? JsonObject).orEmpty() + updates)),
    )

    private fun stableId(prefix: String, raw: JsonObject): String = "${prefix}_${raw.toString().hashCode().toUInt().toString(16)}"

    private fun JsonObject?.text(key: String): String? =
        (this?.get(key) as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject?.integer(key: String): Int? = (this?.get(key) as? JsonPrimitive)?.intOrNull

    private fun JsonObject?.boolean(key: String): Boolean? = (this?.get(key) as? JsonPrimitive)?.booleanOrNull

    private fun JsonObject?.strings(key: String): List<String>? = (this?.get(key) as? JsonArray)?.mapNotNull {
        (it as? JsonPrimitive)?.takeIf { value -> value.isString }?.content
    }

    private fun List<String>.jsonArray(): JsonArray = JsonArray(map(::JsonPrimitive))
}
