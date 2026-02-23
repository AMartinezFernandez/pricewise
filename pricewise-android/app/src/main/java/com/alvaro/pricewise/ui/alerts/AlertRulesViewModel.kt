package com.alvaro.pricewise.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.AlertRuleResponse
import com.alvaro.pricewise.data.model.CreateAlertRuleRequest
import com.alvaro.pricewise.data.model.UpdateAlertRuleRequest
import com.alvaro.pricewise.data.repository.AnalyticsRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlertRulesUiState(
    val rules: List<AlertRuleResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val showCreateDialog: Boolean = false,
    val editingRule: AlertRuleResponse? = null,
    val isSaving: Boolean = false
)

@HiltViewModel
class AlertRulesViewModel @Inject constructor(
    private val repository: AnalyticsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlertRulesUiState())
    val uiState: StateFlow<AlertRulesUiState> = _uiState.asStateFlow()

    init {
        loadRules()
    }

    fun loadRules() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = repository.getAlertRules()) {
                is Result.Success -> {
                    val rules = result.data.data ?: emptyList()
                    _uiState.value = _uiState.value.copy(
                        rules = rules,
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
                        editingRule = null,
                        actionMessage = "Regla creada"
                    )
                    loadRules()
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    actionMessage = "Error al crear regla"
                )
            }
        }
    }

    fun updateRule(ruleId: Long, threshold: Double?, name: String?) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)
            val request = UpdateAlertRuleRequest(
                threshold = threshold,
                name = name
            )
            when (repository.updateAlertRule(ruleId, request)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSaving = false,
                        showCreateDialog = false,
                        editingRule = null,
                        actionMessage = "Regla actualizada"
                    )
                    loadRules()
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    actionMessage = "Error al actualizar regla"
                )
            }
        }
    }

    fun toggleRule(ruleId: Long) {
        viewModelScope.launch {
            when (repository.toggleAlertRule(ruleId)) {
                is Result.Success -> {
                    val updated = _uiState.value.rules.map {
                        if (it.id == ruleId) it.copy(enabled = !it.enabled) else it
                    }
                    _uiState.value = _uiState.value.copy(rules = updated)
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = "Error al cambiar estado"
                )
            }
        }
    }

    fun deleteRule(ruleId: Long) {
        viewModelScope.launch {
            when (repository.deleteAlertRule(ruleId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        rules = _uiState.value.rules.filter { it.id != ruleId },
                        actionMessage = "Regla eliminada"
                    )
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    actionMessage = "Error al eliminar regla"
                )
            }
        }
    }

    fun showCreateDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = true, editingRule = null)
    }

    fun showEditDialog(rule: AlertRuleResponse) {
        _uiState.value = _uiState.value.copy(editingRule = rule, showCreateDialog = true)
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(showCreateDialog = false, editingRule = null)
    }

    fun clearActionMessage() {
        _uiState.value = _uiState.value.copy(actionMessage = null)
    }
}
