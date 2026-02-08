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
    val deleteSuccess: Boolean = false
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
            
            // Check if query is an ASIN (Basic validation: 10 chars, usually starts with B0)
            val isAsin = query.length == 10 && query.uppercase().startsWith("B0")
            
            if (isAsin) {
                 when (val result = repository.getAmazonPrice(query)) {
                     is Result.Success -> {
                         val apiResponse = result.data
                         val data = apiResponse.data
                         
                         if (apiResponse.success && data != null) {
                             // Map CompetitorPriceResponse to a temporary ProductResponse
                             val tempProduct = ProductResponse(
                                 id = -1L, // Temporary ID to indicate it's not in DB
                                 name = data.title ?: data.competitorProductTitle ?: "Producto de Amazon",
                                 sku = data.asin,
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
        val result = if (query.isBlank()) {
            repository.getProducts(0)
        } else {
            repository.searchProducts(name = query)
        }
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
            _detailState.value = ProductDetailUiState(isLoading = true)
            when (val result = repository.getProduct(id)) {
                is Result.Success -> _detailState.value = ProductDetailUiState(
                    product = result.data.data
                )
                is Result.Error -> _detailState.value = ProductDetailUiState(
                    error = result.message
                )
            }
        }
    }

    fun syncWithAmazon(productId: Long) {
        val sku = _detailState.value.product?.sku
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isSyncing = true, syncError = null, syncResult = null)
            when (val result = repository.syncWithAmazon(productId, sku)) {
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
        sku: String,
        currentPrice: String,
        costPrice: String,
        category: String,
        brand: String,
        description: String,
        monitoringEnabled: Boolean
    ) {
        val price = currentPrice.toDoubleOrNull()
        if (name.isBlank() || price == null || price <= 0) {
            _formState.value = ProductFormUiState(error = "Nombre y precio son obligatorios (precio > 0)")
            return
        }
        viewModelScope.launch {
            _formState.value = ProductFormUiState(isLoading = true)
            val request = CreateProductRequest(
                name = name,
                sku = sku.ifBlank { null },
                currentPrice = price,
                costPrice = costPrice.toDoubleOrNull(),
                category = category.ifBlank { null },
                brand = brand.ifBlank { null },
                description = description.ifBlank { null },
                monitoringEnabled = monitoringEnabled
            )
            when (val result = repository.createProduct(request)) {
                is Result.Success -> {
                    _formState.value = ProductFormUiState(success = true)
                    loadProducts(refresh = true)
                }
                is Result.Error -> _formState.value = ProductFormUiState(error = result.message)
            }
        }
    }

    fun resetFormState() {
        _formState.value = ProductFormUiState()
    }

    fun clearDetailSyncResult() {
        _detailState.value = _detailState.value.copy(syncResult = null, syncError = null)
    }
}
