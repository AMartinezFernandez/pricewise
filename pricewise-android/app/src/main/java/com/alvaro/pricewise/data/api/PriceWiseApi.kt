package com.alvaro.pricewise.data.api

import com.alvaro.pricewise.data.model.*
import retrofit2.Response
import retrofit2.http.*

interface PriceWiseApi {

    // ─── Auth ────────────────────────────────────────────────────────────
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<AuthData>>

    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<ApiResponse<AuthData>>

    @POST("api/auth/create-employee")
    suspend fun createEmployee(@Body request: CreateEmployeeRequest): Response<ApiResponse<AuthData>>

    @POST("api/auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<String?>>

    // ─── Google OAuth2 ────────────────────────────────────────────────────
    @POST("api/auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<ApiResponse<GoogleLoginResponse>>

    @POST("api/auth/google/complete-new-company")
    suspend fun googleCompleteNewCompany(@Body request: GoogleCompleteNewCompanyRequest): Response<ApiResponse<AuthData>>

    @POST("api/auth/google/complete-join")
    suspend fun googleCompleteJoin(@Body request: GoogleCompleteJoinRequest): Response<ApiResponse<AuthData>>

    @GET("api/auth/profile")
    suspend fun getProfile(): Response<ApiResponse<UserProfile>>

    // ─── Productos ───────────────────────────────────────────────────────
    @GET("api/products")
    suspend fun getProducts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "name",
        @Query("sortDir") sortDir: String = "asc"
    ): Response<ApiResponse<PageResponse<ProductResponse>>>

    @GET("api/products/monitored")
    suspend fun getMonitoredProducts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "name",
        @Query("sortDir") sortDir: String = "asc"
    ): Response<ApiResponse<PageResponse<ProductResponse>>>

    @GET("api/products/search")
    suspend fun searchProducts(
        @Query("name") name: String? = null,
        @Query("category") category: String? = null,
        @Query("brand") brand: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageResponse<ProductResponse>>>

    @GET("api/products/{id}")
    suspend fun getProduct(@Path("id") id: Long): Response<ApiResponse<ProductResponse>>

    @POST("api/products")
    suspend fun createProduct(@Body request: CreateProductRequest): Response<ApiResponse<ProductResponse>>

    @PUT("api/products/{id}")
    suspend fun updateProduct(
        @Path("id") id: Long,
        @Body request: UpdateProductRequest
    ): Response<ApiResponse<ProductResponse>>

    @DELETE("api/products/{id}")
    suspend fun deleteProduct(@Path("id") id: Long): Response<Unit>

    @GET("api/products/categories")
    suspend fun getCategories(): Response<ApiResponse<List<String>>>

    @GET("api/products/brands")
    suspend fun getBrands(): Response<ApiResponse<List<String>>>

    @GET("api/products/count")
    suspend fun getProductCount(): Response<ApiResponse<Long>>

    // ─── Historial de precios ───────────────────────────────────────────
    @GET("api/products/{productId}/history/recent")
    suspend fun getRecentPriceHistory(
        @Path("productId") productId: Long
    ): Response<ApiResponse<List<PriceHistoryResponse>>>

    @GET("api/products/{productId}/history")
    suspend fun getPriceHistory(
        @Path("productId") productId: Long,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageResponse<PriceHistoryResponse>>>

    // ─── Keepa / Competidores ────────────────────────────────────────────
    @GET("api/competitors/status")
    suspend fun getKeepaStatus(): Response<ApiResponse<KeepaStatus>>

    @GET("api/competitors/amazon/price/{asin}")
    suspend fun getAmazonPrice(@Path("asin") asin: String): Response<ApiResponse<CompetitorPriceResponse>>

    @POST("api/competitors/amazon/sync/{productId}")
    suspend fun syncWithAmazon(
        @Path("productId") productId: Long,
        @Query("asin") asin: String? = null
    ): Response<ApiResponse<CompetitorPriceResponse>>

    // ─── Recomendaciones ─────────────────────────────────────────────────
    @GET("api/analytics/recommendations")
    suspend fun getRecommendations(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): Response<ApiResponse<PageResponse<RecommendationResponse>>>

    @POST("api/analytics/recommendations/{id}/apply")
    suspend fun applyRecommendation(@Path("id") id: Long): Response<ApiResponse<String>>

    @POST("api/analytics/recommendations/{id}/dismiss")
    suspend fun dismissRecommendation(@Path("id") id: Long): Response<ApiResponse<String>>

    // ─── Alertas ─────────────────────────────────────────────────────────
    @GET("api/analytics/alerts")
    suspend fun getAlerts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("onlyUnread") onlyUnread: Boolean = false
    ): Response<ApiResponse<PageResponse<AlertResponse>>>

    @POST("api/analytics/alerts/{id}/read")
    suspend fun markAlertRead(@Path("id") id: Long): Response<ApiResponse<String>>

    @POST("api/analytics/alerts/read-all")
    suspend fun markAllAlertsAsRead(): Response<ApiResponse<Map<String, Any>>>

    @POST("api/analytics/analyze")
    suspend fun runAnalysis(): Response<ApiResponse<Map<String, Any>>>

    // ─── Reglas de Alertas ────────────────────────────────────────────────
    @GET("api/alert-rules")
    suspend fun getAlertRules(): Response<ApiResponse<List<AlertRuleResponse>>>

    @POST("api/alert-rules")
    suspend fun createAlertRule(@Body request: CreateAlertRuleRequest): Response<ApiResponse<AlertRuleResponse>>

    @PUT("api/alert-rules/{id}")
    suspend fun updateAlertRule(
        @Path("id") id: Long,
        @Body request: UpdateAlertRuleRequest
    ): Response<ApiResponse<AlertRuleResponse>>

    @DELETE("api/alert-rules/{id}")
    suspend fun deleteAlertRule(@Path("id") id: Long): Response<ApiResponse<String>>

    @POST("api/alert-rules/{id}/toggle")
    suspend fun toggleAlertRule(@Path("id") id: Long): Response<ApiResponse<AlertRuleResponse>>

    // ─── Admin / Empresas ────────────────────────────────────────────────
    @GET("api/admin/companies")
    suspend fun getCompanies(): Response<ApiResponse<List<CompanyResponse>>>

    @GET("api/admin/companies/{companyId}")
    suspend fun getAdminCompany(@Path("companyId") companyId: Long): Response<ApiResponse<CompanyResponse>>

    @POST("api/admin/companies")
    suspend fun createCompany(@Body request: CreateCompanyRequest): Response<ApiResponse<CompanyResponse>>

    // ─── Admin / Dashboard & Stats ─────────────────────────────────────
    @GET("api/admin/dashboard")
    suspend fun getAdminDashboard(): Response<ApiResponse<AdminDashboardResponse>>

    @GET("api/admin/stats")
    suspend fun getAdminStats(): Response<ApiResponse<Map<String, Any>>>

    // ─── Admin / Usuarios ──────────────────────────────────────────────
    @GET("api/admin/users")
    suspend fun getAdminUsers(): Response<ApiResponse<List<UserSummaryResponse>>>

    @GET("api/admin/users/{userId}")
    suspend fun getAdminUser(@Path("userId") userId: Long): Response<ApiResponse<AdminUserDetail>>

    @PUT("api/admin/users/{userId}")
    suspend fun updateAdminUser(
        @Path("userId") userId: Long,
        @Body request: AdminUpdateUserRequest
    ): Response<ApiResponse<AdminUserDetail>>

    @PUT("api/admin/users/{userId}/password")
    suspend fun changeAdminUserPassword(
        @Path("userId") userId: Long,
        @Body request: AdminPasswordChangeRequest
    ): Response<ApiResponse<String>>

    @PUT("api/admin/users/{userId}/role")
    suspend fun changeAdminUserRole(
        @Path("userId") userId: Long,
        @Body request: AdminRoleChangeRequest
    ): Response<ApiResponse<UserSummaryResponse>>

    @PUT("api/admin/users/{userId}/status")
    suspend fun changeAdminUserStatus(
        @Path("userId") userId: Long,
        @Body request: AdminStatusChangeRequest
    ): Response<ApiResponse<UserSummaryResponse>>

    @DELETE("api/admin/users/{userId}")
    suspend fun deleteAdminUser(@Path("userId") userId: Long): Response<ApiResponse<String>>

    // ─── Dashboard ───────────────────────────────────────────────────────
    @GET("api/analytics/dashboard")
    suspend fun getDashboard(): Response<ApiResponse<DashboardResponse>>

    // ─── Usuarios ───────────────────────────────────────────────────────
    @GET("api/users")
    suspend fun getUsers(): Response<ApiResponse<List<UserSummaryResponse>>>

    @GET("api/users/count")
    suspend fun getUserCount(): Response<ApiResponse<Long>>

    @DELETE("api/users/{userId}")
    suspend fun deleteCompanyUser(@retrofit2.http.Path("userId") userId: Long): Response<ApiResponse<Void>>
}
