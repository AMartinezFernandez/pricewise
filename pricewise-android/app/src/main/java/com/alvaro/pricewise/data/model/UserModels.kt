package com.alvaro.pricewise.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────
// Usuarios
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class UserSummaryResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "username") val username: String,
    @Json(name = "email") val email: String,
    @Json(name = "companyName") val companyName: String? = null,
    @Json(name = "role") val role: String,
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "productCount") val productCount: Long? = null
)
