package com.alvaro.pricewise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.alvaro.pricewise.data.repository.TokenRepository
import com.alvaro.pricewise.ui.navigation.RootNavGraph
import com.alvaro.pricewise.ui.theme.PriceWiseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenRepository: TokenRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PriceWiseTheme {
                RootNavGraph(
                    tokenRepository = tokenRepository,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
