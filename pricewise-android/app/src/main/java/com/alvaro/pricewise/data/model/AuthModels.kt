package com.alvaro.pricewise.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────
// Auth
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class LoginRequest(
    @Json(name = "emailOrUsername") val emailOrUsername: String,
    @Json(name = "password") val password: String
)

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "companyCode") val companyCode: String
)

@JsonClass(generateAdapter = true)
data class AuthData(
    @Json(name = "token") val token: String,
    @Json(name = "type") val type: String = "Bearer",
    @Json(name = "userId") val userId: Long,
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "role") val role: String,
    @Json(name = "companyId") val companyId: Long? = null,
    @Json(name = "companyName") val companyName: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateEmployeeRequest(
    val username: String,
    val email: String,
    val password: String,
    val companyId: Long? = null,
    val role: String? = null
)

@JsonClass(generateAdapter = true)
data class CompanyResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "companyCode") val companyCode: String? = null,
    @Json(name = "businessType") val businessType: String? = null,
    @Json(name = "taxId") val taxId: String? = null,
    @Json(name = "plan") val plan: String? = null,
    @Json(name = "adminUsername") val adminUsername: String? = null
)

@JsonClass(generateAdapter = true)
data class ChangePasswordRequest(
    @Json(name = "currentPassword") val currentPassword: String,
    @Json(name = "newPassword") val newPassword: String
)

@JsonClass(generateAdapter = true)
data class UserProfile(
    @Json(name = "id") val id: Long,
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "companyId") val companyId: Long? = null,
    @Json(name = "companyName") val companyName: String? = null,
    @Json(name = "companyType") val companyType: String? = null,
    @Json(name = "companyPlan") val companyPlan: String? = null,
    @Json(name = "role") val role: String,
    @Json(name = "totalProducts") val totalProducts: Int = 0
)

// ─── Google OAuth2 ─────────────────────────────

@JsonClass(generateAdapter = true)
data class GoogleLoginRequest(
    @Json(name = "idToken") val idToken: String
)

@JsonClass(generateAdapter = true)
data class GoogleLoginResponse(
    @Json(name = "status") val status: String,
    @Json(name = "token") val token: String? = null,
    @Json(name = "userId") val userId: Long? = null,
    @Json(name = "username") val username: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "companyId") val companyId: Long? = null,
    @Json(name = "companyName") val companyName: String? = null,
    @Json(name = "googleEmail") val googleEmail: String? = null,
    @Json(name = "googleName") val googleName: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleCompleteNewCompanyRequest(
    @Json(name = "googleIdToken") val googleIdToken: String,
    @Json(name = "companyName") val companyName: String,
    @Json(name = "businessType") val businessType: String? = null
)

@JsonClass(generateAdapter = true)
data class GoogleCompleteJoinRequest(
    @Json(name = "googleIdToken") val googleIdToken: String,
    @Json(name = "companyCode") val companyCode: String
)
