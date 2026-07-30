package com.utn.sistematutorias.ui.alumno

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sistematutorias.data.local.AlmacenSesion
import com.utn.sistematutorias.data.remote.RetrofitClient
import com.utn.sistematutorias.data.remote.SolicitarTutoriaRequest
import com.utn.sistematutorias.data.remote.Tutoria
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class AlumnoUiState(
    val nombre: String = "",
    val tutorias: List<Tutoria> = emptyList(),
    val cargando: Boolean = true,
    val enviandoSolicitud: Boolean = false,
    val error: String? = null
)

class AlumnoViewModel(application: Application) : AndroidViewModel(application) {

    private val almacenSesion = AlmacenSesion(application)
    private val _uiState = MutableStateFlow(AlumnoUiState())
    val uiState: StateFlow<AlumnoUiState> = _uiState.asStateFlow()

    init {
        cargarTutorias()
    }

    fun cargarTutorias() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.tutoriasAlumno("Bearer ${sesion.token}")
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    _uiState.value = _uiState.value.copy(
                        nombre = sesion.nombre,
                        tutorias = respuesta.body()!!.tutorias,
                        cargando = false
                    )
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

    fun cerrarSesion(alTerminar: () -> Unit) {
        viewModelScope.launch {
            almacenSesion.cerrarSesion()
            alTerminar()
        }
    }
}
