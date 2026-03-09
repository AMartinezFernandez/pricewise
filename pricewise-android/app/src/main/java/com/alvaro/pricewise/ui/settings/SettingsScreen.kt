package com.alvaro.pricewise.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.composables.icons.lucide.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.alvaro.pricewise.ui.theme.PwDarkNavy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.CompanyResponse
import com.alvaro.pricewise.data.model.UserSummaryResponse
import com.alvaro.pricewise.data.repository.AdminRepository
import com.alvaro.pricewise.data.repository.TokenRepository
import com.alvaro.pricewise.data.repository.UserRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────

data class SettingsUiState(
    val username: String = "",
    val role: String = "",
    val companyName: String = "",
    val companyId: Long? = null,
    val users: List<UserSummaryResponse> = emptyList(),
    val companies: List<CompanyResponse> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val passwordChangeSuccess: Boolean = false,
    val employeeCreated: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val userRepository: UserRepository,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val username = tokenRepository.getUsername().firstOrNull() ?: ""
            val role = tokenRepository.getRole().firstOrNull() ?: ""
            val companyName = tokenRepository.getCompanyName().firstOrNull() ?: ""
            val companyId = tokenRepository.getCompanyId().firstOrNull()

            _uiState.value = _uiState.value.copy(
                username = username,
                role = role,
                companyName = companyName,
                companyId = companyId
            )

            if (role == "ADMIN" || role == "COMPANY_ADMIN") {
                loadUsers()
                if (role == "ADMIN") {
                    loadCompanies()
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    private suspend fun loadUsers() {
        when (val result = userRepository.getUsers()) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    users = result.data.data ?: emptyList(),
                    isLoading = false
                )
            }
            is Result.Error -> {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message
                )
            }
        }
    }

    private suspend fun loadCompanies() {
        when (val result = adminRepository.getCompanies()) {
            is Result.Success -> {
                _uiState.value = _uiState.value.copy(
                    companies = result.data.data ?: emptyList()
                )
            }
            is Result.Error -> { /* Non-critical */ }
        }
    }

    fun createEmployee(username: String, email: String, password: String, companyId: Long?, role: String) {
        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Todos los campos son obligatorios")
            return
        }
        if (_uiState.value.role == "ADMIN" && companyId == null) {
            _uiState.value = _uiState.value.copy(error = "Debes seleccionar una empresa")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, employeeCreated = false)
            when (val result = userRepository.createEmployee(username, email, password, companyId, role)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(employeeCreated = true, error = null)
                    loadUsers()
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

    fun changePassword(currentPassword: String, newPassword: String) {
        if (currentPassword.isBlank() || newPassword.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Todos los campos son obligatorios")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, passwordChangeSuccess = false)
            when (val result = userRepository.changePassword(currentPassword, newPassword)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null, passwordChangeSuccess = true)
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message,
                        passwordChangeSuccess = false
                    )
                }
            }
        }
    }

    fun resetPasswordChangeSuccess() {
        _uiState.value = _uiState.value.copy(passwordChangeSuccess = false)
    }

    fun resetEmployeeCreated() {
        _uiState.value = _uiState.value.copy(employeeCreated = false)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun logout() {
        viewModelScope.launch {
            tokenRepository.clearSession()
        }
    }
}


// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddUserDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(uiState.passwordChangeSuccess) {
        if (uiState.passwordChangeSuccess) {
            showChangePasswordDialog = false
            android.widget.Toast.makeText(context, "Contrasena actualizada correctamente", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetPasswordChangeSuccess()
        }
    }

    LaunchedEffect(uiState.employeeCreated) {
        if (uiState.employeeCreated) {
            showAddUserDialog = false
            android.widget.Toast.makeText(context, "Usuario creado correctamente", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetEmployeeCreated()
        }
    }

    if (showAddUserDialog) {
        AddUserDialog(
            isAdmin = uiState.role == "ADMIN",
            companies = uiState.companies,
            defaultCompanyId = uiState.companyId,
            defaultCompanyName = uiState.companyName,
            onDismiss = { showAddUserDialog = false },
            onConfirm = { username, email, password, companyId, role ->
                viewModel.createEmployee(username, email, password, companyId, role)
            }
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { current, newPwd ->
                viewModel.changePassword(current, newPwd)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Lucide.ArrowLeft, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PwDarkNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Error display
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
                        }
                    }
                }
            }

            // General Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Informacion de la Cuenta",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HorizontalDivider()
                    InfoRow("Usuario", uiState.username)
                    InfoRow("Rol", uiState.role)
                    InfoRow("Empresa", uiState.companyName)
                }
            }

            // User Management (Admin Only)
            if (uiState.role == "ADMIN" || uiState.role == "COMPANY_ADMIN") {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gestion de Usuarios",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { showAddUserDialog = true }) {
                                Icon(Lucide.UserPlus, contentDescription = "Anadir Usuario")
                            }
                        }
                        HorizontalDivider()

                        if (uiState.isLoading) {
                            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (uiState.users.isEmpty()) {
                            Text("No se encontraron usuarios.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            uiState.users.forEach { user ->
                                UserRow(user)
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // Change Password
            Button(
                onClick = { showChangePasswordDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Lucide.Lock, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cambiar Contrasena")
            }

            // Logout
            Button(
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Lucide.LogOut, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesion")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddUserDialog(
    isAdmin: Boolean,
    companies: List<CompanyResponse>,
    defaultCompanyId: Long?,
    defaultCompanyName: String,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Long?, String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }

    // Company selection
    var selectedCompanyId by remember { mutableStateOf(defaultCompanyId) }
    var selectedCompanyName by remember { mutableStateOf(defaultCompanyName) }
    var companyDropdownExpanded by remember { mutableStateOf(false) }

    // Role selection
    val roleOptions = listOf("EMPLOYEE", "COMPANY_ADMIN")
    var selectedRole by remember { mutableStateOf("EMPLOYEE") }
    var roleDropdownExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Anadir Usuario") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nombre de usuario") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; passwordError = null },
                    label = { Text("Contrasena") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    supportingText = { Text("Min 8 chars, 1 mayuscula, 1 minuscula, 1 numero") }
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; passwordError = null },
                    label = { Text("Confirmar Contrasena") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = passwordError != null
                )
                if (passwordError != null) {
                    Text(
                        text = passwordError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Company field
                if (isAdmin) {
                    // ADMIN: dropdown to select company
                    ExposedDropdownMenuBox(
                        expanded = companyDropdownExpanded,
                        onExpandedChange = { companyDropdownExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCompanyName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Empresa") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = companyDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = companyDropdownExpanded,
                            onDismissRequest = { companyDropdownExpanded = false }
                        ) {
                            companies.forEach { company ->
                                DropdownMenuItem(
                                    text = { Text(company.name) },
                                    onClick = {
                                        selectedCompanyId = company.id
                                        selectedCompanyName = company.name
                                        companyDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // COMPANY_ADMIN: show their own company (read-only)
                    OutlinedTextField(
                        value = defaultCompanyName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Empresa") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )
                }

                // Role selection dropdown
                ExposedDropdownMenuBox(
                    expanded = roleDropdownExpanded,
                    onExpandedChange = { roleDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = when (selectedRole) {
                            "EMPLOYEE" -> "Empleado"
                            "COMPANY_ADMIN" -> "Administrador de Empresa"
                            else -> selectedRole
                        },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rol") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = roleDropdownExpanded) },
                        modifier = Modifier
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = roleDropdownExpanded,
                        onDismissRequest = { roleDropdownExpanded = false }
                    ) {
                        roleOptions.forEach { role ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        when (role) {
                                            "EMPLOYEE" -> "Empleado"
                                            "COMPANY_ADMIN" -> "Administrador de Empresa"
                                            else -> role
                                        }
                                    )
                                },
                                onClick = {
                                    selectedRole = role
                                    roleDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (password != confirmPassword) {
                        passwordError = "Las contrasenas no coinciden"
                    } else {
                        val companyIdToSend = if (isAdmin) selectedCompanyId else null
                        onConfirm(username, email, password, companyIdToSend, selectedRole)
                    }
                },
                enabled = username.isNotBlank() && email.isNotBlank() && password.isNotBlank()
                        && confirmPassword.isNotBlank()
                        && (!isAdmin || selectedCompanyId != null)
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar Contrasena") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = { currentPassword = it },
                    label = { Text("Contrasena Actual") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; error = null },
                    label = { Text("Nueva Contrasena") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    supportingText = { Text("Min 8 chars, 1 mayuscula, 1 minuscula, 1 numero") }
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    label = { Text("Confirmar Nueva Contrasena") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    isError = error != null
                )
                if (error != null) {
                    Text(
                        text = error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (newPassword != confirmPassword) {
                        error = "Las contrasenas no coinciden"
                    } else {
                        onConfirm(currentPassword, newPassword)
                    }
                },
                enabled = currentPassword.isNotBlank() && newPassword.isNotBlank() && confirmPassword.isNotBlank()
            ) {
                Text("Cambiar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun UserRow(user: UserSummaryResponse) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(text = user.username, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(text = user.email, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = when (user.role) {
                    "EMPLOYEE" -> "Empleado"
                    "COMPANY_ADMIN" -> "Admin Empresa"
                    "ADMIN" -> "Administrador"
                    else -> user.role
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            user.companyName?.let { company ->
                Text(
                    text = company,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
