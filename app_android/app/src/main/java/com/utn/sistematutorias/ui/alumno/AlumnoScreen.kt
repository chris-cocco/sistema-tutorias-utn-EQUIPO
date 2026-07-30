package com.utn.sistematutorias.ui.alumno

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
            when {
                estado.cargando -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                estado.error != null -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text(estado.error ?: "") }

                estado.tutorias.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { Text("Aún no tienes tutorías registradas") }

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(estado.tutorias) { tutoria ->
                        TutoriaCard(tutoria = tutoria)
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
    val formato = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
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
