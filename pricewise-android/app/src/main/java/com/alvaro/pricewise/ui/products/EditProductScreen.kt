package com.alvaro.pricewise.ui.products

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.composables.icons.lucide.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.alvaro.pricewise.ui.theme.PwDarkNavy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    productId: Long,
    onProductUpdated: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: ProductViewModel = hiltViewModel()
) {
    val detailState by viewModel.detailState.collectAsState()
    val formState by viewModel.formState.collectAsState()

    // Cargar producto al abrir
    LaunchedEffect(productId) {
        viewModel.resetFormState()
        viewModel.loadProduct(productId)
    }

    // Inicializar campos cuando carga el producto
    val product = detailState.product
    var name by remember { mutableStateOf("") }
    var asin by remember { mutableStateOf("") }
    var currentPrice by remember { mutableStateOf("") }
    var costPrice by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var monitoringEnabled by remember { mutableStateOf(true) }
    var initialized by remember { mutableStateOf(false) }

    LaunchedEffect(product) {
        if (product != null && !initialized) {
            name = product.name
            asin = product.asin ?: product.sku ?: ""
            currentPrice = product.currentPrice?.toBigDecimal()?.toPlainString() ?: ""
            costPrice = product.costPrice?.toBigDecimal()?.toPlainString() ?: ""
            category = product.category ?: ""
            brand = product.brand ?: ""
            description = product.description ?: ""
            monitoringEnabled = product.monitoringEnabled ?: true
            initialized = true
        }
    }

    // Navegar al guardar con éxito
    LaunchedEffect(formState.success) {
        if (formState.success) {
            viewModel.resetFormState()
            onProductUpdated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar producto") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Lucide.ArrowLeft, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PwDarkNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        when {
            detailState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            detailState.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Lucide.CircleAlert, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text(detailState.error ?: "Error al cargar producto")
                    }
                }
            }

            else -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
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

                        SectionTitle("Datos principales")

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nombre del producto *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            isError = formState.error != null && name.isBlank()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = currentPrice,
                                onValueChange = { currentPrice = it },
                                label = { Text("Precio venta (\u20ac) *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = formState.error != null && currentPrice.isBlank()
                            )
                            OutlinedTextField(
                                value = costPrice,
                                onValueChange = { costPrice = it },
                                label = { Text("Precio coste (\u20ac) *") },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                isError = formState.error != null && costPrice.isBlank()
                            )
                        }

                        OutlinedTextField(
                            value = asin,
                            onValueChange = { asin = it },
                            label = { Text("ASIN de Amazon") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            supportingText = { Text("C\u00f3digo ASIN de Amazon (10 caracteres, ej. B0D2QM4QBT)") }
                        )

                        SectionTitle("Clasificaci\u00f3n")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { category = it },
                                label = { Text("Categor\u00eda") },
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
                            label = { Text("Descripci\u00f3n") },
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            maxLines = 4
                        )

                        SectionTitle("Monitorizaci\u00f3n")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Monitorizaci\u00f3n activa", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "El sistema actualizar\u00e1 el precio de Amazon cada 6 horas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = monitoringEnabled,
                                onCheckedChange = { monitoringEnabled = it }
                            )
                        }

                        if (formState.error != null) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = formState.error!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.updateProduct(
                                    id = productId,
                                    name = name,
                                    asin = asin,
                                    currentPrice = currentPrice,
                                    costPrice = costPrice,
                                    category = category,
                                    brand = brand,
                                    description = description,
                                    monitoringEnabled = monitoringEnabled
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = !formState.isLoading
                        ) {
                            if (formState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Guardar cambios")
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
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
