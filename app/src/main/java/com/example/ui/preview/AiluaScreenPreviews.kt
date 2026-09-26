package com.example.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import com.example.data.model.WorldClock
import com.example.ui.apps.AppLibraryScreen
import com.example.ui.call.CallScreen
import com.example.ui.call.IncomingCallScreen
import com.example.ui.components.HomeThemeCatalog
import com.example.ui.components.ThemePickerContent
import com.example.ui.components.themeWallpaper
import com.example.ui.home.AiluaWidgetHost
import com.example.ui.home.HomeWidgetId
import com.example.ui.home.VirtualHomeScreen
import com.example.ui.home.WidgetHostContext
import com.example.ui.home.WidgetHostLayout
import com.example.ui.home.WidgetRegistry
import com.example.ui.living.LivingScreen
import com.example.ui.mailbox.MailboxScreen
import com.example.ui.moments.MomentsScreen

// ==========================================
// 1. VirtualHomeScreen Previews
// ==========================================

@Preview(
    name = "VirtualHomeScreen - Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewVirtualHomeScreenLight() {
    AiluaPreviewDevice(isDarkTheme = false) {
        VirtualHomeScreen(
            character = PreviewFixtures.sampleMira,
            isDarkTheme = false
        )
    }
}

@Preview(
    name = "VirtualHomeScreen - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewVirtualHomeScreenDark() {
    AiluaPreviewDevice(isDarkTheme = true) {
        VirtualHomeScreen(
            character = PreviewFixtures.sampleMira,
            isDarkTheme = true
        )
    }
}

@Preview(
    name = "VirtualHomeScreen - Edit Mode - Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewVirtualHomeEditModeLight() {
    AiluaPreviewDevice(isDarkTheme = false) {
        VirtualHomeScreen(
            character = PreviewFixtures.sampleMira,
            isDarkTheme = false,
            initialEditing = true
        )
    }
}

@Preview(
    name = "VirtualHomeScreen - Edit Mode - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewVirtualHomeEditModeDark() {
    AiluaPreviewDevice(isDarkTheme = true) {
        VirtualHomeScreen(
            character = PreviewFixtures.sampleMira,
            isDarkTheme = true,
            initialEditing = true
        )
    }
}

// ==========================================
// 2. LivingScreen Previews
// ==========================================

@Preview(
    name = "LivingScreen - Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewLivingScreenLight() {
    AiluaPreviewDevice(isDarkTheme = false) {
        LivingScreen(
            character = PreviewFixtures.sampleMira,
            isDarkTheme = false,
            worldClockOverride = WorldClock(
                minutesOfDay = 14 * 60,
                dayPhase = DayPhase.AFTERNOON,
                weather = WeatherState.CLEAR
            )
        )
    }
}

@Preview(
    name = "LivingScreen - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewLivingScreenDark() {
    AiluaPreviewDevice(isDarkTheme = true) {
        LivingScreen(
            character = PreviewFixtures.sampleMira,
            isDarkTheme = true,
            worldClockOverride = WorldClock(
                minutesOfDay = 21 * 60 + 40,
                dayPhase = DayPhase.NIGHT,
                weather = WeatherState.RAIN
            )
        )
    }
}

@Preview(
    name = "LivingScreen - Starry Snow Theme",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewLivingScreenStarrySnowTheme() {
    AiluaPreviewDevice(isDarkTheme = true) {
        LivingScreen(
            character = PreviewFixtures.sampleMira,
            isDarkTheme = true,
            worldClockOverride = WorldClock(
                minutesOfDay = 1 * 60 + 20,
                dayPhase = DayPhase.LATE_NIGHT,
                weather = WeatherState.SNOW
            )
        )
    }
}

// ==========================================
// 3. MailboxScreen Previews
// ==========================================

@Preview(
    name = "MailboxScreen - Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewMailboxScreenLight() {
    AiluaPreviewDevice(isDarkTheme = false) {
        MailboxScreen(
            isDarkTheme = false
        )
    }
}

@Preview(
    name = "MailboxScreen - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewMailboxScreenDark() {
    AiluaPreviewDevice(isDarkTheme = true) {
        MailboxScreen(
            isDarkTheme = true
        )
    }
}

// ==========================================
// 4. MomentsScreen Previews
// ==========================================

@Preview(
    name = "MomentsScreen - Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewMomentsScreenLight() {
    AiluaPreviewDevice(isDarkTheme = false) {
        MomentsScreen(
            isDarkTheme = false
        )
    }
}

@Preview(
    name = "MomentsScreen - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewMomentsScreenDark() {
    AiluaPreviewDevice(isDarkTheme = true) {
        MomentsScreen(
            isDarkTheme = true
        )
    }
}

// ==========================================
// 5. IncomingCallScreen Previews
// ==========================================

@Preview(
    name = "IncomingCallScreen - Ringing",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewIncomingCallScreen() {
    AiluaPreviewDevice(isDarkTheme = true) {
        IncomingCallScreen(
            previewSession = PreviewFixtures.sampleIncomingCall
        )
    }
}

// ==========================================
// 6. CallScreen Previews
// ==========================================

@Preview(
    name = "CallScreen - Connected",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewCallScreen() {
    AiluaPreviewDevice(isDarkTheme = true) {
        CallScreen(
            previewCallSession = PreviewFixtures.sampleActiveCall
        )
    }
}

// ==========================================
// 7. AppLibraryScreen Previews
// ==========================================

@Preview(
    name = "AppLibraryScreen - Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewAppLibraryScreenLight() {
    AiluaPreviewDevice(isDarkTheme = false) {
        AppLibraryScreen(
            isDarkTheme = false
        )
    }
}

@Preview(
    name = "AppLibraryScreen - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewAppLibraryScreenDark() {
    AiluaPreviewDevice(isDarkTheme = true) {
        AppLibraryScreen(
            isDarkTheme = true
        )
    }
}

// ==========================================
// 8. Theme Picker Previews
// ==========================================

@Preview(
    name = "ThemePicker - Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewThemePickerLight() {
    AiluaPreviewDevice(isDarkTheme = false) {
        ThemePickerContent(
            selectedId = "follow",
            onSelect = {}
        )
    }
}

@Preview(
    name = "ThemePicker - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewThemePickerDark() {
    AiluaPreviewDevice(isDarkTheme = true) {
        ThemePickerContent(
            selectedId = "sakura",
            onSelect = {}
        )
    }
}

// ==========================================
// 9. Home Widget Host Previews
// ==========================================

private fun sampleWidgetContext(accentThemeId: String = "follow"): WidgetHostContext {
    val clock = WorldHeartbeatEngine.worldClock.value
    return WidgetHostContext(
        character = PreviewFixtures.sampleMira,
        accent = HomeThemeCatalog.byId(accentThemeId).accent,
        worldClock = clock,
        heartbeatState = WorldHeartbeatEngine.heartbeatState.value,
        onOpenDevTime = {},
        onOpenLiving = {},
        onOpenChat = {},
        onOpenProfile = {},
        onNavigateToMemories = {}
    )
}

private fun sampleWallpaperColors(themeId: String, isDarkTheme: Boolean): List<Color> {
    val clock = WorldHeartbeatEngine.worldClock.value
    return themeWallpaper(
        theme = HomeThemeCatalog.byId(themeId),
        dayPhase = clock.dayPhase,
        weather = clock.weather,
        isDarkTheme = isDarkTheme
    ).colors
}

@Preview(
    name = "Home Widgets - World Clock - Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewHomeWidgetWorldClock() {
    AiluaPreviewDevice(isDarkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(sampleWallpaperColors("follow", false)))
        ) {
            AiluaWidgetHost(
                widgetIds = listOf(HomeWidgetId.WORLD_CLOCK),
                context = sampleWidgetContext(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            )
        }
    }
}

@Preview(
    name = "Home Widgets - Character - Light",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewHomeWidgetCharacter() {
    AiluaPreviewDevice(isDarkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(sampleWallpaperColors("follow", false)))
        ) {
            AiluaWidgetHost(
                widgetIds = listOf(HomeWidgetId.CHARACTER_LIVING),
                context = sampleWidgetContext(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            )
        }
    }
}

@Preview(
    name = "Home Widgets - Host - Dark",
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewHomeWidgetHostDark() {
    AiluaPreviewDevice(isDarkTheme = true) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(sampleWallpaperColors("follow", true)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AiluaWidgetHost(
                    widgetIds = WidgetRegistry.defaultOrder.take(2),
                    context = sampleWidgetContext()
                )
                AiluaWidgetHost(
                    widgetIds = WidgetRegistry.defaultOrder.drop(2),
                    context = sampleWidgetContext(),
                    layout = WidgetHostLayout.Row,
                    spacing = 12.dp
                )
            }
        }
    }
}

@Preview(
    name = "Home Widgets - Host - Starry",
    showBackground = true,
    device = "spec:width=411dp,height=891dp"
)
@Composable
fun PreviewHomeWidgetHostStarry() {
    AiluaPreviewDevice(isDarkTheme = false) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(sampleWallpaperColors("starry", false)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AiluaWidgetHost(
                    widgetIds = WidgetRegistry.defaultOrder.take(2),
                    context = sampleWidgetContext(accentThemeId = "starry")
                )
                AiluaWidgetHost(
                    widgetIds = WidgetRegistry.defaultOrder.drop(2),
                    context = sampleWidgetContext(accentThemeId = "starry"),
                    layout = WidgetHostLayout.Row,
                    spacing = 12.dp
                )
            }
        }
    }
}
