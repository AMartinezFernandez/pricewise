package com.alvaro.pricewise.ui.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.DashboardResponse
import com.alvaro.pricewise.data.model.RecommendationResponse
import com.alvaro.pricewise.data.repository.AnalyticsRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RecommendationsUiState(
    val recommendations: List<RecommendationResponse> = emptyList(),
    val dashboard: DashboardResponse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)

@HiltViewModel
class RecommendationsViewModel @Inject constructor(
    private val repository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecommendationsUiState())
    val uiState: StateFlow<RecommendationsUiState> = _uiState.asStateFlow()

    fun loadAll() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val dashboardDeferred = async { repository.getDashboard() }
            val recsDeferred = async { repository.getRecommendations() }

            val dashboardResult = dashboardDeferred.await()
            val recsResult = recsDeferred.await()

            val dashboard = if (dashboardResult is Result.Success) dashboardResult.data.data else null
            val recs = if (recsResult is Result.Success) recsResult.data.data?.content ?: emptyList() else emptyList()

            val error = when {
                dashboardResult is Result.Error -> dashboardResult.message
                recsResult is Result.Error -> recsResult.message
                else -> null
            }

            _uiState.value = RecommendationsUiState(
                dashboard = dashboard,
                recommendations = recs,
                error = error
            )
        }
    }

    fun applyRecommendation(id: Long) {
        viewModelScope.launch {
            when (val result = repository.applyRecommendation(id)) {
                is Result.Success -> {
                    val updated = _uiState.value.recommendations.filter { it.id != id }
                    val updatedDash = _uiState.value.dashboard?.let {
                        it.copy(pendingRecommendations = maxOf(0, it.pendingRecommendations - 1))
                    }
                    _uiState.value = _uiState.value.copy(
                        recommendations = updated,
                        dashboard = updatedDash,
                        actionMessage = "Recomendacion aplicada"
                    )
                    refreshDashboard()
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = "Error: ${result.message}"
                )
            }
        }
    }

    fun dismissRecommendation(id: Long) {
        viewModelScope.launch {
            when (val result = repository.dismissRecommendation(id)) {
                is Result.Success -> {
                    val updated = _uiState.value.recommendations.filter { it.id != id }
                    val updatedDash = _uiState.value.dashboard?.let {
                        it.copy(pendingRecommendations = maxOf(0, it.pendingRecommendations - 1))
                    }
                    _uiState.value = _uiState.value.copy(
                        recommendations = updated,
                        dashboard = updatedDash,
                        actionMessage = "Recomendacion descartada"
                    )
                    refreshDashboard()
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = "Error: ${result.message}"
                )
            }
        }
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    private suspend fun refreshDashboard() {
        val result = repository.getDashboard()
        if (result is Result.Success) {
            _uiState.value = _uiState.value.copy(dashboard = result.data.data)
        }
    }
}
