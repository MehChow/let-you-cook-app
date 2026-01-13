package com.mehchow.letyoucook

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mehchow.letyoucook.NavRoutes.homeRoute
import com.mehchow.letyoucook.ui.screens.AuthScreen
import com.mehchow.letyoucook.ui.screens.HomeScreen
import com.mehchow.letyoucook.ui.screens.SplashScreen
import com.mehchow.letyoucook.ui.theme.LetYouCookTheme
import com.mehchow.letyoucook.ui.viewmodel.AuthState
import com.mehchow.letyoucook.ui.viewmodel.MainViewModel
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
fun AppNavigation(
    viewModel: MainViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                navController.navigate(homeRoute(state.user.username)) {
                    popUpTo(NavRoutes.SPLASH) { inclusive = true }
                }
            }

            is AuthState.Unauthenticated -> {
                navController.navigate(NavRoutes.AUTH) {
                    popUpTo(0) { inclusive = true }
                }
            }

            is AuthState.Loading -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = NavRoutes.SPLASH
    ) {
        composable(NavRoutes.SPLASH) {
            SplashScreen()
        }

        composable(NavRoutes.AUTH) {
            AuthScreen(onLoginSuccess = { response ->
                navController.navigate(homeRoute(response.username)) {
                    popUpTo(NavRoutes.AUTH) { inclusive = true }
                }
            })
        }

        composable(NavRoutes.HOME_WITH_USERNAME) { backStackEntry ->
            val username = backStackEntry.arguments?.getString("username") ?: "User"
            HomeScreen(username = username)
        }
    }
}