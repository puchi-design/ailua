package com.example.ui.preview

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.ui.apps.AppLibraryScreen
import com.example.ui.call.CallScreen
import com.example.ui.call.IncomingCallScreen
import com.example.ui.components.ThemePickerContent
import com.example.ui.home.VirtualHomeScreen
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
            isDarkTheme = false
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
            isDarkTheme = true
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
