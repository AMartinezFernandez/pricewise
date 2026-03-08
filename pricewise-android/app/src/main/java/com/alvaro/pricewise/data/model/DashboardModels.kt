package com.alvaro.pricewise.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────
// Admin — Dashboard global
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AdminDashboardResponse(
    @Json(name = "totalUsers") val totalUsers: Long = 0,
    @Json(name = "activeUsers") val activeUsers: Long = 0,
    @Json(name = "totalProducts") val totalProducts: Long = 0,
    @Json(name = "trackedProducts") val trackedProducts: Long = 0,
    @Json(name = "productsWithPriceDrop") val productsWithPriceDrop: Long = 0,
    @Json(name = "competitorsTracked") val competitorsTracked: Long = 0,
    @Json(name = "keepaStatus") val keepaStatus: Boolean = false,
    @Json(name = "schedulerStatus") val schedulerStatus: String? = null,
    @Json(name = "totalCompanies") val totalCompanies: Long = 0,
    @Json(name = "activeCompanies") val activeCompanies: Long = 0,
    @Json(name = "productsByCategory") val productsByCategory: Map<String, Long> = emptyMap(),
    @Json(name = "userCountByCompany") val userCountByCompany: Map<String, Long> = emptyMap()
)

// ─────────────────────────────────────────────
// Dashboard de analiticas (por empresa)
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class DashboardResponse(
    @Json(name = "totalProducts") val totalProducts: Int = 0,
    @Json(name = "activeMonitoring") val activeMonitoring: Int = 0,
    @Json(name = "pendingRecommendations") val pendingRecommendations: Int = 0,
    @Json(name = "unreadAlerts") val unreadAlerts: Int = 0,
    @Json(name = "potentialSavings") val potentialSavings: Double = 0.0,
    @Json(name = "topRecommendations") val topRecommendations: List<RecommendationResponse> = emptyList()
)
