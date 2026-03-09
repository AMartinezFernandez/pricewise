package com.alvaro.pricewise.ui.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import com.composables.icons.lucide.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.alvaro.pricewise.ui.theme.PwDarkNavy
import com.alvaro.pricewise.data.model.CompanyResponse

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateUserScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateUserViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("EMPLOYEE") }
    // ADMIN can select company, COMPANY_ADMIN cannot (it's null)
    var selectedCompany by remember { mutableStateOf<CompanyResponse?>(null) }
    
    var isroleExpanded by remember { mutableStateOf(false) }
    var isCompanyExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nuevo Usuario") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Nombre de usuario") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )

            val passwordMismatch = confirmPassword.isNotBlank() && password != confirmPassword
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar contraseña") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = passwordMismatch,
                supportingText = if (passwordMismatch) {
                    { Text("Las contraseñas no coinciden") }
                } else null
            )

            // Role Selection
            ExposedDropdownMenuBox(
                expanded = isroleExpanded,
                onExpandedChange = { isroleExpanded = !isroleExpanded }
            ) {
                OutlinedTextField(
                    value = selectedRole,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Rol") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isroleExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(
                    expanded = isroleExpanded,
                    onDismissRequest = { isroleExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Empleado") },
                        onClick = { selectedRole = "EMPLOYEE"; isroleExpanded = false }
                    )
                    DropdownMenuItem(
                        text = { Text("Admin de Empresa") },
                        onClick = { selectedRole = "COMPANY_ADMIN"; isroleExpanded = false }
                    )
                }
            }

            // Company Selection (Only for ADMIN)
            // Company Selection (Only for ADMIN) or Display (for COMPANY_ADMIN)
            if (uiState.isAdmin) {
                 ExposedDropdownMenuBox(
                    expanded = isCompanyExpanded,
                    onExpandedChange = { isCompanyExpanded = !isCompanyExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedCompany?.name ?: "Seleccionar Empresa",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Empresa") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCompanyExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = isCompanyExpanded,
                        onDismissRequest = { isCompanyExpanded = false }
                    ) {
                        uiState.companies.forEach { company ->
                            DropdownMenuItem(
                                text = { Text(company.name) },
                                onClick = { selectedCompany = company; isCompanyExpanded = false }
                            )
                        }
                    }
                }
            } else if (uiState.currentCompanyName != null) {
                OutlinedTextField(
                    value = uiState.currentCompanyName!!,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Empresa") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = false
                )
            }

            if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = {
                    viewModel.createUser(
                        username, email, password, 
                        companyId = selectedCompany?.id,
                        role = selectedRole
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading && username.isNotBlank() && email.isNotBlank() && password.isNotBlank() && password == confirmPassword
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Crear Usuario")
                }
            }
        }
    }
}
