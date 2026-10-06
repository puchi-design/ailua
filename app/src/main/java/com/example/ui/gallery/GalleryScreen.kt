package com.example.ui.gallery

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.model.GalleryAsset
import com.example.data.repository.GalleryRepository
import com.example.data.gallery.GalleryImportException
import com.example.data.model.GalleryAssetType
import com.example.data.engine.WorldStateRepository
import com.example.data.projection.projectGalleryAssets
import com.example.data.registry.CharacterRegistry
import com.example.ui.designsystem.*
import com.example.ui.themeengine.LocalAiluaTheme
import kotlinx.coroutines.launch

@Composable
fun GalleryScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {},
    onGoHome: () -> Unit = onBack,
    initialCharacterId: String? = null,
    initialAssetId: String? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val seedAssets by GalleryRepository.assets.collectAsStateWithLifecycle()
    val worldEvents by WorldStateRepository.events.collectAsStateWithLifecycle()
    val projectedAssets = remember(seedAssets, worldEvents) {
        projectGalleryAssets(seedAssets, worldEvents, CharacterRegistry.getAllCharacters().associate { it.id to it.name })
    }
    val assets = remember(projectedAssets, initialCharacterId) {
        if (initialCharacterId == null) projectedAssets
        else projectedAssets.filter { it.characterId == initialCharacterId && it.type != GalleryAssetType.USER_IMPORTED }
    }
    val albums = remember(assets) { (GalleryRepository.albums + assets.map { it.album }).distinct() }
    var selectedAlbum by remember { mutableStateOf("全部") }
    var viewingAsset by remember { mutableStateOf<GalleryAsset?>(null) }
    var initialPhotoOpened by remember(initialAssetId) { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<GalleryAsset?>(null) }
    var busy by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

    LaunchedEffect(initialAssetId, assets) {
        if (!initialPhotoOpened && initialAssetId != null) {
            assets.firstOrNull { it.id == initialAssetId }?.let { photo ->
                viewingAsset = photo
                initialPhotoOpened = true
            }
        }
    }

    LaunchedEffect(context) {
        GalleryRepository.loadUserImports(context).onFailure { error ->
            snackbar.showSnackbar("无法读取已导入照片：${galleryErrorMessage(error)}")
        }
    }

    // Android Photo Picker launcher (zero-permission media picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val result = GalleryRepository.importPickedPhoto(context, uri)
            busy = false
            result.onSuccess {
                selectedAlbum = "我的导入"
                snackbar.showSnackbar("照片已保存到相册")
            }.onFailure { error ->
                snackbar.showSnackbar("导入失败：${galleryErrorMessage(error)}")
            }
        }
    }

    val filteredAssets = assets.filter { asset ->
        selectedAlbum == "全部" || asset.album == selectedAlbum
    }

    val theme = LocalAiluaTheme.current
    Box(Modifier.fillMaxSize().testTag("gallery_screen")) {
        if (viewingAsset == null) {
            AiluaScreenScaffold(
                title = initialCharacterId?.let {
                    "${publicCharacterName(it, CharacterRegistry.getCharacter(it).name)}的相册"
                } ?: "相册", onBack = onBack, onGoHome = onGoHome, backTestTag = "gallery_back_btn",
                trailing = {
                    if (initialCharacterId == null) {
                        IconButton(
                            onClick = { photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.testTag("gallery_import_photo_btn"),
                            enabled = !busy,
                        ) { Icon(Icons.Default.Add, "导入相片", tint = theme.palette.onSurface) }
                    }
                },
            ) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = theme.layout.screenHorizontalPadding.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(albums) { album ->
                        val count = if (album == "全部") assets.size else assets.count { it.album == album }
                        val displayAlbum = com.example.ui.designsystem.CharacterDisplayNames.projectLegacyMentions(album)
                        AiluaChip(if (count > 0) "$displayAlbum $count" else displayAlbum,
                            selected = selectedAlbum == album, onClick = { selectedAlbum = album })
                    }
                }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    state = gridState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    items(filteredAssets, key = { it.id }) { asset ->
                        GalleryPhotoCard(asset, onClick = { viewingAsset = asset })
                    }
                }
            }
        } else {
            GalleryDetailDialog(checkNotNull(viewingAsset), onDismiss = { viewingAsset = null },
                onGoHome = onGoHome, onRequestDelete = { pendingDelete = viewingAsset }, isBusy = busy)
        }
        SnackbarHost(snackbar, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp))
    }
    pendingDelete?.let { photo ->
        AlertDialog(
            onDismissRequest = { if (!busy) pendingDelete = null },
            title = { Text("删除照片？") },
            text = { Text("这会从 AILUA 相册中永久删除「${photo.title}」。手机原相册中的照片不会受影响。") },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    pendingDelete = null
                    scope.launch {
                        busy = true
                        val result = GalleryRepository.deleteImportedPhoto(context, photo.id)
                        busy = false
                        result.onSuccess {
                            viewingAsset = null
                            snackbar.showSnackbar("已删除导入的照片")
                        }.onFailure { error ->
                            snackbar.showSnackbar("删除失败：${galleryErrorMessage(error)}")
                        }
                    }
                }, modifier = Modifier.testTag("gallery_confirm_delete_btn")) { Text("删除") }
            },
            dismissButton = { TextButton(enabled = !busy, onClick = { pendingDelete = null }) { Text("取消") } },
        )
    }
}

private fun galleryErrorMessage(error: Throwable): String =
    if (error is GalleryImportException) error.message ?: "请稍后重试"
    else "请确认照片仍可读取、手机有足够空间后重试"

@Composable
private fun GalleryPhotoCard(asset: GalleryAsset, onClick: () -> Unit) {
    AiluaMediaFrame(
        Modifier.fillMaxWidth().aspectRatio(1f).clickable(onClick = onClick).testTag("gallery_card_${asset.id}"),
    ) {
        GalleryImage(asset, crop = true)
    }
}

@Composable
private fun GalleryImage(asset: GalleryAsset, crop: Boolean) {
    if (asset.uriString != null) {
        var loadFailed by remember(asset.uriString) { mutableStateOf(false) }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            AsyncImage(model = asset.uriString, contentDescription = asset.title,
                modifier = Modifier.fillMaxSize(), contentScale = if (crop) ContentScale.Crop else ContentScale.Fit,
                onSuccess = { loadFailed = false }, onError = { loadFailed = true })
            if (loadFailed) Text("图片文件无法读取", style = LocalAiluaTheme.current.text.caption,
                color = LocalAiluaTheme.current.palette.onSurfaceMuted)
        }
    } else {
        // Keep the original procedural artwork on its original dark canvas until real assets exist.
        Box(Modifier.fillMaxSize().background(Color(0xFF23212C))) { GalleryVisualCanvas(asset.visualReference) }
    }
}

/** Full-page media viewer inside the existing SystemUI host, with standard Back handling. */
@Composable
private fun GalleryDetailDialog(
    asset: GalleryAsset,
    onDismiss: () -> Unit,
    onGoHome: () -> Unit,
    onRequestDelete: () -> Unit,
    isBusy: Boolean,
) {
    val theme = LocalAiluaTheme.current
    val events by WorldStateRepository.events.collectAsStateWithLifecycle()
    val lifeEvent = events.firstOrNull { it.id == asset.lifeEventId }
    val name = if (asset.characterId == "user") "我" else CharacterRegistry.getCharacter(asset.characterId).name
    BackHandler(onBack = onDismiss)
    AiluaScreenScaffold(
        title = "", onBack = onDismiss, onGoHome = onGoHome,
        modifier = Modifier.testTag("gallery_detail_dialog"),
        trailing = {
            Text(asset.type.label, style = theme.text.caption, color = theme.palette.onSurfaceMuted)
            if (asset.type == GalleryAssetType.USER_IMPORTED) {
                IconButton(onClick = onRequestDelete, enabled = !isBusy,
                    modifier = Modifier.testTag("gallery_delete_photo_btn")) {
                    Icon(Icons.Default.Delete, contentDescription = "删除导入照片", tint = theme.palette.onSurface)
                }
            }
        },
    ) {
        AiluaMediaFrame(Modifier.weight(1f).fillMaxWidth()) { GalleryImage(asset, crop = false) }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = theme.layout.screenHorizontalPadding.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("$name · ${asset.createdAtVirtualTime}", style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            val displayTitle = if (asset.type == GalleryAssetType.USER_IMPORTED) asset.title else
                com.example.ui.designsystem.CharacterDisplayNames.projectLegacyMentions(asset.title)
            val displayCaption = if (asset.type == GalleryAssetType.USER_IMPORTED) asset.caption else
                com.example.ui.designsystem.CharacterDisplayNames.projectLegacyMentions(asset.caption)
            Text(displayTitle, style = theme.text.section, color = theme.palette.onSurface)
            Text(displayCaption, style = theme.text.body, color = theme.palette.onSurface)
            if (asset.lifeEventId != null) {
                Text("相关生活事件", style = theme.text.caption, color = theme.palette.onSurfaceMuted)
                Text(lifeEvent?.let { "${it.time} · ${com.example.ui.designsystem.CharacterDisplayNames.projectLegacyMentions(it.title)}" }
                    ?: "已关联生活动态",
                    style = theme.text.secondary, color = theme.palette.onSurfaceMuted)
            }
        }
    }
}

/** Original Canvas fallback; fixed paint colors belong to the illustration, not screen chrome. */
@Composable
private fun GalleryVisualCanvas(visualReference: String) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        when (visualReference) {
            "pudding" -> {
                // Glass dish & Caramel Custard Pudding (Convenience store pudding)
                drawCircle(
                    color = Color(0xFFFFF8EA).copy(alpha = 0.4f),
                    center = Offset(w * 0.5f, h * 0.55f),
                    radius = 38.dp.toPx()
                )
                drawRoundRect(
                    color = Color(0xFFFFE89E),
                    topLeft = Offset(w * 0.36f, h * 0.38f),
                    size = Size(w * 0.28f, h * 0.34f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    color = Color(0xFF8F4200),
                    topLeft = Offset(w * 0.36f, h * 0.38f),
                    size = Size(w * 0.28f, h * 0.12f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(10f, 10f)
                )
                drawCircle(
                    color = Color(0xFF8F4200),
                    center = Offset(w * 0.42f, h * 0.53f),
                    radius = 3.dp.toPx()
                )
            }
            "flowers" -> {
                drawCircle(
                    color = Color(0xFFFFFDF8),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = 24.dp.toPx()
                )
                drawCircle(
                    color = Color(0xFFFFF6E4),
                    center = Offset(w * 0.44f, h * 0.46f),
                    radius = 18.dp.toPx()
                )
                drawCircle(
                    color = Color(0xFFD6B57E).copy(alpha = 0.7f),
                    center = Offset(w * 0.5f, h * 0.5f),
                    radius = 6.dp.toPx()
                )
            }
            "night_book" -> {
                drawRoundRect(
                    color = Color(0xFF2C243B),
                    topLeft = Offset(w * 0.25f, h * 0.3f),
                    size = Size(w * 0.5f, h * 0.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                )
                drawRoundRect(
                    color = Color(0xFFFFF9ED),
                    topLeft = Offset(w * 0.28f, h * 0.33f),
                    size = Size(w * 0.44f, h * 0.34f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                )
                drawCircle(
                    color = Color(0xFFD6B57E).copy(alpha = 0.4f),
                    center = Offset(w * 0.65f, h * 0.25f),
                    radius = 14.dp.toPx()
                )
            }
            else -> {
                // Rain window
                drawCircle(
                    color = Color(0xFFFFF2D9).copy(alpha = 0.3f),
                    center = Offset(w * 0.7f, h * 0.4f),
                    radius = h * 0.4f
                )
                for (i in 0..10) {
                    val x = (i * 24f) % w
                    val y = (i * 18f) % (h * 0.8f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(x, y),
                        end = Offset(x - 5f, y + 20f),
                        strokeWidth = 2f
                    )
                }
            }
        }
    }
}
