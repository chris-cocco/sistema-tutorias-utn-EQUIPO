package com.utn.sistematutorias.wear.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService

private const val RUTA_RESUMEN = "/resumen_tutorias"
private const val ID_CANAL = "resumen_tutorias"

/**
 * Recibe los datos que manda la app del teléfono cada vez que carga tutorías
 * (vía Wearable DataClient) y notifica al usuario en el reloj.
 */
class EscuchaDatosService : WearableListenerService() {

    override fun onDataChanged(eventos: DataEventBuffer) {
        for (evento in eventos) {
            if (evento.type != DataEvent.TYPE_CHANGED) continue
            val item = evento.dataItem
            if (item.uri.path != RUTA_RESUMEN) continue

            val mapa = DataMapItem.fromDataItem(item).dataMap
            val resumen = ResumenTutorias(
                rol = mapa.getString("rol", ""),
                nombre = mapa.getString("nombre", ""),
                total = mapa.getInt("total"),
                pendientes = mapa.getInt("pendientes")
            )
            EstadoResumen.actualizar(resumen)
            notificar(resumen)
        }
    }

    private fun notificar(resumen: ResumenTutorias) {
        crearCanalSiNoExiste()

        val texto = when {
            resumen.pendientes > 0 -> "Tienes ${resumen.pendientes} tutoría(s) pendiente(s)"
            else -> "No tienes tutorías pendientes"
        }

        val notificacion = NotificationCompat.Builder(this, ID_CANAL)
            .setContentTitle("Sistema de Tutorías")
            .setContentText(texto)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        NotificationManagerCompat.from(this).notify(1, notificacion)
    }

    private fun crearCanalSiNoExiste() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val gestor = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (gestor.getNotificationChannel(ID_CANAL) != null) return
        gestor.createNotificationChannel(
            NotificationChannel(ID_CANAL, "Resumen de tutorías", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}
