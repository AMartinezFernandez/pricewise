package com.alvaro.pricewise.ui.search

import app.cash.turbine.test
import com.alvaro.pricewise.data.model.ApiResponse
import com.alvaro.pricewise.data.model.CompetitorPriceResponse
import com.alvaro.pricewise.data.model.PageResponse
import com.alvaro.pricewise.data.model.ProductResponse
import com.alvaro.pricewise.data.repository.ProductRepository
import com.alvaro.pricewise.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val repository = mockk<ProductRepository>()
    private lateinit var viewModel: SearchViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        viewModel = SearchViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // --- ASIN Detection ---

    @Test
    fun `search con ASIN valido llama getAmazonPrice`() = runTest {
        val priceResponse = CompetitorPriceResponse(
            asin = "B0TEST1234",
            title = "Test Product",
            price = 29.99
        )
        coEvery { repository.getAmazonPrice("B0TEST1234") } returns
                Result.Success(ApiResponse(success = true, data = priceResponse))

        viewModel.search("B0TEST1234")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.products.size)
        assertEquals(-1L, state.products[0].id)
        assertEquals("Test Product", state.products[0].name)
        assertEquals("B0TEST1234", state.products[0].asin)
        coVerify { repository.getAmazonPrice("B0TEST1234") }
    }

    @Test
    fun `search con ASIN en minusculas lo convierte a mayusculas`() = runTest {
        coEvery { repository.getAmazonPrice("B0TEST1234") } returns
                Result.Success(ApiResponse(success = true, data = CompetitorPriceResponse(
                    asin = "B0TEST1234", price = 10.0
                )))

        viewModel.search("b0test1234")
        advanceUntilIdle()

        coVerify { repository.getAmazonPrice("B0TEST1234") }
    }

    @Test
    fun `search ASIN con espacios los recorta`() = runTest {
        coEvery { repository.getAmazonPrice("B0TEST1234") } returns
                Result.Success(ApiResponse(success = true, data = CompetitorPriceResponse(
                    asin = "B0TEST1234", price = 10.0
                )))

        viewModel.search("  B0TEST1234  ")
        advanceUntilIdle()

        coVerify { repository.getAmazonPrice("B0TEST1234") }
    }

    @Test
    fun `search ASIN con error muestra mensaje`() = runTest {
        coEvery { repository.getAmazonPrice("B0TEST1234") } returns
                Result.Error("Keepa no disponible")

        viewModel.search("B0TEST1234")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Keepa no disponible", state.error)
        assertTrue(state.products.isEmpty())
    }

    @Test
    fun `search ASIN con success false muestra error`() = runTest {
        coEvery { repository.getAmazonPrice("B0TEST1234") } returns
                Result.Success(ApiResponse(success = false, message = "ASIN no encontrado", data = null))

        viewModel.search("B0TEST1234")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("ASIN no encontrado", state.error)
    }

    // --- Normal Search ---

    @Test
    fun `search con texto normal llama searchProducts`() = runTest {
        val products = listOf(
            ProductResponse(id = 1L, name = "Producto 1", currentPrice = 10.0),
            ProductResponse(id = 2L, name = "Producto 2", currentPrice = 20.0)
        )
        coEvery { repository.searchProducts(name = "test", category = null, brand = null, page = 0) } returns
                Result.Success(ApiResponse(
                    success = true,
                    data = PageResponse(content = products, totalElements = 2, hasNext = false)
                ))

        viewModel.search("test")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.products.size)
        assertEquals(2L, state.totalElements)
        assertFalse(state.hasMore)
    }

    @Test
    fun `search con query de 9 caracteres NO es ASIN`() = runTest {
        coEvery { repository.searchProducts(name = "B0TEST123", category = null, brand = null, page = 0) } returns
                Result.Success(ApiResponse(success = true, data = PageResponse()))

        viewModel.search("B0TEST123")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.getAmazonPrice(any()) }
        coVerify { repository.searchProducts(name = "B0TEST123", category = null, brand = null, page = 0) }
    }

    @Test
    fun `search con query de 11 caracteres NO es ASIN`() = runTest {
        coEvery { repository.searchProducts(name = "B0TEST12345", category = null, brand = null, page = 0) } returns
                Result.Success(ApiResponse(success = true, data = PageResponse()))

        viewModel.search("B0TEST12345")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.getAmazonPrice(any()) }
    }

    @Test
    fun `search con query con caracteres especiales NO es ASIN`() = runTest {
        coEvery { repository.searchProducts(name = "B0TEST123!", category = null, brand = null, page = 0) } returns
                Result.Success(ApiResponse(success = true, data = PageResponse()))

        viewModel.search("B0TEST123!")
        advanceUntilIdle()

        coVerify(exactly = 0) { repository.getAmazonPrice(any()) }
    }

    // --- Clear ---

    @Test
    fun `clearResults resetea el estado`() = runTest {
        // First populate some state
        coEvery { repository.searchProducts(name = "test", category = null, brand = null, page = 0) } returns
                Result.Success(ApiResponse(
                    success = true,
                    data = PageResponse(content = listOf(ProductResponse(id = 1L, name = "P1", currentPrice = 5.0)), totalElements = 1)
                ))
        viewModel.search("test")
        advanceUntilIdle()

        viewModel.clearResults()

        val state = viewModel.uiState.value
        assertTrue(state.products.isEmpty())
        assertEquals(0L, state.totalElements)
        assertNull(state.error)
        assertEquals("", state.searchQuery)
    }

    @Test
    fun `search con query vacia limpia resultados`() = runTest {
        viewModel.search("")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.products.isEmpty())
        assertFalse(state.isLoading)
    }

    // --- Loading state ---

    @Test
    fun `search establece isLoading durante la busqueda`() = runTest {
        coEvery { repository.searchProducts(name = "test", category = null, brand = null, page = 0) } returns
                Result.Success(ApiResponse(success = true, data = PageResponse()))

        viewModel.uiState.test {
            assertEquals(SearchUiState(), awaitItem()) // initial

            viewModel.search("test")
            val loading = awaitItem()
            assertTrue(loading.isLoading)
            assertEquals("test", loading.searchQuery)

            val result = awaitItem()
            assertFalse(result.isLoading)
        }
    }
}
