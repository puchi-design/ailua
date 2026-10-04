package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.engine.UserActivityRecorder
import com.example.data.gallery.GalleryImportException
import com.example.data.gallery.GalleryImportStore
import com.example.data.mock.OfficialCharacters
import com.example.data.mock.LegacyOfficialCharacters
import com.example.data.local.AiluaLocalStore
import com.example.data.model.GalleryAsset
import com.example.data.model.GalleryAssetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

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

    private val importMutex = Mutex()

    /** Read saved copies on opening Gallery. A corrupt index is surfaced, never silently reset. */
    suspend fun loadUserImports(context: Context): Result<Int> = withContext(Dispatchers.IO) {
        importMutex.withLock {
            safely {
                val saved = GalleryImportStore(context).load()
                replaceImported(saved)
                saved.size
            }
        }
    }

    /** Copy during the picker grant, then publish only after the image and index are durable. */
    suspend fun importPickedPhoto(context: Context, uri: Uri): Result<GalleryAsset> = withContext(Dispatchers.IO) {
        importMutex.withLock {
            safely {
                val store = GalleryImportStore(context)
                val asset = store.importPhoto(uri)
                replaceImported(store.load())
                // Keep the existing cross-app life event, but photo persistence must not depend on it.
                // Photo Picker file names may contain private names or locations. The
                // cross-app event reaches chat prompts, so only record a generic fact.
                try { UserActivityRecorder.recordPhotoImport(title = "一张照片") } catch (_: Exception) {}
                asset
            }
        }
    }

    suspend fun deleteImportedPhoto(context: Context, assetId: String): Result<Unit> = withContext(Dispatchers.IO) {
        importMutex.withLock {
            safely {
                if (!assetId.startsWith("user_import_")) throw GalleryImportException("只能删除自己导入的照片")
                val store = GalleryImportStore(context)
                store.delete(assetId)
                replaceImported(store.load())
            }
        }
    }

    private fun replaceImported(saved: List<GalleryAsset>) {
        _assets.value = saved + _assets.value.filterNot { it.type == GalleryAssetType.USER_IMPORTED }
    }

    private inline fun <T> safely(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }
}
