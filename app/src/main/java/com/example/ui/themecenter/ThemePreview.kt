package com.example.ui.themecenter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppIconDefaults
import com.example.ui.components.AppIconItem
import com.example.data.characterassets.CharacterAssetResolver
import com.example.data.characterassets.CharacterAvatarSlot
import com.example.ui.designsystem.CharacterPortrait
import com.example.ui.designsystem.PortraitVariant
import com.example.ui.home.hotseat.HomeDockFrame
import com.example.ui.home.widget.ThemeWidgetFrame
import com.example.ui.themeengine.AiluaThemeProvider
import com.example.ui.themeengine.AiluaThemeRuntime
import com.example.ui.themeengine.DockContainerMode
import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.ThemeWallpaper
import com.example.ui.themeengine.material.AiluaBackdropProvider
import com.example.ui.themeengine.material.ailuaBackdropSource
import kotlin.math.roundToInt

private val PreviewWidth = 360.dp
private val PreviewHeight = 640.dp

/** A fixed 9:16 fixture using the same wallpaper, widget, icon and dock renderers as Home. */
@Composable
fun ThemePreview(
    theme: AiluaThemeRuntime,
    modifier: Modifier = Modifier,
    selection: ThemeSelection = ThemeSelection(theme.id),
    onClick: (() -> Unit)? = null,
) {
    val fixture = remember(selection) { ThemePreviewFixture(selection) }
    AiluaThemeProvider(theme) {
        BoxWithConstraints(
            modifier.clip(RoundedCornerShape(theme.shapes.medium.dp))
                .testTag("theme_preview_${theme.id}"),
            contentAlignment = Alignment.Center,
        ) {
            val availableWidth = if (constraints.hasBoundedWidth) maxWidth else PreviewWidth
            val availableHeight = if (constraints.hasBoundedHeight) maxHeight else PreviewHeight
            val scale = minOf(availableWidth / PreviewWidth, availableHeight / PreviewHeight)
                .coerceAtLeast(0.01f)
            Box(Modifier.scaledPreviewCanvas(scale)) {
                AiluaBackdropProvider {
                    Box(
                        Modifier.fillMaxSize()
                            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
                    ) {
                        ThemeWallpaper(
                            modifier = Modifier.fillMaxSize().ailuaBackdropSource(),
                            runtime = theme,
                            selection = fixture.selection,
                        )
                        PreviewContent(theme, fixture, onClick)
                    }
                }
            }
        }
    }
}

/** Scale the whole measured phone, including type and material, rather than redrawing tiny UI. */
private fun Modifier.scaledPreviewCanvas(scale: Float): Modifier = layout { measurable, _ ->
    val width = PreviewWidth.roundToPx()
    val height = PreviewHeight.roundToPx()
    val phone = measurable.measure(Constraints.fixed(width, height))
    layout((width * scale).roundToInt(), (height * scale).roundToInt()) {
        phone.placeWithLayer(0, 0) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

@Composable
private fun PreviewContent(
    theme: AiluaThemeRuntime,
    fixture: ThemePreviewFixture,
    onClick: (() -> Unit)?,
) {
    val label = theme.icons.labelColor
    val horizontalPadding = theme.layout.screenHorizontalPadding.dp
    Column(Modifier.fillMaxSize().padding(top = 18.dp, bottom = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(fixture.time, style = theme.text.secondary, color = label, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Wifi, contentDescription = null, tint = label, modifier = Modifier.size(17.dp))
                Icon(Icons.Default.BatteryFull, contentDescription = null, tint = label, modifier = Modifier.size(19.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        ThemeWidgetFrame(
            selected = false,
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding).height(88.dp),
        ) {
            Row(
                Modifier.fillMaxSize().padding(theme.widgets.contentPaddingDp.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(fixture.time, style = theme.text.display, color = theme.widgets.foregroundColor)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(fixture.date, style = theme.text.secondary, color = theme.widgets.foregroundColor, maxLines = 1)
                    Text(fixture.weather, style = theme.text.caption, color = theme.widgets.foregroundColor, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ThemeWidgetFrame(
            selected = false,
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding).height(156.dp),
        ) {
            Column(
                Modifier.fillMaxSize().padding(theme.widgets.contentPaddingDp.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Explicit fixture image and label keep previews independent of saved cards.
                    CharacterPortrait(
                        characterId = fixture.characterId,
                        variant = PortraitVariant.AVATAR,
                        modifier = Modifier.size(58.dp),
                        avatarReferenceOverride = CharacterAssetResolver.officialUri(fixture.characterId, CharacterAvatarSlot.MAIN),
                        characterNameOverride = fixture.characterName,
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(fixture.characterName, style = theme.text.section, color = theme.widgets.foregroundColor, maxLines = 1)
                        Text(fixture.location, style = theme.text.secondary, color = theme.widgets.foregroundColor, maxLines = 1)
                    }
                }
                Text(
                    fixture.status,
                    style = theme.text.secondary,
                    color = theme.widgets.foregroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "“${fixture.quote}”",
                    style = theme.text.caption,
                    color = theme.widgets.foregroundColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            fixture.apps.forEach { app ->
                AppIconItem(
                    name = app.name,
                    iconKey = app.iconKey,
                    selection = fixture.selection,
                    size = AppIconDefaults.ContainerSize,
                    onClick = onClick ?: {},
                )
            }
        }
        Spacer(Modifier.weight(1f))
        HomeDockFrame(
            spec = theme.dock,
            modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding, vertical = 8.dp),
        ) {
            Row(
                Modifier.fillMaxWidth()
                    .heightIn(min = if (theme.dock.containerMode != DockContainerMode.NONE) 82.dp else 0.dp)
                    .padding(
                        horizontal = theme.dock.horizontalPaddingDp.coerceAtLeast(0f).dp,
                        vertical = theme.dock.verticalPaddingDp.coerceAtLeast(0f).dp,
                    ),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                fixture.dockApps.forEach { app ->
                    AppIconItem(
                        name = app.name,
                        iconKey = app.iconKey,
                        selection = fixture.selection,
                        size = AppIconDefaults.ContainerSize * theme.dock.iconScale * 0.94f,
                        showLabel = false,
                        onClick = onClick ?: {},
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.size(width = 68.dp, height = 4.dp).clip(RoundedCornerShape(2.dp))
                .background(label.copy(alpha = 0.55f)).align(Alignment.CenterHorizontally),
        )
    }
}

/** Standalone swatches share the actual icon renderer, including bitmap mask and pixel sampling. */
@Composable
fun ThemePreviewIcon(
    theme: AiluaThemeRuntime,
    key: String,
    size: Dp,
    selection: ThemeSelection = ThemeSelection(theme.id),
    onClick: (() -> Unit)? = null,
) {
    AiluaThemeProvider(theme) {
        AppIconItem(
            name = key,
            iconKey = key,
            size = size,
            showLabel = false,
            selection = selection,
            onClick = onClick ?: {},
        )
    }
}
