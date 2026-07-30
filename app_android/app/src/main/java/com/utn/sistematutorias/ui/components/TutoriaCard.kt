package com.utn.sistematutorias.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.utn.sistematutorias.data.remote.Tutoria

private fun colorEstado(estado: String): androidx.compose.ui.graphics.Color = when (estado) {
    "Realizada" -> androidx.compose.ui.graphics.Color(0xFF15803D)
    "Confirmada" -> androidx.compose.ui.graphics.Color(0xFF0E7C70)
    "Solicitada" -> androidx.compose.ui.graphics.Color(0xFFB45309)
    else -> androidx.compose.ui.graphics.Color(0xFF64748B)
}

@Composable
fun TutoriaCard(
    tutoria: Tutoria,
    mostrarAlumno: Boolean = false,
    contenidoAcciones: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = tutoria.tema,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = colorEstado(tutoria.estado),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = tutoria.estado,
                        color = androidx.compose.ui.graphics.Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = "Fecha: ${tutoria.fecha}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (mostrarAlumno && !tutoria.alumno.isNullOrBlank()) {
                Text(
                    text = "Alumno: ${tutoria.alumno}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (tutoria.observaciones.isNotBlank()) {
                Text(
                    text = tutoria.observaciones,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            contenidoAcciones()
        }
    }
}
