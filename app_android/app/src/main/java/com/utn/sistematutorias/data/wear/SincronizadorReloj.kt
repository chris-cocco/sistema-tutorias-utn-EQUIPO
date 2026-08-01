package com.utn.sistematutorias.data.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val RUTA_RESUMEN = "/resumen_tutorias"

/**
 * Manda al reloj (si hay uno emparejado) un resumen de tutorías cada vez que
 * la app del teléfono carga datos frescos, usando la Wearable Data Layer API.
 * Si no hay reloj emparejado esto simplemente no tiene efecto.
 */
object SincronizadorReloj {

    fun enviarResumen(contexto: Context, rol: String, nombre: String, total: Int, pendientes: Int) {
        val solicitud = PutDataMapRequest.create(RUTA_RESUMEN).apply {
            dataMap.putString("rol", rol)
            dataMap.putString("nombre", nombre)
            dataMap.putInt("total", total)
            dataMap.putInt("pendientes", pendientes)
            dataMap.putLong("marcaDeTiempo", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(contexto).putDataItem(solicitud)
    }

    /**
     * Consulta si hay algún reloj (nodo Wear OS) realmente emparejado y
     * conectado con este teléfono, para poder mostrarlo en la pantalla en
     * vez de mandar los datos "a ciegas" sin saber si llegan a algún lado.
     */
    suspend fun hayRelojConectado(contexto: Context): Boolean = suspendCancellableCoroutine { continuacion ->
        Wearable.getNodeClient(contexto).connectedNodes
            .addOnSuccessListener { nodos -> continuacion.resume(nodos.isNotEmpty()) }
            .addOnFailureListener { continuacion.resume(false) }
    }
}
