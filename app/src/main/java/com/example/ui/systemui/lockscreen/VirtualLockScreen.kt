package com.example.ui.systemui.lockscreen

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.context.CharacterContext
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.projection.projectPresence
import com.example.data.registry.CharacterRegistry
import com.example.ui.themeengine.LocalAiluaTheme
import com.example.ui.themeengine.ThemeWallpaper
import kotlinx.coroutines.launch

/** A virtual phone surface. Device security and app navigation remain outside this component. */
@Composable
fun VirtualLockScreen(
    onUnlock: () -> Unit,
    onOpenCommunications: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
    statusBar: @Composable () -> Unit = {},
    notificationContent: @Composable () -> Unit = {},
    liveActivityContent: @Composable () -> Unit = {}
) {
    val runtime = LocalAiluaTheme.current
    val worldClock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    val characterId by CharacterContext.selectedId.collectAsStateWithLifecycle()
    val characterCards by CharacterRegistry.allCards.collectAsStateWithLifecycle()
    val character = remember(characterId, characterCards) { CharacterRegistry.getCharacter(characterId) }
    val events by WorldStateRepository.events.collectAsStateWithLifecycle()
    val presence = remember(character, events) { projectPresence(character, events) }
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val latestUnlock by rememberUpdatedState(onUnlock)
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dismissing by remember { mutableStateOf(false) }
    val exitProgress = remember { Animatable(0f) }
    val velocityTracker = remember { VelocityTracker() }

    fun finishUnlock() {
        if (dismissing) return
        dismissing = true
        scope.launch {
            exitProgress.animateTo(1f, tween(180))
            latestUnlock()
        }
    }

    BoxWithConstraints(modifier.fillMaxSize().testTag("virtual_lock_screen")) {
        val compact = maxHeight < 640.dp
        val screenHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
        Box(Modifier.fillMaxSize().graphicsLayer {
            translationY = dragOffset - screenHeightPx * exitProgress.value
            alpha = (1f - exitProgress.value) * (1f + dragOffset / (screenHeightPx * 1.5f)).coerceIn(0.25f, 1f)
        }) {
            ThemeWallpaper(Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(runtime.palette.backgroundPrimary.copy(alpha = runtime.lockscreen.scrimAlpha)))
            Column(Modifier.fillMaxSize()) {
                // Keep status gestures outside the upward-unlock detector.
                statusBar()
                Column(
                    Modifier.weight(1f).fillMaxWidth()
                        .pointerInput(density) {
                            detectVerticalDragGestures(
                                onDragStart = { velocityTracker.resetTracking(); dragOffset = 0f },
                                onVerticalDrag = { change, delta ->
                                    if (!dismissing) {
                                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                                        dragOffset = (dragOffset + delta).coerceAtMost(0f)
                                        change.consume()
                                    }
                                },
                                onDragCancel = { dragOffset = 0f },
                                onDragEnd = {
                                    if (LockScreenGesture.shouldUnlock(-dragOffset, velocityTracker.calculateVelocity().y, density)) {
                                        finishUnlock()
                                    } else dragOffset = 0f
                                }
                            )
                        }.padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(if (compact) 12.dp else 28.dp))
                    Icon(Icons.Default.Lock, contentDescription = "已锁定", tint = runtime.lockscreen.foregroundColor.copy(alpha = 0.7f))
                    Spacer(Modifier.height(8.dp))
                    LockScreenClock(compact = compact)
                    Spacer(Modifier.height(if (compact) 16.dp else 24.dp))
                    Text("${character.name} · ${presence.currentActivity}", color = runtime.lockscreen.foregroundColor,
                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("AILUA · ${worldClock.timeFormatted} · ${worldClock.weather.label}",
                        modifier = Modifier.padding(top = 5.dp), color = runtime.lockscreen.foregroundColor.copy(alpha = 0.68f), fontSize = 12.sp)
                    Spacer(Modifier.height(if (compact) 18.dp else 28.dp))
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        liveActivityContent()
                        notificationContent()
                    }
                    LockScreenShortcuts(onOpenCommunications, onOpenGallery, Modifier.padding(top = 12.dp))
                    Column(
                        Modifier.fillMaxWidth().clickable(onClick = ::finishUnlock).padding(vertical = 12.dp).testTag("lock_unlock"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.KeyboardArrowUp, null, tint = runtime.lockscreen.foregroundColor.copy(alpha = 0.7f))
                        Text("上滑解锁", fontSize = 12.sp, color = runtime.lockscreen.foregroundColor.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

internal object LockScreenGesture {
    fun shouldUnlock(upwardDistancePx: Float, velocityYPxPerSecond: Float, density: Float): Boolean =
        upwardDistancePx >= 120f * density ||
            (upwardDistancePx >= 24f * density && velocityYPxPerSecond <= -900f * density)
}
