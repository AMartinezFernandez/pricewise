package com.alvaro.pricewise.ui.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alvaro.pricewise.data.model.CreateProductRequest
import com.alvaro.pricewise.data.model.UpdateProductRequest
import com.alvaro.pricewise.data.repository.ProductRepository
import com.alvaro.pricewise.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductFormUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

@HiltViewModel
class ProductFormViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductFormUiState())
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    fun createProduct(
        name: String,
        asin: String,
        currentPrice: String,
        costPrice: String,
        category: String,
        brand: String,
        description: String,
        monitoringEnabled: Boolean
    ) {
        val price = currentPrice.toDoubleOrNull()
        val cost = costPrice.toDoubleOrNull()
        if (name.isBlank() || price == null || price <= 0) {
            _uiState.value = ProductFormUiState(error = "Nombre y precio de venta son obligatorios (precio > 0)")
            return
        }
        if (cost == null || cost < 0) {
            _uiState.value = ProductFormUiState(error = "El precio de coste es obligatorio (≥ 0)")
            return
        }
        viewModelScope.launch {
            _uiState.value = ProductFormUiState(isLoading = true)
            val request = CreateProductRequest(
                name = name,
                asin = asin.ifBlank { null },
                currentPrice = price,
                costPrice = cost,
                category = category.ifBlank { null },
                brand = brand.ifBlank { null },
                description = description.ifBlank { null },
                monitoringEnabled = monitoringEnabled
            )
            when (val result = repository.createProduct(request)) {
                is Result.Success -> _uiState.value = ProductFormUiState(success = true)
                is Result.Error -> _uiState.value = ProductFormUiState(error = result.message)
            }
        }
    }

    fun updateProduct(
        id: Long,
        name: String,
        asin: String,
        currentPrice: String,
        costPrice: String,
        category: String,
        brand: String,
        description: String,
        monitoringEnabled: Boolean
    ) {
        val price = currentPrice.toDoubleOrNull()
        val cost = costPrice.toDoubleOrNull()
        if (name.isBlank() || price == null || price <= 0) {
            _uiState.value = ProductFormUiState(error = "Nombre y precio de venta son obligatorios (precio > 0)")
            return
        }
        if (cost == null || cost < 0) {
            _uiState.value = ProductFormUiState(error = "El precio de coste es obligatorio (≥ 0)")
            return
        }
        viewModelScope.launch {
            _uiState.value = ProductFormUiState(isLoading = true)
            val request = UpdateProductRequest(
                name = name,
                asin = asin.ifBlank { null },
                currentPrice = price,
                costPrice = cost,
                category = category.ifBlank { null },
                brand = brand.ifBlank { null },
                description = description.ifBlank { null },
                monitoringEnabled = monitoringEnabled
            )
            when (val result = repository.updateProduct(id, request)) {
                is Result.Success -> _uiState.value = ProductFormUiState(success = true)
                is Result.Error -> _uiState.value = ProductFormUiState(error = result.message)
            }
        }
    }

    fun resetFormState() {
        _uiState.value = ProductFormUiState()
    }
}
