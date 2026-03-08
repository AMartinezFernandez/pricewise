package com.alvaro.pricewise.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────
// Alertas
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AlertResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "productId") val productId: Long? = null,
    @Json(name = "productName") val productName: String? = null,
    @Json(name = "alertType") val alertType: String,
    @Json(name = "title") val title: String,
    @Json(name = "message") val message: String? = null,
    @Json(name = "previousPrice") val previousPrice: Double? = null,
    @Json(name = "newPrice") val newPrice: Double? = null,
    @Json(name = "changePercent") val changePercent: Double? = null,
    @Json(name = "severity") val severity: String,
    @Json(name = "isRead") val isRead: Boolean = false,
    @Json(name = "createdAt") val createdAt: String? = null
)

// ─────────────────────────────────────────────
// Reglas de Alertas (configuracion)
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class AlertRuleResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "alertType") val alertType: String,
    @Json(name = "threshold") val threshold: Double,
    @Json(name = "enabled") val enabled: Boolean = true,
    @Json(name = "name") val name: String? = null,
    @Json(name = "productId") val productId: Long? = null,
    @Json(name = "productName") val productName: String? = null,
    @Json(name = "targetPrice") val targetPrice: Double? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateAlertRuleRequest(
    @Json(name = "alertType") val alertType: String,
    @Json(name = "threshold") val threshold: Double,
    @Json(name = "name") val name: String? = null,
    @Json(name = "productId") val productId: Long? = null,
    @Json(name = "targetPrice") val targetPrice: Double? = null
)

@JsonClass(generateAdapter = true)
data class UpdateAlertRuleRequest(
    @Json(name = "threshold") val threshold: Double? = null,
    @Json(name = "enabled") val enabled: Boolean? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "targetPrice") val targetPrice: Double? = null
)
