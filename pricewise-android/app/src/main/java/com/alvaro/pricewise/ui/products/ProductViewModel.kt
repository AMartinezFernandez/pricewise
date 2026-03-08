package com.alvaro.pricewise.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.*
import com.alvaro.pricewise.data.repository.ProductRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListUiState(
    val products: List<ProductResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalElements: Long = 0,
    val currentPage: Int = 0,
    val hasMore: Boolean = false,
    val searchQuery: String = ""
)

data class ProductDetailUiState(
    val product: ProductResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val syncResult: CompetitorPriceResponse? = null,
    val isSyncing: Boolean = false,
    val syncError: String? = null,
    val deleteSuccess: Boolean = false,
    val priceHistory: List<PriceHistoryResponse> = emptyList(),
    val isLoadingHistory: Boolean = false
)

data class ProductFormUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(ProductListUiState())
    val listState: StateFlow<ProductListUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(ProductDetailUiState())
    val detailState: StateFlow<ProductDetailUiState> = _detailState.asStateFlow()

    private val _formState = MutableStateFlow(ProductFormUiState())
    val formState: StateFlow<ProductFormUiState> = _formState.asStateFlow()

    // ─── Listado ────────────────────────────────────────────────────────

    fun loadProducts(refresh: Boolean = false) {
        val page = if (refresh) 0 else _listState.value.currentPage
        viewModelScope.launch {
            if (refresh) _listState.value = ProductListUiState(isLoading = true)
            else _listState.value = _listState.value.copy(isLoading = true)

            when (val result = repository.getProducts(page)) {
                is Result.Success -> {
                    val pageData = result.data.data
                    val newProducts = if (refresh) pageData?.content ?: emptyList()
                    else _listState.value.products + (pageData?.content ?: emptyList())
                    _listState.value = ProductListUiState(
                        products = newProducts,
                        totalElements = pageData?.totalElements ?: 0,
                        currentPage = (pageData?.pageNumber ?: 0) + 1,
                        hasMore = pageData?.hasNext ?: false
                    )
                }
                is Result.Error -> _listState.value = _listState.value.copy(
                    isLoading = false, error = result.message
                )
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _listState.value = ProductListUiState(isLoading = true, searchQuery = query)
            
            val trimmedQuery = query.trim()
            // Check if query is an ASIN (10 chars, alphanumeric, case-insensitive)
            val upperQuery = trimmedQuery.uppercase()
            val isAsin = upperQuery.length == 10 && upperQuery.matches(Regex("^[A-Z0-9]{10}$"))

            if (isAsin) {
                 when (val result = repository.getAmazonPrice(upperQuery)) {
                     is Result.Success -> {
                         val apiResponse = result.data
                         val data = apiResponse.data
                         
                         if (apiResponse.success && data != null) {
                             // Map CompetitorPriceResponse to a temporary ProductResponse
                             val tempProduct = ProductResponse(
                                 id = -1L, // Temporary ID to indicate it's not in DB
                                 name = data.title ?: data.competitorProductTitle ?: "Producto de Amazon",
                                 sku = data.asin,
                                 asin = data.asin,
                                 currentPrice = data.price,
                                 description = "Producto encontrado en Amazon. Pulsa para añadirlo.",
                                 category = "Amazon",
                                 brand = "Amazon",
                                 active = true,
                                 monitoringEnabled = true
                             )
                             _listState.value = ProductListUiState(
                                 products = listOf(tempProduct),
                                 totalElements = 1,
                                 hasMore = false,
                                 searchQuery = query
                             )
                         } else {
                             // Handle soft error or empty data
                             val errorMessage = apiResponse.message ?: "No se encontraron datos para este ASIN"
                             _listState.value = ProductListUiState(
                                 isLoading = false,
                                 error = errorMessage,
                                 searchQuery = query
                             )
                         }
                     }
                     is Result.Error -> {
                         _listState.value = ProductListUiState(
                             isLoading = false,
                             error = result.message,
                             searchQuery = query
                         )
                     }
                 }
            } else {
                performNormalSearch(query)
            }
        }
    }

    private suspend fun performNormalSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) {
            clearSearchResults()
            return
        }
        val result = repository.searchProducts(name = trimmedQuery)
        when (result) {
            is Result.Success -> {
                val pageData = result.data.data
                _listState.value = ProductListUiState(
                    products = pageData?.content ?: emptyList(),
                    totalElements = pageData?.totalElements ?: 0,
                    hasMore = pageData?.hasNext ?: false,
                    searchQuery = query
                )
            }
            is Result.Error -> _listState.value = _listState.value.copy(
                isLoading = false, error = result.message
            )
        }
    }

    // ─── Detalle ────────────────────────────────────────────────────────

    fun loadProduct(id: Long) {
        viewModelScope.launch {
            val previousSyncResult = _detailState.value.syncResult
            _detailState.value = ProductDetailUiState(isLoading = true, syncResult = previousSyncResult)
            when (val result = repository.getProduct(id)) {
                is Result.Success -> _detailState.value = ProductDetailUiState(
                    product = result.data.data,
                    syncResult = previousSyncResult
                )
                is Result.Error -> _detailState.value = ProductDetailUiState(
                    error = result.message,
                    syncResult = previousSyncResult
                )
            }
        }
    }

    fun syncWithAmazon(productId: Long) {
        val asin = _detailState.value.product?.asin
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isSyncing = true, syncError = null, syncResult = null)
            when (val result = repository.syncWithAmazon(productId, asin)) {
                is Result.Success -> _detailState.value = _detailState.value.copy(
                    isSyncing = false,
                    syncResult = result.data.data
                )
                is Result.Error -> _detailState.value = _detailState.value.copy(
                    isSyncing = false,
                    syncError = result.message
                )
            }
        }
    }

    fun deleteProduct(id: Long) {
        viewModelScope.launch {
            when (repository.deleteProduct(id)) {
                is Result.Success -> {
                    _detailState.value = _detailState.value.copy(deleteSuccess = true)
                    loadProducts(refresh = true)
                }
                is Result.Error -> _detailState.value = _detailState.value.copy(
                    error = "No se pudo eliminar el producto"
                )
            }
        }
    }

    // ─── Formulario ─────────────────────────────────────────────────────

    fun createProduct(
        name: String,
        asin: String,
        currentPrice: String,
        costPrice: String,
        category: String,
        brand: String,
        description: String,
        monitoringEnabled: Boolean
    ) {
        val price = currentPrice.toDoubleOrNull()
        val cost = costPrice.toDoubleOrNull()
        if (name.isBlank() || price == null || price <= 0) {
            _formState.value = ProductFormUiState(error = "Nombre y precio de venta son obligatorios (precio > 0)")
            return
        }
        if (cost == null || cost < 0) {
            _formState.value = ProductFormUiState(error = "El precio de coste es obligatorio (≥ 0)")
            return
        }
        viewModelScope.launch {
            _formState.value = ProductFormUiState(isLoading = true)
            val request = CreateProductRequest(
                name = name,
                asin = asin.ifBlank { null },
                currentPrice = price,
                costPrice = cost,
                category = category.ifBlank { null },
                brand = brand.ifBlank { null },
                description = description.ifBlank { null },
                monitoringEnabled = monitoringEnabled
            )
            when (val result = repository.createProduct(request)) {
                is Result.Success -> {
                    _formState.value = ProductFormUiState(success = true)
                    // No llamar a loadProducts() aquí: TrackingScreen tiene su propio ViewModel
                    // y recarga automáticamente. Llamarlo aquí contaminaba _listState de SearchScreen.
                }
                is Result.Error -> _formState.value = ProductFormUiState(error = result.message)
            }
        }
    }

    fun updateProduct(
        id: Long,
        name: String,
        asin: String,
        currentPrice: String,
        costPrice: String,
        category: String,
        brand: String,
        description: String,
        monitoringEnabled: Boolean
    ) {
        val price = currentPrice.toDoubleOrNull()
        val cost = costPrice.toDoubleOrNull()
        if (name.isBlank() || price == null || price <= 0) {
            _formState.value = ProductFormUiState(error = "Nombre y precio de venta son obligatorios (precio > 0)")
            return
        }
        if (cost == null || cost < 0) {
            _formState.value = ProductFormUiState(error = "El precio de coste es obligatorio (≥ 0)")
            return
        }
        viewModelScope.launch {
            _formState.value = ProductFormUiState(isLoading = true)
            val request = UpdateProductRequest(
                name = name,
                asin = asin.ifBlank { null },
                currentPrice = price,
                costPrice = cost,
                category = category.ifBlank { null },
                brand = brand.ifBlank { null },
                description = description.ifBlank { null },
                monitoringEnabled = monitoringEnabled
            )
            when (repository.updateProduct(id, request)) {
                is Result.Success -> _formState.value = ProductFormUiState(success = true)
                is Result.Error -> _formState.value = ProductFormUiState(error = "Error al actualizar el producto")
            }
        }
    }

    fun resetFormState() {
        _formState.value = ProductFormUiState()
    }

    fun clearSearchResults() {
        _listState.value = ProductListUiState()
    }

    fun clearDetailSyncResult() {
        _detailState.value = _detailState.value.copy(syncResult = null, syncError = null)
    }

    // ─── Historial de precios ─────────────────────────────────────────

    fun loadPriceHistory(productId: Long) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isLoadingHistory = true)
            when (val result = repository.getRecentPriceHistory(productId)) {
                is Result.Success -> _detailState.value = _detailState.value.copy(
                    priceHistory = result.data.data ?: emptyList(),
                    isLoadingHistory = false
                )
                is Result.Error -> _detailState.value = _detailState.value.copy(
                    isLoadingHistory = false
                )
            }
        }
    }
}
