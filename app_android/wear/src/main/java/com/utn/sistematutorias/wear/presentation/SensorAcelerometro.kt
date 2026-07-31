package com.utn.sistematutorias.wear.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

data class LecturaAcelerometro(val x: Float = 0f, val y: Float = 0f, val z: Float = 0f)

/**
 * Lee el acelerómetro del reloj en tiempo real. Cumple el requisito de usar
 * al menos un sensor del dispositivo wearable en una funcionalidad del proyecto.
 */
@Composable
fun rememberLecturaAcelerometro(): LecturaAcelerometro {
    val contexto = LocalContext.current
    var lectura by remember { mutableStateOf(LecturaAcelerometro()) }

    DisposableEffect(Unit) {
        val gestorSensores = contexto.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = gestorSensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val escucha = object : SensorEventListener {
            override fun onSensorChanged(evento: SensorEvent) {
                lectura = LecturaAcelerometro(evento.values[0], evento.values[1], evento.values[2])
            }

            override fun onAccuracyChanged(sensor: Sensor?, precision: Int) {}
        }

        if (sensor != null) {
            gestorSensores.registerListener(escucha, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            gestorSensores.unregisterListener(escucha)
        }
    }

    return lectura
}
