package com.alvaro.pricewise.ui.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.alvaro.pricewise.data.repository.AuthRepository
import com.alvaro.pricewise.ui.products.CreateProductScreen
import com.alvaro.pricewise.ui.products.ProductDetailScreen
import com.alvaro.pricewise.ui.products.ProductListScreen
import com.alvaro.pricewise.ui.recommendations.RecommendationsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import javax.inject.Inject
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
    object Products : Tab("tab_products")
    object Recommendations : Tab("tab_recommendations")
}

@Composable
fun MainScreen(
    onLogout: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val innerNav = rememberNavController()
    val currentRoute by innerNav.currentBackStackEntryFlow.collectAsState(
        initial = innerNav.currentBackStackEntry
    )

    val bottomItems = listOf(
        Triple(Tab.Products.route, "Productos", Icons.Default.Inventory2),
        Triple(Tab.Recommendations.route, "Análisis", Icons.Default.Lightbulb)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            val currentDest = currentRoute?.destination?.route
            // Mostrar bottom bar solo en las tabs principales
            val showBar = currentDest in listOf(Tab.Products.route, Tab.Recommendations.route)
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
                                        popUpTo(Tab.Products.route) { saveState = true }
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
    ) { _ ->
        NavHost(
            navController = innerNav,
            startDestination = Tab.Products.route
        ) {
            composable(Tab.Products.route) {
                ProductListScreen(
                    onProductClick = { id -> innerNav.navigate("detail/$id") },
                    onAddProduct = { product ->
                        if (product != null) {
                            fun enc(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8.toString())
                            val route = "add_product?name=${enc(product.name)}&sku=${enc(product.sku ?: "")}&price=${enc(product.currentPrice.toString())}&category=${enc(product.category ?: "")}&brand=${enc(product.brand ?: "")}&description=${enc(product.description ?: "")}"
                            innerNav.navigate(route)
                        } else {
                            innerNav.navigate("add_product")
                        }
                    },
                    onLogout = { viewModel.logout(onLogout) }
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

            composable(
                route = "add_product?name={name}&sku={sku}&price={price}&category={category}&brand={brand}&description={description}",
                arguments = listOf(
                    navArgument("name") { defaultValue = "" },
                    navArgument("sku") { defaultValue = "" },
                    navArgument("price") { defaultValue = "" },
                    navArgument("category") { defaultValue = "" },
                    navArgument("brand") { defaultValue = "" },
                    navArgument("description") { defaultValue = "" }
                )
            ) { backStackEntry ->
                CreateProductScreen(
                    onProductCreated = { innerNav.popBackStack() },
                    onNavigateBack = { innerNav.popBackStack() },
                    initialName = backStackEntry.arguments?.getString("name") ?: "",
                    initialSku = backStackEntry.arguments?.getString("sku") ?: "",
                    initialPrice = backStackEntry.arguments?.getString("price") ?: "",
                    initialCategory = backStackEntry.arguments?.getString("category") ?: "",
                    initialBrand = backStackEntry.arguments?.getString("brand") ?: "",
                    initialDescription = backStackEntry.arguments?.getString("description") ?: ""
                )
            }

            composable(Tab.Recommendations.route) {
                RecommendationsScreen()
            }
        }
    }
}
