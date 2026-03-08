package com.alvaro.pricewise.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ─────────────────────────────────────────────
// Wrapper genérico de respuesta del backend
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
    // Último precio de Amazon (persistido en backend)
    @Json(name = "amazonPrice") val amazonPrice: Double? = null,
    @Json(name = "amazonProductTitle") val amazonProductTitle: String? = null,
    @Json(name = "amazonPriceUpdatedAt") val amazonPriceUpdatedAt: String? = null
)

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
// Reglas de Alertas (configuración)
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

// ─────────────────────────────────────────────
// Admin — Gestión de usuarios
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
// Dashboard de analíticas (por empresa)
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
