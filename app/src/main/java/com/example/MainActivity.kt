package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.ai.repository.ProviderGraph
import com.example.data.memory.repository.MemoryGraph
import com.example.data.context.CharacterContext
import com.example.data.engine.CallStateEngine
import com.example.data.engine.ProactiveGraph
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldPlanRuntime
import com.example.data.engine.WorldStateRepository
import com.example.data.local.AiluaLocalStore
import com.example.data.firstsession.FirstSessionStore
import com.example.data.model.CallAction
import com.example.data.model.CallState
import com.example.data.registry.CharacterRegistry
import com.example.data.relationship.repository.RelationshipStateRepository
import com.example.data.reality.RealityRepository
import com.example.navigation.AiluaDestinations
import com.example.navigation.AppRouter
import com.example.ui.apps.AppLibraryScreen
import com.example.ui.call.CallHistoryScreen
import com.example.ui.call.CallScreen
import com.example.ui.call.IncomingCallScreen
import com.example.ui.character.CharacterProfileScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ConversationListScreen
import com.example.ui.chat.GroupChatScreen
import com.example.ui.checkphone.CheckPhoneScreen
import com.example.ui.components.LocalOsChromeState
import com.example.ui.components.rememberOsChromeState
import com.example.ui.contacts.ContactsScreen
import com.example.ui.creator.CharacterCreatorScreen
import com.example.ui.diary.DiaryScreen
import com.example.ui.gallery.GalleryScreen
import com.example.ui.home.VirtualHomeScreen
import com.example.ui.living.LivingScreen
import com.example.ui.lore.WorldBookScreen
import com.example.ui.mailbox.MailboxScreen
import com.example.ui.memories.MemoriesScreen
import com.example.ui.motion.AppMotion
import com.example.ui.moments.MomentsScreen
import com.example.ui.onboarding.WelcomeScreen
import com.example.ui.relations.RelationsScreen
import com.example.ui.reality.RealityBridgeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.PrivacyScreen
import com.example.ui.theater.TheaterScreen
import com.example.ui.theme.AiluaTheme
import com.example.ui.world.WorldPlacesScreen
import com.example.data.systemui.lock.VirtualLockStore
import com.example.data.systemui.notification.VirtualNotificationGraph
import com.example.data.systemui.control.ControlCenterStore
import com.example.ui.systemui.VirtualSystemUiSession
import com.example.ui.systemui.VirtualSystemUiHost
import com.example.ui.themeengine.AiluaThemeProvider
import com.example.ui.themeengine.ThemeResolver
import com.example.ui.themeengine.ThemeStore
import com.example.ui.themeengine.external.ExternalThemeRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AiluaLocalStore.init(applicationContext)
        CharacterContext.init(applicationContext)
        FirstSessionStore.init(applicationContext)
        VirtualLockStore.initialize(applicationContext)
        ControlCenterStore.initialize(applicationContext)
        VirtualSystemUiSession.controller.initializeColdStart(
            FirstSessionStore.state.value.onboardingComplete,
            VirtualLockStore.lockOnColdStart.value
        )
        RealityRepository.init(applicationContext)
        RelationshipStateRepository.restore()
        ProviderGraph.init(applicationContext)
        MemoryGraph.init(applicationContext)
        VirtualNotificationGraph.init(applicationContext)
        WorldPlanRuntime.init(applicationContext)
        ProactiveGraph.init(applicationContext)
        WorldStateRepository.syncWithLocalStore()
        RelationshipStateRepository.rebuild(WorldStateRepository.events.value)
        CallStateEngine.syncWithLocalStore()
        lifecycleScope.launch { WorldPlanRuntime.maybePlan() }

        // Lightweight foreground-only world ticker (Step 6)
        // 60 real seconds -> advance virtual time by 1 minute
        // Stops automatically when Activity is not in RESUMED lifecycle state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    delay(60_000L)
                    WorldHeartbeatEngine.advanceTime(1)
                    WorldPlanRuntime.maybePlan()
                    // P3D-2: same heartbeat drives the proactive-message rule check.
                    ProactiveGraph.maybeFire()
                }
            }
        }

        enableEdgeToEdge()
        // Virtual OS status bar replaces the system status bar; hide the native
        // one so the two no longer overlap (swipe down reveals it transiently).
        hideNativeStatusBar()
        setContent {
            CompositionLocalProvider(LocalOsChromeState provides rememberOsChromeState()) {
                AiluaAppRoot()
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                VirtualSystemUiSession.controller.state.collect {
                    // A pull from the physical top edge can reveal Android's transient
                    // status bar without changing window focus. Reapply immersive mode
                    // after the virtual surface has opened, and after returning to the app.
                    window.decorView.post { hideNativeStatusBar() }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // System pickers can restore the native bar when this window regains focus.
        if (hasFocus) hideNativeStatusBar()
    }

    private fun hideNativeStatusBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
fun AiluaAppRoot() {
    val systemDark = isSystemInDarkTheme()
    val context = LocalContext.current
    remember(context) {
        ThemeStore.initialize(context)
        ExternalThemeRepository.initialize(context)
        true
    }
    val worldClock by WorldHeartbeatEngine.worldClock.collectAsStateWithLifecycle()
    val appearancePrefs = remember(context) { context.getSharedPreferences("ailua_settings", android.content.Context.MODE_PRIVATE) }
    var isDarkTheme by remember { mutableStateOf(appearancePrefs.getBoolean("dark_theme", systemDark)) }
    val themeRuntime = ThemeResolver.resolve(
        ThemeStore.selection, isDarkTheme, worldClock.dayPhase, worldClock.weather
    )
    LaunchedEffect(isDarkTheme) { appearancePrefs.edit().putBoolean("dark_theme", isDarkTheme).apply() }
    val navController = rememberNavController()
    val firstSession by FirstSessionStore.state.collectAsStateWithLifecycle()
    var postWelcomeRoute by remember { mutableStateOf<String?>(null) }

    // Step 2 & 3: Observe CallStateEngine as sole authority with lifecycle awareness
    val currentCall by CallStateEngine.currentCall.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    LaunchedEffect(firstSession.onboardingComplete, postWelcomeRoute) {
        val route = postWelcomeRoute
        if (firstSession.onboardingComplete && route != null) {
            navController.navigate(route)
            postWelcomeRoute = null
        }
    }
    LaunchedEffect(currentRoute) {
        when (currentRoute) {
            AiluaDestinations.LIVING -> FirstSessionStore.markLiving()
            AiluaDestinations.MOMENTS -> FirstSessionStore.markMoments()
            AiluaDestinations.CHECK_PHONE -> FirstSessionStore.markCheckPhone()
        }
    }

    // Active companion (CharacterContext bootstrap default: Mira) — profiles always
    // resolve through CharacterRegistry, never a hardcoded sample profile
    val selectedCharacterId by CharacterContext.selectedId.collectAsStateWithLifecycle()
    val selectedCharacter = remember(selectedCharacterId) {
        CharacterRegistry.getCharacter(selectedCharacterId)
    }

    // Reactively navigate to IncomingCallScreen exactly once when INCOMING state detected
    LaunchedEffect(currentCall?.id, currentCall?.state, currentRoute) {
        val call = currentCall
        if (call != null && call.state == CallState.INCOMING && currentRoute != AiluaDestinations.INCOMING_CALL) {
            navController.navigate(AiluaDestinations.INCOMING_CALL) {
                launchSingleTop = true
            }
        }
    }

    AiluaTheme(darkTheme = isDarkTheme) {
        AiluaThemeProvider(themeRuntime) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!firstSession.onboardingComplete) {
                WelcomeScreen { characterId, importCard ->
                    CharacterContext.select(characterId)
                    postWelcomeRoute = if (importCard) AiluaDestinations.CHARACTER_CREATOR else AiluaDestinations.chatRoute(characterId)
                    FirstSessionStore.completeOnboarding()
                }
                return@Surface
            }
            VirtualSystemUiHost(
                currentCall = currentCall,
                currentRoute = currentRoute,
                isDarkTheme = isDarkTheme,
                onToggleDarkMode = { isDarkTheme = !isDarkTheme },
                onLaunchRoute = { route -> navController.navigate(route) { launchSingleTop = true } },
            ) {
            NavHost(
                navController = navController,
                startDestination = AiluaDestinations.HOME,
                enterTransition = {
                    fadeIn(animationSpec = AppMotion.enterSpec()) +
                        scaleIn(
                            initialScale = AppMotion.OPEN_SCALE,
                            animationSpec = AppMotion.enterSpec()
                        )
                },
                exitTransition = {
                    scaleOut(
                        targetScale = AppMotion.CLOSE_SCALE,
                        animationSpec = AppMotion.exitSpec()
                    )
                },
                popEnterTransition = {
                    fadeIn(animationSpec = AppMotion.enterSpec()) +
                        scaleIn(
                            initialScale = AppMotion.OPEN_SCALE,
                            animationSpec = AppMotion.enterSpec()
                        )
                },
                popExitTransition = {
                    scaleOut(
                        targetScale = AppMotion.CLOSE_SCALE,
                        animationSpec = AppMotion.exitSpec()
                    )
                }
            ) {
                // Screen 1: Virtual OS Home Desktop
                composable(AiluaDestinations.HOME) {
                    VirtualHomeScreen(
                        character = selectedCharacter,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onNavigateToMessages = { navController.navigate(AiluaDestinations.MESSAGES) },
                        onNavigateToChat = { navController.navigate(AiluaDestinations.chatRoute(selectedCharacterId)) },
                        onNavigateToGroupChat = { navController.navigate(AiluaDestinations.GROUP_CHAT) },
                        onNavigateToContacts = { navController.navigate(AiluaDestinations.CONTACTS) },
                        onNavigateToRelations = { navController.navigate(AiluaDestinations.RELATIONS) },
                        onNavigateToCheckPhone = { navController.navigate(AiluaDestinations.CHECK_PHONE) },
                        onNavigateToDiary = { navController.navigate(AiluaDestinations.DIARY) },
                        onNavigateToMoments = { navController.navigate(AiluaDestinations.MOMENTS) },
                        onNavigateToLiving = { navController.navigate(AiluaDestinations.LIVING) },
                        onNavigateToMemories = { navController.navigate(AiluaDestinations.MEMORIES) },
                        onNavigateToApps = { preferredPageId ->
                            val route = preferredPageId?.let {
                                "${AiluaDestinations.APPS}?preferredPageId=${android.net.Uri.encode(it)}"
                            } ?: AiluaDestinations.APPS
                            navController.navigate(route)
                        },
                        onOpenProfile = { navController.navigate(AiluaDestinations.profileRoute(selectedCharacterId)) },
                        onNavigateToMailbox = { navController.navigate(AiluaDestinations.MAILBOX) },
                        onNavigateToCall = { navController.navigate(AiluaDestinations.callRoute(selectedCharacterId)) },
                        onNavigateToCallHistory = { navController.navigate(AiluaDestinations.CALL_HISTORY) },
                        onNavigateToGallery = { navController.navigate(AiluaDestinations.GALLERY) },
                        onAppClick = { appId ->
                            navController.navigate(
                                AppRouter.launchRouteOrNull(appId, selectedCharacterId)
                                    ?: AppRouter.resolve(appId)
                            )
                        }
                    )
                }

                // Screen 2: Conversation List
                composable(AiluaDestinations.MESSAGES) {
                    ConversationListScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onOpenMiraChat = {
                            navController.navigate(AiluaDestinations.chatRoute("mira"))
                        },
                        onOpenGroupChat = {
                            navController.navigate(AiluaDestinations.GROUP_CHAT)
                        },
                        onOpenContacts = {
                            navController.navigate(AiluaDestinations.CONTACTS)
                        },
                        onSelectCharacterChat = { charId ->
                            navController.navigate(AiluaDestinations.chatRoute(charId))
                        }
                    )
                }

                // Screen 3: Chat Screen
                composable(
                    route = "chat/{characterId}",
                    arguments = listOf(navArgument("characterId") {
                        type = NavType.StringType
                        defaultValue = "mira"
                    })
                ) { backStackEntry ->
                    val charId = backStackEntry.arguments?.getString("characterId") ?: "mira"
                    val character = CharacterRegistry.getCharacter(charId)
                    ChatScreen(
                        character = character,
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onOpenProfile = {
                            navController.navigate(AiluaDestinations.profileRoute(character.id))
                        }
                    )
                }

                // Screen 4: Group Chat Screen
                composable(AiluaDestinations.GROUP_CHAT) {
                    GroupChatScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() },
                        onOpenRelations = { navController.navigate(AiluaDestinations.RELATIONS) }
                    )
                }

                // Screen 5: Contacts List Screen
                composable(AiluaDestinations.CONTACTS) {
                    ContactsScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onOpenProfile = { charId ->
                            navController.navigate(AiluaDestinations.profileRoute(charId))
                        },
                        onStartPrivateChat = { charId ->
                            navController.navigate(AiluaDestinations.chatRoute(charId))
                        },
                        onOpenRelations = { navController.navigate(AiluaDestinations.RELATIONS) }
                    )
                }

                // Screen 6: Check Phone Screen
                composable(AiluaDestinations.CHECK_PHONE) {
                    CheckPhoneScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        characterId = selectedCharacterId,
                        characterName = selectedCharacter.name
                    )
                }

                // Screen 7: Diary Screen
                composable(AiluaDestinations.DIARY) {
                    DiaryScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        characterId = selectedCharacterId,
                        characterName = selectedCharacter.name
                    )
                }

                // Screen 8: Social Graph / Relations Screen
                composable(AiluaDestinations.RELATIONS) {
                    RelationsScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Screen 9: Moments Screen
                composable(AiluaDestinations.MOMENTS) {
                    MomentsScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onOpenProfile = { charId ->
                            navController.navigate(AiluaDestinations.profileRoute(charId))
                        }
                    )
                }

                // Screen 10: Living World Timeline Screen
                composable(AiluaDestinations.LIVING) {
                    LivingScreen(
                        character = selectedCharacter,
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onNavigateToChat = { navController.navigate(AiluaDestinations.chatRoute(selectedCharacter.id)) },
                        onNavigateToCall = { navController.navigate(AiluaDestinations.callRoute(selectedCharacter.id)) },
                        onNavigateToMailbox = { navController.navigate(AiluaDestinations.MAILBOX) },
                        onOpenProfile = { navController.navigate(AiluaDestinations.profileRoute(selectedCharacter.id)) }
                    )
                }

                // Screen 11: App Library Screen
                composable(
                    "${AiluaDestinations.APPS}?preferredPageId={preferredPageId}",
                    arguments = listOf(navArgument("preferredPageId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    AppLibraryScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        preferredPageId = backStackEntry.arguments?.getString("preferredPageId"),
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onOpenApp = { appId ->
                            AppRouter.launchRouteOrNull(appId, selectedCharacterId)?.let { route ->
                                navController.navigate(route)
                            }
                        }
                    )
                }
                composable(AiluaDestinations.REALITY) {
                    RealityBridgeScreen(onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) }, onBack = { navController.popBackStack() }, isDarkTheme = isDarkTheme, onToggleTheme = { isDarkTheme = !isDarkTheme })
                }
                composable(AiluaDestinations.SETTINGS) {
                    SettingsScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        onBack = { navController.popBackStack() },
                        onReality = { navController.navigate(AiluaDestinations.REALITY) },
                        onPrivacy = { navController.navigate(AiluaDestinations.PRIVACY) },
                        onCharacters = { navController.navigate(AiluaDestinations.CHARACTER_CREATOR) },
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        isDarkTheme = isDarkTheme,
                    )
                }
                composable(AiluaDestinations.PRIVACY) {
                    PrivacyScreen(onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) }, onBack = { navController.popBackStack() }, isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme })
                }

                // Screen 12: Character Profile
                composable(
                    route = "profile/{characterId}",
                    arguments = listOf(navArgument("characterId") {
                        type = NavType.StringType
                        defaultValue = "mira"
                    })
                ) { backStackEntry ->
                    val charId = backStackEntry.arguments?.getString("characterId") ?: "mira"
                    val character = CharacterRegistry.getCharacter(charId)
                    CharacterProfileScreen(
                        character = character,
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onClose = { navController.popBackStack() },
                        onStartChat = {
                            navController.navigate(AiluaDestinations.chatRoute(character.id)) {
                                popUpTo(AiluaDestinations.HOME)
                            }
                        },
                        onOpenLiving = {
                            navController.navigate(AiluaDestinations.LIVING) {
                                popUpTo(AiluaDestinations.HOME)
                            }
                        }
                    )
                }

                // Screen 13: Memories
                composable(AiluaDestinations.MEMORIES) {
                    MemoriesScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        characterId = selectedCharacterId
                    )
                }

                // Screen 14: Character Creator
                composable(AiluaDestinations.CHARACTER_CREATOR) {
                    CharacterCreatorScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() },
                        onPreviewCharacter = { charId ->
                            CharacterContext.select(charId)
                            navController.navigate(AiluaDestinations.profileRoute(charId))
                        }
                    )
                }

                // Screen 15: World Book / Lore
                composable(AiluaDestinations.WORLD_BOOK) {
                    WorldBookScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Screen 16: World Places / Map
                composable(AiluaDestinations.WORLD_MAP) {
                    WorldPlacesScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() },
                        onVisitPlaceChat = { charId ->
                            navController.navigate(AiluaDestinations.chatRoute(charId))
                        }
                    )
                }

                // Screen 17: Theater / Branching Narrative
                composable(AiluaDestinations.THEATER) {
                    TheaterScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Screen 18: Mailbox / Letters & Postcards
                composable(AiluaDestinations.MAILBOX) {
                    MailboxScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() },
                        onReplyInChat = { charId ->
                            navController.navigate(AiluaDestinations.chatRoute(charId))
                        }
                    )
                }

                // Screen 19: Call History / Companion Voice Logs
                composable(AiluaDestinations.CALL_HISTORY) {
                    CallHistoryScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() },
                        onStartCall = { charId ->
                            CallStateEngine.triggerIncomingCall(
                                characterId = charId,
                                callerName = CharacterRegistry.getCharacter(charId).name,
                                reason = "主动拨通伴生语音倾听"
                            )
                            CallStateEngine.handleAction(CallAction.ANSWER)
                            navController.navigate(AiluaDestinations.callRoute(charId))
                        }
                    )
                }

                // Screen 20: Companion Call (Active Call Session)
                composable(
                    route = "call/{characterId}",
                    arguments = listOf(navArgument("characterId") {
                        type = NavType.StringType
                        defaultValue = "mira"
                    })
                ) {
                    CallScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onCallEnded = {
                            navController.navigate(AiluaDestinations.CALL_HISTORY) {
                                popUpTo(AiluaDestinations.HOME)
                                launchSingleTop = true
                            }
                        }
                    )
                }

                // Screen 21: Incoming Call (Proactive Heartbeat Ringing)
                composable(AiluaDestinations.INCOMING_CALL) {
                    val session = currentCall
                    IncomingCallScreen(
                        onAnswer = {
                            val targetId = session?.characterId ?: CharacterContext.currentId()
                            CallStateEngine.handleAction(CallAction.ANSWER)
                            navController.navigate(AiluaDestinations.callRoute(targetId)) {
                                popUpTo(AiluaDestinations.INCOMING_CALL) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onDecline = {
                            CallStateEngine.handleAction(CallAction.DECLINE)
                            navController.popBackStack()
                        },
                        onDismissLater = {
                            CallStateEngine.handleAction(CallAction.DECLINE)
                            navController.popBackStack()
                        }
                    )
                }

                // Screen 22: Gallery / Visual Companion Moments
                composable(AiluaDestinations.GALLERY) {
                    GalleryScreen(
                        onGoHome = { navController.popBackStack(AiluaDestinations.HOME, false) },
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
            }
        }
        }
    }
}
