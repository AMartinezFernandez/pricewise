package com.alvaro.pricewise.ui.search

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaro.pricewise.ui.products.ProductCard
import com.alvaro.pricewise.ui.products.ProductViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onProductClick: (Long) -> Unit,
    onAddProduct: (com.alvaro.pricewise.data.model.ProductResponse) -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    val uiState by viewModel.listState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Limpiar resultados temporales al salir de la pantalla de búsqueda
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSearchResults()
        }
    }

    // Debounce: esperar 500ms tras dejar de escribir antes de buscar
    LaunchedEffect(query) {
        if (query.isBlank()) {
            viewModel.clearSearchResults()
            return@LaunchedEffect
        }
        // Si parece ASIN completo (10 chars alfanuméricos), buscar inmediatamente
        val upper = query.trim().uppercase()
        if (upper.length == 10 && upper.matches(Regex("^[A-Z0-9]{10}$"))) {
            viewModel.search(query)
        } else {
            delay(500)
            viewModel.search(query)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar Productos") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("ASIN de Amazon (ej. B0...)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    focusManager.clearFocus()
                    viewModel.search(query)
                })
            )

            // Results
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.products) { product ->
                        ProductCard(
                            product = product,
                            onClick = {
                                if (product.id != -1L) onProductClick(product.id)
                                else onAddProduct(product)
                            },
                            onAddClick = { onAddProduct(product) }
                        )
                    }
                    if (uiState.products.isEmpty() && query.isNotEmpty()) {
                        item {
                            Text(
                                "No se encontraron resultados",
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
