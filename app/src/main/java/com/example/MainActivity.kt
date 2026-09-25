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
import com.example.ui.home.VirtualHomeScreen
import com.example.ui.living.LivingScreen
import com.example.ui.memories.MemoriesScreen
import com.example.ui.moments.MomentsScreen
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
                // Screen 1: Virtual Home
                composable(AiluaDestinations.HOME) {
                    VirtualHomeScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onNavigateToChat = { navController.navigate(AiluaDestinations.CHAT) },
                        onNavigateToMoments = { navController.navigate(AiluaDestinations.MOMENTS) },
                        onNavigateToLiving = { navController.navigate(AiluaDestinations.LIVING) },
                        onNavigateToMemories = { navController.navigate(AiluaDestinations.MEMORIES) },
                        onNavigateToApps = { navController.navigate(AiluaDestinations.APPS) },
                        onOpenProfile = { navController.navigate(AiluaDestinations.PROFILE) },
                        onAppClick = { appId ->
                            when (appId) {
                                "chat" -> navController.navigate(AiluaDestinations.CHAT)
                                "moments" -> navController.navigate(AiluaDestinations.MOMENTS)
                                "living" -> navController.navigate(AiluaDestinations.LIVING)
                                "memories" -> navController.navigate(AiluaDestinations.MEMORIES)
                                "apps" -> navController.navigate(AiluaDestinations.APPS)
                                else -> navController.navigate(AiluaDestinations.APPS)
                            }
                        }
                    )
                }

                // Screen 2: Chat
                composable(AiluaDestinations.CHAT) {
                    ChatScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onOpenProfile = { navController.navigate(AiluaDestinations.PROFILE) }
                    )
                }

                // Screen 3: Moments
                composable(AiluaDestinations.MOMENTS) {
                    MomentsScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onOpenProfile = { navController.navigate(AiluaDestinations.PROFILE) }
                    )
                }

                // Screen 4: Living
                composable(AiluaDestinations.LIVING) {
                    LivingScreen(
                        isDarkTheme = isDarkTheme,
                        onToggleTheme = { isDarkTheme = !isDarkTheme },
                        onBackToHome = { navController.popBackStack() },
                        onNavigateToChat = { navController.navigate(AiluaDestinations.CHAT) },
                        onOpenProfile = { navController.navigate(AiluaDestinations.PROFILE) }
                    )
                }

                // Screen 5: Apps Library
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

                // Character Profile
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

                // Memories
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
