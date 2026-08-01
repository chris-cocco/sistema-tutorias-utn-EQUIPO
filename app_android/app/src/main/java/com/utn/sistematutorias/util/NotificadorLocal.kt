package com.utn.sistematutorias.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

private const val ID_CANAL = "tutorias_pendientes"
private const val ID_NOTIFICACION = 2001

/**
 * Notifica en el propio teléfono cuántas tutorías tiene pendientes el usuario
 * (mismo criterio que ya se le manda al reloj vía SincronizadorReloj), para
 * que la app avise aunque no haya ningún Wear OS emparejado.
 */
fun notificarPendientesEnTelefono(contexto: Context, pendientes: Int) {
    if (pendientes <= 0) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val gestor = contexto.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (gestor.getNotificationChannel(ID_CANAL) == null) {
            gestor.createNotificationChannel(
                NotificationChannel(ID_CANAL, "Tutorías pendientes", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
    }

    if (ContextCompat.checkSelfPermission(contexto, android.Manifest.permission.POST_NOTIFICATIONS)
        != PackageManager.PERMISSION_GRANTED
    ) return

    val notificacion = NotificationCompat.Builder(contexto, ID_CANAL)
        .setContentTitle("Sistema de Tutorías")
        .setContentText("Tienes $pendientes tutoría(s) pendiente(s)")
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(contexto).notify(ID_NOTIFICACION, notificacion)
}
