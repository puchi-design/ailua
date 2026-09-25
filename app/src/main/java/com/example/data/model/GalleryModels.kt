package com.example.data.model

enum class GalleryAssetType(val label: String) {
    PHOTO("抓拍摄影"),
    SELFIE("陪伴自拍"),
    SCENE("街区风景"),
    MEMORY_IMAGE("心契结晶"),
    USER_IMPORTED("用户参考导入")
}

data class GalleryAsset(
    val id: String,
    val characterId: String,
    val lifeEventId: String? = null,
    val type: GalleryAssetType,
    val title: String,
    val caption: String,
    val createdAtVirtualTime: String,
    val locationId: String? = null,
    val visualReference: String,
    val uriString: String? = null,
    val isPrivate: Boolean = false,
    val album: String = "共同回忆"
)
