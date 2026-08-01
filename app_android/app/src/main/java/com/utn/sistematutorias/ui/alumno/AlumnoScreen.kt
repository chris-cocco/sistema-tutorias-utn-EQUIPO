package com.utn.sistematutorias.ui.alumno

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.utn.sistematutorias.ui.components.BarraDato
import com.utn.sistematutorias.ui.components.EstadoReloj
import com.utn.sistematutorias.ui.components.GraficaBarras
import com.utn.sistematutorias.ui.components.TarjetaIndicador
import com.utn.sistematutorias.ui.components.TutoriaCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlumnoScreen(
    alCerrarSesion: () -> Unit,
    vm: AlumnoViewModel = viewModel()
) {
    val estado by vm.uiState.collectAsState()
    var mostrarDialogoNuevaTutoria by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Tutorías") },
                actions = {
                    IconButton(onClick = { vm.cerrarSesion(alCerrarSesion) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoNuevaTutoria = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Solicitar tutoría")
            }
        }
    ) { relleno ->
        Box(modifier = Modifier.padding(relleno)) {
            if (estado.cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        SeccionMisDatos(estado = estado, vm = vm)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        SeccionMisReportes(estado = estado, vm = vm)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                        Text(
                            text = "Mis Tutorías",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    when {
                        estado.error != null -> item {
                            Column {
                                Text(estado.error ?: "", color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = { vm.cargarTutorias() }) { Text("Reintentar") }
                            }
                        }
                        estado.tutorias.isEmpty() -> item {
                            Text(
                                "Aún no tienes tutorías registradas",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> items(estado.tutorias) { tutoria -> TutoriaCard(tutoria = tutoria) }
                    }
                }
            }
        }
    }

    if (mostrarDialogoNuevaTutoria) {
        DialogoSolicitarTutoria(
            enviando = estado.enviandoSolicitud,
            onCerrar = { mostrarDialogoNuevaTutoria = false },
            onEnviar = { fecha, tema ->
                vm.solicitarTutoria(fecha, tema) { _, _ ->
                    mostrarDialogoNuevaTutoria = false
                }
            }
        )
    }
}

@Composable
private fun SeccionMisDatos(estado: AlumnoUiState, vm: AlumnoViewModel) {
    var nombreTexto by remember(estado.nombre) { mutableStateOf(estado.nombre) }

    Text(
        text = "Mis Datos",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = nombreTexto,
            onValueChange = { nombreTexto = it },
            label = { Text("Nombre") },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.padding(4.dp))
        Button(
            onClick = { vm.actualizarNombre(nombreTexto) },
            enabled = !estado.actualizandoNombre
        ) { Text(if (estado.actualizandoNombre) "..." else "Guardar") }
    }
    if (estado.mensajeNombre != null) {
        Text(
            estado.mensajeNombre,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    EstadoReloj(conectado = estado.relojConectado, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SeccionMisReportes(estado: AlumnoUiState, vm: AlumnoViewModel) {
    val reporte = estado.reporte
    Text(
        text = "Mis Reportes",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    FlowRow(modifier = Modifier.fillMaxWidth()) {
        TarjetaIndicador("Total", reporte.total)
        TarjetaIndicador("Realizadas", reporte.realizadas)
        TarjetaIndicador("Pendientes", reporte.pendientes)
    }
    GraficaBarras(
        datos = listOf(
            BarraDato("Realizadas", reporte.realizadas, Color(0xFF15803D)),
            BarraDato("Pendientes", reporte.pendientes, Color(0xFFB45309))
        )
    )
    OutlinedButton(
        onClick = { vm.descargarReportePdf() },
        enabled = !estado.descargandoPdf,
        modifier = Modifier.padding(top = 8.dp)
    ) { Text(if (estado.descargandoPdf) "Generando..." else "Descargar reporte PDF") }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoSolicitarTutoria(
    enviando: Boolean,
    onCerrar: () -> Unit,
    onEnviar: (fecha: String, tema: String) -> Unit
) {
    var tema by remember { mutableStateOf("") }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    val estadoFecha = rememberDatePickerState()
    val formato = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
    }
    val fechaTexto = estadoFecha.selectedDateMillis?.let { formato.format(Date(it)) } ?: ""

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Solicitar tutoría") },
        text = {
            Column {
                OutlinedTextField(
                    value = tema,
                    onValueChange = { tema = it },
                    label = { Text("Tema") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box {
                    OutlinedTextField(
                        value = fechaTexto,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fecha") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    // Capa transparente encima: un OutlinedTextField de solo lectura
                    // intercepta el toque para el cursor antes que un .clickable propio,
                    // así que se necesita esta capa para que el toque sí abra el selector.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { mostrarSelectorFecha = true }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                enabled = fechaTexto.isNotBlank() && tema.isNotBlank() && !enviando,
                onClick = { onEnviar(fechaTexto, tema) }
            ) { Text(if (enviando) "Enviando..." else "Enviar") }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) { Text("Cancelar") }
        }
    )

    if (mostrarSelectorFecha) {
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = {
                TextButton(onClick = { mostrarSelectorFecha = false }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarSelectorFecha = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = estadoFecha)
        }
    }
}
