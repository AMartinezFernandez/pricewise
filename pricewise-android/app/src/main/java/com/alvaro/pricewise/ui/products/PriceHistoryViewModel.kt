package com.alvaro.pricewise.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.PriceHistoryResponse
import com.alvaro.pricewise.data.repository.ProductRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PriceHistoryUiState(
    val entries: List<PriceHistoryResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 0,
    val hasMore: Boolean = false,
    val totalElements: Long = 0,
    val productName: String = ""
)

@HiltViewModel
class PriceHistoryViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PriceHistoryUiState())
    val uiState: StateFlow<PriceHistoryUiState> = _uiState.asStateFlow()

    private var currentProductId: Long = -1
    private var currentPage = 0

    fun loadHistory(productId: Long, refresh: Boolean = false) {
        if (refresh || productId != currentProductId) {
            currentPage = 0
            currentProductId = productId
            _uiState.value = PriceHistoryUiState(isLoading = true)
        } else {
            _uiState.value = _uiState.value.copy(isLoading = true)
        }

        viewModelScope.launch {
            // Load product name
            if (_uiState.value.productName.isEmpty()) {
                when (val productResult = repository.getProduct(productId)) {
                    is Result.Success -> {
                        _uiState.value = _uiState.value.copy(
                            productName = productResult.data.data?.name ?: "Producto"
                        )
                    }
                    is Result.Error -> { /* ignore, just won't show name */ }
                }
            }

            when (val result = repository.getPriceHistory(productId, currentPage)) {
                is Result.Success -> {
                    val page = result.data.data
                    val newEntries = page?.content ?: emptyList()
                    val allEntries = if (currentPage == 0) newEntries
                        else _uiState.value.entries + newEntries
                    val isLast = page?.last ?: true
                    _uiState.value = _uiState.value.copy(
                        entries = allEntries,
                        isLoading = false,
                        totalElements = page?.totalElements ?: 0,
                        hasMore = !isLast
                    )
                    if (!isLast) currentPage++
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun loadMore() {
        if (!_uiState.value.isLoading && _uiState.value.hasMore) {
            loadHistory(currentProductId)
        }
    }
}
