package com.alvaro.pricewise.ui.users

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.CompanyResponse
import com.alvaro.pricewise.data.repository.TokenRepository
import com.alvaro.pricewise.data.repository.UserRepository
import com.alvaro.pricewise.data.repository.AdminRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateUserUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false,
    val isAdmin: Boolean = false,
    val currentCompanyName: String? = null,
    val companies: List<CompanyResponse> = emptyList()
)

@HiltViewModel
class CreateUserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val adminRepository: AdminRepository // Assuming it exists to fetch companies
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateUserUiState())
    val uiState: StateFlow<CreateUserUiState> = _uiState.asStateFlow()

    init {
        checkRoleAndLoadCompanies()
    }

    private fun checkRoleAndLoadCompanies() {
        viewModelScope.launch {
            val role = tokenRepository.getRole().firstOrNull()
            val companyName = tokenRepository.getCompanyName().firstOrNull()
            
            _uiState.value = _uiState.value.copy(currentCompanyName = companyName)

            if (role == "ADMIN" || role == "ROLE_ADMIN") {
                _uiState.value = _uiState.value.copy(isAdmin = true, isLoading = true)
                // Load companies
                when (val result = adminRepository.getCompanies()) {
                     is Result.Success -> {
                         _uiState.value = _uiState.value.copy(
                             companies = result.data.data ?: emptyList(),
                             isLoading = false
                         )
                     }
                     is Result.Error -> {
                         _uiState.value = _uiState.value.copy(
                             isLoading = false, 
                             error = "Error cargando empresas: ${result.message}"
                         )
                     }
                }
            }
        }
    }

    fun createUser(username: String, email: String, password: String, companyId: Long?, role: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = userRepository.createEmployee(username, email, password, companyId, role)
            when (result) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                }
            }
        }
    }
}
