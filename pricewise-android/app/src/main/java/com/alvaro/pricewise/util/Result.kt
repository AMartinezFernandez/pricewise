package com.alvaro.pricewise.util

import retrofit2.Response

/**
 * Wrapper de resultado para manejar éxito y error de forma uniforme
 * en toda la aplicación, sin excepciones sin controlar.
 */
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val code: Int? = null) : Result<Nothing>()
}

/**
 * Ejecuta una llamada a la API de forma segura.
 * Captura excepciones de red y errores HTTP y los convierte en Result.Error.
 */
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.Success(body)
            } else {
                Result.Error("Respuesta vacía del servidor", response.code())
            }
        } else {
            val serverMessage = parseErrorBody(response)
            Result.Error(serverMessage ?: httpErrorMessage(response.code()), response.code())
        }
    } catch (e: java.net.ConnectException) {
        Result.Error("No se puede conectar con el servidor. ¿Está arrancado el backend?")
    } catch (e: java.net.SocketTimeoutException) {
        Result.Error("El servidor tardó demasiado en responder.")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error desconocido")
    }
}

/**
 * Variante para llamadas sin cuerpo de respuesta (DELETE, PUT → 204 No Content).
 * Considera exitosa cualquier respuesta 2xx aunque el body sea null.
 */
suspend fun safeApiCallNoBody(call: suspend () -> Response<*>): Result<Unit> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            Result.Success(Unit)
        } else {
            val serverMessage = parseErrorBody(response)
            Result.Error(serverMessage ?: httpErrorMessage(response.code()), response.code())
        }
    } catch (e: java.net.ConnectException) {
        Result.Error("No se puede conectar con el servidor. ¿Está arrancado el backend?")
    } catch (e: java.net.SocketTimeoutException) {
        Result.Error("El servidor tardó demasiado en responder.")
    } catch (e: Exception) {
        Result.Error(e.message ?: "Error desconocido")
    }
}

/**
 * Intenta extraer el campo "message" del cuerpo de error JSON devuelto por el backend.
 * El backend siempre devuelve ApiResponse con formato: {"success":false,"message":"...","data":null}
 */
private fun parseErrorBody(response: Response<*>): String? {
    return try {
        val errorBody = response.errorBody()?.string() ?: return null
        // Extraer "message" del JSON sin depender de Moshi/Gson para evitar dependencias circulares
        val messageRegex = """"message"\s*:\s*"([^"]+)"""".toRegex()
        messageRegex.find(errorBody)?.groupValues?.getOrNull(1)
    } catch (_: Exception) {
        null
    }
}

private fun httpErrorMessage(code: Int) = when (code) {
    400 -> "Datos incorrectos. Revisa los campos."
    401 -> "Credenciales incorrectas o sesión expirada."
    403 -> "No tienes permisos para esta acción."
    404 -> "El recurso no fue encontrado."
    409 -> "El registro ya existe."
    500 -> "Error interno del servidor."
    else -> "Error $code"
}
