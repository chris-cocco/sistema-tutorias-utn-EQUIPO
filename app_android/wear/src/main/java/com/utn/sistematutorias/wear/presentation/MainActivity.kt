package com.utn.sistematutorias.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.utn.sistematutorias.wear.data.EstadoResumen
import com.utn.sistematutorias.wear.data.ResumenTutorias

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PantallaPrincipal()
        }
    }
}

@Composable
fun PantallaPrincipal() {
    var mostrarSensor by remember { mutableStateOf(false) }
    val resumen by EstadoResumen.resumen.collectAsState()

    MaterialTheme {
        if (mostrarSensor) {
            PantallaSensor(alRegresar = { mostrarSensor = false })
        } else {
            PantallaResumen(resumen = resumen, alVerSensor = { mostrarSensor = true })
        }
    }
}

@Composable
private fun PantallaResumen(resumen: ResumenTutorias?, alVerSensor: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Sistema de Tutorías", style = MaterialTheme.typography.title3)

        if (resumen == null) {
            Text(text = "Esperando datos del teléfono…", style = MaterialTheme.typography.body2)
        } else {
            Text(text = resumen.nombre, style = MaterialTheme.typography.body1)
            Text(text = resumen.rol.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.caption1)
            Text(text = "${resumen.pendientes} pendiente(s) de ${resumen.total}", style = MaterialTheme.typography.body2)
        }

        Chip(
            onClick = alVerSensor,
            label = { Text("Ver sensor") },
            colors = ChipDefaults.primaryChipColors(),
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun PantallaSensor(alRegresar: () -> Unit) {
    val lectura = rememberLecturaAcelerometro()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Acelerómetro", style = MaterialTheme.typography.title3)
        Text(text = "X: %.2f".format(lectura.x), style = MaterialTheme.typography.body2)
        Text(text = "Y: %.2f".format(lectura.y), style = MaterialTheme.typography.body2)
        Text(text = "Z: %.2f".format(lectura.z), style = MaterialTheme.typography.body2)

        Chip(
            onClick = alRegresar,
            label = { Text("Regresar") },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
