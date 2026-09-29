package com.example.data.projection

import com.example.data.model.GalleryAsset
import com.example.data.model.GalleryAssetType
import com.example.data.model.LifeEvent
import com.example.data.model.LifeEventType
import com.example.data.model.isUserActivity

/** The same PHOTO fact drives Gallery and Moments; seed assets remain display fixtures. */
fun projectGalleryAssets(
    seed: List<GalleryAsset>,
    events: List<LifeEvent>,
    characterNames: Map<String, String> = emptyMap(),
): List<GalleryAsset> {
    val seedEventIds = seed.mapNotNull { it.lifeEventId }.toSet()
    val runtime = events.asSequence()
        .filter { it.type == LifeEventType.PHOTO && !it.isUserActivity() && it.id !in seedEventIds }
        .distinctBy { it.id }
        .map { event ->
            val album = when (event.characterId) {
                "mira" -> "小弥的生活"
                "yuna" -> "悠奈的相机"
                "noa" -> "诺亚的书阁"
                else -> "${characterNames[event.characterId] ?: event.characterId}的生活"
            }
            GalleryAsset(
                id = "gallery_${event.id}", characterId = event.characterId,
                lifeEventId = event.id, type = GalleryAssetType.PHOTO,
                title = event.metadata["photo_title"]?.takeIf { it.isNotBlank() } ?: event.title,
                caption = event.description, createdAtVirtualTime = event.time,
                locationId = event.location, visualReference = event.imageReference ?: "runtime_event",
                album = album,
            )
        }.toList()
    return runtime + seed
}
