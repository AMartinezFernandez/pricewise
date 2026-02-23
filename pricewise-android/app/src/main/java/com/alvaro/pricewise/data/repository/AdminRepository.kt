package com.alvaro.pricewise.data.repository

import com.alvaro.pricewise.data.api.PriceWiseApi
import com.alvaro.pricewise.data.model.*
import com.alvaro.pricewise.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdminRepository @Inject constructor(
    private val api: PriceWiseApi
) {
    suspend fun getCompanies() =
        safeApiCall { api.getCompanies() }

    suspend fun getUsers() =
        safeApiCall { api.getAdminUsers() }

    suspend fun getUser(userId: Long) =
        safeApiCall { api.getAdminUser(userId) }

    suspend fun updateUser(userId: Long, request: AdminUpdateUserRequest) =
        safeApiCall { api.updateAdminUser(userId, request) }

    suspend fun changePassword(userId: Long, newPassword: String) =
        safeApiCall { api.changeAdminUserPassword(userId, AdminPasswordChangeRequest(newPassword)) }

    suspend fun changeRole(userId: Long, role: String) =
        safeApiCall { api.changeAdminUserRole(userId, AdminRoleChangeRequest(role)) }

    suspend fun changeStatus(userId: Long, active: Boolean) =
        safeApiCall { api.changeAdminUserStatus(userId, AdminStatusChangeRequest(active)) }

    suspend fun deleteUser(userId: Long) =
        safeApiCall { api.deleteAdminUser(userId) }

    suspend fun getCompany(companyId: Long) =
        safeApiCall { api.getAdminCompany(companyId) }

    suspend fun createCompany(request: CreateCompanyRequest) =
        safeApiCall { api.createCompany(request) }

    suspend fun getAdminDashboard() =
        safeApiCall { api.getAdminDashboard() }

    suspend fun getAdminStats() =
        safeApiCall { api.getAdminStats() }
}
