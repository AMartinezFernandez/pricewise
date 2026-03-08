package com.alvaro.pricewise.data.repository

import com.alvaro.pricewise.data.api.PriceWiseApi
import com.alvaro.pricewise.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val api: PriceWiseApi
) {
    suspend fun getUsers() =
        safeApiCall { api.getUsers() }

    suspend fun getUserCount() =
        safeApiCall { api.getUserCount() }

    suspend fun createEmployee(username: String, email: String, password: String, companyId: Long? = null, role: String? = null) =
        safeApiCall { 
            api.createEmployee(
                com.alvaro.pricewise.data.model.CreateEmployeeRequest(username, email, password, companyId, role)
            ) 
        }

    suspend fun changePassword(currentPassword: String, newPassword: String) =
        safeApiCall {
            api.changePassword(
                com.alvaro.pricewise.data.model.ChangePasswordRequest(currentPassword, newPassword)
            )
        }

    suspend fun deleteUser(userId: Long) =
        safeApiCall { api.deleteCompanyUser(userId) }
}
