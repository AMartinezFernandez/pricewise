package com.alvaro.pricewise.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.alvaro.pricewise.data.repository.AuthRepository
import com.alvaro.pricewise.ui.alerts.AlertsScreen
import com.alvaro.pricewise.ui.dashboard.DashboardScreen
import com.alvaro.pricewise.ui.products.ProductDetailScreen
import com.alvaro.pricewise.ui.search.SearchScreen
import com.alvaro.pricewise.ui.tracking.TrackingScreen
import dagger.hilt.android.lifecycle.HiltViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ViewModel sencillo solo para gestionar el logout
@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}

// Rutas internas de la pantalla principal (con bottom nav)
private sealed class Tab(val route: String) {
    object Dashboard : Tab("tab_dashboard")
    object Tracking : Tab("tab_tracking")
    object Search : Tab("tab_search")
    object Alerts : Tab("tab_alerts")
    object Users : Tab("tab_users")
}

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {

    val innerNav = rememberNavController()
    val currentRoute by innerNav.currentBackStackEntryFlow.collectAsState(
        initial = innerNav.currentBackStackEntry
    )

    val bottomItems = listOf(
        Triple(Tab.Dashboard.route, "Inicio", Icons.Default.Dashboard),
        Triple(Tab.Tracking.route, "Seguimiento", Icons.Default.Inventory),
        Triple(Tab.Search.route, "Buscar", Icons.Default.Search),
        Triple(Tab.Alerts.route, "Alertas", Icons.Default.Notifications)
    )


    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val currentDest = currentRoute?.destination?.route
            // Mostrar bottom bar solo en las tabs principales
            val showBar = currentDest in bottomItems.map { it.first }
            if (showBar) {
                NavigationBar {
                    bottomItems.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentDest == route,
                            onClick = {
                                if (currentDest != route) {
                                    innerNav.navigate(route) {
                                        popUpTo(innerNav.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = innerNav,
            startDestination = Tab.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Tab.Dashboard.route) {
                DashboardScreen(
                    onNavigateToTracking = {
                        innerNav.navigate(Tab.Tracking.route) {
                            popUpTo(innerNav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToAlerts = {
                        innerNav.navigate(Tab.Alerts.route) {
                            popUpTo(innerNav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToUsers = {
                        innerNav.navigate(Tab.Users.route)
                    },
                    onNavigateToSettings = onNavigateToSettings
                )
            }

            composable(Tab.Tracking.route) {
                TrackingScreen(
                    onProductClick = { id -> innerNav.navigate("detail/$id") }
                )
            }

            composable(Tab.Alerts.route) {
                AlertsScreen()
            }

            composable(Tab.Search.route) {
                SearchScreen(
                    onProductClick = { id -> innerNav.navigate("detail/$id") },
                    onAddProduct = { product ->
                        val encodedName = java.net.URLEncoder.encode(product.name, java.nio.charset.StandardCharsets.UTF_8.toString())
                        val encodedBrand = java.net.URLEncoder.encode(product.brand ?: "", java.nio.charset.StandardCharsets.UTF_8.toString())
                        val encodedAsin = java.net.URLEncoder.encode(product.asin ?: product.sku ?: "", java.nio.charset.StandardCharsets.UTF_8.toString())
                        val price = product.currentPrice?.toString() ?: ""

                        innerNav.navigate("create_product?name=$encodedName&brand=$encodedBrand&asin=$encodedAsin&price=$price")
                    }
                )
            }

            composable(
                route = "create_product?name={name}&brand={brand}&asin={asin}&price={price}",
                arguments = listOf(
                    navArgument("name") { type = NavType.StringType; defaultValue = "" },
                    navArgument("brand") { type = NavType.StringType; defaultValue = "" },
                    navArgument("asin") { type = NavType.StringType; defaultValue = "" },
                    navArgument("price") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStack ->
                val charset = StandardCharsets.UTF_8.toString()
                val name = URLDecoder.decode(backStack.arguments?.getString("name") ?: "", charset)
                val brand = URLDecoder.decode(backStack.arguments?.getString("brand") ?: "", charset)
                val asin = URLDecoder.decode(backStack.arguments?.getString("asin") ?: "", charset)
                val price = backStack.arguments?.getString("price") ?: ""

                com.alvaro.pricewise.ui.products.CreateProductScreen(
                    onNavigateBack = { innerNav.popBackStack() },
                    onProductCreated = {
                        // Navegar al tab de Seguimiento tras crear producto
                        // (en vez de volver a Búsqueda donde aparecería duplicado)
                        innerNav.navigate(Tab.Tracking.route) {
                            popUpTo(innerNav.graph.findStartDestination().id) {
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    initialName = name,
                    initialBrand = brand,
                    initialAsin = asin,
                    initialPrice = price
                )
            }

            composable(
                route = "detail/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStack ->
                val productId = backStack.arguments?.getLong("productId") ?: return@composable
                ProductDetailScreen(
                    productId = productId,
                    onNavigateBack = { innerNav.popBackStack() }
                )
            }

            composable(Tab.Users.route) {
                com.alvaro.pricewise.ui.users.UsersScreen(
                    onNavigateBack = { innerNav.popBackStack() },
                    onNavigateToCreateUser = { innerNav.navigate("create_user") }
                )
            }

            composable("create_user") {
                com.alvaro.pricewise.ui.users.CreateUserScreen(
                    onNavigateBack = { innerNav.popBackStack() }
                )
            }
        }
    }
}
