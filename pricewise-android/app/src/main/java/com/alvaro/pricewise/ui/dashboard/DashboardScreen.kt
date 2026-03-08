package com.alvaro.pricewise.ui.dashboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.composables.icons.lucide.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.alvaro.pricewise.ui.theme.PwDarkNavy
import com.alvaro.pricewise.ui.theme.PwOrangeDark
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.repository.AnalyticsRepository
import com.alvaro.pricewise.data.repository.TokenRepository
import com.alvaro.pricewise.data.repository.UserRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Locale

// ─────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────

data class DashboardUiState(
    val username: String = "Usuario",
    val companyName: String = "",
    val role: String = "",
    val totalProducts: Long = 0,
    val totalSavings: String = "0 €",
    val unreadAlerts: Long = 0,
    val pendingRecommendations: Int = 0,
    val usersCount: Long = 0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)


@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    fun refresh() {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = !_uiState.value.isRefreshing,
                isRefreshing = _uiState.value.username != "Usuario",
                error = null
            )

            // Collect user info from DataStore
            val username = tokenRepository.getUsername().firstOrNull() ?: "Usuario"
            val companyName = tokenRepository.getCompanyName().firstOrNull().orEmpty()
            val role = tokenRepository.getRole().firstOrNull() ?: ""

            _uiState.value = _uiState.value.copy(
                username = username,
                companyName = companyName,
                role = role
            )

            when (val result = analyticsRepository.getDashboard()) {
                is Result.Success -> {
                    val metrics = result.data.data
                    if (metrics != null) {
                        _uiState.value = _uiState.value.copy(
                            totalProducts = metrics.totalProducts.toLong(),
                            totalSavings = formatCurrency(metrics.potentialSavings),
                            unreadAlerts = metrics.unreadAlerts.toLong(),
                            pendingRecommendations = metrics.pendingRecommendations,
                            isLoading = false,
                            isRefreshing = false
                        )
                    }
                }

                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isRefreshing = false,
                        error = result.message
                    )
                }
            }

            // Users count (ADMIN/COMPANY_ADMIN)
            if (role == "ADMIN" || role == "COMPANY_ADMIN") {
                when (val result = userRepository.getUserCount()) {
                    is Result.Success -> {
                        val count = result.data.data ?: 0L
                        _uiState.value = _uiState.value.copy(usersCount = count)
                    }
                    is Result.Error -> { /* silencioso */ }
                }
            }
        }
    }

    private fun formatCurrency(value: Double): String {
        return String.format(Locale.getDefault(), "%.2f €", value)
    }

}

// ─────────────────────────────────────────────
// Screen
// ─────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToTracking: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onNavigateToUsers: () -> Unit = {},
    onNavigateToAdminUsers: () -> Unit = {},
    onNavigateToAdminDashboard: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsState()

    // Refresh data every time the screen becomes visible (ON_RESUME)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.companyName) },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Lucide.Settings, contentDescription = "Ajustes")
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refresh() },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Bienvenido ${uiState.username} !",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

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
                            Icon(
                                Lucide.TriangleAlert,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                //  Grid
                Text(
                    text = "Panel de Control",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "Alertas",
                        value = "",
                        icon = Lucide.Bell,
                        onClick = onNavigateToAlerts,
                        modifier = Modifier.weight(1f)
                    )

                    StatCard(
                        title = "Productos",
                        value = "",
                        icon = Lucide.Package,
                        onClick = onNavigateToTracking,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (uiState.role == "ADMIN") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Usuarios",
                            value = "",
                            icon = Lucide.Users,
                            onClick = onNavigateToAdminUsers,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Administracion",
                            value = "",
                            icon = Lucide.ShieldCheck,
                            onClick = onNavigateToAdminDashboard,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else if (uiState.role == "COMPANY_ADMIN") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Usuarios",
                            value = "",
                            icon = Lucide.Users,
                            onClick = onNavigateToUsers,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            } // PullToRefreshBox
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    height: Dp = 140.dp,
    color: Color = PwOrangeDark,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .height(height)
            .then(
                if (onClick != null) Modifier.clickable { onClick() }
                else Modifier
            ),
        colors = CardDefaults.cardColors(containerColor = PwDarkNavy)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (value.isNotBlank()) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
