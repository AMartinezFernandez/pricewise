package com.alvaro.pricewise.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.ProductResponse
import com.alvaro.pricewise.data.repository.ProductRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val products: List<ProductResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalElements: Long = 0,
    val hasMore: Boolean = false,
    val searchQuery: String = ""
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.value = SearchUiState(isLoading = true, searchQuery = query)

            val trimmedQuery = query.trim()
            val upperQuery = trimmedQuery.uppercase()
            val isAsin = upperQuery.length == 10 && upperQuery.matches(Regex("^[A-Z0-9]{10}$"))

            if (isAsin) {
                when (val result = repository.getAmazonPrice(upperQuery)) {
                    is Result.Success -> {
                        val apiResponse = result.data
                        val data = apiResponse.data

                        if (apiResponse.success && data != null) {
                            val tempProduct = ProductResponse(
                                id = -1L,
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
                            _uiState.value = SearchUiState(
                                products = listOf(tempProduct),
                                totalElements = 1,
                                hasMore = false,
                                searchQuery = query
                            )
                        } else {
                            _uiState.value = SearchUiState(
                                error = apiResponse.message ?: "No se encontraron datos para este ASIN",
                                searchQuery = query
                            )
                        }
                    }
                    is Result.Error -> {
                        _uiState.value = SearchUiState(error = result.message, searchQuery = query)
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
            clearResults()
            return
        }
        when (val result = repository.searchProducts(name = trimmedQuery)) {
            is Result.Success -> {
                val pageData = result.data.data
                _uiState.value = SearchUiState(
                    products = pageData?.content ?: emptyList(),
                    totalElements = pageData?.totalElements ?: 0,
                    hasMore = pageData?.hasNext ?: false,
                    searchQuery = query
                )
            }
            is Result.Error -> _uiState.value = _uiState.value.copy(
                isLoading = false, error = result.message
            )
        }
    }

    fun clearResults() {
        _uiState.value = SearchUiState()
    }
}
