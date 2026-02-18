package com.alvaro.pricewise.data.api

import com.alvaro.pricewise.data.repository.TokenRepository
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/**
 * Interceptor de OkHttp que añade automáticamente el token JWT
 * a todas las peticiones que lo requieren.
 * Usa token cacheado para evitar runBlocking en el thread de red.
 */
class AuthInterceptor @Inject constructor(
    private val tokenRepository: TokenRepository
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

        return chain.proceed(request)
    }
}
