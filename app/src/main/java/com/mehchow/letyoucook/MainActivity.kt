package com.mehchow.letyoucook

import AuthScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mehchow.letyoucook.ui.screens.HomeScreen
import com.mehchow.letyoucook.ui.theme.LetYouCookTheme
import dagger.hilt.android.AndroidEntryPoint

// telling Hilt to inject dependencies into MainActivity
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LetYouCookTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "auth") {
        // Route for the Login Page
        composable("auth") {
            AuthScreen(onLoginSuccess = { response ->
                // Navigate to home and pass the username in the URL
                navController.navigate("home/${response.username}") {
                    // Pop "auth" off the stack so back button doesn't go back to login
                    popUpTo("auth") { inclusive = true }
                }
            })
        }

        // Route for the Home Page
        composable("home/{username}") { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: "User"
            HomeScreen(username = username)
        }
    }
}