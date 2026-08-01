package com.utn.sistematutorias.ui.tutor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sistematutorias.data.local.AlmacenSesion
import com.utn.sistematutorias.data.remote.AlumnoAsignado
import com.utn.sistematutorias.data.remote.CrearTutoriaTutorRequest
import com.utn.sistematutorias.data.remote.EditarTutoriaRequest
import com.utn.sistematutorias.data.remote.HorarioRequest
import com.utn.sistematutorias.data.remote.RetrofitClient
import com.utn.sistematutorias.data.remote.Tutoria
import com.utn.sistematutorias.data.wear.SincronizadorReloj
import com.utn.sistematutorias.util.abrirPdfDescargado
import com.utn.sistematutorias.util.notificarPendientesEnTelefono
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val ESTADOS_PENDIENTES = setOf("Solicitada", "Confirmada", "Asignada por tutor")

data class ReporteTutor(val total: Int, val realizadas: Int, val pendientes: Int)

data class TutorUiState(
    val nombre: String = "",
    val tutorias: List<Tutoria> = emptyList(),
    val alumnos: List<AlumnoAsignado> = emptyList(),
    val horario: String = "",
    val cargando: Boolean = true,
    val actualizandoHorario: Boolean = false,
    val descargandoPdf: Boolean = false,
    val relojConectado: Boolean? = null,
    val error: String? = null
) {
    val reporte: ReporteTutor
        get() = ReporteTutor(
            total = tutorias.size,
            realizadas = tutorias.count { it.estado == "Realizada" },
            pendientes = tutorias.count { it.estado in ESTADOS_PENDIENTES }
        )
}

class TutorViewModel(application: Application) : AndroidViewModel(application) {

    private val almacenSesion = AlmacenSesion(application)
    private val _uiState = MutableStateFlow(TutorUiState())
    val uiState: StateFlow<TutorUiState> = _uiState.asStateFlow()

    init {
        cargarTutorias()
        cargarAlumnos()
    }

    fun cargarTutorias() {
        viewModelScope.launch {
            val sesion = almacenSesion.sesion.first() ?: return@launch
            // El nombre viene de la sesión guardada localmente, así que se
            // muestra de inmediato aunque la carga de tutorías falle o tarde.
            _uiState.value = _uiState.value.copy(cargando = true, error = null, nombre = sesion.nombre)
            try {
                val respuesta = RetrofitClient.api.tutoriasTutor("Bearer ${sesion.token}")
                val cuerpo = respuesta.body()
                if (respuesta.isSuccessful && cuerpo != null) {
                    val tutorias = cuerpo.tutorias
                    _uiState.value = _uiState.value.copy(
                        tutorias = tutorias,
                        horario = cuerpo.horario ?: _uiState.value.horario,
                        cargando = false
                    )
                    val pendientes = tutorias.count { it.estado == "Solicitada" }
                    SincronizadorReloj.enviarResumen(getApplication(), "tutor", sesion.nombre, tutorias.size, pendientes)
                    notificarPendientesEnTelefono(getApplication(), pendientes)
                    _uiState.value = _uiState.value.copy(
                        relojConectado = SincronizadorReloj.hayRelojConectado(getApplication())
                    )
                } else {
                    _uiState.value = _uiState.value.copy(cargando = false, error = "No se pudieron cargar tus tutorías")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(cargando = false, error = "Sin conexión con el servidor")
            }
        }
    }

    private fun cargarAlumnos() {
        viewModelScope.launch {
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.alumnosDelTutor("Bearer ${sesion.token}")
                val cuerpo = respuesta.body()
                if (respuesta.isSuccessful && cuerpo != null) {
                    _uiState.value = _uiState.value.copy(alumnos = cuerpo.alumnos)
                }
            } catch (excepcion: Exception) {
                // La lista de alumnos es solo para el selector de "Asignar nueva tutoría";
                // si falla, ese diálogo simplemente aparece vacío, no bloquea la pantalla.
            }
        }
    }

    fun aceptarTutoria(id: Int) = actualizarEstado { token -> RetrofitClient.api.aceptarTutoria(token, id) }

    fun completarTutoria(id: Int) = actualizarEstado { token -> RetrofitClient.api.completarTutoria(token, id) }

    fun editarTutoria(
        id: Int,
        fecha: String,
        tema: String,
        estado: String,
        observaciones: String,
        alTerminar: (exito: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.editarTutoria(
                    "Bearer ${sesion.token}", id,
                    EditarTutoriaRequest(fecha, tema, estado, observaciones)
                )
                if (respuesta.isSuccessful) {
                    alTerminar(true)
                    cargarTutorias()
                } else {
                    alTerminar(false)
                    _uiState.value = _uiState.value.copy(error = "No se pudo actualizar la tutoría (${respuesta.code()})")
                }
            } catch (excepcion: Exception) {
                alTerminar(false)
                _uiState.value = _uiState.value.copy(error = "Sin conexión con el servidor")
            }
        }
    }

    fun crearTutoria(idAlumno: Int, fecha: String, tema: String, alTerminar: (exito: Boolean, mensaje: String) -> Unit) {
        if (tema.isBlank()) {
            alTerminar(false, "El tema no puede estar vacío")
            return
        }
        viewModelScope.launch {
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.crearTutoriaTutor(
                    "Bearer ${sesion.token}",
                    CrearTutoriaTutorRequest(idAlumno, fecha, tema)
                )
                if (respuesta.isSuccessful) {
                    alTerminar(true, "Tutoría creada")
                    cargarTutorias()
                } else {
                    alTerminar(false, "No se pudo crear la tutoría")
                }
            } catch (excepcion: Exception) {
                alTerminar(false, "Sin conexión con el servidor")
            }
        }
    }

    fun actualizarHorario(horario: String) {
        if (horario.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(actualizandoHorario = true)
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.actualizarHorario("Bearer ${sesion.token}", HorarioRequest(horario))
                if (respuesta.isSuccessful) {
                    _uiState.value = _uiState.value.copy(actualizandoHorario = false, horario = horario)
                } else {
                    _uiState.value = _uiState.value.copy(actualizandoHorario = false, error = "No se pudo actualizar el horario")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(actualizandoHorario = false, error = "Sin conexión con el servidor")
            }
        }
    }

    fun descargarReportePdf() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(descargandoPdf = true)
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.reporteTutorPdf("Bearer ${sesion.token}")
                val cuerpo = respuesta.body()
                if (respuesta.isSuccessful && cuerpo != null) {
                    abrirPdfDescargado(getApplication(), cuerpo, "tutorias_tutor.pdf")
                    _uiState.value = _uiState.value.copy(descargandoPdf = false)
                } else {
                    _uiState.value = _uiState.value.copy(descargandoPdf = false, error = "No se pudo generar el reporte PDF")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(descargandoPdf = false, error = "Sin conexión con el servidor")
            }
        }
    }

    private fun actualizarEstado(accion: suspend (String) -> retrofit2.Response<*>) {
        viewModelScope.launch {
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = accion("Bearer ${sesion.token}")
                if (respuesta.isSuccessful) {
                    cargarTutorias()
                } else {
                    _uiState.value = _uiState.value.copy(
                        error = "No se pudo actualizar la tutoría (${respuesta.code()}). Puede que ya no exista, intenta recargar."
                    )
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(error = "Sin conexión con el servidor")
            }
        }
    }

    fun buscarReloj() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                relojConectado = SincronizadorReloj.hayRelojConectado(getApplication())
            )
        }
    }

    fun cerrarSesion(alTerminar: () -> Unit) {
        viewModelScope.launch {
            almacenSesion.cerrarSesion()
            alTerminar()
        }
    }
}
