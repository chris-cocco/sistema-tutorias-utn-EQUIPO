package com.utn.sistematutorias.ui.coordinador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sistematutorias.data.local.AlmacenSesion
import com.utn.sistematutorias.data.remote.AsignarTutorRequest
import com.utn.sistematutorias.data.remote.AuditoriaItem
import com.utn.sistematutorias.data.remote.ConfigRespaldosRequest
import com.utn.sistematutorias.data.remote.CrearUsuarioRequest
import com.utn.sistematutorias.data.remote.ResumenCoordinadorResponse
import com.utn.sistematutorias.data.remote.RestaurarRequest
import com.utn.sistematutorias.data.remote.RetrofitClient
import com.utn.sistematutorias.data.remote.Usuario
import com.utn.sistematutorias.data.wear.SincronizadorReloj
import com.utn.sistematutorias.util.abrirPdfDescargado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CoordinadorUiState(
    val nombre: String = "",
    val resumen: ResumenCoordinadorResponse? = null,
    val usuarios: List<Usuario> = emptyList(),
    val respaldos: List<String> = emptyList(),
    val respaldosActivo: Boolean = false,
    val respaldosIntervalo: Int = 24,
    val auditoria: List<AuditoriaItem> = emptyList(),
    val cargando: Boolean = true,
    val descargandoPdf: Boolean = false,
    val error: String? = null
)

class CoordinadorViewModel(application: Application) : AndroidViewModel(application) {

    private val almacenSesion = AlmacenSesion(application)
    private val _uiState = MutableStateFlow(CoordinadorUiState())
    val uiState: StateFlow<CoordinadorUiState> = _uiState.asStateFlow()

    init {
        cargarDatos()
    }

    fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val token = "Bearer ${sesion.token}"
                val respuestaResumen = RetrofitClient.api.resumenCoordinador(token)
                val respuestaUsuarios = RetrofitClient.api.usuariosCoordinador(token)
                val respuestaRespaldos = RetrofitClient.api.respaldos(token)
                val respuestaAuditoria = RetrofitClient.api.auditoria(token)

                if (respuestaResumen.isSuccessful && respuestaUsuarios.isSuccessful) {
                    val resumen = respuestaResumen.body()
                    val cuerpoRespaldos = respuestaRespaldos.body()
                    _uiState.value = _uiState.value.copy(
                        nombre = sesion.nombre,
                        resumen = resumen,
                        usuarios = respuestaUsuarios.body()?.usuarios ?: emptyList(),
                        respaldos = cuerpoRespaldos?.respaldos ?: emptyList(),
                        respaldosActivo = cuerpoRespaldos?.activo ?: false,
                        respaldosIntervalo = cuerpoRespaldos?.intervalo_horas ?: 24,
                        auditoria = respuestaAuditoria.body()?.auditoria ?: emptyList(),
                        cargando = false
                    )
                    if (resumen != null) {
                        SincronizadorReloj.enviarResumen(
                            getApplication(), "coordinador", sesion.nombre,
                            resumen.tutorias.total, resumen.tutorias.solicitadas
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(cargando = false, error = "No se pudieron cargar los datos")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(cargando = false, error = "Sin conexión con el servidor")
            }
        }
    }

    fun crearUsuario(
        tipo: String,
        credencial: String,
        nombre: String,
        contrasena: String,
        alTerminar: (exito: Boolean, mensaje: String) -> Unit
    ) {
        if (credencial.isBlank() || nombre.isBlank() || contrasena.isBlank()) {
            alTerminar(false, "Completa todos los campos")
            return
        }
        viewModelScope.launch {
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.crearUsuario(
                    "Bearer ${sesion.token}",
                    CrearUsuarioRequest(tipo, credencial, nombre, contrasena)
                )
                if (respuesta.isSuccessful) {
                    alTerminar(true, "Usuario creado correctamente")
                    cargarDatos()
                } else {
                    alTerminar(false, "No se pudo crear el usuario (¿credencial repetida?)")
                }
            } catch (excepcion: Exception) {
                alTerminar(false, "Sin conexión con el servidor")
            }
        }
    }

    fun asignarTutor(idAlumno: Int, idTutor: Int) {
        viewModelScope.launch {
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.asignarTutor(
                    "Bearer ${sesion.token}", idAlumno, AsignarTutorRequest(idTutor)
                )
                if (respuesta.isSuccessful) {
                    cargarDatos()
                } else {
                    _uiState.value = _uiState.value.copy(error = "No se pudo asignar el tutor")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(error = "Sin conexión con el servidor")
            }
        }
    }

    fun cambiarEstadoUsuario(id: Int) {
        viewModelScope.launch {
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.cambiarEstadoUsuario("Bearer ${sesion.token}", id)
                if (respuesta.isSuccessful) {
                    cargarDatos()
                } else {
                    _uiState.value = _uiState.value.copy(error = "No se pudo actualizar el estado del usuario")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(error = "Sin conexión con el servidor")
            }
        }
    }

    fun crearRespaldoManual() {
        viewModelScope.launch {
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.crearRespaldoManual("Bearer ${sesion.token}")
                if (respuesta.isSuccessful) {
                    cargarDatos()
                } else {
                    _uiState.value = _uiState.value.copy(error = "No se pudo crear el respaldo")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(error = "Sin conexión con el servidor")
            }
        }
    }

    fun restaurarRespaldo(nombre: String) {
        viewModelScope.launch {
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.restaurarRespaldo("Bearer ${sesion.token}", RestaurarRequest(nombre))
                if (respuesta.isSuccessful) {
                    cargarDatos()
                } else {
                    _uiState.value = _uiState.value.copy(error = "No se pudo restaurar el respaldo")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(error = "Sin conexión con el servidor")
            }
        }
    }

    fun guardarConfigRespaldos(activo: Boolean, intervaloHoras: Int) {
        viewModelScope.launch {
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.configRespaldos(
                    "Bearer ${sesion.token}", ConfigRespaldosRequest(activo, intervaloHoras)
                )
                if (respuesta.isSuccessful) {
                    cargarDatos()
                } else {
                    _uiState.value = _uiState.value.copy(error = "No se pudo guardar la configuración")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(error = "Sin conexión con el servidor")
            }
        }
    }

    fun descargarReporteGeneralPdf() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(descargandoPdf = true)
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.reporteGeneralPdf("Bearer ${sesion.token}")
                val cuerpo = respuesta.body()
                if (respuesta.isSuccessful && cuerpo != null) {
                    abrirPdfDescargado(getApplication(), cuerpo, "reporte_general.pdf")
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
