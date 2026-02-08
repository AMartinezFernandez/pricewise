package com.alvaro.pricewise.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProductScreen(
    onProductCreated: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel(),
    initialName: String = "",
    initialSku: String = "",
    initialPrice: String = "",
    initialCategory: String = "",
    initialBrand: String = "",
    initialDescription: String = ""
) {
    val uiState by viewModel.formState.collectAsState()

    var name by remember { mutableStateOf(initialName) }
    var sku by remember { mutableStateOf(initialSku) }
    var currentPrice by remember { mutableStateOf(initialPrice) }
    var costPrice by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(initialCategory) }
    var brand by remember { mutableStateOf(initialBrand) }
    var description by remember { mutableStateOf(initialDescription) }
    var monitoringEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.success) {
        if (uiState.success) {
            viewModel.resetFormState()
            onProductCreated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo producto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        // Box centra el formulario en pantallas anchas (tablets)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ─── Datos principales ────────────────────────────────────
            SectionTitle("Datos principales")

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre del producto *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = uiState.error != null && name.isBlank()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = currentPrice,
                    onValueChange = { currentPrice = it },
                    label = { Text("Precio venta (€) *") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = uiState.error != null && currentPrice.isBlank()
                )
                OutlinedTextField(
                    value = costPrice,
                    onValueChange = { costPrice = it },
                    label = { Text("Precio coste (€)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }

            OutlinedTextField(
                value = sku,
                onValueChange = { sku = it },
                label = { Text("SKU / ASIN Amazon") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text("Si el SKU es un ASIN de Amazon (B0...) se activará la monitorización") }
            )

            // ─── Clasificación ────────────────────────────────────────
            SectionTitle("Clasificación")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Marca") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4
            )

            // ─── Monitorización ───────────────────────────────────────
            SectionTitle("Monitorización")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Monitorización activa", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "El sistema actualizará el precio de Amazon cada 6 horas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = monitoringEnabled,
                    onCheckedChange = { monitoringEnabled = it }
                )
            }

            // ─── Error ────────────────────────────────────────────────
            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.error!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // ─── Botón guardar ────────────────────────────────────────
            Button(
                onClick = {
                    viewModel.createProduct(
                        name, sku, currentPrice, costPrice,
                        category, brand, description, monitoringEnabled
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Guardar producto")
                }
            }

            Spacer(Modifier.height(16.dp))
        }
        } // Box
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary
    )
}
