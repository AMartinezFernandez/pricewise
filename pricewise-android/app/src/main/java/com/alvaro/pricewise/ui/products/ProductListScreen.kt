package com.alvaro.pricewise.ui.products

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaro.pricewise.data.model.ProductResponse
import com.alvaro.pricewise.ui.common.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    onProductClick: (Long) -> Unit,
    onAddProduct: (ProductResponse?) -> Unit,
    onLogout: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.listState.collectAsState()
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        viewModel.loadProducts(refresh = true)
    }

    // Abrir teclado automáticamente al activar búsqueda
    LaunchedEffect(showSearch) {
        if (showSearch) focusRequester.requestFocus()
    }

    // Cargar más al llegar al final
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= uiState.products.size - 3 && uiState.hasMore && !uiState.isLoading
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadProducts()
    }

    Scaffold(
        topBar = {
            // La búsqueda vive DENTRO del TopAppBar para respetar el
            // padding del status bar y alinearse correctamente en cualquier pantalla.
            TopAppBar(
                title = {
                    if (showSearch) {
                        TextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                viewModel.search(it)
                            },
                            placeholder = { Text("Buscar productos...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor   = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedIndicatorColor   = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = { focusManager.clearFocus() }
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = {
                                        searchQuery = ""
                                        viewModel.search("")
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpiar")
                                    }
                                }
                            }
                        )
                    } else {
                        Text("Productos")
                    }
                },
                actions = {
                    if (!showSearch) {
                        IconButton(onClick = { viewModel.loadProducts(refresh = true) }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                        }
                        IconButton(onClick = { showSearch = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    } else {
                         IconButton(onClick = {
                             showSearch = false
                             searchQuery = ""
                             focusManager.clearFocus()
                             viewModel.loadProducts(refresh = true)
                         }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar búsqueda")
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onAddProduct(null) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Añadir") },
                // Se contrae al hacer scroll para no tapar contenido
                expanded = !listState.isScrollInProgress
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading && uiState.products.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.error != null && uiState.products.isEmpty() -> {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.loadProducts(refresh = true) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.products.isEmpty() -> {
                    EmptyState(
                        message = if (searchQuery.isNotBlank())
                            "Sin resultados para \"$searchQuery\""
                        else
                            "Aún no tienes productos.\nPulsa Añadir para crear el primero.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = 12.dp,
                            bottom = 96.dp   // espacio bajo el FAB extendido
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Text(
                                text = "${uiState.totalElements} producto(s)",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        items(
                            uiState.products.filter { it.id != -1L },
                            key = { it.id }
                        ) { product ->
                            ProductCard(
                                product = product,
                                onClick = { onProductClick(product.id) },
                                onAddClick = { onAddProduct(product) }
                            )
                        }
                        if (uiState.isLoading) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp)) {
                                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ProductResponse,
    onClick: () -> Unit,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Columna izquierda: nombre, categoría, SKU
            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                if (!product.category.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = product.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!product.asin.isNullOrBlank() || !product.sku.isNullOrBlank()) {
                    Text(
                        text = "ASIN: ${product.asin ?: product.sku}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Columna derecha: precio, margen, icono monitor
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(product.currentPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (product.id == -1L) {
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onAddClick,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("Añadir", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    if (product.margin != null) {
                        val marginColor = when {
                            product.margin >= 20 -> MaterialTheme.colorScheme.secondary
                            product.margin >= 0  -> MaterialTheme.colorScheme.onSurfaceVariant
                            else                 -> MaterialTheme.colorScheme.error
                        }
                        Text(
                            text = "${"%.1f".format(product.margin)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = marginColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (product.monitoringEnabled) {
                        Spacer(Modifier.height(2.dp))
                        Icon(
                            imageVector = Icons.Default.Radar,
                            contentDescription = "Monitorización activa",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Icons.Default.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}
