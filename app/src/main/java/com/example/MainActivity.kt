package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.db.AppDatabase
import com.example.data.repository.ChatRepository
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.ChatViewModel
import com.example.ui.viewmodel.ChatViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Database, Repository, and ViewModel
        val database = AppDatabase.getDatabase(this)
        val repository = ChatRepository(database.appDao())
        val viewModel = ViewModelProvider(this, ChatViewModelFactory(repository))[ChatViewModel::class.java]

        setContent {
            val currentUser by viewModel.currentUser.collectAsState()
            val isSeedingCompleted by viewModel.isSeedingCompleted.collectAsState()

            // Handle dark mode dynamically
            val isDarkMode = currentUser?.darkModeEnabled ?: false
            // Handle RTL/LTR direction dynamically based on language choice
            val isArabic = (currentUser?.languageCode ?: "ar") == "ar"

            MyApplicationTheme(darkTheme = isDarkMode, dynamicColor = false) {
                val direction = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
                CompositionLocalProvider(LocalLayoutDirection provides direction) {
                    if (!isSeedingCompleted) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = com.example.ui.theme.MessengerBlue)
                        }
                    } else {
                        val navController = rememberNavController()
                        // Auto-login if currentUser is already in state
                        val startDestination = if (currentUser != null) "chats" else "login"

                        NavHost(
                            navController = navController,
                            startDestination = startDestination,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            composable("login") {
                                LoginScreen(
                                    viewModel = viewModel,
                                    onNavigateToRegister = { navController.navigate("register") },
                                    onLoginSuccess = {
                                        navController.navigate("chats") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("register") {
                                RegisterScreen(
                                    viewModel = viewModel,
                                    onNavigateToLogin = { navController.navigate("login") },
                                    onRegisterSuccess = {
                                        navController.navigate("chats") {
                                            popUpTo("register") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("chats") {
                                ChatsListScreen(
                                    viewModel = viewModel,
                                    onNavigateToChat = { chatId -> navController.navigate("chat/$chatId") },
                                    onNavigateToProfile = { navController.navigate("profile") },
                                    onNavigateToSettings = { navController.navigate("settings") },
                                    onNavigateToFriends = { navController.navigate("friends") },
                                    onNavigateToGroupCreate = { navController.navigate("group_create") },
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo("chats") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("chat/{chatId}") {
                                ChatScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = {
                                        viewModel.setActiveChat(null)
                                        navController.navigateUp()
                                    }
                                )
                            }
                            composable("friends") {
                                FriendsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.navigateUp() },
                                    onStartChat = { chatId ->
                                        navController.navigate("chat/$chatId") {
                                            popUpTo("friends") { inclusive = true }
                                        }
                                    }
                                )
                            }
                            composable("group_create") {
                                GroupScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable("profile") {
                                ProfileScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.navigateUp() }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateBack = { navController.navigateUp() },
                                    onLogout = {
                                        navController.navigate("login") {
                                            popUpTo("settings") { inclusive = true }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

