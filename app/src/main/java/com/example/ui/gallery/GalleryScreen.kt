package com.example.ui.gallery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.data.model.GalleryAsset
import com.example.data.repository.GalleryRepository
import com.example.ui.components.AiluaAvatar
import com.example.ui.components.VirtualPhoneHomeBar
import com.example.ui.components.VirtualPhoneStatusBar
import com.example.ui.theme.AiluaDustyRose
import com.example.ui.theme.AiluaMistBlue
import com.example.ui.theme.AiluaMoonGold

@Composable
fun GalleryScreen(
    isDarkTheme: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBack: () -> Unit = {}
) {
    val assets by GalleryRepository.assets.collectAsStateWithLifecycle()
    var selectedAlbum by remember { mutableStateOf("全部") }
    var viewingAsset by remember { mutableStateOf<GalleryAsset?>(null) }

    // Android Photo Picker launcher (zero-permission media picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            GalleryRepository.addImportedAsset(it.toString(), "自选心契相片")
            selectedAlbum = "我的导入"
        }
    }

    val filteredAssets = assets.filter { asset ->
        selectedAlbum == "全部" || asset.album == selectedAlbum
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("gallery_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            VirtualPhoneStatusBar(
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("gallery_back_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "光影相册 · Gallery",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.5.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "角色生活瞬间与心契珍藏影集",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                // Import Reference Photo via Android Photo Picker
                Button(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AiluaMistBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("gallery_import_photo_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "导入相片", fontSize = 11.5.sp)
                }
            }

            // Albums Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(GalleryRepository.albums) { album ->
                    val isSelected = selectedAlbum == album
                    val count = if (album == "全部") assets.size else assets.count { it.album == album }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                1.dp,
                                if (isSelected) AiluaMistBlue.copy(alpha = 0.8f)
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedAlbum = album }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (count > 0) "$album ($count)" else album,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 11.5.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Photo Grid (LazyVerticalGrid from Android PhotoPicker sample)
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAssets, key = { it.id }) { asset ->
                    GalleryPhotoCard(
                        asset = asset,
                        onClick = { viewingAsset = asset }
                    )
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }

            VirtualPhoneHomeBar(
                canGoBack = true,
                onBack = onBack,
                onGoHome = onBack
            )
        }

        // Photo Detail Dialog
        viewingAsset?.let { asset ->
            GalleryDetailDialog(
                asset = asset,
                onDismiss = { viewingAsset = null }
            )
        }
    }
}

@Composable
private fun GalleryPhotoCard(
    asset: GalleryAsset,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                0.5.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp)
            .testTag("gallery_card_${asset.id}")
    ) {
        Column {
            // Visual Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF23212C)),
                contentAlignment = Alignment.Center
            ) {
                if (asset.uriString != null) {
                    AsyncImage(
                        model = asset.uriString,
                        contentDescription = asset.title,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    GalleryVisualCanvas(visualReference = asset.visualReference)
                }

                // Album Pill overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = asset.type.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            color = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = asset.title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = asset.caption,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 10.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

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
                    color = AiluaMoonGold.copy(alpha = 0.7f),
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
                    color = AiluaMoonGold.copy(alpha = 0.4f),
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

@Composable
private fun GalleryDetailDialog(
    asset: GalleryAsset,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                .padding(18.dp)
                .testTag("gallery_detail_dialog")
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AiluaAvatar(
                            avatarId = asset.characterId,
                            size = 28.dp,
                            showHalo = false
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = asset.album,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = AiluaMistBlue
                            )
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "关闭")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Big Visual Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF1E1C27)),
                    contentAlignment = Alignment.Center
                ) {
                    if (asset.uriString != null) {
                        AsyncImage(
                            model = asset.uriString,
                            contentDescription = asset.title,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        GalleryVisualCanvas(visualReference = asset.visualReference)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = asset.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = asset.caption,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                )

                if (asset.lifeEventId != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(AiluaMoonGold.copy(alpha = 0.12f))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "✨ 关联生活动态脉搏：${asset.lifeEventId} · 已在瞬间与相册双向连通",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.5.sp,
                                color = AiluaMoonGold,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }
        }
    }
}
