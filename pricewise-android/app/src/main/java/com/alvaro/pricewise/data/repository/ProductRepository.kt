package com.alvaro.pricewise.data.repository

import com.alvaro.pricewise.data.api.PriceWiseApi
import com.alvaro.pricewise.data.model.*
import com.alvaro.pricewise.util.Result
import com.alvaro.pricewise.util.safeApiCall
import com.alvaro.pricewise.util.safeApiCallNoBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductRepository @Inject constructor(
    private val api: PriceWiseApi
) {
    suspend fun getProducts(page: Int = 0, size: Int = 20) =
        safeApiCall { api.getProducts(page, size) }

    suspend fun searchProducts(name: String? = null, category: String? = null, brand: String? = null, page: Int = 0) =
        safeApiCall { api.searchProducts(name, category, brand, page) }

    suspend fun getProduct(id: Long) =
        safeApiCall { api.getProduct(id) }

    suspend fun createProduct(request: CreateProductRequest) =
        safeApiCall { api.createProduct(request) }

    suspend fun updateProduct(id: Long, request: UpdateProductRequest) =
        safeApiCall { api.updateProduct(id, request) }

    suspend fun deleteProduct(id: Long) =
        safeApiCallNoBody { api.deleteProduct(id) }

    suspend fun getCategories() =
        safeApiCall { api.getCategories() }

    suspend fun getBrands() =
        safeApiCall { api.getBrands() }

    suspend fun syncWithAmazon(productId: Long, asin: String? = null) =
        safeApiCall { api.syncWithAmazon(productId, asin) }

    suspend fun getKeepaStatus() =
        safeApiCall { api.getKeepaStatus() }

    suspend fun getAmazonPrice(asin: String) =
        safeApiCall { api.getAmazonPrice(asin) }
}
