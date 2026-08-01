package com.utn.sistematutorias.ui.alumno

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sistematutorias.data.local.AlmacenSesion
import com.utn.sistematutorias.data.remote.NombreRequest
import com.utn.sistematutorias.data.remote.RetrofitClient
import com.utn.sistematutorias.data.remote.SolicitarTutoriaRequest
import com.utn.sistematutorias.data.remote.Tutoria
import com.utn.sistematutorias.data.wear.SincronizadorReloj
import com.utn.sistematutorias.util.abrirPdfDescargado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val ESTADOS_PENDIENTES = setOf("Solicitada", "Confirmada", "Asignada por tutor")

data class ReporteIndicadores(val total: Int, val realizadas: Int, val pendientes: Int)

data class AlumnoUiState(
    val nombre: String = "",
    val tutorias: List<Tutoria> = emptyList(),
    val cargando: Boolean = true,
    val enviandoSolicitud: Boolean = false,
    val descargandoPdf: Boolean = false,
    val actualizandoNombre: Boolean = false,
    val error: String? = null,
    val mensajeNombre: String? = null
) {
    val reporte: ReporteIndicadores
        get() = ReporteIndicadores(
            total = tutorias.size,
            realizadas = tutorias.count { it.estado == "Realizada" },
            pendientes = tutorias.count { it.estado in ESTADOS_PENDIENTES }
        )
}

class AlumnoViewModel(application: Application) : AndroidViewModel(application) {

    private val almacenSesion = AlmacenSesion(application)
    private val _uiState = MutableStateFlow(AlumnoUiState())
    val uiState: StateFlow<AlumnoUiState> = _uiState.asStateFlow()

    init {
        cargarTutorias()
    }

    fun cargarTutorias() {
        viewModelScope.launch {
            val sesion = almacenSesion.sesion.first() ?: return@launch
            // El nombre viene de la sesión guardada localmente, así que se
            // muestra de inmediato aunque la carga de tutorías falle o tarde.
            _uiState.value = _uiState.value.copy(cargando = true, error = null, nombre = sesion.nombre)
            try {
                val respuesta = RetrofitClient.api.tutoriasAlumno("Bearer ${sesion.token}")
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    val tutorias = respuesta.body()!!.tutorias
                    _uiState.value = _uiState.value.copy(tutorias = tutorias, cargando = false)
                    val pendientes = tutorias.count { it.estado in ESTADOS_PENDIENTES }
                    SincronizadorReloj.enviarResumen(getApplication(), "alumno", sesion.nombre, tutorias.size, pendientes)
                } else {
                    _uiState.value = _uiState.value.copy(cargando = false, error = "No se pudieron cargar tus tutorías")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(cargando = false, error = "Sin conexión con el servidor")
            }
        }
    }

    fun solicitarTutoria(fecha: String, tema: String, alTerminar: (exito: Boolean, mensaje: String) -> Unit) {
        if (tema.isBlank()) {
            alTerminar(false, "El tema no puede estar vacío")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(enviandoSolicitud = true)
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.solicitarTutoria(
                    "Bearer ${sesion.token}",
                    SolicitarTutoriaRequest(fecha, tema)
                )
                _uiState.value = _uiState.value.copy(enviandoSolicitud = false)
                if (respuesta.isSuccessful) {
                    alTerminar(true, "Solicitud enviada al tutor")
                    cargarTutorias()
                } else {
                    alTerminar(false, "No se pudo enviar la solicitud")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(enviandoSolicitud = false)
                alTerminar(false, "Sin conexión con el servidor")
            }
        }
    }

    fun actualizarNombre(nombre: String) {
        if (nombre.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actualizandoNombre = true, mensajeNombre = null)
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.actualizarNombreAlumno("Bearer ${sesion.token}", NombreRequest(nombre))
                if (respuesta.isSuccessful) {
                    almacenSesion.guardarSesion(sesion.token, sesion.rol, nombre)
                    _uiState.value = _uiState.value.copy(actualizandoNombre = false, nombre = nombre, mensajeNombre = "Nombre actualizado")
                } else {
                    _uiState.value = _uiState.value.copy(actualizandoNombre = false, mensajeNombre = "No se pudo actualizar el nombre")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(actualizandoNombre = false, mensajeNombre = "Sin conexión con el servidor")
            }
        }
    }

    fun descargarReportePdf() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(descargandoPdf = true)
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.reporteAlumnoPdf("Bearer ${sesion.token}")
                val cuerpo = respuesta.body()
                if (respuesta.isSuccessful && cuerpo != null) {
                    abrirPdfDescargado(getApplication(), cuerpo, "mis_tutorias.pdf")
                    _uiState.value = _uiState.value.copy(descargandoPdf = false)
                } else {
                    _uiState.value = _uiState.value.copy(descargandoPdf = false, error = "No se pudo generar el reporte PDF")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(descargandoPdf = false, error = "Sin conexión con el servidor")
            }
        }
    }

    fun cerrarSesion(alTerminar: () -> Unit) {
        viewModelScope.launch {
            almacenSesion.cerrarSesion()
            alTerminar()
        }
    }
}
