package com.alvaro.pricewise.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.CompetitorPriceResponse
import com.alvaro.pricewise.data.model.PriceHistoryResponse
import com.alvaro.pricewise.data.model.ProductResponse
import com.alvaro.pricewise.data.repository.ProductRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    fun loadProduct(id: Long) {
        viewModelScope.launch {
            val previousSyncResult = _uiState.value.syncResult
            _uiState.value = ProductDetailUiState(isLoading = true, syncResult = previousSyncResult)
            when (val result = repository.getProduct(id)) {
                is Result.Success -> _uiState.value = ProductDetailUiState(
                    product = result.data.data,
                    syncResult = previousSyncResult
                )
                is Result.Error -> _uiState.value = ProductDetailUiState(
                    error = result.message,
                    syncResult = previousSyncResult
                )
            }
        }
    }

    fun syncWithAmazon(productId: Long) {
        val asin = _uiState.value.product?.asin
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true, syncError = null, syncResult = null)
            when (val result = repository.syncWithAmazon(productId, asin)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncResult = result.data.data
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
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
                    _uiState.value = _uiState.value.copy(deleteSuccess = true)
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    error = "No se pudo eliminar el producto"
                )
            }
        }
    }

    fun clearSyncResult() {
        _uiState.value = _uiState.value.copy(syncResult = null, syncError = null)
    }

    fun loadPriceHistory(productId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingHistory = true)
            when (val result = repository.getRecentPriceHistory(productId)) {
                is Result.Success -> _uiState.value = _uiState.value.copy(
                    priceHistory = result.data.data ?: emptyList(),
                    isLoadingHistory = false
                )
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingHistory = false
                )
            }
        }
    }
}
