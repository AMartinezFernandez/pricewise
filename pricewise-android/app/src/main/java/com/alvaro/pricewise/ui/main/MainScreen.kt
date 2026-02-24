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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.alvaro.pricewise.ui.alerts.AlertsScreen
import com.alvaro.pricewise.ui.common.OfflineBanner
import com.alvaro.pricewise.ui.dashboard.DashboardScreen
import com.alvaro.pricewise.ui.products.ProductDetailScreen
import com.alvaro.pricewise.ui.search.SearchScreen
import com.alvaro.pricewise.ui.tracking.TrackingScreen
import com.alvaro.pricewise.util.NetworkObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.compose.foundation.layout.Column
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

// ViewModel para estado de red
@HiltViewModel
class MainViewModel @Inject constructor(
    networkObserver: NetworkObserver
) : ViewModel() {

    val isOffline = networkObserver.isConnected
        .map { connected -> !connected }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
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
    onNavigateToSettings: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {

    val innerNav = rememberNavController()
    val currentRoute by innerNav.currentBackStackEntryFlow.collectAsState(
        initial = innerNav.currentBackStackEntry
    )
    val isOffline by viewModel.isOffline.collectAsState()

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
        Column(modifier = Modifier.padding(padding)) {
            OfflineBanner(isOffline = isOffline)

            NavHost(
                navController = innerNav,
                startDestination = Tab.Dashboard.route,
                modifier = Modifier.weight(1f)
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
                    onNavigateToAdminUsers = {
                        innerNav.navigate("admin_users")
                    },
                    onNavigateToAdminDashboard = {
                        innerNav.navigate("admin_dashboard")
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
                        val charset = StandardCharsets.UTF_8.toString()
                        val encodedName = URLEncoder.encode(product.name, charset)
                        val encodedBrand = URLEncoder.encode(product.brand ?: "", charset)
                        val encodedAsin = URLEncoder.encode(product.asin ?: product.sku ?: "", charset)
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
                    onNavigateBack = { innerNav.popBackStack() },
                    onNavigateToHistory = { id -> innerNav.navigate("price_history/$id") }
                )
            }

            composable(
                route = "price_history/{productId}",
                arguments = listOf(navArgument("productId") { type = NavType.LongType })
            ) { backStack ->
                val productId = backStack.arguments?.getLong("productId") ?: return@composable
                com.alvaro.pricewise.ui.products.PriceHistoryScreen(
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

            composable("admin_users") {
                com.alvaro.pricewise.ui.admin.AdminUsersScreen(
                    onNavigateBack = { innerNav.popBackStack() },
                    onNavigateToUserDetail = { userId ->
                        innerNav.navigate("admin_user_detail/$userId")
                    }
                )
            }

            composable(
                route = "admin_user_detail/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.LongType })
            ) { backStack ->
                val userId = backStack.arguments?.getLong("userId") ?: return@composable
                com.alvaro.pricewise.ui.admin.AdminUserDetailScreen(
                    userId = userId,
                    onNavigateBack = { innerNav.popBackStack() }
                )
            }

            composable("admin_dashboard") {
                com.alvaro.pricewise.ui.admin.AdminDashboardScreen(
                    onNavigateBack = { innerNav.popBackStack() }
                )
            }
        }
        }
    }
}
