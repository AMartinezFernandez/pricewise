package com.alvaro.pricewise.ui.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.RecommendationResponse
import com.alvaro.pricewise.data.repository.AnalyticsRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecommendationsUiState(
    val recommendations: List<RecommendationResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val currentPage: Int = 0,
    val hasMore: Boolean = true,
    val isLoadingMore: Boolean = false
)

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val repository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendationsUiState())
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    init {
        loadRecommendations(refresh = true)
    }

    fun loadRecommendations(refresh: Boolean = false) {
        viewModelScope.launch {
            if (refresh) {
                _uiState.value = _uiState.value.copy(
                    isLoading = _uiState.value.recommendations.isEmpty(),
                    isRefreshing = _uiState.value.recommendations.isNotEmpty(),
                    error = null,
                    currentPage = 0
                )
            }

            when (val result = repository.getRecommendations(page = 0)) {
                is Result.Success -> {
                    val page = result.data.data
                    _uiState.value = _uiState.value.copy(
                        recommendations = page?.content ?: emptyList(),
                        isLoading = false,
                        isRefreshing = false,
                        currentPage = 0,
                        hasMore = page?.hasNext ?: false
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = result.message
                )
            }
        }
    }

    fun loadMore() {
        if (_uiState.value.isLoadingMore || !_uiState.value.hasMore) return
        viewModelScope.launch {
            val nextPage = _uiState.value.currentPage + 1
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            when (val result = repository.getRecommendations(page = nextPage)) {
                is Result.Success -> {
                    val page = result.data.data
                    _uiState.value = _uiState.value.copy(
                        recommendations = _uiState.value.recommendations + (page?.content ?: emptyList()),
                        currentPage = nextPage,
                        hasMore = page?.hasNext ?: false,
                        isLoadingMore = false
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingMore = false,
                    actionMessage = "Error al cargar más recomendaciones"
                )
            }
        }
    }

    fun applyRecommendation(id: Long) {
        viewModelScope.launch {
            when (repository.applyRecommendation(id)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        recommendations = _uiState.value.recommendations.map {
                            if (it.id == id) it.copy(status = "APPLIED") else it
                        },
                        actionMessage = "Recomendación aplicada"
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = "Error al aplicar recomendación"
                )
            }
        }
    }

    fun dismissRecommendation(id: Long) {
        viewModelScope.launch {
            when (repository.dismissRecommendation(id)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        recommendations = _uiState.value.recommendations.filter { it.id != id },
                        actionMessage = "Recomendación descartada"
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = "Error al descartar recomendación"
                )
            }
        }
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }
}
