package com.alvaro.pricewise.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.alvaro.pricewise.ui.common.formatPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceHistoryScreen(
    productId: Long,
    onNavigateBack: () -> Unit,
    viewModel: PriceHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(productId) {
        viewModel.loadHistory(productId)
    }

    // Load more when reaching the end
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= uiState.entries.size - 3 && uiState.hasMore && !uiState.isLoading
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) viewModel.loadMore()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Historial de precios")
                        if (uiState.productName.isNotEmpty()) {
                            Text(
                                uiState.productName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading && uiState.entries.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            uiState.error != null && uiState.entries.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { viewModel.loadHistory(productId, refresh = true) }) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            uiState.entries.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay historial de precios",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                Column(Modifier.fillMaxSize().padding(padding)) {
                    // Summary bar
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "${uiState.totalElements} cambios de precio registrados",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.entries, key = { it.id }) { entry ->
                            PriceHistoryItem(entry)
                        }

                        if (uiState.isLoading && uiState.entries.isNotEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
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
private fun PriceHistoryItem(entry: com.alvaro.pricewise.data.model.PriceHistoryResponse) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Change type icon
                val (icon, color, label) = when (entry.changeType) {
                    "INCREASE" -> Triple(
                        Icons.AutoMirrored.Filled.TrendingUp,
                        MaterialTheme.colorScheme.error,
                        "Subida"
                    )
                    "DECREASE" -> Triple(
                        Icons.AutoMirrored.Filled.TrendingDown,
                        MaterialTheme.colorScheme.primary,
                        "Bajada"
                    )
                    "INITIAL" -> Triple(
                        Icons.Default.FiberNew,
                        MaterialTheme.colorScheme.tertiary,
                        "Precio inicial"
                    )
                    else -> Triple(
                        Icons.Default.Remove,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                        "Sin cambio"
                    )
                }

                Icon(
                    icon,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = color
                )

                Column {
                    Text(
                        formatPrice(entry.price),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (entry.previousPrice != null && entry.changeType != "INITIAL") {
                        Text(
                            "Anterior: ${formatPrice(entry.previousPrice)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (!entry.changeReason.isNullOrBlank()) {
                        Text(
                            entry.changeReason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (entry.percentageChange != null && entry.changeType != "INITIAL") {
                    val changeColor = when (entry.changeType) {
                        "INCREASE" -> MaterialTheme.colorScheme.error
                        "DECREASE" -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Text(
                        "${if (entry.percentageChange > 0) "+" else ""}${"%.1f".format(entry.percentageChange)}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = changeColor
                    )
                }
                Text(
                    entry.recordedAt?.take(10) ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Show time portion
                val time = entry.recordedAt?.let {
                    if (it.length >= 16) it.substring(11, 16) else null
                }
                if (time != null) {
                    Text(
                        time,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
