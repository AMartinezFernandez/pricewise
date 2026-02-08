package com.alvaro.pricewise.ui.recommendations

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaro.pricewise.data.model.AlertResponse
import com.alvaro.pricewise.data.model.RecommendationResponse
import com.alvaro.pricewise.ui.common.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecommendationsScreen(
    viewModel: RecommendationsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Recomendaciones", "Alertas")

    LaunchedEffect(Unit) { viewModel.loadAll() }

    // Snackbar para acciones
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Análisis de precios") },
                    actions = {
                        IconButton(onClick = { viewModel.loadAll() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                        }
                    }
                )

                // Dashboard resumen
                uiState.dashboard?.let { dash ->
                    Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DashboardChip(
                                label = "Productos",
                                value = "${dash.totalProducts}"
                            )
                            VerticalDivider(
                                modifier = Modifier.height(40.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            )
                            DashboardChip(
                                label = "Pendientes",
                                value = "${dash.pendingRecommendations}",
                                highlighted = dash.pendingRecommendations > 0
                            )
                            VerticalDivider(
                                modifier = Modifier.height(40.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                            )
                            DashboardChip(
                                label = "Alertas sin leer",
                                value = "${dash.unreadAlerts}",
                                highlighted = dash.unreadAlerts > 0
                            )
                        }
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title) },
                            icon = {
                                when (index) {
                                    0 -> Icon(Icons.Default.Lightbulb, null, Modifier.size(18.dp))
                                    1 -> Icon(Icons.Default.Notifications, null, Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Mostrar contenido aunque esté cargando (evita parpadeo al refrescar)
            when {
                // Solo pantalla de carga en la primera carga (sin datos previos)
                uiState.isLoading && uiState.dashboard == null -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                // Error sin datos: pantalla de error con reintentar
                uiState.error != null && uiState.dashboard == null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
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
                            uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { viewModel.loadAll() }) { Text("Reintentar") }
                    }
                }
                // Contenido normal (con indicador de carga pequeño si está refrescando)
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Barra de progreso fina durante refresco
                        if (uiState.isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        // Error en refresco (con datos ya cargados): snackbar ya lo muestra,
                        // pero también ponemos un aviso inline si hay datos
                        if (uiState.error != null) {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = uiState.error!!,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            if (selectedTab == 0) {
                                RecommendationsList(
                                    recommendations = uiState.recommendations,
                                    onApply = { viewModel.applyRecommendation(it) },
                                    onDismiss = { viewModel.dismissRecommendation(it) }
                                )
                            } else {
                                AlertsList(
                                    alerts = uiState.alerts,
                                    onMarkRead = { viewModel.markAlertRead(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardChip(label: String, value: String, highlighted: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlighted) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun RecommendationsList(
    recommendations: List<RecommendationResponse>,
    onApply: (Long) -> Unit,
    onDismiss: (Long) -> Unit
) {
    if (recommendations.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Text("Sin recomendaciones pendientes", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(recommendations, key = { it.id }) { rec ->
            RecommendationCard(rec = rec, onApply = onApply, onDismiss = onDismiss)
        }
    }
}

@Composable
private fun RecommendationCard(
    rec: RecommendationResponse,
    onApply: (Long) -> Unit,
    onDismiss: (Long) -> Unit
) {
    val priorityColor = when (rec.priority) {
        "URGENT" -> MaterialTheme.colorScheme.error
        "HIGH"   -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        "MEDIUM" -> MaterialTheme.colorScheme.primary
        else     -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    rec.productName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Badge(containerColor = priorityColor) {
                    Text(
                        priorityLabel(rec.priority),
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Text(
                recommendationTypeLabel(rec.recommendationType),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )

            if (rec.reason != null) {
                Text(
                    rec.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tu precio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatPrice(rec.currentPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Amazon", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatPrice(rec.competitorPrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Sugerido", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        formatPrice(rec.suggestedPrice),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onDismiss(rec.id) },
                    modifier = Modifier.weight(1f)
                ) { Text("Descartar") }

                Button(
                    onClick = { onApply(rec.id) },
                    modifier = Modifier.weight(1f)
                ) { Text("Aplicar") }
            }
        }
    }
}

@Composable
private fun AlertsList(
    alerts: List<AlertResponse>,
    onMarkRead: (Long) -> Unit
) {
    if (alerts.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.NotificationsNone,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text("Sin alertas recientes", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(alerts, key = { it.id }) { alert ->
            AlertCard(alert = alert, onMarkRead = onMarkRead)
        }
    }
}

@Composable
private fun AlertCard(alert: AlertResponse, onMarkRead: (Long) -> Unit) {
    val severityColor = when (alert.severity) {
        "CRITICAL" -> MaterialTheme.colorScheme.error
        "WARNING"  -> MaterialTheme.colorScheme.tertiary
        else       -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (!alert.isRead) MaterialTheme.colorScheme.surface
                             else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = when (alert.severity) {
                    "CRITICAL" -> Icons.Default.Error
                    "WARNING"  -> Icons.Default.Warning
                    else       -> Icons.Default.Info
                },
                contentDescription = null,
                tint = severityColor,
                modifier = Modifier.size(24.dp).padding(top = 2.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    alert.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!alert.isRead) FontWeight.Bold else FontWeight.Normal
                )
                if (alert.productName != null) {
                    Text(
                        alert.productName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (alert.message != null) {
                    Text(
                        alert.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (alert.changePercent != null) {
                    Text(
                        "Variación: ${"%.1f".format(alert.changePercent)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = severityColor
                    )
                }
            }
            if (!alert.isRead) {
                IconButton(
                    onClick = { onMarkRead(alert.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.DoneAll, contentDescription = "Marcar como leída", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
