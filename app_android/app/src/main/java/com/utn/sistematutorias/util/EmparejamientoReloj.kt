package com.utn.sistematutorias.util

import android.content.Context
import android.content.Intent
import android.provider.Settings

private const val PAQUETE_WEAR_OS = "com.google.android.wearable.app"

/**
 * Abre la app "Wear OS by Google" para que el usuario empareje su reloj desde
 * ahí (una app normal no puede iniciar el emparejamiento de Bluetooth con un
 * Wear OS por su cuenta). Si no la tiene instalada, abre los ajustes de
 * Bluetooth del sistema como alternativa.
 */
fun abrirEmparejamientoReloj(contexto: Context) {
    val intentWearOs = contexto.packageManager.getLaunchIntentForPackage(PAQUETE_WEAR_OS)
    if (intentWearOs != null) {
        intentWearOs.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        contexto.startActivity(intentWearOs)
    } else {
        contexto.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
