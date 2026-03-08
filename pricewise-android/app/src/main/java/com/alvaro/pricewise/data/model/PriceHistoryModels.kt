package com.alvaro.pricewise.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────
// Historial de precios
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class PriceHistoryResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "price") val price: Double,
    @Json(name = "previousPrice") val previousPrice: Double? = null,
    @Json(name = "changeType") val changeType: String? = null,
    @Json(name = "changeReason") val changeReason: String? = null,
    @Json(name = "percentageChange") val percentageChange: Double? = null,
    @Json(name = "recordedAt") val recordedAt: String? = null
)
