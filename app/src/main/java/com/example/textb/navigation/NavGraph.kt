package com.example.textb.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.textb.ui.screens.auth.LoginScreen
import com.example.textb.ui.screens.auth.SignupScreen
import com.example.textb.ui.screens.books.BooksScreen
import com.example.textb.ui.screens.chat.ChatScreen
import com.example.textb.ui.screens.chat.CometChatScreen
import com.example.textb.ui.screens.profile.ProfileScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Signup : Screen("signup")
    object Books : Screen("books")
    object Profile : Screen("profile")
    object Chat : Screen("chat/{bookOwnerId}/{bookOwnerName}")
    object CometChat : Screen("cometchat/{receiverId}/{receiverName}")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Books.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSignupClick = {
                    navController.navigate(Screen.Signup.route)
                }
            )
        }
        
        composable(Screen.Signup.route) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Signup.route) { inclusive = true }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Books.route) {
            BooksScreen(
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onChatClick = { sellerId, sellerName ->
                    // Use CometChat instead of the custom chat screen
                    val chatRoute = "cometchat/$sellerId/$sellerName"
                    navController.navigate(chatRoute)
                }
            )
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("bookOwnerId") { type = NavType.StringType },
                navArgument("bookOwnerName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val bookOwnerId = backStackEntry.arguments?.getString("bookOwnerId") ?: ""
            val bookOwnerName = backStackEntry.arguments?.getString("bookOwnerName") ?: ""
            
            ChatScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                bookOwnerId = bookOwnerId,
                bookOwnerName = bookOwnerName
            )
        }
        
        // CometChat Screen with real-time messaging
        composable(
            route = Screen.CometChat.route,
            arguments = listOf(
                navArgument("receiverId") { type = NavType.StringType },
                navArgument("receiverName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val receiverId = backStackEntry.arguments?.getString("receiverId") ?: ""
            val receiverName = backStackEntry.arguments?.getString("receiverName") ?: ""
            
            CometChatScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                receiverId = receiverId,
                receiverName = receiverName
            )
        }
    }
} 