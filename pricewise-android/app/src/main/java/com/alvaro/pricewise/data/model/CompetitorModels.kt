package com.alvaro.pricewise.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────
// Competidores / Keepa
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class KeepaStatus(
    @Json(name = "keepaAvailable") val keepaAvailable: Boolean,
    @Json(name = "status") val status: String,
    @Json(name = "message") val message: String? = null
)

@JsonClass(generateAdapter = true)
data class CompetitorPriceResponse(
    @Json(name = "id") val id: Long? = null,
    @Json(name = "asin") val asin: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "competitorProductTitle") val competitorProductTitle: String? = null,
    @Json(name = "price") val price: Double,
    @Json(name = "currency") val currency: String = "EUR",
    @Json(name = "available") val available: Boolean = true,
    @Json(name = "productUrl") val productUrl: String? = null,
    @Json(name = "scrapedAt") val scrapedAt: String? = null,
    @Json(name = "source") val source: String? = null
)
