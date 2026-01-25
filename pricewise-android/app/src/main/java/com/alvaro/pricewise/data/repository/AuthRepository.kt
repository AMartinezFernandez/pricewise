package com.alvaro.pricewise.data.repository

import com.alvaro.pricewise.data.api.PriceWiseApi
import com.alvaro.pricewise.data.model.LoginRequest
import com.alvaro.pricewise.data.model.RegisterRequest
import com.alvaro.pricewise.util.Result
import com.alvaro.pricewise.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val api: PriceWiseApi,
    private val tokenRepository: TokenRepository
) {
    suspend fun login(emailOrUsername: String, password: String): Result<Unit> {
        val result = safeApiCall { api.login(LoginRequest(emailOrUsername, password)) }
        return when (result) {
            is Result.Success -> {
                val data = result.data.data
                if (data != null) {
                    tokenRepository.saveSession(
                        token = data.token.removePrefix("Bearer "),
                        userId = data.userId,
                        username = data.username,
                        role = data.role
                    )
                    Result.Success(Unit)
                } else {
                    Result.Error(result.data.message ?: "Error al iniciar sesión")
                }
            }
            is Result.Error -> result
        }
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        businessName: String?,
        businessType: String?
    ): Result<Unit> {
        val result = safeApiCall {
            api.register(RegisterRequest(username, email, password, businessName, businessType))
        }
        return when (result) {
            is Result.Success -> {
                val data = result.data.data
                if (data != null) {
                    tokenRepository.saveSession(
                        token = data.token.removePrefix("Bearer "),
                        userId = data.userId,
                        username = data.username,
                        role = data.role
                    )
                    Result.Success(Unit)
                } else {
                    Result.Error(result.data.message ?: "Error al registrarse")
                }
            }
            is Result.Error -> result
        }
    }

    suspend fun logout() {
        tokenRepository.clearSession()
    }
}
