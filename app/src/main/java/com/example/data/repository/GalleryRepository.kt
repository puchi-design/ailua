package com.example.data.repository

import com.example.data.engine.UserActivityRecorder
import com.example.data.mock.OfficialCharacters
import com.example.data.mock.LegacyOfficialCharacters
import com.example.data.local.AiluaLocalStore
import com.example.data.model.GalleryAsset
import com.example.data.model.GalleryAssetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object GalleryRepository {

    val albums = listOf(
        "全部",
        "贺闻川的空间",
        "周见野的现场",
        "裴叙白的声音",
        "苏晚宁的生活",
        "许朝颜的相机",
        "宋知微的记录",
        "共同回忆",
        "私密收藏",
        "我的导入"
    )

    /** Original IDs and contents remain available for already saved event references. */
    val archivedAssets = listOf(
        GalleryAsset(
            id = "ga_pulse_10",
            characterId = "yuna",
            lifeEventId = "pulse_10",
            type = GalleryAssetType.PHOTO,
            title = "街角便利店的手作焦糖布丁",
            caption = "冒雨抢到的限定版焦糖布丁！浓郁奶香与焦糖微苦的完美碰撞～🍮",
            createdAtVirtualTime = "20:50",
            locationId = "place_convenience",
            visualReference = "pudding",
            album = "悠奈的相机"
        ),
        GalleryAsset(
            id = "ga_pulse_4",
            characterId = "mira",
            lifeEventId = "pulse_4",
            type = GalleryAssetType.SCENE,
            title = "白洋桔梗与瓷瓶",
            caption = "巷尾白色花店清晨采摘的白洋桔梗，放在青石街飘窗前格外恬静。",
            createdAtVirtualTime = "14:15",
            locationId = "place_flower_shop",
            visualReference = "flowers",
            album = "小弥的生活"
        ),
        GalleryAsset(
            id = "ga_pulse_11",
            characterId = "noa",
            lifeEventId = "pulse_11",
            type = GalleryAssetType.PHOTO,
            title = "月光书阁负一层古典唱片角",
            caption = "老唱针轻触黑胶唱片的微响，是夜读时最令人安心的白噪音。",
            createdAtVirtualTime = "22:15",
            locationId = "place_moonlight",
            visualReference = "night_book",
            album = "诺亚的书阁"
        ),
        GalleryAsset(
            id = "ga_mira_window",
            characterId = "mira",
            lifeEventId = "pulse_1",
            type = GalleryAssetType.SELFIE,
            title = "秋雨飘窗前的晨读时光",
            caption = "早晨第一束柔光穿过白纱窗，小弥正在翻开昨晚未读完的诗集。",
            createdAtVirtualTime = "08:30",
            locationId = "place_street_23",
            visualReference = "rain_window",
            album = "小弥的生活"
        ),
        GalleryAsset(
            id = "ga_mulan_tea",
            characterId = "yuna",
            lifeEventId = "pulse_2",
            type = GalleryAssetType.SCENE,
            title = "木兰茶馆的午后司康",
            caption = "现烤柠檬司康配白桃乌龙，悠奈在茶馆记录下的惬意午后。",
            createdAtVirtualTime = "15:20",
            locationId = "place_mulan",
            visualReference = "pudding",
            album = "共同回忆"
        )
    ) + LegacyOfficialCharacters.galleryAssets

    private val _assets = MutableStateFlow<List<GalleryAsset>>(
        OfficialCharacters.galleryAssets + archivedAssets.filter { asset ->
            AiluaLocalStore.savedWorldEvents.value.any { event ->
                event.id == asset.lifeEventId || event.sourceRefId == asset.id
            }
        }
    )
    val assets: StateFlow<List<GalleryAsset>> = _assets.asStateFlow()

    fun addImportedAsset(uriString: String, title: String = "本地导入参考照片") {
        val newAsset = GalleryAsset(
            id = "user_import_${System.currentTimeMillis()}",
            characterId = "user",
            lifeEventId = null,
            type = GalleryAssetType.USER_IMPORTED,
            title = title,
            caption = "从 Android Photo Picker 选入的心意参考相片",
            createdAtVirtualTime = "刚刚",
            locationId = "place_street_23",
            visualReference = "imported",
            uriString = uriString,
            album = "我的导入"
        )
        _assets.value = listOf(newAsset) + _assets.value

        // P3D-3: 收藏照片 becomes a LifeEvent fact the companion can know about.
        UserActivityRecorder.recordPhotoImport(title = title)
    }
}
