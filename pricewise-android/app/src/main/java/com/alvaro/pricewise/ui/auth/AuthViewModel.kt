package com.alvaro.pricewise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.repository.AuthRepository
import com.alvaro.pricewise.util.Result
import com.alvaro.pricewise.util.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    // Google Sign-In
    val googleSetupNeeded: Boolean = false,
    val googleIdToken: String? = null,
    val googleEmail: String? = null,
    val googleName: String? = null
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(emailOrUsername: String, password: String) {
        if (emailOrUsername.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(error = "Rellena todos los campos")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.login(emailOrUsername, password)) {
                is Result.Success -> {
                    sessionManager.resetExpiredFlag()
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is Result.Error   -> _uiState.value = AuthUiState(error = result.message)
            }
        }
    }

    fun register(
        username: String,
        email: String,
        password: String,
        confirmPassword: String,
        companyCode: String
    ) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState(error = "Rellena los campos obligatorios")
            return
        }
        if (companyCode.isBlank() || companyCode.length != 8) {
            _uiState.value = AuthUiState(error = "El código de empresa debe tener 8 caracteres")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState(error = "Las contraseñas no coinciden")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState(error = "La contraseña debe tener al menos 6 caracteres")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.register(
                username, email, password, companyCode.uppercase()
            )) {
                is Result.Success -> _uiState.value = AuthUiState(isSuccess = true)
                is Result.Error   -> _uiState.value = AuthUiState(error = result.message)
            }
        }
    }

    fun handleGoogleSignIn(idToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isLoading = true)
            when (val result = authRepository.googleLogin(idToken)) {
                is Result.Success -> {
                    val data = result.data
                    if (data.status == "AUTHENTICATED") {
                        sessionManager.resetExpiredFlag()
                        _uiState.value = AuthUiState(isSuccess = true)
                    } else {
                        _uiState.value = AuthUiState(
                            googleSetupNeeded = true,
                            googleIdToken = idToken,
                            googleEmail = data.googleEmail,
                            googleName = data.googleName
                        )
                    }
                }
                is Result.Error -> _uiState.value = AuthUiState(error = result.message)
            }
        }
    }

    fun googleCompleteNewCompany(companyName: String, businessType: String?) {
        val idToken = _uiState.value.googleIdToken
        if (idToken == null) {
            _uiState.value = _uiState.value.copy(error = "Token de Google no disponible")
            return
        }
        if (companyName.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "El nombre de empresa es obligatorio")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authRepository.googleCompleteNewCompany(idToken, companyName, businessType)) {
                is Result.Success -> {
                    sessionManager.resetExpiredFlag()
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun googleCompleteJoin(companyCode: String) {
        val idToken = _uiState.value.googleIdToken
        if (idToken == null) {
            _uiState.value = _uiState.value.copy(error = "Token de Google no disponible")
            return
        }
        if (companyCode.isBlank() || companyCode.length != 8) {
            _uiState.value = _uiState.value.copy(error = "El código de empresa debe tener 8 caracteres")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authRepository.googleCompleteJoin(idToken, companyCode.uppercase())) {
                is Result.Success -> {
                    sessionManager.resetExpiredFlag()
                    _uiState.value = AuthUiState(isSuccess = true)
                }
                is Result.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    fun setGoogleToken(idToken: String) {
        _uiState.value = _uiState.value.copy(googleIdToken = idToken)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearGoogleSetup() {
        _uiState.value = AuthUiState()
    }
}
