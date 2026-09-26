package com.example.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.engine.WorldHeartbeatState
import com.example.data.model.CharacterProfile
import com.example.data.model.WorldClock
import com.example.ui.components.BondProgressWidget
import com.example.ui.components.MemorySnippetWidget

/**
 * Stable widget identity for the Home widget host.
 * The [stableId] strings are part of the data contract and must never change.
 */
enum class HomeWidgetId(val stableId: String) {
    CHARACTER_LIVING("character_living"),
    WORLD_CLOCK("world_clock"),
    MEMORY_ECHO("memory_echo"),
    BOND("bond")
}

/**
 * Everything a Home widget may read. State stays where it already lives —
 * the host only forwards it, it never copies or owns it.
 */
data class WidgetHostContext(
    val character: CharacterProfile,
    val accent: Color,
    val worldClock: WorldClock,
    val heartbeatState: WorldHeartbeatState,
    val onOpenDevTime: () -> Unit,
    val onOpenLiving: () -> Unit,
    val onOpenChat: () -> Unit,
    val onOpenProfile: () -> Unit,
    val onNavigateToMemories: () -> Unit
)

/** A registered widget: identity + the composable that renders it. */
data class WidgetSpec(
    val id: HomeWidgetId,
    val title: String,
    val content: @Composable (WidgetHostContext) -> Unit
)

enum class WidgetHostLayout { Stack, Row }

/**
 * Widget registry: adding a Home widget means registering a [WidgetSpec]
 * here (plus providing its composable) — Home itself does not change.
 */
object WidgetRegistry {
    val fallbackId: HomeWidgetId = HomeWidgetId.WORLD_CLOCK

    /** Stable default render order (top of desktop → bottom), asserted by tests. */
    val defaultOrder: List<HomeWidgetId> = listOf(
        HomeWidgetId.WORLD_CLOCK,
        HomeWidgetId.CHARACTER_LIVING,
        HomeWidgetId.MEMORY_ECHO,
        HomeWidgetId.BOND
    )

    private val specs: Map<String, WidgetSpec> = listOf(
        WidgetSpec(HomeWidgetId.CHARACTER_LIVING, "Character Living") { context ->
            LivingPresenceStrip(
                character = context.character,
                onOpenChat = context.onOpenChat,
                onOpenLiving = context.onOpenLiving,
                onOpenProfile = context.onOpenProfile
            )
        },
        WidgetSpec(HomeWidgetId.WORLD_CLOCK, "World Clock") { context ->
            DesktopWorldClock(
                worldClock = context.worldClock,
                heartbeatState = context.heartbeatState,
                accent = context.accent,
                onOpenDevTime = context.onOpenDevTime
            )
        },
        WidgetSpec(HomeWidgetId.MEMORY_ECHO, "Memory Echo") { context ->
            MemorySnippetWidget(
                title = "记忆回响",
                snippet = context.character.memories.firstOrNull()?.snippet.orEmpty(),
                onClick = context.onNavigateToMemories
            )
        },
        WidgetSpec(HomeWidgetId.BOND, "Bond") { context ->
            BondProgressWidget(
                character = context.character,
                onClick = context.onOpenProfile
            )
        }
    ).associateBy { it.id.stableId }

    fun spec(stableId: String): WidgetSpec? = specs[stableId]

    /** Unknown / missing ids safely fall back to a always-available widget. */
    fun resolve(stableId: String): WidgetSpec {
        return spec(stableId) ?: specs.getValue(fallbackId.stableId)
    }

    fun stableIds(): List<String> = HomeWidgetId.entries.map { it.stableId }
}

/**
 * Renders the requested widgets in order. Unknown ids are replaced by the
 * registry fallback, so a stale id can never blank the Home screen.
 */
@Composable
fun AiluaWidgetHost(
    widgetIds: List<HomeWidgetId>,
    context: WidgetHostContext,
    modifier: Modifier = Modifier,
    layout: WidgetHostLayout = WidgetHostLayout.Stack,
    spacing: Dp = 14.dp
) {
    val resolved = widgetIds.map { WidgetRegistry.resolve(it.stableId) }
    when (layout) {
        WidgetHostLayout.Stack -> Column(
            modifier = modifier.testTag("home_widget_host"),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            resolved.forEach { spec -> spec.content(context) }
        }
        WidgetHostLayout.Row -> Row(
            modifier = modifier.testTag("home_widget_host"),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            resolved.forEach { spec ->
                Box(modifier = Modifier.weight(1f)) {
                    spec.content(context)
                }
            }
        }
    }
}
