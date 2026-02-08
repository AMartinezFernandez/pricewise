package com.alvaro.pricewise.ui.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.AlertResponse
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
    val alerts: List<AlertResponse> = emptyList(),
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

            // Cargar las tres fuentes en paralelo con async
            val dashboardDeferred  = async { repository.getDashboard() }
            val recsDeferred       = async { repository.getRecommendations() }
            val alertsDeferred     = async { repository.getAlerts(onlyUnread = false) }

            val dashboardResult = dashboardDeferred.await()
            val recsResult      = recsDeferred.await()
            val alertsResult    = alertsDeferred.await()

            val dashboard = if (dashboardResult is Result.Success) dashboardResult.data.data else null
            val recs      = if (recsResult is Result.Success)      recsResult.data.data?.content ?: emptyList() else emptyList()
            val alerts    = if (alertsResult is Result.Success)    alertsResult.data.data?.content ?: emptyList() else emptyList()

            val error = when {
                dashboardResult is Result.Error -> dashboardResult.message
                recsResult      is Result.Error -> recsResult.message
                else -> null
            }

            _uiState.value = RecommendationsUiState(
                dashboard       = dashboard,
                recommendations = recs,
                alerts          = alerts,
                error           = error
            )
        }
    }

    fun applyRecommendation(id: Long) {
        viewModelScope.launch {
            when (val result = repository.applyRecommendation(id)) {
                is Result.Success -> {
                    // Quitar la recomendación de la lista local inmediatamente
                    val updated = _uiState.value.recommendations.filter { it.id != id }
                    // Actualizar el contador del dashboard localmente para que se refleje al instante
                    val updatedDash = _uiState.value.dashboard?.let {
                        it.copy(pendingRecommendations = maxOf(0, it.pendingRecommendations - 1))
                    }
                    _uiState.value = _uiState.value.copy(
                        recommendations = updated,
                        dashboard       = updatedDash,
                        actionMessage   = "Recomendación aplicada"
                    )
                    // Recargar dashboard en segundo plano para sincronizar con el servidor
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
                        dashboard       = updatedDash,
                        actionMessage   = "Recomendación descartada"
                    )
                    refreshDashboard()
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = "Error: ${result.message}"
                )
            }
        }
    }

    fun markAlertRead(id: Long) {
        viewModelScope.launch {
            when (repository.markAlertRead(id)) {
                is Result.Success -> {
                    val updatedAlerts = _uiState.value.alerts.map {
                        if (it.id == id) it.copy(isRead = true) else it
                    }
                    val updatedDash = _uiState.value.dashboard?.let {
                        it.copy(unreadAlerts = maxOf(0, it.unreadAlerts - 1))
                    }
                    _uiState.value = _uiState.value.copy(
                        alerts    = updatedAlerts,
                        dashboard = updatedDash
                    )
                }
                is Result.Error -> { /* silencioso, el estado local ya se actualizó */ }
            }
        }
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }

    // Recarga solo el dashboard sin mostrar loading global
    private suspend fun refreshDashboard() {
        val result = repository.getDashboard()
        if (result is Result.Success) {
            _uiState.value = _uiState.value.copy(dashboard = result.data.data)
        }
    }
}
