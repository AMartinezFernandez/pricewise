package com.alvaro.pricewise.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────
// Recomendaciones
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class RecommendationResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "productId") val productId: Long,
    @Json(name = "productName") val productName: String,
    @Json(name = "recommendationType") val recommendationType: String,
    @Json(name = "currentPrice") val currentPrice: Double,
    @Json(name = "competitorPrice") val competitorPrice: Double,
    @Json(name = "suggestedPrice") val suggestedPrice: Double,
    @Json(name = "priceDifferencePercent") val priceDifferencePercent: Double? = null,
    @Json(name = "potentialSavingOrProfit") val potentialSavingOrProfit: Double? = null,
    @Json(name = "priority") val priority: String,
    @Json(name = "reason") val reason: String? = null,
    @Json(name = "status") val status: String,
    @Json(name = "createdAt") val createdAt: String? = null
)
