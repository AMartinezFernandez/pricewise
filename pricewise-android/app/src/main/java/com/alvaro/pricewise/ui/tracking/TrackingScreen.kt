package com.alvaro.pricewise.ui.tracking

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.alvaro.pricewise.ui.theme.PwDarkNavy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.ProductResponse
import com.alvaro.pricewise.data.repository.ProductRepository
import com.alvaro.pricewise.ui.common.formatPrice
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────

data class TrackingUiState(
    val products: List<ProductResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalElements: Long = 0,
    val hasMore: Boolean = false
)

@HiltViewModel
class TrackingViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrackingUiState())
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    private var currentPage = 0
    private val pageSize = 20

    init {
        loadMonitoredProducts(refresh = true)
    }

    fun loadMonitoredProducts(refresh: Boolean = false) {
        if (!refresh && uiState.value.isLoading) return
        if (refresh) currentPage = 0

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = if (refresh) null else _uiState.value.error)

            when (val result = productRepository.getMonitoredProducts(page = currentPage, size = pageSize)) {
                is Result.Success -> {
                    val page = result.data.data
                    val newProducts = page?.content ?: emptyList()
                    val currentProducts = if (refresh) emptyList() else _uiState.value.products
                    val isLast = page?.last ?: true
                    _uiState.value = _uiState.value.copy(
                        products = currentProducts + newProducts,
                        totalElements = page?.totalElements ?: 0,
                        hasMore = !isLast,
                        isLoading = false
                    )
                    if (!isLast) currentPage++
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    onProductClick: (Long) -> Unit,
    viewModel: TrackingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    // Refresh data every time the screen becomes visible (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadMonitoredProducts(refresh = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguimiento de Precios") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PwDarkNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading && uiState.products.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null && uiState.products.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(padding)
            ) {
                item {
                    Text(
                        text = "Productos en seguimiento (${uiState.totalElements})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(uiState.products) { product ->
                    TrackingProductCard(
                        product = product,
                        onClick = { onProductClick(product.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrackingProductCard(
    product: ProductResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = product.category ?: "Sin categoría",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (product.monitoringEnabled) {
                   Text(
                       text = "• En seguimiento",
                       style = MaterialTheme.typography.labelSmall,
                       color = MaterialTheme.colorScheme.primary
                   )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatPrice(product.currentPrice),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                if (product.margin != null) {
                    val color = if (product.margin < 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    Row(verticalAlignment = Alignment.CenterVertically) {
                         Icon(
                             if (product.margin < 10) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                             contentDescription = null,
                             tint = color,
                             modifier = Modifier.size(16.dp)
                         )
                         Text(
                             text = "${"%.1f".format(product.margin)}%",
                             style = MaterialTheme.typography.bodySmall,
                             color = color,
                             fontWeight = FontWeight.Bold
                         )
                    }
                }
            }
        }
    }
}
