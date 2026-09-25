package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.navigation.AiluaDestinations
import com.example.ui.apps.AppLibraryScreen
import com.example.ui.character.CharacterProfileScreen
import com.example.ui.chat.ChatScreen
import com.example.ui.chat.ConversationListScreen
import com.example.ui.chat.GroupChatScreen
import com.example.ui.checkphone.CheckPhoneScreen
import com.example.ui.contacts.ContactsScreen
import com.example.ui.home.VirtualHomeScreen
import com.example.ui.living.LivingScreen
import com.example.ui.memories.MemoriesScreen
import com.example.ui.moments.MomentsScreen
import com.example.ui.relations.RelationsScreen
import com.example.ui.theme.AiluaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiluaAppRoot()
        }
    }
}

@Composable
fun AiluaAppRoot() {
    val systemDark = isSystemInDarkTheme()
    var isDarkTheme by remember { mutableStateOf(systemDark) }
    val navController = rememberNavController()

    AiluaTheme(darkTheme = isDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavHost(
                navController = navController,
                startDestination = AiluaDestinations.HOME,
                enterTransition = {
                    fadeIn(animationSpec = tween(220)) + slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(240)
                    )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(180))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(200))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(180)) + slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(220)
                    )
                }
            ) {
                // Screen 1: Virtual Home (ARK Launcher multi-page workspace)
                composable(AiluaDestinations.HOME) {
                    VirtualHomeScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onNavigateToMessages = { navController.navigate(AiluaDestinations.MESSAGES) },
                        onNavigateToChat = { navController.navigate(AiluaDestinations.CHAT) },
                        onNavigateToGroupChat = { navController.navigate(AiluaDestinations.GROUP_CHAT) },
                        onNavigateToContacts = { navController.navigate(AiluaDestinations.CONTACTS) },
                        onNavigateToRelations = { navController.navigate(AiluaDestinations.RELATIONS) },
                        onNavigateToCheckPhone = { navController.navigate(AiluaDestinations.CHECK_PHONE) },
                        onNavigateToMoments = { navController.navigate(AiluaDestinations.MOMENTS) },
                        onNavigateToLiving = { navController.navigate(AiluaDestinations.LIVING) },
                        onNavigateToMemories = { navController.navigate(AiluaDestinations.MEMORIES) },
                        onNavigateToApps = { navController.navigate(AiluaDestinations.APPS) },
                        onOpenProfile = { navController.navigate(AiluaDestinations.PROFILE) },
                        onAppClick = { appId ->
                            when (appId) {
                                "messages", "chat" -> navController.navigate(AiluaDestinations.MESSAGES)
                                "group_chat" -> navController.navigate(AiluaDestinations.GROUP_CHAT)
                                "contacts" -> navController.navigate(AiluaDestinations.CONTACTS)
                                "relations" -> navController.navigate(AiluaDestinations.RELATIONS)
                                "check_phone", "diary" -> navController.navigate(AiluaDestinations.CHECK_PHONE)
                                "moments" -> navController.navigate(AiluaDestinations.MOMENTS)
                                "living" -> navController.navigate(AiluaDestinations.LIVING)
                                "memories" -> navController.navigate(AiluaDestinations.MEMORIES)
                                "apps" -> navController.navigate(AiluaDestinations.APPS)
                                else -> navController.navigate(AiluaDestinations.APPS)
                            }
                        }
                    )
                }

                // Screen 2: Messages / Conversation List (Reply & Jetchat inspired)
                composable(AiluaDestinations.MESSAGES) {
                    ConversationListScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onOpenMiraChat = { navController.navigate(AiluaDestinations.CHAT) },
                        onOpenGroupChat = { navController.navigate(AiluaDestinations.GROUP_CHAT) },
                        onOpenContacts = { navController.navigate(AiluaDestinations.CONTACTS) },
                        onSelectCharacterChat = { _ -> navController.navigate(AiluaDestinations.CHAT) }
                    )
                }

                // Screen 3: Private Companion Chat (Jetchat inspired chat stream)
                composable(AiluaDestinations.CHAT) {
                    ChatScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onOpenProfile = { navController.navigate(AiluaDestinations.PROFILE) }
                    )
                }

                // Screen 4: Multi-Character Group Chat (SillyTavern-GroupWorld Director inspired)
                composable(AiluaDestinations.GROUP_CHAT) {
                    GroupChatScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() },
                        onOpenRelations = { navController.navigate(AiluaDestinations.RELATIONS) }
                    )
                }

                // Screen 5: Contacts Screen
                composable(AiluaDestinations.CONTACTS) {
                    ContactsScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onOpenProfile = { _ -> navController.navigate(AiluaDestinations.PROFILE) },
                        onStartPrivateChat = { _ -> navController.navigate(AiluaDestinations.CHAT) },
                        onOpenRelations = { navController.navigate(AiluaDestinations.RELATIONS) }
                    )
                }

                // Screen 6: Relations & Social Graph Screen (SillyTavern-GroupWorld social map)
                composable(AiluaDestinations.RELATIONS) {
                    RelationsScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Screen 7: Check Phone Screen (SillyTavern-GroupWorld & Companion OS peek)
                composable(AiluaDestinations.CHECK_PHONE) {
                    CheckPhoneScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() }
                    )
                }

                // Screen 8: Moments
                composable(AiluaDestinations.MOMENTS) {
                    MomentsScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onOpenProfile = { navController.navigate(AiluaDestinations.PROFILE) }
                    )
                }

                // Screen 9: Living
                composable(AiluaDestinations.LIVING) {
                    LivingScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onNavigateToChat = { navController.navigate(AiluaDestinations.CHAT) },
                        onOpenProfile = { navController.navigate(AiluaDestinations.PROFILE) }
                    )
                }

                // Screen 10: Apps Library
                composable(AiluaDestinations.APPS) {
                    AppLibraryScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onNavigateToChat = { navController.navigate(AiluaDestinations.CHAT) },
                        onNavigateToMoments = { navController.navigate(AiluaDestinations.MOMENTS) },
                        onNavigateToLiving = { navController.navigate(AiluaDestinations.LIVING) },
                        onNavigateToMemories = { navController.navigate(AiluaDestinations.MEMORIES) }
                    )
                }

                // Screen 11: Character Profile
                composable(AiluaDestinations.PROFILE) {
                    CharacterProfileScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onClose = { navController.popBackStack() },
                        onStartChat = {
                            navController.navigate(AiluaDestinations.CHAT) {
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

                // Screen 12: Memories
                composable(AiluaDestinations.MEMORIES) {
                    MemoriesScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
