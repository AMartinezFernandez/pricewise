package com.alvaro.pricewise.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.alvaro.pricewise.ui.theme.PwDarkNavy
import com.alvaro.pricewise.ui.theme.PwOrangeDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompanySetupScreen(
    googleIdToken: String,
    googleEmail: String,
    googleName: String,
    onSetupSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    // Crear empresa
    var companyName by remember { mutableStateOf("") }
    var businessType by remember { mutableStateOf("") }

    // Unirse a empresa
    var companyCode by remember { mutableStateOf("") }

    // Set the google token in ViewModel on first composition
    LaunchedEffect(Unit) {
        viewModel.setGoogleToken(googleIdToken)
    }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) onSetupSuccess()
    }

    val darkFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = PwOrangeDark,
        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
        focusedLabelColor = PwOrangeDark,
        unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
        cursorColor = PwOrangeDark,
        focusedSupportingTextColor = Color.White.copy(alpha = 0.5f),
        unfocusedSupportingTextColor = Color.White.copy(alpha = 0.5f)
    )

    Scaffold(
        containerColor = PwDarkNavy,
        topBar = {
            TopAppBar(
                title = { Text("Configurar empresa") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PwDarkNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Welcome message
            Text(
                text = "Bienvenido, $googleName",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = googleEmail,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Para continuar, crea una empresa nueva o únete a una existente con su código",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tab selector
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Crear empresa") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Unirse") }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Error
            if (uiState.error != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF3A2020)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.error!!,
                        color = Color(0xFFF2B8B5),
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            when (selectedTab) {
                0 -> {
                    // Crear empresa nueva
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Nombre de la empresa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = darkFieldColors,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = businessType,
                        onValueChange = { businessType = it },
                        label = { Text("Tipo de negocio (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = darkFieldColors,
                        supportingText = { Text("Ej: ecommerce, retail, mayorista...") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.googleCompleteNewCompany(
                                companyName,
                                businessType.ifBlank { null }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !uiState.isLoading && companyName.isNotBlank()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Crear empresa")
                        }
                    }
                }

                1 -> {
                    // Unirse a empresa existente
                    OutlinedTextField(
                        value = companyCode,
                        onValueChange = {
                            if (it.length <= 8) companyCode = it.uppercase()
                        },
                        label = { Text("Código de empresa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = darkFieldColors,
                        supportingText = { Text("Código de 8 caracteres proporcionado por tu administrador") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { viewModel.googleCompleteJoin(companyCode) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = !uiState.isLoading && companyCode.length == 8
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Unirme a la empresa")
                        }
                    }
                }
            }
        }
    }
}
