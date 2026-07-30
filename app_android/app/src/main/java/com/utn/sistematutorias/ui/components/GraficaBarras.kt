package com.utn.sistematutorias.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class BarraDato(val etiqueta: String, val valor: Int, val color: Color)

/**
 * Grafica de barras horizontales sencilla, hecha con componentes básicos de Compose
 * (sin librerías externas) para cumplir el requisito de mostrar información mediante
 * gráficas en el panel del coordinador.
 */
@Composable
fun GraficaBarras(datos: List<BarraDato>) {
    val maximo = (datos.maxOfOrNull { it.valor } ?: 0).coerceAtLeast(1)
    Column(modifier = Modifier.fillMaxWidth()) {
        datos.forEach { dato ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = dato.etiqueta,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(90.dp)
                )
                Box(modifier = Modifier.weight(1f)) {
                    val fraccion = (dato.valor.toFloat() / maximo).coerceIn(0.03f, 1f)
                    Surface(
                        color = dato.color,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .fillMaxWidth(fraction = fraccion)
                            .height(20.dp)
                    ) {}
                }
                Text(
                    text = dato.valor.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(32.dp)
                )
            }
        }
    }
}
