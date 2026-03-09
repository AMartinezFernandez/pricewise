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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.alvaro.pricewise.data.model.ApiKeyResponse
import com.alvaro.pricewise.data.model.UserProfile
import com.alvaro.pricewise.data.repository.PreferencesRepository
import com.alvaro.pricewise.data.repository.TokenRepository
import com.alvaro.pricewise.data.repository.UserRepository
import com.alvaro.pricewise.data.api.PriceWiseApi
import com.alvaro.pricewise.ui.theme.PwDarkNavy
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

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────

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
                api.saveApiKey(com.alvaro.pricewise.data.model.SaveApiKeyRequest(apiKey = apiKey))
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
    val isDarkTheme by viewModel.isDarkTheme.collectAsState()
    val currency by viewModel.currency.collectAsState()

    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showCurrencyMenu by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(uiState.passwordChangeSuccess) {
        if (uiState.passwordChangeSuccess) {
            showChangePasswordDialog = false
            android.widget.Toast.makeText(context, "Contrasena actualizada correctamente", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.resetPasswordChangeSuccess()
        }
    }

    LaunchedEffect(uiState.apiKeyMessage) {
        uiState.apiKeyMessage?.let {
            showApiKeyDialog = false
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearApiKeyMessage()
        }
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showChangePasswordDialog = false },
            onConfirm = { current, newPwd -> viewModel.changePassword(current, newPwd) }
        )
    }

    if (showApiKeyDialog) {
        ApiKeyDialog(
            onDismiss = { showApiKeyDialog = false },
            onConfirm = { viewModel.saveApiKey(it) }
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Error
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
                        TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
                    }
                }
            }

            if (uiState.isLoading && uiState.profile == null) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // ─── Mi Perfil ───────────────────────────────
            uiState.profile?.let { profile ->
                SectionCard(title = "Mi Perfil") {
                    InfoRow("Usuario", profile.username)
                    InfoRow("Email", profile.email)
                    InfoRow("Rol", when (profile.role) {
                        "ADMIN" -> "Administrador"
                        "COMPANY_ADMIN" -> "Admin Empresa"
                        "EMPLOYEE" -> "Empleado"
                        else -> profile.role
                    })
                    profile.companyName?.let { InfoRow("Empresa", it) }
                    profile.companyPlan?.let { InfoRow("Plan", it) }
                }
            }

            // ─── Apariencia ──────────────────────────────
            SectionCard(title = "Apariencia") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isDarkTheme) Lucide.Moon else Lucide.Sun,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Tema oscuro", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = { viewModel.setDarkTheme(it) }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                // Moneda
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Lucide.BadgeDollarSign,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text("Moneda", style = MaterialTheme.typography.bodyMedium)
                    }
                    Box {
                        TextButton(onClick = { showCurrencyMenu = true }) {
                            Text(currencyLabel(currency))
                        }
                        DropdownMenu(
                            expanded = showCurrencyMenu,
                            onDismissRequest = { showCurrencyMenu = false }
                        ) {
                            listOf("EUR" to "Euro (EUR)", "USD" to "Dolar (USD)", "GBP" to "Libra (GBP)").forEach { (code, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.setCurrency(code)
                                        showCurrencyMenu = false
                                    },
                                    trailingIcon = {
                                        if (currency == code) Icon(Lucide.Check, contentDescription = null)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ─── API Key Keepa (solo ADMIN/COMPANY_ADMIN) ─────
            val role = uiState.profile?.role
            if (role == "ADMIN" || role == "COMPANY_ADMIN") {
                SectionCard(title = "Integracion Keepa") {
                    if (uiState.apiKeys.isEmpty()) {
                        Text(
                            "Sin API key configurada. Necesitas una API key de Keepa para monitorizar precios de Amazon.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Lucide.Plus, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Anadir API Key")
                        }
                    } else {
                        uiState.apiKeys.forEach { key ->
                            ApiKeyRow(
                                apiKey = key,
                                onToggle = { viewModel.toggleApiKey(key.id) },
                                onDelete = { viewModel.deleteApiKey(key.id) }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showApiKeyDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Lucide.RefreshCw, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cambiar API Key")
                        }
                    }
                }
            }

            // ─── Cuenta ──────────────────────────────────
            SectionCard(title = "Cuenta") {
                OutlinedButton(
                    onClick = { showChangePasswordDialog = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Lucide.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cambiar Contrasena")
                }

                Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────
// Componentes auxiliares
// ─────────────────────────────────────────────

@Composable
private fun SectionCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun ApiKeyRow(
    apiKey: ApiKeyResponse,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = apiKey.provider,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = apiKey.maskedKey,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = apiKey.enabled, onCheckedChange = { onToggle() })
        IconButton(onClick = onDelete) {
            Icon(
                Lucide.Trash2,
                contentDescription = "Eliminar",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ApiKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var apiKey by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("API Key de Keepa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Introduce tu API key de Keepa para habilitar la monitorizacion de precios en Amazon.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(apiKey) },
                enabled = apiKey.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun ChangePasswordDialog(
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
                    visualTransformation = PasswordVisualTransformation()
                )
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it; error = null },
                    label = { Text("Nueva Contrasena") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    supportingText = { Text("Min 8 chars, 1 mayuscula, 1 minuscula, 1 numero") }
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it; error = null },
                    label = { Text("Confirmar Nueva Contrasena") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    isError = error != null
                )
                if (error != null) {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
            ) { Text("Cambiar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
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

private fun currencyLabel(code: String): String = when (code) {
    "EUR" -> "EUR"
    "USD" -> "USD"
    "GBP" -> "GBP"
    else -> code
}
