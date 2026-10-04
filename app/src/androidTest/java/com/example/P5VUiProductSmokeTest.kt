package com.example

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.context.CharacterContext
import com.example.data.desktop.CellRect
import com.example.data.desktop.DesktopContainer
import com.example.data.desktop.DesktopItem
import com.example.data.desktop.DesktopItemType
import com.example.data.desktop.WidgetPlacement
import com.example.data.desktop.WorkspaceGraph
import com.example.data.engine.CallStateEngine
import com.example.data.firstsession.FirstSessionStore
import com.example.ui.launcher.LauncherAppCatalog
import com.example.ui.systemui.VirtualSystemUiSession
import com.example.ui.themecenter.ThemeLabTools
import com.example.ui.themecenter.withImportedTheme
import com.example.ui.themeengine.ThemeSelection
import com.example.ui.themeengine.ThemeStore
import com.example.ui.themeengine.ThemeResolver
import com.example.ui.themeengine.WallpaperCatalog
import com.example.ui.themeengine.external.ImportedThemeSource
import com.example.ui.themeengine.external.UnifiedThemeImporter
import com.example.data.model.DayPhase
import com.example.data.model.WeatherState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Opt-in product QA: current user content, reversible UI gestures, no AI/provider requests. */
@RunWith(AndroidJUnit4::class)
class P5VUiProductSmokeTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val controller get() = VirtualSystemUiSession.controller
    private val output get() = File(checkNotNull(context.getExternalFilesDir(null)), "qa/P5.V-UIProduct")
        .apply { mkdirs() }

    @Test fun workspaceGestureAndRestore() {
        assumeTrue(FirstSessionStore.state.value.onboardingComplete)
        assumeTrue(CallStateEngine.currentCall.value == null)
        compose.runOnIdle { controller.unlock() }
        goHome(); findWidgetPage()
        // Explicit host coordinates only recover an interrupted QA gesture, through the real UI.
        val args = InstrumentationRegistry.getArguments()
        args.getString("qa_restore_source")?.let { source ->
            val x = checkNotNull(args.getString("qa_restore_x")).toInt()
            val y = checkNotNull(args.getString("qa_restore_y")).toInt()
            require(x in 0..3 && y in 0..5)
            val item = WorkspaceGraph.repository.snapshot().items.single {
                it.sourceId == source && it.container == DesktopContainer.WORKSPACE
            }
            drag(item, x, y)
            compose.waitUntil(5_000) { WorkspaceGraph.repository.snapshot().items.single { it.id == item.id }
                .let { it.cellX == x && it.cellY == y } }
            if (visible("home_edit_done")) compose.onNodeWithTag("home_edit_done").performClick()
        }
        verifyDragAndRestore()
    }

    @Test fun captureLoadedThemePreview() {
        assumeTrue(FirstSessionStore.state.value.onboardingComplete)
        assumeTrue(CallStateEngine.currentCall.value == null)
        val original = ThemeStore.selection
        try {
            compose.runOnIdle { controller.unlock(); ThemeStore.update(ThemeSelection("default")) }
            goHome(); findWidgetPage(); openThemes()
            compose.waitUntil(15_000) { visible("theme_wallpaper_loaded_builtin_default", unmerged = true) }
            screenshot("07-theme-center.png")
            captureSoftThemeDetail()
            closeThemes()
        } finally {
            compose.runOnIdle { ThemeStore.update(original); controller.unlock() }
        }
    }

    @Test fun consumerCenterSystemAndFrozenWorkspace() {
        assumeTrue("An already onboarded phone is required", FirstSessionStore.state.value.onboardingComplete)
        assumeTrue("Do not disturb a current call", CallStateEngine.currentCall.value == null)
        val original = ThemeStore.selection
        val originalCharacter = CharacterContext.selectedId.value
        val originalWorkspace = WorkspaceGraph.repository.snapshot()
        val prefs = context.getSharedPreferences("ailua_settings", 0)
        val hadDeveloper = prefs.contains("developer")
        val originalDeveloper = prefs.getBoolean("developer", false)
        compose.runOnIdle { controller.unlock() }
        goHome()
        try {
            findWidgetPage()
            openThemes()
            compose.onNodeWithTag("theme_option_soft_home").performScrollTo().performClick()
            compose.onNodeWithTag("theme_apply_full").performScrollTo().performClick()
            assertEquals("soft_home", ThemeStore.selection.themePresetId)
            assertEquals(original.iconSourceOverrideId, ThemeStore.selection.iconSourceOverrideId)
            assertEquals(original.manualIconOverrides, ThemeStore.selection.manualIconOverrides)
            closeThemes()
            record("Full theme retains user's explicit external/manual icons: PASS")

            compose.runOnIdle { ThemeStore.update(ThemeSelection(themePresetId = "default")) }
            openThemes()
            screenshot("07-theme-center.png")
            captureSoftThemeDetail()
            compose.onNodeWithTag("theme_apply_wallpaper").performScrollTo().performClick()
            assertEquals("default", ThemeStore.selection.themePresetId)
            assertEquals("soft_home", ThemeStore.selection.wallpaperOverrideId)
            Espresso.pressBack()
            compose.onNodeWithTag("theme_center_sheet").assertIsDisplayed()
            assertTrue(!visible("theme_detail"))
            compose.onNodeWithTag("theme_center_tab_wallpapers").performClick()
            compose.waitUntil(15_000) { visible("theme_wallpaper_loaded_builtin_oil_rose_garden", unmerged = true) }
            screenshot("09-wallpaper-center.png")
            compose.onNodeWithTag("wallpaper_option_default").performScrollTo().performClick()
            compose.onNodeWithTag("wallpaper_preview").assertIsDisplayed()
            compose.onNodeWithTag("wallpaper_apply").performScrollTo().performClick()
            assertEquals("default", ThemeStore.selection.themePresetId)
            assertEquals("default", ThemeStore.selection.wallpaperOverrideId)
            Espresso.pressBack()
            compose.onNodeWithTag("theme_center_sheet").assertIsDisplayed()
            assertTrue(!visible("wallpaper_preview"))
            val beforeOils = WorkspaceGraph.repository.snapshot()
            for (option in WallpaperCatalog.options.filter { it.id.startsWith("oil_") }) {
                compose.onNodeWithTag("wallpaper_option_${option.id}").performScrollTo().performClick()
                compose.waitUntil(15_000) { visible("theme_wallpaper_loaded_builtin_${option.id}", unmerged = true) }
                screenshot("wallpaper-${option.id}.png")
                compose.onNodeWithTag("wallpaper_apply").performScrollTo().performClick()
                assertEquals("default", ThemeStore.selection.themePresetId)
                assertEquals(option.id, ThemeStore.selection.wallpaperOverrideId)
                Espresso.pressBack()
                compose.onNodeWithTag("theme_center_sheet").assertIsDisplayed()
            }
            assertEquals(beforeOils.items, WorkspaceGraph.repository.snapshot().items)
            record("Six oil wallpapers via actual preview/apply, shell and Workspace retained: PASS")
            compose.onNodeWithTag("theme_center_tab_icons").performClick()
            screenshot("10-icon-center.png")
            compose.onNodeWithTag("icon_option_y2k_icons").performScrollTo().performClick()
            compose.onNodeWithTag("icon_preview").assertIsDisplayed()
            compose.onNodeWithTag("icon_apply").performScrollTo().performClick()
            assertEquals("default", ThemeStore.selection.themePresetId)
            assertEquals("y2k_icons", ThemeStore.selection.iconStyleOverrideId)
            Espresso.pressBack()
            compose.onNodeWithTag("theme_center_sheet").assertIsDisplayed()
            assertTrue(!visible("icon_preview"))
            compose.onNodeWithTag("theme_center_tab_mine").performClick()
            screenshot("11-my-theme.png")
            closeThemes()
            record("Wallpaper-only and icon-only preserve the current shell: PASS")

            openLibrary()
            screenshot("13-app-library.png")
            compose.onNodeWithTag("settings_entry").performClick()
            compose.onNodeWithTag("settings_screen").assertIsDisplayed()
            if (!originalDeveloper) repeat(7) {
                compose.onNodeWithTag("settings_about").performScrollTo().performClick()
            }
            compose.onNodeWithTag("settings_developer").performScrollTo().performClick()
            compose.onNodeWithTag("developer_theme_lab").performClick()
            compose.onNodeWithTag("theme_lab_sheet").assertIsDisplayed()
            screenshot("12-theme-lab.png")
            compose.onNodeWithTag("theme_lab_inspect").performScrollTo().performClick()
            compose.waitUntil(10_000) { visible("theme_asset_inspector") }
            screenshot("theme-asset-inspector.png")
            Espresso.pressBack()
            compose.onNodeWithTag("theme_lab_sheet").assertIsDisplayed()
            assertTrue(!visible("theme_asset_inspector"))
            Espresso.pressBack()
            goHome()
            verifyActualExport(original)

            compose.runOnIdle { ThemeStore.update(ThemeSelection(themePresetId = "midnight_glass")) }
            pullStatusBar(true)
            screenshot("16-control-center.png")
            compose.onNodeWithTag("control_lock").performClick()
            compose.onNodeWithTag("virtual_lock_screen").assertIsDisplayed()
            screenshot("14-lockscreen.png")
            compose.onNodeWithTag("lock_unlock").performClick()
            pullStatusBar(false)
            compose.onNodeWithTag("notification_shade").assertIsDisplayed()
            screenshot("15-notification.png")
            compose.onNodeWithContentDescription("收起通知中心").performClick()
            record("Control Center, lock/unlock, notification shade: PASS")

            compose.runOnIdle { ThemeStore.update(ThemeSelection(themePresetId = "default")) }
            openApp("chat", "conversation_list_screen")
            compose.onNodeWithTag("conv_item_conv_$originalCharacter").performClick()
            compose.onNodeWithTag("chat_screen").assertIsDisplayed()
            compose.onNodeWithTag("chat_text_input").performClick().performTextReplacement("UI visual QA")
            compose.onNodeWithTag("chat_text_input").performTextReplacement("")
            Espresso.closeSoftKeyboard()
            screenshot("17-chat.png")
            goHome()
            openApp("living", "living_screen")
            screenshot("18-living.png")
            goHome()
            findWidgetPage()
            verifyResize()
            verifyDragAndRestore()
            assertEquals(originalWorkspace.pages, WorkspaceGraph.repository.snapshot().pages)
            assertEquals(originalWorkspace.items, WorkspaceGraph.repository.snapshot().items)
            assertEquals(originalWorkspace.folders, WorkspaceGraph.repository.snapshot().folders)
            assertEquals(originalCharacter, CharacterContext.selectedId.value)
        } catch (error: Throwable) {
            runCatching { screenshot("failure-product.png") }
            record("FAIL: ${error.javaClass.simpleName}: ${error.message}")
            throw error
        } finally {
            compose.runOnIdle { ThemeStore.update(original); controller.unlock() }
            prefs.edit().apply {
                if (hadDeveloper) putBoolean("developer", originalDeveloper) else remove("developer")
            }.commit()
        }
        assertEquals(original, ThemeStore.selection)
    }

    private fun verifyActualExport(original: ThemeSelection) {
        for ((label, selection) in listOf(
            "current-mix" to ThemeStore.selection, "original-mix" to original,
            "milk" to ThemeSelection("milk"), "glass" to ThemeSelection("glass"),
            "diary" to ThemeSelection("diary"), "mono" to ThemeSelection("mono"),
            "default-mono" to ThemeSelection("default", iconStyleOverrideId = "mono"),
            "midnight-oil" to ThemeSelection("midnight_glass", wallpaperOverrideId = "oil_rose_garden"),
            "soft-midnight" to ThemeSelection("soft_home", wallpaperOverrideId = "midnight_glass"),
        )) {
            val runtime = ThemeResolver.resolve(selection, false, DayPhase.NOON, WeatherState.RAIN)
            val file = File(output, "$label.ailuatheme")
            var bitmapCount = 0
            runBlocking {
                val inspected = ThemeLabTools.inspect(context, selection, runtime)
                assertEquals(19, inspected.size)
                val bitmaps = inspected.filter { it.isBitmap }
                assertTrue(bitmaps.all { it.width > 0 && it.height > 0 && it.sha256.length == 64 })
                assertTrue(inspected.filterNot { it.isBitmap }.all { it.source.contains("fallback") })
                bitmapCount = bitmaps.size - 1
                file.outputStream().use { ThemeLabTools.export(context, selection, runtime, it) }
            }
            val imported = UnifiedThemeImporter.inspect(ImportedThemeSource(file.name, file.readBytes()))
            assertEquals(bitmapCount, imported.theme.icons?.mappings?.size ?: 0)
            assertEquals(1, imported.theme.wallpapers.size)
            assertEquals(selection.themePresetId, imported.theme.basePresetId)
            val restored = ThemeSelection("midnight_glass").withImportedTheme(imported.theme)
            val after = ThemeResolver.resolve(restored, false, DayPhase.NOON, WeatherState.RAIN)
            assertEquals(runtime.widgets, after.widgets)
            assertEquals(runtime.dock, after.dock)
            assertEquals(runtime.text.display.fontFamily, after.text.display.fontFamily)
            assertEquals(runtime.icons.labelColor, after.icons.labelColor)
            assertEquals(runtime.statusBar, after.statusBar)
            assertEquals(runtime.lockscreen, after.lockscreen)
            assertEquals(ThemeResolver.resolveIconStyleId(selection), ThemeResolver.resolveIconStyleId(restored))
            record("Actual Android export→import→shell/icon-style restore $label ($bitmapCount bitmaps): PASS")
        }
    }

    private fun verifyResize() {
        val before = WorkspaceGraph.repository.snapshot()
        val widget = before.items.first { it.sourceId == "character_living" }
        val sizes = WidgetPlacement.supportedSizes.getValue(widget.sourceId)
        openEditing()
        compose.onNodeWithTag("workspace_widget_${widget.id}").performClick()
        try {
            repeat(sizes.size) {
                val old = WorkspaceGraph.repository.snapshot().items.single { it.id == widget.id }
                compose.onNodeWithTag("widget_resize").performClick()
                compose.waitUntil(5_000) {
                    WorkspaceGraph.repository.snapshot().items.single { it.id == widget.id }
                        .let { it.spanX != old.spanX || it.spanY != old.spanY }
                }
            }
        } finally {
            repeat(sizes.size) {
                val actual = WorkspaceGraph.repository.snapshot().items.single { it.id == widget.id }
                if ((actual.spanX != widget.spanX || actual.spanY != widget.spanY) && visible("widget_resize")) {
                    compose.onNodeWithTag("widget_resize").performClick()
                    compose.waitForIdle()
                }
            }
            if (visible("home_edit_done")) compose.onNodeWithTag("home_edit_done").performClick()
        }
        assertEquals(before.items, WorkspaceGraph.repository.snapshot().items)
        record("Production Widget resize cycle and original span restore: PASS")
    }

    private fun verifyDragAndRestore() {
        val before = WorkspaceGraph.repository.snapshot()
        val item = before.items.first { it.sourceId == "relations" && it.container == DesktopContainer.WORKSPACE }
        val occupied = before.itemsFor(checkNotNull(item.pageId)).map { CellRect(it.cellX, it.cellY, it.spanX, it.spanY) }
        val destination = (listOf(item.cellY) + (0..5).filter { it != item.cellY })
            .flatMap { y -> (3 downTo 0).map { x -> CellRect(x, y, 1, 1) } }
            .first { candidate -> occupied.none(candidate::overlaps) }
        try {
            drag(item, destination.x, destination.y)
            compose.waitUntil(5_000) { WorkspaceGraph.repository.snapshot().items.single { it.id == item.id }
                .let { it.cellX == destination.x && it.cellY == destination.y } }
            screenshot("drag-icon-moved.png")
        } finally {
            val current = WorkspaceGraph.repository.snapshot().items.single { it.id == item.id }
            if (current.cellX != item.cellX || current.cellY != item.cellY) {
                drag(current, item.cellX, item.cellY)
                compose.waitUntil(5_000) { WorkspaceGraph.repository.snapshot().items.single { it.id == item.id } == item }
            }
            if (visible("home_edit_done")) compose.onNodeWithTag("home_edit_done").performClick()
        }
        assertEquals(before.items, WorkspaceGraph.repository.snapshot().items)
        record("Actual icon long-press drag→empty cell→drag back, exact layout: PASS")
    }

    private fun drag(item: DesktopItem, toX: Int, toY: Int) {
        val home = compose.onNodeWithTag("virtual_home_screen")
        val root = home.fetchSemanticsNode().boundsInRoot
        val gridNodes = compose.onAllNodesWithTag("home_app_grid")
        val index = gridNodes.fetchSemanticsNodes().indices.first { gridNodes[it].isDisplayed() }
        val grid = gridNodes[index].fetchSemanticsNode()
        // The scroll viewport clips boundsInRoot. Cell geometry uses full layout size.
        val origin = grid.positionInRoot
        val start = Offset(origin.x + grid.size.width * (item.cellX + 0.5f) / 4 - root.left,
            origin.y + grid.size.height * (item.cellY + 0.5f) / 6 - root.top)
        val end = Offset(origin.x + grid.size.width * (toX + 0.5f) / 4 - root.left,
            origin.y + grid.size.height * (toY + 0.5f) / 6 - root.top)
        record("Drag ${item.sourceId} ${item.cellX},${item.cellY}→$toX,$toY; $start→$end, grid=${grid.size}")
        try {
        home.performTouchInput {
            down(start)
            advanceEventTime(android.view.ViewConfiguration.getLongPressTimeout().toLong() + 100)
            moveTo(start + Offset(4f, 0f), delayMillis = 32)
        }
        compose.waitForIdle()
        home.performTouchInput { moveTo(end, delayMillis = 450) }
        compose.waitForIdle()
        home.performTouchInput { up() }
        compose.waitForIdle()
        } finally {
            home.performTouchInput { if (currentPosition(0) != null) cancel() }
        }
    }

    private fun openThemes() { openEditing(); compose.onNodeWithTag("home_edit_theme").performClick() }
    private fun captureSoftThemeDetail() {
        compose.onNodeWithTag("theme_option_soft_home").performScrollTo().performClick()
        compose.waitUntil(15_000) { visible("theme_wallpaper_loaded_builtin_soft_home", unmerged = true) }
        screenshot("08-theme-detail.png")
    }
    private fun openEditing() = compose.onNodeWithTag("virtual_home_screen").performTouchInput {
        longClick(Offset(width * 0.90f, height * 0.45f))
    }
    private fun closeThemes() {
        if (compose.onAllNodesWithTag("theme_detail").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("theme_detail_back").performScrollTo().performClick()
        }
        Espresso.pressBack()
        compose.onNodeWithTag("home_edit_done").performClick()
    }
    private fun findWidgetPage() {
        repeat(4) { if (!visible("living_character_widget")) {
            compose.onNodeWithTag("virtual_home_screen").performTouchInput { swipeLeft() }; compose.waitForIdle()
        } }
        repeat(4) { if (!visible("living_character_widget")) {
            compose.onNodeWithTag("virtual_home_screen").performTouchInput { swipeRight() }; compose.waitForIdle()
        } }
        compose.onNodeWithTag("living_character_widget").assertIsDisplayed()
    }
    private fun openLibrary() {
        val icons = compose.onAllNodesWithTag("app_icon_apps")
        icons[0].performClick()
        compose.onNodeWithTag("app_library_screen").assertIsDisplayed()
    }
    private fun openApp(id: String, tag: String) {
        openLibrary()
        val app = LauncherAppCatalog.drawerApps().single { it.id == id }
        compose.onNodeWithTag("app_library_search").performTextReplacement(app.name)
        Espresso.closeSoftKeyboard()
        compose.onNode(hasTestTag("app_icon_${app.iconKey}") and hasAnyAncestor(hasTestTag("app_library_item_$id")))
            .performClick()
        compose.waitUntil(10_000) { visible(tag) }
    }
    private fun goHome() {
        if (!visible("virtual_home_screen") && visible("virtual_phone_home_indicator")) {
            compose.onNodeWithTag("virtual_phone_home_indicator").performClick()
        }
        compose.waitUntil(10_000) { visible("virtual_home_screen") }
    }
    private fun pullStatusBar(right: Boolean) = compose.onNodeWithTag("virtual_home_screen").performTouchInput {
        val x = width * if (right) 0.85f else 0.2f
        swipe(Offset(x, height * 0.015f), Offset(x, height * 0.4f), 450)
    }
    private fun visible(tag: String, unmerged: Boolean = false) = compose.onAllNodesWithTag(tag, useUnmergedTree = unmerged)
        .fetchSemanticsNodes().indices.any { compose.onAllNodesWithTag(tag, useUnmergedTree = unmerged)[it].isDisplayed() }
    private fun screenshot(name: String) {
        compose.waitForIdle(); SystemClock.sleep(700); compose.mainClock.advanceTimeBy(100); compose.waitForIdle()
        val bitmap = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
        try { File(output, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
        finally { bitmap.recycle() }
    }
    private fun record(message: String) = File(output, "product-smoke-events.txt").appendText(message + "\n")
}
