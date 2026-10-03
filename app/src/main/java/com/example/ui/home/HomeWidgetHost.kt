package com.example.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.engine.WorldHeartbeatState
import com.example.data.engine.WorldStateRepository
import com.example.data.model.CharacterProfile
import com.example.data.model.WorldClock
import com.example.data.model.isUserActivity
import com.example.data.model.sortedChronologically
import com.example.data.projection.projectPresence
import com.example.data.desktop.WidgetPlacement
import com.example.ui.components.BondProgressWidget
import com.example.ui.components.MemorySnippetWidget
import com.example.ui.themeengine.LocalAiluaTheme

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
data class WidgetSize(val spanX: Int, val spanY: Int)

data class WidgetSpec(
    val id: HomeWidgetId,
    val title: String,
    val defaultSize: WidgetSize,
    val supportedSizes: List<WidgetSize>,
    val content: @Composable (WidgetHostContext, WidgetSize) -> Unit
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

    private fun sizes(id: HomeWidgetId) = WidgetPlacement.supportedSizes.getValue(id.stableId)
        .map { WidgetSize(it.first, it.second) }

    private val specs: Map<String, WidgetSpec> = listOf(
        WidgetSpec(HomeWidgetId.CHARACTER_LIVING, "角色近况", WidgetSize(4, 2),
            sizes(HomeWidgetId.CHARACTER_LIVING)) { context, size ->
            val theme = LocalAiluaTheme.current
            if (size.spanX == 2) {
                val events by WorldStateRepository.events.collectAsStateWithLifecycle()
                val presence = remember(context.character, events) { projectPresence(context.character, events) }
                val latest = remember(context.character.id, events) {
                    events.filter { it.characterId == context.character.id && !it.isUserActivity() }
                        .sortedChronologically().lastOrNull()
                }
                Column(Modifier.fillMaxWidth().clickable { context.onOpenLiving() },
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(context.character.name, style = theme.text.section, color = theme.widgets.foregroundColor,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(presence.currentActivity, style = theme.text.secondary,
                        color = theme.widgets.foregroundColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(latest?.description ?: context.character.contextualQuote, style = theme.text.secondary,
                        color = theme.widgets.foregroundColor.copy(alpha = 0.78f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("发消息", style = theme.text.caption, color = theme.widgets.foregroundColor,
                        modifier = Modifier.clickable { context.onOpenChat() })
                }
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    LivingPresenceStrip(context.character, context.onOpenChat,
                        context.onOpenLiving, context.onOpenProfile, compact = size.spanY == 1)
                }
            }
        },
        WidgetSpec(HomeWidgetId.WORLD_CLOCK, "时间与天气", WidgetSize(4, 1),
            sizes(HomeWidgetId.WORLD_CLOCK)) { context, size ->
            val theme = LocalAiluaTheme.current
            if (size.spanX == 4) {
                Row(Modifier.fillMaxWidth().clickable { context.onOpenDevTime() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(context.worldClock.timeFormatted,
                        style = theme.text.display, color = theme.widgets.foregroundColor)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(context.worldClock.dateLabel,
                            style = theme.text.secondary, color = theme.widgets.foregroundColor, maxLines = 1)
                        Text("${context.worldClock.dayPhase.label} · ${context.worldClock.weather.label}",
                            style = theme.text.caption, color = theme.widgets.foregroundColor.copy(alpha = 0.78f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            } else {
                Column(Modifier.clickable { context.onOpenDevTime() }) {
                    Text(context.worldClock.timeFormatted,
                        style = theme.text.display, color = theme.widgets.foregroundColor)
                    Text(context.worldClock.weather.label, style = theme.text.secondary,
                        color = theme.widgets.foregroundColor.copy(alpha = 0.78f))
                    if (size.spanY == 2) {
                        Spacer(Modifier.height(8.dp))
                        Text(context.worldClock.dateLabel, style = theme.text.caption,
                            color = theme.widgets.foregroundColor.copy(alpha = 0.78f))
                    }
                }
            }
        },
        WidgetSpec(HomeWidgetId.MEMORY_ECHO, "记忆回响", WidgetSize(2, 2),
            sizes(HomeWidgetId.MEMORY_ECHO)) { context, size ->
            val theme = LocalAiluaTheme.current
            if (size.spanX == 4) {
                Column(Modifier.clickable { context.onNavigateToMemories() },
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("记忆回响", style = theme.text.section,
                        color = theme.widgets.foregroundColor)
                    Text(context.character.memories.firstOrNull()?.snippet.orEmpty(),
                        style = theme.text.secondary, color = theme.widgets.foregroundColor.copy(alpha = 0.78f),
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            } else MemorySnippetWidget(
                title = "记忆回响",
                snippet = context.character.memories.firstOrNull()?.snippet.orEmpty(),
                onClick = context.onNavigateToMemories
            )
        },
        WidgetSpec(HomeWidgetId.BOND, "关系近况", WidgetSize(2, 2),
            sizes(HomeWidgetId.BOND)) { context, size ->
            val theme = LocalAiluaTheme.current
            if (size.spanY == 1) {
                Row(Modifier.fillMaxWidth().clickable { context.onOpenProfile() },
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("你们之间", style = theme.text.secondary, color = theme.widgets.foregroundColor)
                }
            } else BondProgressWidget(context.character, onClick = context.onOpenProfile)
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
            resolved.forEach { spec -> spec.content(context, spec.defaultSize) }
        }
        WidgetHostLayout.Row -> Row(
            modifier = modifier.testTag("home_widget_host"),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            resolved.forEach { spec ->
                Box(modifier = Modifier.weight(1f)) {
                    spec.content(context, spec.defaultSize)
                }
            }
        }
    }
}
