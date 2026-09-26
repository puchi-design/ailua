package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.example.data.engine.CallStateEngine
import com.example.data.engine.WorldHeartbeatEngine
import com.example.data.engine.WorldStateRepository
import com.example.data.local.AiluaLocalStore
import com.example.data.mock.MockData
import com.example.data.model.CallAction
import com.example.data.model.CallState
import com.example.data.registry.CharacterRegistry
import com.example.navigation.AiluaDestinations
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
import com.example.ui.relations.RelationsScreen
import com.example.ui.theater.TheaterScreen
import com.example.ui.theme.AiluaTheme
import com.example.ui.world.WorldPlacesScreen
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AiluaLocalStore.init(applicationContext)
        WorldStateRepository.syncWithLocalStore()
        CallStateEngine.syncWithLocalStore()

        // Lightweight foreground-only world ticker (Step 6)
        // 60 real seconds -> advance virtual time by 1 minute
        // Stops automatically when Activity is not in RESUMED lifecycle state
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (isActive) {
                    delay(60_000L)
                    WorldHeartbeatEngine.advanceTime(1)
                }
            }
        }

        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalOsChromeState provides rememberOsChromeState()) {
                AiluaAppRoot()
            }
        }
    }
}

@Composable
fun AiluaAppRoot() {
    val systemDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemDark) }
    val navController = rememberNavController()

    // Step 2 & 3: Observe CallStateEngine as sole authority with lifecycle awareness
    val currentCall by CallStateEngine.currentCall.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
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
                        character = MockData.sampleCharacter,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onNavigateToMessages = { navController.navigate(AiluaDestinations.MESSAGES) },
                        onNavigateToChat = { navController.navigate(AiluaDestinations.chatRoute("mira")) },
                        onNavigateToGroupChat = { navController.navigate(AiluaDestinations.GROUP_CHAT) },
                        onNavigateToContacts = { navController.navigate(AiluaDestinations.CONTACTS) },
                        onNavigateToRelations = { navController.navigate(AiluaDestinations.RELATIONS) },
                        onNavigateToCheckPhone = { navController.navigate(AiluaDestinations.CHECK_PHONE) },
                        onNavigateToDiary = { navController.navigate(AiluaDestinations.DIARY) },
                        onNavigateToMoments = { navController.navigate(AiluaDestinations.MOMENTS) },
                        onNavigateToLiving = { navController.navigate(AiluaDestinations.LIVING) },
                        onNavigateToMemories = { navController.navigate(AiluaDestinations.MEMORIES) },
                        onNavigateToApps = { navController.navigate(AiluaDestinations.APPS) },
                        onOpenProfile = { navController.navigate(AiluaDestinations.profileRoute("mira")) },
                        onNavigateToMailbox = { navController.navigate(AiluaDestinations.MAILBOX) },
                        onNavigateToCall = { navController.navigate(AiluaDestinations.callRoute("mira")) },
                        onNavigateToCallHistory = { navController.navigate(AiluaDestinations.CALL_HISTORY) },
                        onNavigateToGallery = { navController.navigate(AiluaDestinations.GALLERY) },
                        onAppClick = { appId ->
                            when (appId) {
                                "messages", "chat" -> navController.navigate(AiluaDestinations.MESSAGES)
                                "group_chat" -> navController.navigate(AiluaDestinations.GROUP_CHAT)
                                "contacts" -> navController.navigate(AiluaDestinations.CONTACTS)
                                "relations" -> navController.navigate(AiluaDestinations.RELATIONS)
                                "check_phone" -> navController.navigate(AiluaDestinations.CHECK_PHONE)
                                "diary" -> navController.navigate(AiluaDestinations.DIARY)
                                "moments" -> navController.navigate(AiluaDestinations.MOMENTS)
                                "living" -> navController.navigate(AiluaDestinations.LIVING)
                                "memories" -> navController.navigate(AiluaDestinations.MEMORIES)
                                "apps" -> navController.navigate(AiluaDestinations.APPS)
                                "creator" -> navController.navigate(AiluaDestinations.CHARACTER_CREATOR)
                                "world_book" -> navController.navigate(AiluaDestinations.WORLD_BOOK)
                                "world_map" -> navController.navigate(AiluaDestinations.WORLD_MAP)
                                "theater" -> navController.navigate(AiluaDestinations.THEATER)
                                "mailbox" -> navController.navigate(AiluaDestinations.MAILBOX)
                                "call_history" -> navController.navigate(AiluaDestinations.CALL_HISTORY)
                                "gallery" -> navController.navigate(AiluaDestinations.GALLERY)
                                else -> navController.navigate(AiluaDestinations.APPS)
                            }
                        }
                    )
                }

                // Screen 2: Conversation List
                composable(AiluaDestinations.MESSAGES) {
                    ConversationListScreen(
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
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() },
                        onOpenRelations = { navController.navigate(AiluaDestinations.RELATIONS) }
                    )
                }

                // Screen 5: Contacts List Screen
                composable(AiluaDestinations.CONTACTS) {
                    ContactsScreen(
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
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() }
                    )
                }

                // Screen 7: Diary Screen
                composable(AiluaDestinations.DIARY) {
                    DiaryScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() }
                    )
                }

                // Screen 8: Social Graph / Relations Screen
                composable(AiluaDestinations.RELATIONS) {
                    RelationsScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Screen 9: Moments Screen
                composable(AiluaDestinations.MOMENTS) {
                    MomentsScreen(
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
                        character = MockData.sampleCharacter,
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onNavigateToChat = { navController.navigate(AiluaDestinations.chatRoute("mira")) },
                        onNavigateToCall = { navController.navigate(AiluaDestinations.callRoute("mira")) },
                        onNavigateToMailbox = { navController.navigate(AiluaDestinations.MAILBOX) },
                        onOpenProfile = { navController.navigate(AiluaDestinations.profileRoute("mira")) }
                    )
                }

                // Screen 11: App Library Screen
                composable(AiluaDestinations.APPS) {
                    AppLibraryScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onNavigateToMessages = { navController.navigate(AiluaDestinations.MESSAGES) },
                        onNavigateToContacts = { navController.navigate(AiluaDestinations.CONTACTS) },
                        onNavigateToMoments = { navController.navigate(AiluaDestinations.MOMENTS) },
                        onNavigateToLiving = { navController.navigate(AiluaDestinations.LIVING) },
                        onNavigateToDiary = { navController.navigate(AiluaDestinations.DIARY) },
                        onNavigateToCheckPhone = { navController.navigate(AiluaDestinations.CHECK_PHONE) },
                        onNavigateToRelations = { navController.navigate(AiluaDestinations.RELATIONS) },
                        onNavigateToMemories = { navController.navigate(AiluaDestinations.MEMORIES) },
                        onNavigateToCharacterCreator = { navController.navigate(AiluaDestinations.CHARACTER_CREATOR) },
                        onNavigateToWorldBook = { navController.navigate(AiluaDestinations.WORLD_BOOK) },
                        onNavigateToWorldMap = { navController.navigate(AiluaDestinations.WORLD_MAP) },
                        onNavigateToTheater = { navController.navigate(AiluaDestinations.THEATER) },
                        onNavigateToMailbox = { navController.navigate(AiluaDestinations.MAILBOX) },
                        onNavigateToCall = { navController.navigate(AiluaDestinations.callRoute("mira")) },
                        onNavigateToGallery = { navController.navigate(AiluaDestinations.GALLERY) }
                    )
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
                    val character = MockData.allCharacters[charId] ?: MockData.sampleCharacter
                    CharacterProfileScreen(
                        character = character,
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
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() }
                    )
                }

                // Screen 14: Character Creator
                composable(AiluaDestinations.CHARACTER_CREATOR) {
                    CharacterCreatorScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() },
                        onPreviewCharacter = { charId ->
                            navController.navigate(AiluaDestinations.profileRoute(charId))
                        }
                    )
                }

                // Screen 15: World Book / Lore
                composable(AiluaDestinations.WORLD_BOOK) {
                    WorldBookScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Screen 16: World Places / Map
                composable(AiluaDestinations.WORLD_MAP) {
                    WorldPlacesScreen(
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
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Screen 18: Mailbox / Letters & Postcards
                composable(AiluaDestinations.MAILBOX) {
                    MailboxScreen(
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
                            val targetId = session?.characterId ?: "mira"
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
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
