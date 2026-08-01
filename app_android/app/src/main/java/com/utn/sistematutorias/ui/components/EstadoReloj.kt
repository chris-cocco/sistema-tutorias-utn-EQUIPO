package com.utn.sistematutorias.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Punto de color + texto indicando si hay un reloj Wear OS emparejado y
 * conectado. `null` = todavía no se sabe (no ha terminado la primera carga).
 */
@Composable
fun EstadoReloj(conectado: Boolean?, modifier: Modifier = Modifier) {
    val (color, texto) = when (conectado) {
        true -> Color(0xFF15803D) to "Reloj conectado"
        false -> Color(0xFF64748B) to "Sin reloj conectado"
        null -> Color(0xFF64748B) to "Buscando reloj..."
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Surface(color = color, shape = CircleShape, modifier = Modifier.padding(end = 6.dp).size(8.dp)) {}
        Text(texto, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
