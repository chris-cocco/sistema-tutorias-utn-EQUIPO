package com.utn.sistematutorias.ui.coordinador

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.FlowRow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.utn.sistematutorias.ui.components.BarraDato
import com.utn.sistematutorias.ui.components.GraficaBarras
import com.utn.sistematutorias.ui.components.TarjetaIndicador

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoordinadorScreen(
    alCerrarSesion: () -> Unit,
    vm: CoordinadorViewModel = viewModel()
) {
    val estado by vm.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel del Coordinador") },
                actions = {
                    IconButton(onClick = { vm.cerrarSesion(alCerrarSesion) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                }
            )
        }
    ) { relleno ->
        Box(modifier = Modifier.padding(relleno)) {
            when {
                estado.cargando -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                estado.error != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text(estado.error ?: "") }

                estado.resumen != null -> {
                    val resumen = estado.resumen!!
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        item {
                            FlowRow(modifier = Modifier.fillMaxWidth()) {
                                TarjetaIndicador("Alumnos", resumen.usuarios.alumnos)
                                TarjetaIndicador("Tutores", resumen.usuarios.tutores)
                                TarjetaIndicador("Activos", resumen.usuarios.activos)
                                TarjetaIndicador("Bloqueados", resumen.usuarios.bloqueados)
                            }
                            Text(
                                text = "Tutorías por estado",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                            )
                            GraficaBarras(
                                datos = listOf(
                                    BarraDato("Solicitadas", resumen.tutorias.solicitadas, Color(0xFFB45309)),
                                    BarraDato("Confirmadas", resumen.tutorias.confirmadas, Color(0xFF0E7C70)),
                                    BarraDato("Asignadas", resumen.tutorias.asignadas, Color(0xFF6D28D9)),
                                    BarraDato("Realizadas", resumen.tutorias.realizadas, Color(0xFF15803D))
                                )
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                            Text(
                                text = "Usuarios registrados",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(estado.usuarios) { usuario ->
                            Card(modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(usuario.nombre, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${usuario.tipo.uppercase()} · ${usuario.credencial}" +
                                            if (usuario.bloqueado) " · Bloqueado" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
