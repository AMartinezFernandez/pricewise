package com.alvaro.pricewise.ui.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.alvaro.pricewise.data.repository.TokenRepository
import com.alvaro.pricewise.util.SessionManager
import com.alvaro.pricewise.ui.auth.CompanySetupScreen
import com.alvaro.pricewise.ui.auth.LoginScreen
import com.alvaro.pricewise.ui.auth.RegisterScreen
import com.alvaro.pricewise.ui.main.MainScreen
import com.alvaro.pricewise.ui.settings.SettingsScreen
import com.alvaro.pricewise.ui.splash.SplashScreen
import kotlinx.coroutines.delay
import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {

    object Splash      : Screen("splash")
    object Login       : Screen("login")
    object Register    : Screen("register")
    object CompanySetup : Screen("company_setup/{idToken}/{email}/{name}") {
        fun createRoute(idToken: String, email: String, name: String): String {
            val enc = java.nio.charset.StandardCharsets.UTF_8.toString()
            return "company_setup/${URLEncoder.encode(idToken, enc)}/${URLEncoder.encode(email, enc)}/${URLEncoder.encode(name, enc)}"
        }
    }
    object Main        : Screen("main")
    object Settings    : Screen("settings")
}


@Composable
fun RootNavGraph(
    tokenRepository: TokenRepository,
    sessionManager: SessionManager,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    // Escuchar evento de sesión expirada → redirigir a login
    LaunchedEffect(Unit) {
        sessionManager.sessionExpired.collect {
            tokenRepository.clearSession()
            navController.navigate(Screen.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        // Splash: muestra logo mientras se hidrata el token
        composable(Screen.Splash.route) {
            val isLoggedIn by tokenRepository.isLoggedIn()
                .collectAsState(initial = null) // null = aún cargando

            LaunchedEffect(isLoggedIn) {
                if (isLoggedIn != null) {
                    delay(400) // transición suave
                    val destination = if (isLoggedIn == true) Screen.Main.route else Screen.Login.route
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }

            SplashScreen()
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                onGoogleSetupNeeded = { idToken, email, name ->
                    navController.navigate(Screen.CompanySetup.createRoute(idToken, email, name))
                }
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

        composable(
            route = Screen.CompanySetup.route,
            arguments = listOf(
                navArgument("idToken") { type = NavType.StringType },
                navArgument("email") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val enc = java.nio.charset.StandardCharsets.UTF_8.toString()
            val idToken = URLDecoder.decode(backStackEntry.arguments?.getString("idToken") ?: "", enc)
            val email = URLDecoder.decode(backStackEntry.arguments?.getString("email") ?: "", enc)
            val name = URLDecoder.decode(backStackEntry.arguments?.getString("name") ?: "", enc)

            CompanySetupScreen(
                googleIdToken = idToken,
                googleEmail = email,
                googleName = name,
                onSetupSuccess = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Main.route) {
            MainScreen(
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
