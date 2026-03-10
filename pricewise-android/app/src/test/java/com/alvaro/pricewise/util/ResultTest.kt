package com.alvaro.pricewise.util

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class ResultTest {

    // --- safeApiCall ---

    @Test
    fun `safeApiCall devuelve Success con body exitoso`() = runTest {
        val result = safeApiCall { Response.success("hello") }

        assertTrue(result is Result.Success)
        assertEquals("hello", (result as Result.Success).data)
    }

    @Test
    fun `safeApiCall devuelve Error con body null`() = runTest {
        val result = safeApiCall<String> { Response.success(null) }

        assertTrue(result is Result.Error)
        assertEquals("Respuesta vacía del servidor", (result as Result.Error).message)
    }

    @Test
    fun `safeApiCall devuelve Error con codigo HTTP 400`() = runTest {
        val errorBody = """{"success":false,"message":"Email duplicado"}"""
            .toResponseBody("application/json".toMediaType())
        val result = safeApiCall<String> {
            Response.error(400, errorBody)
        }

        assertTrue(result is Result.Error)
        assertEquals("Email duplicado", (result as Result.Error).message)
        assertEquals(400, result.code)
    }

    @Test
    fun `safeApiCall devuelve Error con codigo HTTP 500 sin body`() = runTest {
        val errorBody = "".toResponseBody("application/json".toMediaType())
        val result = safeApiCall<String> {
            Response.error(500, errorBody)
        }

        assertTrue(result is Result.Error)
        assertEquals("Error interno del servidor.", (result as Result.Error).message)
        assertEquals(500, result.code)
    }

    @Test
    fun `safeApiCall captura excepcion generica`() = runTest {
        val result = safeApiCall<String> { throw RuntimeException("Boom") }

        assertTrue(result is Result.Error)
        assertEquals("Boom", (result as Result.Error).message)
    }

    @Test
    fun `safeApiCall captura ConnectException`() = runTest {
        val result = safeApiCall<String> { throw java.net.ConnectException("refused") }

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("conectar con el servidor"))
    }

    @Test
    fun `safeApiCall captura SocketTimeoutException`() = runTest {
        val result = safeApiCall<String> { throw java.net.SocketTimeoutException("timeout") }

        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).message.contains("tardó demasiado"))
    }

    // --- safeApiCallNoBody ---

    @Test
    fun `safeApiCallNoBody devuelve Success con 204`() = runTest {
        val result = safeApiCallNoBody { Response.success<Unit>(204, Unit) }

        assertTrue(result is Result.Success)
    }

    @Test
    fun `safeApiCallNoBody devuelve Error con 403`() = runTest {
        val errorBody = "".toResponseBody("application/json".toMediaType())
        val result = safeApiCallNoBody {
            Response.error<Unit>(403, errorBody)
        }

        assertTrue(result is Result.Error)
        assertEquals("No tienes permisos para esta acción.", (result as Result.Error).message)
    }

    // --- parseErrorBody (indirectly via safeApiCall) ---

    @Test
    fun `safeApiCall extrae message de JSON de error`() = runTest {
        val errorBody = """{"success":false,"message":"El SKU ya existe","data":null}"""
            .toResponseBody("application/json".toMediaType())
        val result = safeApiCall<String> {
            Response.error(400, errorBody)
        }

        assertEquals("El SKU ya existe", (result as Result.Error).message)
    }

    @Test
    fun `safeApiCall usa mensaje HTTP si JSON no tiene message`() = runTest {
        val errorBody = """{"error":"something"}"""
            .toResponseBody("application/json".toMediaType())
        val result = safeApiCall<String> {
            Response.error(404, errorBody)
        }

        assertEquals("El recurso no fue encontrado.", (result as Result.Error).message)
    }
}
