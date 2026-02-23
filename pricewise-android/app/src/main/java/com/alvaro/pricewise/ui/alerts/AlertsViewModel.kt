package com.alvaro.pricewise.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.AlertResponse
import com.alvaro.pricewise.data.model.CreateAlertRuleRequest
import com.alvaro.pricewise.data.repository.AnalyticsRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    val alerts: List<AlertResponse> = emptyList(),
    val unreadCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val filterUnreadOnly: Boolean = false,
    val showCreateDialog: Boolean = false,
    val isSaving: Boolean = false
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val repository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    fun loadAlerts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            when (val result = repository.getAlerts(onlyUnread = _uiState.value.filterUnreadOnly)) {
                is Result.Success -> {
                    val alerts = result.data.data?.content ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        alerts = alerts,
                        unreadCount = alerts.count { !it.isRead },
                        isLoading = false
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun toggleFilter() {
        _uiState.value = _uiState.value.copy(
            filterUnreadOnly = !_uiState.value.filterUnreadOnly
        )
        loadAlerts()
    }

    fun markAlertRead(id: Long) {
        viewModelScope.launch {
            when (repository.markAlertRead(id)) {
                is Result.Success -> {
                    val updatedAlerts = _uiState.value.alerts.map {
                        if (it.id == id) it.copy(isRead = true) else it
                    }
                    _uiState.value = _uiState.value.copy(
                        alerts = updatedAlerts,
                        unreadCount = updatedAlerts.count { !it.isRead }
                    )
                }
                is Result.Error -> { /* silencioso */ }
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            when (repository.markAllAlertsAsRead()) {
                is Result.Success -> {
                    val updatedAlerts = _uiState.value.alerts.map { it.copy(isRead = true) }
                    _uiState.value = _uiState.value.copy(
                        alerts = updatedAlerts,
                        unreadCount = 0,
                        actionMessage = "Todas las alertas marcadas como leidas"
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = "Error al marcar alertas como leidas"
                )
            }
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    fun createRule(alertType: String, threshold: Double, name: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val request = CreateAlertRuleRequest(
                alertType = alertType,
                threshold = threshold,
                name = name
            )
            when (repository.createAlertRule(request)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showCreateDialog = false,
                        actionMessage = "Regla creada"
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    actionMessage = "Error al crear regla"
                )
            }
        }
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }
}
