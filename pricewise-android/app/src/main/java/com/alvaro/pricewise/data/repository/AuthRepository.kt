package com.alvaro.pricewise.data.repository

import com.alvaro.pricewise.data.api.PriceWiseApi
import com.alvaro.pricewise.data.model.GoogleCompleteJoinRequest
import com.alvaro.pricewise.data.model.GoogleCompleteNewCompanyRequest
import com.alvaro.pricewise.data.model.GoogleLoginRequest
import com.alvaro.pricewise.data.model.GoogleLoginResponse
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
                        role = data.role,
                        companyId = data.companyId,
                        companyName = data.companyName
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
        companyCode: String
    ): Result<Unit> {
        val result = safeApiCall {
            api.register(RegisterRequest(username, email, password, companyCode))
        }
        return when (result) {
            is Result.Success -> {
                val data = result.data.data
                if (data != null) {
                    tokenRepository.saveSession(
                        token = data.token.removePrefix("Bearer "),
                        userId = data.userId,
                        username = data.username,
                        role = data.role,
                        companyId = data.companyId,
                        companyName = data.companyName
                    )
                    Result.Success(Unit)
                } else {
                    Result.Error(result.data.message ?: "Error al registrarse")
                }
            }
            is Result.Error -> result
        }
    }

    suspend fun googleLogin(idToken: String): Result<GoogleLoginResponse> {
        val result = safeApiCall { api.googleLogin(GoogleLoginRequest(idToken)) }
        return when (result) {
            is Result.Success -> {
                val data = result.data.data
                if (data != null) {
                    if (data.status == "AUTHENTICATED" && data.token != null) {
                        val userId = data.userId
                        val username = data.username
                        val role = data.role
                        if (userId == null || username == null || role == null) {
                            return Result.Error("Respuesta de Google incompleta: faltan datos de usuario")
                        }
                        tokenRepository.saveSession(
                            token = data.token.removePrefix("Bearer "),
                            userId = userId,
                            username = username,
                            role = role,
                            companyId = data.companyId,
                            companyName = data.companyName
                        )
                    }
                    Result.Success(data)
                } else {
                    Result.Error(result.data.message ?: "Error en login con Google")
                }
            }
            is Result.Error -> result
        }
    }

    suspend fun googleCompleteNewCompany(
        idToken: String,
        companyName: String,
        businessType: String?
    ): Result<Unit> {
        val result = safeApiCall {
            api.googleCompleteNewCompany(
                GoogleCompleteNewCompanyRequest(idToken, companyName, businessType)
            )
        }
        return when (result) {
            is Result.Success -> {
                val data = result.data.data
                if (data != null) {
                    tokenRepository.saveSession(
                        token = data.token.removePrefix("Bearer "),
                        userId = data.userId,
                        username = data.username,
                        role = data.role,
                        companyId = data.companyId,
                        companyName = data.companyName
                    )
                    Result.Success(Unit)
                } else {
                    Result.Error(result.data.message ?: "Error al crear empresa")
                }
            }
            is Result.Error -> result
        }
    }

    suspend fun googleCompleteJoin(idToken: String, companyCode: String): Result<Unit> {
        val result = safeApiCall {
            api.googleCompleteJoin(GoogleCompleteJoinRequest(idToken, companyCode))
        }
        return when (result) {
            is Result.Success -> {
                val data = result.data.data
                if (data != null) {
                    tokenRepository.saveSession(
                        token = data.token.removePrefix("Bearer "),
                        userId = data.userId,
                        username = data.username,
                        role = data.role,
                        companyId = data.companyId,
                        companyName = data.companyName
                    )
                    Result.Success(Unit)
                } else {
                    Result.Error(result.data.message ?: "Error al unirse a la empresa")
                }
            }
            is Result.Error -> result
        }
    }

    suspend fun logout() {
        tokenRepository.clearSession()
    }
}
