package com.alvaro.pricewise.data.repository

import com.alvaro.pricewise.data.api.PriceWiseApi
import com.alvaro.pricewise.data.model.CompanyResponse
import com.alvaro.pricewise.util.Result
import com.alvaro.pricewise.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

import com.alvaro.pricewise.data.model.ApiResponse

@Singleton
class AdminRepository @Inject constructor(
    private val api: PriceWiseApi
) {
    suspend fun getCompanies(): Result<ApiResponse<List<CompanyResponse>>> =
        safeApiCall { api.getCompanies() }
}
