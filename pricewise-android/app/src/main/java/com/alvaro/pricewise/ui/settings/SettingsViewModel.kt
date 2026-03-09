package com.alvaro.pricewise.ui.settings

import com.alvaro.pricewise.data.model.ApiKeyResponse
import com.alvaro.pricewise.data.model.SaveApiKeyRequest
import com.alvaro.pricewise.data.model.UserProfile
import com.alvaro.pricewise.data.repository.PreferencesRepository
import com.alvaro.pricewise.data.repository.TokenRepository
import com.alvaro.pricewise.data.repository.UserRepository
import com.alvaro.pricewise.data.api.PriceWiseApi
import com.alvaro.pricewise.util.Result
import com.alvaro.pricewise.util.safeApiCall
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val profile: UserProfile? = null,
    val apiKeys: List<ApiKeyResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val passwordChangeSuccess: Boolean = false,
    val apiKeyMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val userRepository: UserRepository,
    private val preferencesRepository: PreferencesRepository,
    private val api: PriceWiseApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    val isDarkTheme = preferencesRepository.isDarkTheme()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val currency = preferencesRepository.getCurrency()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "EUR")

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            when (val result = userRepository.getProfile()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        profile = result.data.data,
                        isLoading = false
                    )
                    val role = result.data.data?.role
                    if (role == "ADMIN" || role == "COMPANY_ADMIN") {
                        loadApiKeys()
                    }
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

    private suspend fun loadApiKeys() {
        when (val result = safeApiCall { api.getApiKeys() }) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    apiKeys = result.data.data ?: emptyList()
                )
            }
            is Result.Error -> { /* No critico */ }
        }
    }

    fun setDarkTheme(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setDarkTheme(enabled) }
    }

    fun setCurrency(currency: String) {
        viewModelScope.launch { preferencesRepository.setCurrency(currency) }
    }

    fun saveApiKey(apiKey: String) {
        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "La API key no puede estar vacia")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, apiKeyMessage = null)
            when (val result = safeApiCall {
                api.saveApiKey(SaveApiKeyRequest(apiKey = apiKey))
            }) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        apiKeyMessage = "API key guardada correctamente"
                    )
                    loadApiKeys()
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

    fun toggleApiKey(id: Long) {
        viewModelScope.launch {
            when (val result = safeApiCall { api.toggleApiKey(id) }) {
                is Result.Success -> loadApiKeys()
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }
        }
    }

    fun deleteApiKey(id: Long) {
        viewModelScope.launch {
            when (val result = safeApiCall { api.deleteApiKey(id) }) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(apiKeyMessage = "API key eliminada")
                    loadApiKeys()
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(error = result.message)
                }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Todos los campos son obligatorios")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, passwordChangeSuccess = false)
            when (val result = userRepository.changePassword(currentPassword, newPassword)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, passwordChangeSuccess = true)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }

    fun resetPasswordChangeSuccess() {
        _uiState.value = _uiState.value.copy(passwordChangeSuccess = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearApiKeyMessage() {
        _uiState.value = _uiState.value.copy(apiKeyMessage = null)
    }

    fun logout() {
        viewModelScope.launch { tokenRepository.clearSession() }
    }
}
