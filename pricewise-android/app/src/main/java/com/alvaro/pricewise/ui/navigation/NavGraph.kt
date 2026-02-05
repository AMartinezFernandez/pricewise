package com.alvaro.pricewise.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alvaro.pricewise.data.repository.TokenRepository
import com.alvaro.pricewise.ui.auth.LoginScreen
import com.alvaro.pricewise.ui.auth.RegisterScreen
import com.alvaro.pricewise.ui.main.MainScreen
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking

sealed class Screen(val route: String) {
    object Login       : Screen("login")
    object Register    : Screen("register")
    object Main        : Screen("main")
    object Products    : Screen("products")
    object AddProduct  : Screen("products/add")
    object Recommendations : Screen("recommendations")
    object ProductDetail : Screen("products/{productId}") {
        fun createRoute(id: Long) = "products/$id"
    }
}

@Composable
fun RootNavGraph(
    tokenRepository: TokenRepository,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    // Determinar si ya hay sesión guardada
    val startDestination = remember {
        val token = runBlocking { tokenRepository.getToken().firstOrNull() }
        if (!token.isNullOrBlank()) Screen.Main.route else Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
