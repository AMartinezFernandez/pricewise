package com.alvaro.pricewise.ui.products

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.alvaro.pricewise.ui.common.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    onNavigateBack: () -> Unit,
    onNavigateToHistory: (Long) -> Unit = {},
    viewModel: ProductViewModel = hiltViewModel()
) {
    val uiState by viewModel.detailState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSyncDetails by remember { mutableStateOf(false) }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
        viewModel.loadPriceHistory(productId)
    }

    LaunchedEffect(uiState.deleteSuccess) {
        if (uiState.deleteSuccess) onNavigateBack()
    }

    // Auto-expandir el desplegable cuando llega un nuevo resultado
    LaunchedEffect(uiState.syncResult) {
        if (uiState.syncResult != null) {
            showSyncDetails = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        uiState.product?.name ?: "Producto",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
                    // ─── Mis Precios ─────────────────────────────────────
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Mis Precios",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Mi precio de venta", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    formatPrice(product.currentPrice),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Precio de coste", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    if (product.costPrice != null) formatPrice(product.costPrice)
                                    else "No especificado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            if (product.costPrice != null && product.costPrice > 0) {
                                val profitMargin = ((product.currentPrice - product.costPrice) / product.costPrice) * 100
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Margen sobre coste", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        "${"%.1f".format(profitMargin)}%",
                                        color = if (profitMargin > 0) MaterialTheme.colorScheme.primary
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
                            if (!product.asin.isNullOrBlank()) {
                                DetailRow("ASIN", product.asin)
                            } else if (!product.sku.isNullOrBlank()) {
                                DetailRow("ASIN", product.sku)
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

                    // ─── Comparativa con Amazon ──────────────────────────
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Comparativa con Amazon",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            if (product.asin.isNullOrBlank() && product.sku.isNullOrBlank()) {
                                Text(
                                    "Añade el ASIN de Amazon para poder consultar su precio.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                // Datos para cálculos: usar syncResult si acaba de hacerse, si no, el precio persistido
                                val amazonPrice = uiState.syncResult?.price ?: product.amazonPrice
                                val diffVsSale = if (amazonPrice != null && product.currentPrice > 0)
                                    ((amazonPrice - product.currentPrice) / product.currentPrice) * 100 else null
                                val diffVsCost = if (amazonPrice != null && product.costPrice != null && product.costPrice > 0)
                                    ((amazonPrice - product.costPrice) / product.costPrice) * 100 else null

                                // ─── Campos fijos siempre visibles ───
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Mi precio de venta", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        formatPrice(product.currentPrice),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Mi precio de coste", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (product.costPrice != null) formatPrice(product.costPrice) else "—",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Precio en Amazon", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        if (amazonPrice != null) formatPrice(amazonPrice) else "—",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (amazonPrice != null) MaterialTheme.colorScheme.onSurface
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Diferencia vs Amazon", style = MaterialTheme.typography.bodyMedium)
                                    if (diffVsSale != null) {
                                        val color = when {
                                            diffVsSale > 5 -> MaterialTheme.colorScheme.primary
                                            diffVsSale < -5 -> MaterialTheme.colorScheme.error
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                        Text(
                                            "${if (diffVsSale > 0) "+" else ""}${"%.1f".format(diffVsSale)}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = color,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Text("—", style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Amazon vs coste", style = MaterialTheme.typography.bodyMedium)
                                    if (diffVsCost != null) {
                                        Text(
                                            "${if (diffVsCost > 0) "+" else ""}${"%.1f".format(diffVsCost)}%",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (diffVsCost > 0) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    } else {
                                        Text("—", style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                // ─── Error de sync ───
                                if (uiState.syncError != null) {
                                    Text(
                                        text = uiState.syncError!!,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                // ─── Botón buscar precio ───
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
                                        Text("Consultando Amazon...")
                                    } else {
                                        Icon(Icons.Default.CloudSync, contentDescription = null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Buscar precio ahora en Amazon")
                                    }
                                }

                                // ─── Desplegable con detalle del resultado ───
                                AnimatedVisibility(
                                    visible = showSyncDetails && uiState.syncResult != null,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    val sync = uiState.syncResult
                                    if (sync != null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.small,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    "Resultado de la consulta",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )

                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                    Text("Precio Amazon ES", style = MaterialTheme.typography.bodySmall)
                                                    Text(
                                                        formatPrice(sync.price),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }

                                                Text(
                                                    if (sync.available) "En stock" else "Sin stock",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = if (sync.available) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.error
                                                )

                                                val syncTitle = sync.competitorProductTitle ?: sync.title
                                                if (!syncTitle.isNullOrBlank()) {
                                                    Text(
                                                        syncTitle,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2
                                                    )
                                                }

                                                Spacer(Modifier.height(4.dp))

                                                Button(
                                                    onClick = {
                                                        showSyncDetails = false
                                                        // Limpiar syncResult para que al recargar use los datos
                                                        // persistidos del backend (amazonPrice en ProductResponse)
                                                        viewModel.clearDetailSyncResult()
                                                        viewModel.loadProduct(productId)
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null)
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Actualizar con los nuevos datos")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ─── Historial de precios ──────────────────────────
                    if (uiState.priceHistory.isNotEmpty() || uiState.isLoadingHistory) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Historial de precios",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (uiState.isLoadingHistory) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp).align(Alignment.CenterHorizontally),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    uiState.priceHistory.forEach { entry ->
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val (icon, color) = when (entry.changeType) {
                                                    "INCREASE" -> Icons.AutoMirrored.Filled.TrendingUp to MaterialTheme.colorScheme.error
                                                    "DECREASE" -> Icons.AutoMirrored.Filled.TrendingDown to MaterialTheme.colorScheme.primary
                                                    "INITIAL" -> Icons.Default.FiberNew to MaterialTheme.colorScheme.onSurfaceVariant
                                                    else -> Icons.Default.Remove to MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                                Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
                                                Column {
                                                    Text(
                                                        formatPrice(entry.price),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                    if (entry.percentageChange != null && entry.changeType != "INITIAL") {
                                                        Text(
                                                            "${if (entry.percentageChange > 0) "+" else ""}${"%.1f".format(entry.percentageChange)}%",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = color
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                entry.recordedAt?.take(10) ?: "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (entry != uiState.priceHistory.last()) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                                        }
                                    }

                                    // Botón para ver historial completo
                                    OutlinedButton(
                                        onClick = { onNavigateToHistory(productId) },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.History,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text("Ver historial completo")
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
