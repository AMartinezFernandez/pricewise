package com.alvaro.pricewise.data.repository

import com.alvaro.pricewise.data.api.PriceWiseApi
import com.alvaro.pricewise.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepository @Inject constructor(
    private val api: PriceWiseApi
) {
    suspend fun getDashboard() =
        safeApiCall { api.getDashboard() }

    suspend fun getRecommendations(page: Int = 0, size: Int = 20) =
        safeApiCall { api.getRecommendations(page, size) }

    suspend fun applyRecommendation(id: Long) =
        safeApiCall { api.applyRecommendation(id) }

    suspend fun dismissRecommendation(id: Long) =
        safeApiCall { api.dismissRecommendation(id) }

    suspend fun getAlerts(page: Int = 0, onlyUnread: Boolean = false) =
        safeApiCall { api.getAlerts(page = page, onlyUnread = onlyUnread) }

    suspend fun markAlertRead(id: Long) =
        safeApiCall { api.markAlertRead(id) }

    suspend fun markAllAlertsAsRead() =
        safeApiCall { api.markAllAlertsAsRead() }

    suspend fun runAnalysis() =
        safeApiCall { api.runAnalysis() }
}
