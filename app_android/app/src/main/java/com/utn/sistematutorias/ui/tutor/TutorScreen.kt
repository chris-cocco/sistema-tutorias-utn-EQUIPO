package com.utn.sistematutorias.ui.tutor

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.utn.sistematutorias.data.remote.Tutoria
import com.utn.sistematutorias.ui.components.TutoriaCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorScreen(
    alCerrarSesion: () -> Unit,
    vm: TutorViewModel = viewModel()
) {
    val estado by vm.uiState.collectAsState()

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
                ) { Text("No tienes tutorías asignadas") }

                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(estado.tutorias) { tutoria ->
                        TutoriaCard(tutoria = tutoria, mostrarAlumno = true) {
                            AccionesTutoria(tutoria = tutoria, vm = vm)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccionesTutoria(tutoria: Tutoria, vm: TutorViewModel) {
    when (tutoria.estado) {
        "Solicitada" -> Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Button(onClick = { vm.aceptarTutoria(tutoria.id) }) { Text("Aceptar") }
        }
        "Confirmada", "Asignada por tutor" -> Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Button(onClick = { vm.completarTutoria(tutoria.id) }) { Text("Marcar como realizada") }
        }
    }
}
