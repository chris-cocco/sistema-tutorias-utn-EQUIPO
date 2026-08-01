package com.utn.sistematutorias.ui.tutor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.utn.sistematutorias.data.remote.AlumnoAsignado
import com.utn.sistematutorias.data.remote.Tutoria
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
fun TutorScreen(
    alCerrarSesion: () -> Unit,
    vm: TutorViewModel = viewModel()
) {
    val estado by vm.uiState.collectAsState()
    var mostrarDialogoNuevaTutoria by remember { mutableStateOf(false) }
    var tutoriaEditando by remember { mutableStateOf<Tutoria?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tutorías a mi cargo") },
                actions = {
                    IconButton(onClick = { vm.cerrarSesion(alCerrarSesion) }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar sesión")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoNuevaTutoria = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Asignar nueva tutoría")
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
                            text = "Tutorías a mi cargo",
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
                            Text("No tienes tutorías asignadas", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> items(estado.tutorias) { tutoria ->
                            TutoriaCard(tutoria = tutoria, mostrarAlumno = true) {
                                AccionesTutoria(
                                    tutoria = tutoria,
                                    vm = vm,
                                    alEditar = { tutoriaEditando = tutoria }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoNuevaTutoria) {
        DialogoAsignarTutoria(
            alumnos = estado.alumnos,
            onCerrar = { mostrarDialogoNuevaTutoria = false },
            onEnviar = { idAlumno, fecha, tema ->
                vm.crearTutoria(idAlumno, fecha, tema) { _, _ -> mostrarDialogoNuevaTutoria = false }
            }
        )
    }

    tutoriaEditando?.let { tutoria ->
        DialogoEditarTutoria(
            tutoria = tutoria,
            onCerrar = { tutoriaEditando = null },
            onGuardar = { fecha, tema, nuevoEstado, observaciones ->
                vm.editarTutoria(tutoria.id, fecha, tema, nuevoEstado, observaciones) { exito ->
                    if (exito) tutoriaEditando = null
                }
            }
        )
    }
}

@Composable
private fun SeccionMisDatos(estado: TutorUiState, vm: TutorViewModel) {
    var horarioTexto by remember(estado.horario) { mutableStateOf(estado.horario) }

    Text(
        text = "Mis Datos",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = horarioTexto,
            onValueChange = { horarioTexto = it },
            label = { Text("Horario") },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.padding(4.dp))
        Button(
            onClick = { vm.actualizarHorario(horarioTexto) },
            enabled = !estado.actualizandoHorario
        ) { Text(if (estado.actualizandoHorario) "..." else "Guardar") }
    }
    OutlinedButton(
        onClick = { vm.descargarReportePdf() },
        enabled = !estado.descargandoPdf,
        modifier = Modifier.padding(top = 8.dp)
    ) { Text(if (estado.descargandoPdf) "Generando..." else "Descargar reporte PDF") }
    EstadoReloj(conectado = estado.relojConectado, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun SeccionMisReportes(estado: TutorUiState, vm: TutorViewModel) {
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
}

@Composable
private fun AccionesTutoria(tutoria: Tutoria, vm: TutorViewModel, alEditar: () -> Unit) {
    when (tutoria.estado) {
        "Solicitada" -> Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Button(onClick = { vm.aceptarTutoria(tutoria.id) }, modifier = Modifier.padding(end = 8.dp)) { Text("Aceptar") }
            OutlinedButton(onClick = alEditar) { Text("Editar") }
        }
        "Confirmada", "Asignada por tutor" -> Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Button(onClick = { vm.completarTutoria(tutoria.id) }) { Text("Marcar como realizada") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoAsignarTutoria(
    alumnos: List<AlumnoAsignado>,
    onCerrar: () -> Unit,
    onEnviar: (idAlumno: Int, fecha: String, tema: String) -> Unit
) {
    var alumnoSeleccionado by remember { mutableStateOf<AlumnoAsignado?>(null) }
    var mostrarSelectorAlumno by remember { mutableStateOf(false) }
    var tema by remember { mutableStateOf("") }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    val estadoFecha = rememberDatePickerState()
    val formato = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
    }
    val fechaTexto = estadoFecha.selectedDateMillis?.let { formato.format(Date(it)) } ?: ""

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Asignar Nueva Tutoría") },
        text = {
            Column {
                Box {
                    OutlinedTextField(
                        value = alumnoSeleccionado?.nombre ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Alumno") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { mostrarSelectorAlumno = true }
                    )
                    DropdownMenu(expanded = mostrarSelectorAlumno, onDismissRequest = { mostrarSelectorAlumno = false }) {
                        alumnos.forEach { alumno ->
                            DropdownMenuItem(
                                text = { Text(alumno.nombre) },
                                onClick = { alumnoSeleccionado = alumno; mostrarSelectorAlumno = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
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
                enabled = alumnoSeleccionado != null && fechaTexto.isNotBlank() && tema.isNotBlank(),
                onClick = { onEnviar(alumnoSeleccionado!!.id, fechaTexto, tema) }
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )

    if (mostrarSelectorFecha) {
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = { TextButton(onClick = { mostrarSelectorFecha = false }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { mostrarSelectorFecha = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estadoFecha) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoEditarTutoria(
    tutoria: Tutoria,
    onCerrar: () -> Unit,
    onGuardar: (fecha: String, tema: String, estado: String, observaciones: String) -> Unit
) {
    var tema by remember { mutableStateOf(tutoria.tema) }
    var observaciones by remember { mutableStateOf(tutoria.observaciones) }
    var estadoSeleccionado by remember { mutableStateOf(tutoria.estado) }
    var mostrarSelectorEstado by remember { mutableStateOf(false) }
    var mostrarSelectorFecha by remember { mutableStateOf(false) }
    val formato = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
    }
    val estadoFecha = rememberDatePickerState(
        initialSelectedDateMillis = runCatching { formato.parse(tutoria.fecha)?.time }.getOrNull()
    )
    val fechaTexto = estadoFecha.selectedDateMillis?.let { formato.format(Date(it)) } ?: tutoria.fecha
    val opcionesEstado = listOf("Solicitada", "Confirmada", "Realizada")

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Editar Tutoría") },
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
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { mostrarSelectorFecha = true }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Box {
                    OutlinedTextField(
                        value = estadoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Estado") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { mostrarSelectorEstado = true }
                    )
                    DropdownMenu(expanded = mostrarSelectorEstado, onDismissRequest = { mostrarSelectorEstado = false }) {
                        opcionesEstado.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = { estadoSeleccionado = opcion; mostrarSelectorEstado = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = observaciones,
                    onValueChange = { observaciones = it },
                    label = { Text("Observaciones") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = tema.isNotBlank(),
                onClick = { onGuardar(fechaTexto, tema, estadoSeleccionado, observaciones) }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )

    if (mostrarSelectorFecha) {
        DatePickerDialog(
            onDismissRequest = { mostrarSelectorFecha = false },
            confirmButton = { TextButton(onClick = { mostrarSelectorFecha = false }) { Text("Aceptar") } },
            dismissButton = { TextButton(onClick = { mostrarSelectorFecha = false }) { Text("Cancelar") } }
        ) { DatePicker(state = estadoFecha) }
    }
}
