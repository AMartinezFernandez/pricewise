package com.alvaro.pricewise.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────
// Admin — Gestion de usuarios
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AdminUserDetail(
    @Json(name = "id") val id: Long,
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "companyName") val companyName: String? = null,
    @Json(name = "companyType") val companyType: String? = null,
    @Json(name = "role") val role: String,
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class AdminUpdateUserRequest(
    @Json(name = "username") val username: String? = null,
    @Json(name = "email") val email: String? = null,
    @Json(name = "role") val role: String? = null,
    @Json(name = "active") val active: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class AdminPasswordChangeRequest(
    @Json(name = "newPassword") val newPassword: String
)

@JsonClass(generateAdapter = true)
data class AdminRoleChangeRequest(
    @Json(name = "role") val role: String
)

@JsonClass(generateAdapter = true)
data class AdminStatusChangeRequest(
    @Json(name = "active") val active: Boolean
)

// ─────────────────────────────────────────────
// Admin — Empresas
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CreateCompanyRequest(
    @Json(name = "name") val name: String,
    @Json(name = "businessType") val businessType: String? = null,
    @Json(name = "taxId") val taxId: String? = null,
    @Json(name = "adminUsername") val adminUsername: String,
    @Json(name = "adminEmail") val adminEmail: String,
    @Json(name = "adminPassword") val adminPassword: String
)
