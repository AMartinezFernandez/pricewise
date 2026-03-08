package com.alvaro.pricewise.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────
// Productos
// ─────────────────────────────────────────────

@JsonClass(generateAdapter = true)
data class CreateProductRequest(
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "sku") val sku: String? = null,
    @Json(name = "asin") val asin: String? = null,
    @Json(name = "ean") val ean: String? = null,
    @Json(name = "currentPrice") val currentPrice: Double,
    @Json(name = "costPrice") val costPrice: Double? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "brand") val brand: String? = null,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "monitoringEnabled") val monitoringEnabled: Boolean = true,
    @Json(name = "stockQuantity") val stockQuantity: Int? = null
)

@JsonClass(generateAdapter = true)
data class UpdateProductRequest(
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "sku") val sku: String? = null,
    @Json(name = "asin") val asin: String? = null,
    @Json(name = "ean") val ean: String? = null,
    @Json(name = "currentPrice") val currentPrice: Double? = null,
    @Json(name = "costPrice") val costPrice: Double? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "brand") val brand: String? = null,
    @Json(name = "monitoringEnabled") val monitoringEnabled: Boolean? = null,
    @Json(name = "stockQuantity") val stockQuantity: Int? = null
)

@JsonClass(generateAdapter = true)
data class ProductResponse(
    @Json(name = "id") val id: Long,
    @Json(name = "name") val name: String,
    @Json(name = "description") val description: String? = null,
    @Json(name = "sku") val sku: String? = null,
    @Json(name = "asin") val asin: String? = null,
    @Json(name = "ean") val ean: String? = null,
    @Json(name = "currentPrice") val currentPrice: Double,
    @Json(name = "costPrice") val costPrice: Double? = null,
    @Json(name = "margin") val margin: Double? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "brand") val brand: String? = null,
    @Json(name = "imageUrl") val imageUrl: String? = null,
    @Json(name = "active") val active: Boolean = true,
    @Json(name = "monitoringEnabled") val monitoringEnabled: Boolean = true,
    @Json(name = "stockQuantity") val stockQuantity: Int? = null,
    @Json(name = "createdByUsername") val createdByUsername: String? = null,
    @Json(name = "createdAt") val createdAt: String? = null,
    @Json(name = "updatedAt") val updatedAt: String? = null,
    @Json(name = "amazonPrice") val amazonPrice: Double? = null,
    @Json(name = "amazonProductTitle") val amazonProductTitle: String? = null,
    @Json(name = "amazonPriceUpdatedAt") val amazonPriceUpdatedAt: String? = null
)
