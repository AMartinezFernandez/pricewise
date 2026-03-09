package com.alvaro.pricewise.data.api

import com.alvaro.pricewise.data.repository.TokenRepository
import com.alvaro.pricewise.util.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor de OkHttp que añade automáticamente el token JWT
 * a todas las peticiones que lo requieren.
 * Usa token cacheado para evitar runBlocking en el thread de red.
 * Detecta respuestas 401 para notificar sesión expirada.
 */
class AuthInterceptor @Inject constructor(
    private val tokenRepository: TokenRepository,
    private val sessionManager: SessionManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenRepository.getCachedToken()

        val request = if (!token.isNullOrBlank()) {
            chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        // Si el servidor responde 401 y teníamos token, la sesión expiró
        if (response.code == 401 && !token.isNullOrBlank()) {
            tokenRepository.clearCachedToken()
            sessionManager.notifySessionExpired()
        }

        return response
    }
}
