package com.alvaro.pricewise.ui.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.alvaro.pricewise.data.model.AlertRuleResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertRulesScreen(
    onNavigateBack: () -> Unit,
    viewModel: AlertRulesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.actionMessage) {
        uiState.actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    if (uiState.showCreateDialog) {
        AlertRuleDialog(
            editingRule = uiState.editingRule,
            isSaving = uiState.isSaving,
            onDismiss = { viewModel.dismissDialog() },
            onCreate = { alertType, threshold, name ->
                viewModel.createRule(alertType, threshold, name)
            },
            onUpdate = { ruleId, threshold, name ->
                viewModel.updateRule(ruleId, threshold, name)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configurar Alertas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadRules() }) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showCreateDialog() }) {
                Icon(Icons.Default.Add, "Crear regla")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                uiState.isLoading && uiState.rules.isEmpty() ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))

                uiState.error != null && uiState.rules.isEmpty() ->
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline, null,
                            Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            uiState.error!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Button(onClick = { viewModel.loadRules() }) { Text("Reintentar") }
                    }

                uiState.rules.isEmpty() ->
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.NotificationsOff, null,
                            Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Sin reglas de alerta configuradas",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Pulsa + para crear tu primera regla",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                else -> {
                    Column(Modifier.fillMaxSize()) {
                        if (uiState.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(uiState.rules, key = { it.id }) { rule ->
                                AlertRuleCard(
                                    rule = rule,
                                    onToggle = { viewModel.toggleRule(rule.id) },
                                    onEdit = { viewModel.showEditDialog(rule) },
                                    onDelete = { viewModel.deleteRule(rule.id) }
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
private fun AlertRuleCard(
    rule: AlertRuleResponse,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar regla") },
            text = { Text("Esta accion no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
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
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = alertTypeIcon(rule.alertType),
                    contentDescription = null,
                    tint = if (rule.enabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        rule.name ?: alertTypeLabel(rule.alertType),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "Umbral: ${"%.1f".format(rule.threshold)}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (rule.productId != null) "Producto: ${rule.productName ?: "ID ${rule.productId}"}"
                        else "Todos los productos",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = rule.enabled, onCheckedChange = { onToggle() })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Editar")
                }
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Eliminar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertRuleDialog(
    editingRule: AlertRuleResponse?,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onCreate: (alertType: String, threshold: Double, name: String?) -> Unit,
    onUpdate: (ruleId: Long, threshold: Double?, name: String?) -> Unit
) {
    val isEditing = editingRule != null
    val alertTypes = listOf(
        "COMPETITOR_PRICE_DROP" to "Bajada de precio competidor",
        "COMPETITOR_PRICE_RISE" to "Subida de precio competidor",
        "COMPETITOR_OUT_OF_STOCK" to "Competidor sin stock",
        "PRICE_BELOW_COST" to "Precio por debajo del coste",
        "HIGH_MARGIN_OPPORTUNITY" to "Oportunidad de margen alto",
        "PRICE_MATCH_NEEDED" to "Necesario igualar precio"
    )

    var selectedType by remember { mutableStateOf(editingRule?.alertType ?: alertTypes[0].first) }
    var threshold by remember { mutableStateOf(editingRule?.threshold?.let { "%.1f".format(it) } ?: "15.0") }
    var name by remember { mutableStateOf(editingRule?.name ?: "") }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Editar regla" else "Nueva regla de alerta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!isEditing) {
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = alertTypes.find { it.first == selectedType }?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo de alerta") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            alertTypes.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = { selectedType = value; expanded = false }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = alertTypes.find { it.first == editingRule!!.alertType }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Tipo de alerta") },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val thresholdValue = threshold.toDoubleOrNull()
                    if (thresholdValue != null && thresholdValue > 0) {
                        if (isEditing) {
                            onUpdate(editingRule!!.id, thresholdValue, name.ifBlank { null })
                        } else {
                            onCreate(selectedType, thresholdValue, name.ifBlank { null })
                        }
                    }
                },
                enabled = !isSaving && (threshold.toDoubleOrNull() ?: 0.0) > 0
            ) {
                if (isSaving) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (isEditing) "Guardar" else "Crear")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

private fun alertTypeLabel(type: String): String = when (type) {
    "COMPETITOR_PRICE_DROP" -> "Bajada de precio competidor"
    "COMPETITOR_PRICE_RISE" -> "Subida de precio competidor"
    "COMPETITOR_OUT_OF_STOCK" -> "Competidor sin stock"
    "PRICE_BELOW_COST" -> "Precio por debajo del coste"
    "HIGH_MARGIN_OPPORTUNITY" -> "Oportunidad de margen alto"
    "PRICE_MATCH_NEEDED" -> "Necesario igualar precio"
    else -> type
}

private fun alertTypeIcon(type: String) = when (type) {
    "COMPETITOR_PRICE_DROP" -> Icons.Default.TrendingDown
    "COMPETITOR_PRICE_RISE" -> Icons.Default.TrendingUp
    "COMPETITOR_OUT_OF_STOCK" -> Icons.Default.RemoveShoppingCart
    "PRICE_BELOW_COST" -> Icons.Default.Warning
    "HIGH_MARGIN_OPPORTUNITY" -> Icons.Default.TrendingUp
    "PRICE_MATCH_NEEDED" -> Icons.Default.CompareArrows
    else -> Icons.Default.Notifications
}
