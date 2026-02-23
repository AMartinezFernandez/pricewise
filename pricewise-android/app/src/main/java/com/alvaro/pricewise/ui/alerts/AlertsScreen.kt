package com.alvaro.pricewise.ui.alerts

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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.alvaro.pricewise.data.model.AlertResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    onNavigateToRules: () -> Unit = {},
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Cargar al entrar y al volver (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadAlerts()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { viewModel.loadAlerts() }

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
            TopAppBar(
                title = { Text("Alertas") },
                actions = {
                    // Configurar reglas de alertas
                    IconButton(onClick = onNavigateToRules) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurar alertas")
                    }
                    // Filtro no leidas
                    FilterChip(
                        selected = uiState.filterUnreadOnly,
                        onClick = { viewModel.toggleFilter() },
                        label = { Text("No leidas") },
                        leadingIcon = if (uiState.filterUnreadOnly) {
                            { Icon(Icons.Default.FilterList, null, Modifier.size(16.dp)) }
                        } else null,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    IconButton(onClick = { viewModel.loadAlerts() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading && uiState.alerts.isEmpty() -> {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                uiState.error != null && uiState.alerts.isEmpty() -> {
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
                        Button(onClick = { viewModel.loadAlerts() }) { Text("Reintentar") }
                    }
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        if (uiState.isLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }

                        // Barra de resumen + accion masiva
                        if (uiState.alerts.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${uiState.unreadCount} sin leer de ${uiState.alerts.size}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (uiState.unreadCount > 0) {
                                        TextButton(onClick = { viewModel.markAllAsRead() }) {
                                            Icon(
                                                Icons.Default.DoneAll,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Marcar todas")
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.alerts.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.NotificationsNone,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        if (uiState.filterUnreadOnly) "No hay alertas sin leer"
                                        else "Sin alertas recientes",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(uiState.alerts, key = { it.id }) { alert ->
                                    AlertCard(
                                        alert = alert,
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
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
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
                        "Variacion: ${"%.1f".format(alert.changePercent)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = severityColor
                    )
                }
                if (alert.createdAt != null) {
                    Text(
                        formatRelativeTime(alert.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!alert.isRead) {
                IconButton(
                    onClick = { onMarkRead(alert.id) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.DoneAll,
                        contentDescription = "Marcar como leida",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

private fun formatRelativeTime(isoDate: String): String {
    return try {
        val instant = java.time.Instant.parse(isoDate)
        val now = java.time.Instant.now()
        val duration = java.time.Duration.between(instant, now)
        val minutes = duration.toMinutes()
        val hours = duration.toHours()
        val days = duration.toDays()

        when {
            minutes < 1 -> "Ahora"
            minutes < 60 -> "Hace ${minutes}min"
            hours < 24 -> "Hace ${hours}h"
            days < 7 -> "Hace ${days}d"
            else -> {
                val formatter = java.time.format.DateTimeFormatter
                    .ofPattern("dd/MM/yyyy")
                    .withZone(java.time.ZoneId.systemDefault())
                formatter.format(instant)
            }
        }
    } catch (_: Exception) {
        isoDate.take(10)
    }
}
