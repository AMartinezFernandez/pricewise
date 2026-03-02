package com.alvaro.pricewise.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.AlertResponse
import com.alvaro.pricewise.data.model.AlertRuleResponse
import com.alvaro.pricewise.data.model.CreateAlertRuleRequest
import com.alvaro.pricewise.data.model.ProductResponse
import com.alvaro.pricewise.data.repository.AnalyticsRepository
import com.alvaro.pricewise.data.repository.ProductRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertsUiState(
    // Tab seleccionado: 0 = Mis alertas (rules), 1 = Historial (generated alerts)
    val selectedTab: Int = 0,

    // Reglas de alerta (configuradas por el usuario)
    val rules: List<AlertRuleResponse> = emptyList(),
    val isLoadingRules: Boolean = false,

    // Alertas generadas (historial)
    val alerts: List<AlertResponse> = emptyList(),
    val unreadCount: Int = 0,
    val isLoadingAlerts: Boolean = false,
    val filterUnreadOnly: Boolean = false,

    // General
    val error: String? = null,
    val actionMessage: String? = null,

    // Dialogo crear alerta
    val showCreateDialog: Boolean = false,
    val isSaving: Boolean = false,
    val products: List<ProductResponse> = emptyList(),
    val isLoadingProducts: Boolean = false
)

@HiltViewModel
class AlertsViewModel @Inject constructor(
    private val repository: AnalyticsRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertsUiState())
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    fun selectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
    }

    // ─── Reglas de alerta (Mis alertas) ──────────────────────

    fun loadRules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRules = true, error = null)
            when (val result = repository.getAlertRules()) {
                is Result.Success -> {
                    val rules = result.data.data ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        rules = rules,
                        isLoadingRules = false
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingRules = false,
                    error = result.message
                )
            }
        }
    }

    fun deleteRule(id: Long) {
        viewModelScope.launch {
            when (repository.deleteAlertRule(id)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        rules = _uiState.value.rules.filter { it.id != id },
                        actionMessage = "Alerta eliminada"
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = "Error al eliminar alerta"
                )
            }
        }
    }

    fun toggleRule(id: Long) {
        viewModelScope.launch {
            when (repository.toggleAlertRule(id)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        rules = _uiState.value.rules.map {
                            if (it.id == id) it.copy(enabled = !it.enabled) else it
                        }
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = "Error al cambiar estado"
                )
            }
        }
    }

    // ─── Alertas generadas (Historial) ──────────────────────

    fun loadAlerts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAlerts = true, error = null)
            when (val result = repository.getAlerts(onlyUnread = _uiState.value.filterUnreadOnly)) {
                is Result.Success -> {
                    val alerts = result.data.data?.content ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        alerts = alerts,
                        unreadCount = alerts.count { !it.isRead },
                        isLoadingAlerts = false
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingAlerts = false,
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

    // ─── Dialogo crear alerta ────────────────────────────────

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true)
        loadProducts()
    }

    fun dismissCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false)
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingProducts = true)
            when (val result = productRepository.getProducts(page = 0, size = 500)) {
                is Result.Success -> {
                    val products = result.data.data?.content ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        products = products,
                        isLoadingProducts = false
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoadingProducts = false
                )
            }
        }
    }

    fun createRule(alertType: String, threshold: Double, name: String?, productId: Long?, targetPrice: Double? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val request = CreateAlertRuleRequest(
                alertType = alertType,
                threshold = threshold,
                name = name,
                productId = productId,
                targetPrice = targetPrice
            )
            when (repository.createAlertRule(request)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showCreateDialog = false,
                        actionMessage = "Alerta creada"
                    )
                    loadRules()
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    actionMessage = "Error al crear alerta"
                )
            }
        }
    }

    fun loadAll() {
        loadRules()
        loadAlerts()
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }
}
