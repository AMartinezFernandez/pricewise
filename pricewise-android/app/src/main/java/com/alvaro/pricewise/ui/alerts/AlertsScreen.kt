package com.alvaro.pricewise.ui.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.composables.icons.lucide.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import java.util.Locale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.graphics.Color
import com.alvaro.pricewise.data.model.AlertResponse
import com.alvaro.pricewise.data.model.AlertRuleResponse
import com.alvaro.pricewise.data.model.ProductResponse
import com.alvaro.pricewise.ui.theme.PwCyan
import com.alvaro.pricewise.ui.theme.PwCyanDark
import com.alvaro.pricewise.ui.theme.PwDarkNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsScreen(
    viewModel: AlertsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    if (uiState.showCreateDialog) {
        CreateAlertDialog(
            products = uiState.products,
            isLoadingProducts = uiState.isLoadingProducts,
            isSaving = uiState.isSaving,
            onDismiss = { viewModel.dismissCreateDialog() },
            onCreate = { alertType, threshold, name, productId, targetPrice ->
                viewModel.createRule(alertType, threshold, name, productId, targetPrice)
            }
        )
    }

    val editingRule = uiState.editingRule
    if (uiState.showEditDialog && editingRule != null) {
        EditAlertDialog(
            rule = editingRule,
            isSaving = uiState.isSaving,
            onDismiss = { viewModel.dismissEditDialog() },
            onSave = { threshold, name, targetPrice ->
                viewModel.updateRule(
                    id = editingRule.id,
                    threshold = threshold,
                    name = name,
                    targetPrice = targetPrice
                )
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Alertas") },
                actions = {
                    IconButton(onClick = { viewModel.loadAll() }) {
                        Icon(Lucide.RefreshCw, contentDescription = "Actualizar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PwDarkNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(Lucide.Plus, "Crear alerta")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.loadAll() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tabs
            PrimaryTabRow(selectedTabIndex = uiState.selectedTab) {
                Tab(
                    selected = uiState.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = { Text("Mis alertas") },
                    icon = { Icon(Lucide.Bell, null, Modifier.size(18.dp)) }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Historial")
                            if (uiState.unreadCount > 0) {
                                Spacer(Modifier.width(6.dp))
                                Badge { Text("${uiState.unreadCount}") }
                            }
                        }
                    },
                    icon = { Icon(Lucide.Clock, null, Modifier.size(18.dp)) }
                )
            }

            when (uiState.selectedTab) {
                0 -> RulesTab(uiState, viewModel)
                1 -> HistoryTab(uiState, viewModel)
            }
        }
        } // PullToRefreshBox
    }
}

// ─── TAB: Mis alertas (reglas configuradas) ─────────────────

@Composable
private fun RulesTab(uiState: AlertsUiState, viewModel: AlertsViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoadingRules && uiState.rules.isEmpty() -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            uiState.error != null && uiState.rules.isEmpty() -> {
                ErrorState(uiState.error!!, onRetry = { viewModel.loadRules() },
                    modifier = Modifier.align(Alignment.Center))
            }
            uiState.rules.isEmpty() -> {
                EmptyState(
                    icon = Lucide.BellOff,
                    message = "No tienes alertas configuradas",
                    subtitle = "Pulsa + para crear tu primera alerta",
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isLoadingRules) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.rules, key = { it.id }) { rule ->
                            AlertRuleCard(
                                rule = rule,
                                onToggle = { viewModel.toggleRule(rule.id) },
                                onDelete = { viewModel.deleteRule(rule.id) },
                                onEdit = { viewModel.showEditDialog(rule) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AlertRuleCard(
    rule: AlertRuleResponse,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    val typeLabel = alertTypeLabel(rule.alertType)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar alerta") },
            text = { Text("Se eliminara esta alerta. Esta accion no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (rule.enabled) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = alertTypeIcon(rule.alertType),
                contentDescription = null,
                tint = if (rule.enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    rule.name ?: typeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (rule.productName != null) {
                    Text(
                        rule.productName.take(30) + if (rule.productName.length > 30) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                val thresholdText = buildString {
                    append(typeLabel)
                    append("  •  Umbral: ${String.format(Locale.US, "%.1f", rule.threshold)}%")
                    if (rule.targetPrice != null && rule.targetPrice > 0) {
                        append("  •  Precio: ${String.format(Locale.US, "%.2f", rule.targetPrice)} €")
                    }
                }
                Text(
                    thresholdText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(horizontal = 4.dp),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = PwCyan,
                    checkedBorderColor = PwCyanDark,
                    uncheckedThumbColor = Color(0xFF9E9E9E),
                    uncheckedTrackColor = Color(0xFF4A5568),
                    uncheckedBorderColor = Color(0xFF4A5568)
                )
            )
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Lucide.Pencil,
                    contentDescription = "Editar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Lucide.Trash2,
                    contentDescription = "Eliminar",
                    tint = Color(0xFFEF5350),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─── TAB: Historial (alertas generadas) ─────────────────────

@Composable
private fun HistoryTab(uiState: AlertsUiState, viewModel: AlertsViewModel) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoadingAlerts && uiState.alerts.isEmpty() -> {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            uiState.error != null && uiState.alerts.isEmpty() -> {
                ErrorState(uiState.error!!, onRetry = { viewModel.loadAlerts() },
                    modifier = Modifier.align(Alignment.Center))
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (uiState.isLoadingAlerts) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    // Barra filtro + marcar todas
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = uiState.filterUnreadOnly,
                            onClick = { viewModel.toggleFilter() },
                            label = { Text("No leidas") },
                            leadingIcon = if (uiState.filterUnreadOnly) {
                                { Icon(Lucide.Filter, null, Modifier.size(16.dp)) }
                            } else null
                        )
                        if (uiState.unreadCount > 0) {
                            TextButton(onClick = { viewModel.markAllAsRead() }) {
                                Icon(Lucide.CheckCheck, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Marcar todas")
                            }
                        }
                    }

                    if (uiState.alerts.isEmpty()) {
                        EmptyState(
                            icon = Lucide.Clock,
                            message = if (uiState.filterUnreadOnly) "No hay alertas sin leer"
                            else "Sin alertas recientes",
                            subtitle = "Las alertas se generan cuando cambian los precios",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
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
                    "CRITICAL" -> Lucide.CircleAlert
                    "WARNING"  -> Lucide.TriangleAlert
                    else       -> Lucide.Info
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
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
                        "Variacion: ${String.format(Locale.US, "%.1f", alert.changePercent)}%",
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
                        Lucide.CheckCheck,
                        contentDescription = "Marcar como leida",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─── Componentes compartidos ─────────────────────────────────

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            Lucide.CircleAlert,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Text(
            error,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        Button(onClick = onRetry) { Text("Reintentar") }
    }
}

// ─── Dialogo crear alerta ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateAlertDialog(
    products: List<ProductResponse>,
    isLoadingProducts: Boolean,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (alertType: String, threshold: Double, name: String?, productId: Long?, targetPrice: Double?) -> Unit
) {
    val alertTypes = listOf(
        "COMPETITOR_PRICE_DROP" to "Bajada de precio competidor",
        "COMPETITOR_PRICE_RISE" to "Subida de precio competidor",
        "COMPETITOR_OUT_OF_STOCK" to "Competidor sin stock",
        "PRICE_BELOW_COST" to "Precio por debajo del coste",
        "HIGH_MARGIN_OPPORTUNITY" to "Oportunidad de margen alto",
        "PRICE_MATCH_NEEDED" to "Necesario igualar precio"
    )

    var selectedType by remember { mutableStateOf(alertTypes[0].first) }
    var threshold by remember { mutableStateOf("15.0") }
    var targetPrice by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<ProductResponse?>(null) }
    var typeExpanded by remember { mutableStateOf(false) }
    var productExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Nueva alerta",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(Modifier.height(4.dp))

                // Selector de producto
                ExposedDropdownMenuBox(
                    expanded = productExpanded,
                    onExpandedChange = { productExpanded = !productExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedProduct?.let { it.name.take(25) + if (it.name.length > 25) "..." else "" }
                            ?: "Selecciona un producto",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Producto") },
                        trailingIcon = {
                            if (isLoadingProducts) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(productExpanded)
                            }
                        },
                        modifier = Modifier
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = productExpanded,
                        onDismissRequest = { productExpanded = false }
                    ) {
                        products.forEach { product ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        product.name.take(30) + if (product.name.length > 30) "..." else "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                },
                                onClick = {
                                    selectedProduct = product
                                    productExpanded = false
                                }
                            )
                        }
                    }
                }

                // Selector de tipo de alerta
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = alertTypes.find { it.first == selectedType }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de alerta") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        alertTypes.forEach { (value, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { selectedType = value; typeExpanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text("Umbral (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Porcentaje de cambio que activa la alerta") }
                )

                OutlinedTextField(
                    value = targetPrice,
                    onValueChange = { targetPrice = it },
                    label = { Text("Precio objetivo (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Opcional: avisar cuando el precio alcance este valor") }
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val thresholdValue = threshold.replace(',', '.').toDoubleOrNull()
                            val targetPriceValue = targetPrice.replace(',', '.').toDoubleOrNull()
                            if (thresholdValue != null && thresholdValue > 0) {
                                onCreate(
                                    selectedType,
                                    thresholdValue,
                                    name.ifBlank { null },
                                    selectedProduct?.id,
                                    targetPriceValue?.takeIf { it > 0 }
                                )
                            }
                        },
                        enabled = !isSaving
                                && selectedProduct != null
                                && (threshold.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Crear")
                        }
                    }
                }
            }
        }
    }
}

// ─── Dialogo editar alerta ───────────────────────────────────

@Composable
private fun EditAlertDialog(
    rule: AlertRuleResponse,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (threshold: Double, name: String?, targetPrice: Double?) -> Unit
) {
    var threshold by remember { mutableStateOf(String.format(Locale.US, "%.1f", rule.threshold)) }
    var name by remember { mutableStateOf(rule.name ?: "") }
    var targetPrice by remember { mutableStateOf(rule.targetPrice?.let { String.format(Locale.US, "%.2f", it) } ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Editar alerta",
                    style = MaterialTheme.typography.headlineSmall
                )

                // Info de la regla (solo lectura)
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            alertTypeLabel(rule.alertType),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (rule.productName != null) {
                            Text(
                                rule.productName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                OutlinedTextField(
                    value = threshold,
                    onValueChange = { threshold = it },
                    label = { Text("Umbral (%)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Porcentaje de cambio que activa la alerta") }
                )

                OutlinedTextField(
                    value = targetPrice,
                    onValueChange = { targetPrice = it },
                    label = { Text("Precio objetivo (€)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text("Opcional: avisar cuando el precio alcance este valor") }
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(4.dp))

                // Botones
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val thresholdValue = threshold.replace(',', '.').toDoubleOrNull()
                            val targetPriceValue = targetPrice.replace(',', '.').toDoubleOrNull()
                            if (thresholdValue != null && thresholdValue > 0) {
                                onSave(thresholdValue, name.ifBlank { null }, targetPriceValue?.takeIf { it > 0 })
                            }
                        },
                        enabled = !isSaving
                                && (threshold.replace(',', '.').toDoubleOrNull() ?: 0.0) > 0
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Guardar")
                        }
                    }
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────

private fun alertTypeLabel(type: String): String = when (type) {
    "COMPETITOR_PRICE_DROP" -> "Bajada precio"
    "COMPETITOR_PRICE_RISE" -> "Subida precio"
    "COMPETITOR_OUT_OF_STOCK" -> "Sin stock"
    "PRICE_BELOW_COST" -> "Bajo coste"
    "HIGH_MARGIN_OPPORTUNITY" -> "Margen alto"
    "PRICE_MATCH_NEEDED" -> "Igualar precio"
    else -> type
}

private fun alertTypeIcon(type: String): androidx.compose.ui.graphics.vector.ImageVector = when (type) {
    "COMPETITOR_PRICE_DROP" -> Lucide.TrendingDown
    "COMPETITOR_PRICE_RISE" -> Lucide.TrendingUp
    "COMPETITOR_OUT_OF_STOCK" -> Lucide.PackageX
    "PRICE_BELOW_COST" -> Lucide.TriangleAlert
    "HIGH_MARGIN_OPPORTUNITY" -> Lucide.Star
    "PRICE_MATCH_NEEDED" -> Lucide.Scale
    else -> Lucide.Bell
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
