package com.alvaro.pricewise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.alvaro.pricewise.data.repository.PreferencesRepository
import com.alvaro.pricewise.data.repository.TokenRepository
import com.alvaro.pricewise.ui.navigation.RootNavGraph
import com.alvaro.pricewise.ui.theme.PriceWiseTheme
import com.alvaro.pricewise.util.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenRepository: TokenRepository

    @Inject
    lateinit var sessionManager: SessionManager

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkTheme by preferencesRepository.isDarkTheme().collectAsState(initial = false)
            PriceWiseTheme(darkTheme = isDarkTheme) {
                RootNavGraph(
                    tokenRepository = tokenRepository,
                    sessionManager = sessionManager,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
