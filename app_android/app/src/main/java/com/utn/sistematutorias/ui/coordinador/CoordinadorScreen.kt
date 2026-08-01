package com.utn.sistematutorias.ui.coordinador

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.utn.sistematutorias.data.remote.Usuario
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
    var mostrarDialogoCrearUsuario by remember { mutableStateOf(false) }

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
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { mostrarDialogoCrearUsuario = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Crear usuario")
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
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(estado.error ?: "")
                        Button(onClick = { vm.cargarDatos() }, modifier = Modifier.padding(top = 12.dp)) {
                            Text("Reintentar")
                        }
                    }
                }

                estado.resumen != null -> {
                    val resumen = estado.resumen!!
                    val tutores = estado.usuarios.filter { it.tipo == "tutor" }
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
                            OutlinedButton(
                                onClick = { vm.descargarReporteGeneralPdf() },
                                enabled = !estado.descargandoPdf,
                                modifier = Modifier.padding(top = 8.dp)
                            ) { Text(if (estado.descargandoPdf) "Generando..." else "Reporte General PDF") }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                            Text(
                                text = "Usuarios registrados",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        items(estado.usuarios) { usuario ->
                            FilaUsuario(usuario = usuario, tutores = tutores, vm = vm)
                        }
                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                            SeccionRespaldos(estado = estado, vm = vm)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                            SeccionAuditoria(estado = estado)
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoCrearUsuario) {
        DialogoCrearUsuario(
            onCerrar = { mostrarDialogoCrearUsuario = false },
            onCrear = { tipo, credencial, nombre, contrasena ->
                vm.crearUsuario(tipo, credencial, nombre, contrasena) { exito, _ ->
                    if (exito) mostrarDialogoCrearUsuario = false
                }
            }
        )
    }
}

@Composable
private fun FilaUsuario(usuario: Usuario, tutores: List<Usuario>, vm: CoordinadorViewModel) {
    var tutorSeleccionado by remember { mutableStateOf<Usuario?>(null) }
    var mostrarSelectorTutor by remember { mutableStateOf(false) }

    Card(modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                Column {
                    Text(usuario.nombre, fontWeight = FontWeight.Bold)
                    Text(
                        "${usuario.tipo.uppercase()} · ${usuario.credencial}" +
                            if (usuario.bloqueado) " · Bloqueado" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(onClick = { vm.cambiarEstadoUsuario(usuario.id) }) {
                    Text(if (usuario.bloqueado) "Desbloquear" else "Bloquear")
                }
            }
            if (usuario.tipo == "alumno") {
                Row(modifier = Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(160.dp)) {
                        OutlinedTextField(
                            value = tutorSeleccionado?.nombre ?: "Selecciona tutor",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { mostrarSelectorTutor = true }
                        )
                        DropdownMenu(expanded = mostrarSelectorTutor, onDismissRequest = { mostrarSelectorTutor = false }) {
                            tutores.forEach { tutor ->
                                DropdownMenuItem(
                                    text = { Text(tutor.nombre) },
                                    onClick = { tutorSeleccionado = tutor; mostrarSelectorTutor = false }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(
                        enabled = tutorSeleccionado != null,
                        onClick = { tutorSeleccionado?.let { vm.asignarTutor(usuario.id, it.id) } }
                    ) { Text("Asignar") }
                }
            }
        }
    }
}

@Composable
private fun SeccionRespaldos(estado: CoordinadorUiState, vm: CoordinadorViewModel) {
    var intervaloTexto by remember(estado.respaldosIntervalo) { mutableStateOf(estado.respaldosIntervalo.toString()) }

    Text(
        text = "Respaldos y Recuperación",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    Button(onClick = { vm.crearRespaldoManual() }) { Text("Crear Respaldo") }

    Row(modifier = Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Respaldo automático")
        Switch(
            checked = estado.respaldosActivo,
            onCheckedChange = { vm.guardarConfigRespaldos(it, intervaloTexto.toIntOrNull() ?: estado.respaldosIntervalo) }
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = intervaloTexto,
            onValueChange = { intervaloTexto = it },
            label = { Text("Horas") },
            modifier = Modifier.width(100.dp)
        )
        Spacer(modifier = Modifier.padding(4.dp))
        Button(onClick = {
            vm.guardarConfigRespaldos(estado.respaldosActivo, intervaloTexto.toIntOrNull() ?: estado.respaldosIntervalo)
        }) { Text("Guardar") }
    }

    Text(
        text = "Respaldos disponibles:",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
    if (estado.respaldos.isEmpty()) {
        Text("Sin respaldos todavía", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    estado.respaldos.forEach { nombre ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(nombre, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { vm.restaurarRespaldo(nombre) }) { Text("Restaurar") }
        }
    }
}

@Composable
private fun SeccionAuditoria(estado: CoordinadorUiState) {
    Text(
        text = "Auditoría de Acciones",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp)
    )
    if (estado.auditoria.isEmpty()) {
        Text("Sin registros todavía", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    estado.auditoria.forEach { registro ->
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(registro.accion, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text(
                "${registro.usuario ?: "?"} · ${registro.fecha} · ${registro.ip}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoCrearUsuario(
    onCerrar: () -> Unit,
    onCrear: (tipo: String, credencial: String, nombre: String, contrasena: String) -> Unit
) {
    var tipoSeleccionado by remember { mutableStateOf("alumno") }
    var mostrarSelectorTipo by remember { mutableStateOf(false) }
    var credencial by remember { mutableStateOf("") }
    var nombre by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    val opcionesTipo = listOf("alumno", "tutor", "coordinador")

    AlertDialog(
        onDismissRequest = onCerrar,
        title = { Text("Crear Nuevo Usuario") },
        text = {
            Column {
                Box {
                    OutlinedTextField(
                        value = tipoSeleccionado,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Rol") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { mostrarSelectorTipo = true }
                    )
                    DropdownMenu(expanded = mostrarSelectorTipo, onDismissRequest = { mostrarSelectorTipo = false }) {
                        opcionesTipo.forEach { opcion ->
                            DropdownMenuItem(
                                text = { Text(opcion) },
                                onClick = { tipoSeleccionado = opcion; mostrarSelectorTipo = false }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = credencial,
                    onValueChange = { credencial = it },
                    label = { Text("Credencial") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = contrasena,
                    onValueChange = { contrasena = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = credencial.isNotBlank() && nombre.isNotBlank() && contrasena.isNotBlank(),
                onClick = { onCrear(tipoSeleccionado, credencial, nombre, contrasena) }
            ) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onCerrar) { Text("Cancelar") } }
    )
}
