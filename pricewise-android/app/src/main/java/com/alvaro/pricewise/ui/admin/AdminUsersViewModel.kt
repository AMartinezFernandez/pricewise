package com.alvaro.pricewise.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.AdminUpdateUserRequest
import com.alvaro.pricewise.data.model.AdminUserDetail
import com.alvaro.pricewise.data.model.UserSummaryResponse
import com.alvaro.pricewise.data.repository.AdminRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminUsersUiState(
    val users: List<UserSummaryResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null
)

data class AdminUserDetailUiState(
    val user: AdminUserDetail? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val actionMessage: String? = null,
    val isPerformingAction: Boolean = false,
    val deleteSuccess: Boolean = false
)

@HiltViewModel
class AdminUsersViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _listState = MutableStateFlow(AdminUsersUiState())
    val listState: StateFlow<AdminUsersUiState> = _listState.asStateFlow()

    private val _detailState = MutableStateFlow(AdminUserDetailUiState())
    val detailState: StateFlow<AdminUserDetailUiState> = _detailState.asStateFlow()

    fun loadUsers() {
        viewModelScope.launch {
            _listState.value = _listState.value.copy(isLoading = true, error = null)
            when (val result = repository.getUsers()) {
                is Result.Success -> {
                    _listState.value = AdminUsersUiState(
                        users = result.data.data ?: emptyList()
                    )
                }
                is Result.Error -> {
                    _listState.value = _listState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
            }
        }
    }

    fun loadUser(userId: Long) {
        viewModelScope.launch {
            _detailState.value = AdminUserDetailUiState(isLoading = true)
            when (val result = repository.getUser(userId)) {
                is Result.Success -> {
                    _detailState.value = AdminUserDetailUiState(user = result.data.data)
                }
                is Result.Error -> {
                    _detailState.value = AdminUserDetailUiState(error = result.message)
                }
            }
        }
    }

    fun updateUser(userId: Long, username: String?, email: String?, role: String?, active: Boolean?) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isPerformingAction = true)
            val request = AdminUpdateUserRequest(
                username = username?.takeIf { it.isNotBlank() },
                email = email?.takeIf { it.isNotBlank() },
                role = role?.takeIf { it.isNotBlank() },
                active = active
            )
            when (val result = repository.updateUser(userId, request)) {
                is Result.Success -> {
                    _detailState.value = _detailState.value.copy(
                        user = result.data.data,
                        isPerformingAction = false,
                        actionMessage = "Usuario actualizado"
                    )
                    loadUsers()
                }
                is Result.Error -> {
                    _detailState.value = _detailState.value.copy(
                        isPerformingAction = false,
                        actionMessage = "Error: ${result.message}"
                    )
                }
            }
        }
    }

    fun changePassword(userId: Long, newPassword: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isPerformingAction = true)
            when (val result = repository.changePassword(userId, newPassword)) {
                is Result.Success -> {
                    _detailState.value = _detailState.value.copy(
                        isPerformingAction = false,
                        actionMessage = "Contraseña cambiada"
                    )
                }
                is Result.Error -> {
                    _detailState.value = _detailState.value.copy(
                        isPerformingAction = false,
                        actionMessage = "Error: ${result.message}"
                    )
                }
            }
        }
    }

    fun changeRole(userId: Long, role: String) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isPerformingAction = true)
            when (val result = repository.changeRole(userId, role)) {
                is Result.Success -> {
                    // Refresh user detail
                    loadUser(userId)
                    _detailState.value = _detailState.value.copy(
                        actionMessage = "Rol cambiado"
                    )
                    loadUsers()
                }
                is Result.Error -> {
                    _detailState.value = _detailState.value.copy(
                        isPerformingAction = false,
                        actionMessage = "Error: ${result.message}"
                    )
                }
            }
        }
    }

    fun toggleStatus(userId: Long, active: Boolean) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isPerformingAction = true)
            when (val result = repository.changeStatus(userId, active)) {
                is Result.Success -> {
                    loadUser(userId)
                    _detailState.value = _detailState.value.copy(
                        actionMessage = if (active) "Usuario activado" else "Usuario desactivado"
                    )
                    loadUsers()
                }
                is Result.Error -> {
                    _detailState.value = _detailState.value.copy(
                        isPerformingAction = false,
                        actionMessage = "Error: ${result.message}"
                    )
                }
            }
        }
    }

    fun deleteUser(userId: Long) {
        viewModelScope.launch {
            _detailState.value = _detailState.value.copy(isPerformingAction = true)
            when (val result = repository.deleteUser(userId)) {
                is Result.Success -> {
                    _detailState.value = _detailState.value.copy(
                        isPerformingAction = false,
                        deleteSuccess = true,
                        actionMessage = "Usuario eliminado"
                    )
                    loadUsers()
                }
                is Result.Error -> {
                    _detailState.value = _detailState.value.copy(
                        isPerformingAction = false,
                        actionMessage = "Error: ${result.message}"
                    )
                }
            }
        }
    }

    fun clearActionMessage() {
        _detailState.value = _detailState.value.copy(actionMessage = null)
    }

    fun clearListActionMessage() {
        _listState.value = _listState.value.copy(actionMessage = null)
    }
}
