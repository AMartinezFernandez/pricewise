package com.alvaro.pricewise.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gestiona eventos de sesión a nivel de app.
 * Cuando el AuthInterceptor detecta un 401 (token expirado),
 * emite un evento que el NavGraph recoge para redirigir al login.
 */
@Singleton
class SessionManager @Inject constructor() {

    private val _sessionExpired = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val sessionExpired: SharedFlow<Unit> = _sessionExpired.asSharedFlow()

    private val alreadyExpired = AtomicBoolean(false)

    fun notifySessionExpired() {
        if (alreadyExpired.compareAndSet(false, true)) {
            _sessionExpired.tryEmit(Unit)
        }
    }

    /** Llamar tras login exitoso para permitir futuras detecciones de expiración */
    fun resetExpiredFlag() {
        alreadyExpired.set(false)
    }
}
