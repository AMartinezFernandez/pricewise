package com.alvaro.pricewise.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaro.pricewise.ui.common.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    onNavigateBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.detailState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.product?.name ?: "Producto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Eliminar",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            uiState.error != null -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
            }

            uiState.product != null -> {
                val product = uiState.product!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ─── Precios ─────────────────────────────────────────
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Precios",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Precio de venta", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    formatPrice(product.currentPrice),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (product.costPrice != null) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Precio de coste", style = MaterialTheme.typography.bodyMedium)
                                    Text(formatPrice(product.costPrice), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            if (product.margin != null) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Margen bruto", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${"%.1f".format(product.margin)}%",
                                        color = if (product.margin > 0) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // ─── Ficha del producto ───────────────────────────────
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Ficha",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (!product.sku.isNullOrBlank()) {
                                DetailRow("SKU / ASIN", product.sku)
                            }
                            if (!product.ean.isNullOrBlank()) {
                                DetailRow("EAN", product.ean)
                            }
                            if (!product.category.isNullOrBlank()) {
                                DetailRow("Categoría", product.category)
                            }
                            if (!product.brand.isNullOrBlank()) {
                                DetailRow("Marca", product.brand)
                            }
                            if (!product.description.isNullOrBlank()) {
                                HorizontalDivider()
                                Text(
                                    text = product.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // ─── Amazon / Keepa ───────────────────────────────────
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                "Precio en Amazon",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (product.sku.isNullOrBlank()) {
                                Text(
                                    "Añade el ASIN de Amazon en el campo SKU para poder consultar su precio.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                // Resultado del último sync
                                if (uiState.syncResult != null) {
                                    val sync = uiState.syncResult!!
                                    SyncResultCard(
                                        price = sync.price,
                                        title = sync.competitorProductTitle ?: sync.title,
                                        available = sync.available,
                                        ownPrice = product.currentPrice
                                    )
                                }

                                if (uiState.syncError != null) {
                                    Text(
                                        text = uiState.syncError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Button(
                                    onClick = { viewModel.syncWithAmazon(productId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !uiState.isSyncing
                                ) {
                                    if (uiState.isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Consultando Keepa...")
                                    } else {
                                        Icon(Icons.Default.CloudSync, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Consultar precio en Amazon")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Diálogo de confirmación de borrado
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar producto") },
            text = { Text("¿Estás seguro? El producto se marcará como inactivo y no aparecerá en tu catálogo.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteProduct(productId)
                    }
                ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SyncResultCard(
    price: Double,
    title: String?,
    available: Boolean,
    ownPrice: Double
) {
    val diff = ((price - ownPrice) / ownPrice) * 100
    val diffColor = when {
        diff > 10  -> MaterialTheme.colorScheme.primary   // Nuestro precio es más barato
        diff < -10 -> MaterialTheme.colorScheme.error     // Nuestro precio es más caro
        else       -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Precio Amazon ES", style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatPrice(price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    if (available) "En stock" else "Sin stock",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (available) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Text(
                    "${if (diff > 0) "+" else ""}${"%.1f".format(diff)}% vs tu precio",
                    style = MaterialTheme.typography.bodySmall,
                    color = diffColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (!title.isNullOrBlank()) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
        }
    }
}
