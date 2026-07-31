package com.utn.sistematutorias.wear.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Guarda en memoria el último resumen recibido del teléfono, para que la
 * actividad principal lo muestre en cuanto llegue un dato nuevo.
 */
object EstadoResumen {
    private val _resumen = MutableStateFlow<ResumenTutorias?>(null)
    val resumen = _resumen.asStateFlow()

    fun actualizar(nuevo: ResumenTutorias) {
        _resumen.value = nuevo
    }
}
