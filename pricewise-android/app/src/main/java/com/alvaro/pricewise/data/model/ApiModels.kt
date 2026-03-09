package com.alvaro.pricewise.data.model

import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────
// Wrappers genericos de respuesta del backend
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val timestamp: String? = null
)

@JsonClass(generateAdapter = true)
data class PageResponse<T>(
    val content: List<T> = emptyList(),
    val pageNumber: Int = 0,
    val pageSize: Int = 20,
    val totalElements: Long = 0,
    val totalPages: Int = 0,
    val first: Boolean = true,
    val last: Boolean = true,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false
)

// ─────────────────────────────────────────────
// API Keys (Keepa)
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class ApiKeyResponse(
    val id: Long,
    val provider: String,
    val maskedKey: String,
    val enabled: Boolean,
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SaveApiKeyRequest(
    val provider: String = "KEEPA",
    val apiKey: String
)
