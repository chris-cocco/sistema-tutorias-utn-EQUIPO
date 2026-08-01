package com.utn.sistematutorias.ui.tutor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sistematutorias.data.local.AlmacenSesion
import com.utn.sistematutorias.data.remote.RetrofitClient
import com.utn.sistematutorias.data.remote.Tutoria
import com.utn.sistematutorias.data.wear.SincronizadorReloj
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class TutorUiState(
    val nombre: String = "",
    val tutorias: List<Tutoria> = emptyList(),
    val cargando: Boolean = true,
    val error: String? = null
)

class TutorViewModel(application: Application) : AndroidViewModel(application) {

    private val almacenSesion = AlmacenSesion(application)
    private val _uiState = MutableStateFlow(TutorUiState())
    val uiState: StateFlow<TutorUiState> = _uiState.asStateFlow()

    init {
        cargarTutorias()
    }

    fun cargarTutorias() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true, error = null)
            val sesion = almacenSesion.sesion.first() ?: return@launch
            try {
                val respuesta = RetrofitClient.api.tutoriasTutor("Bearer ${sesion.token}")
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    val tutorias = respuesta.body()!!.tutorias
                    _uiState.value = _uiState.value.copy(
                        nombre = sesion.nombre,
                        tutorias = tutorias,
                        cargando = false
                    )
                    val pendientes = tutorias.count { it.estado == "Solicitada" }
                    SincronizadorReloj.enviarResumen(getApplication(), "tutor", sesion.nombre, tutorias.size, pendientes)
                } else {
                    _uiState.value = _uiState.value.copy(cargando = false, error = "No se pudieron cargar tus tutorías")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(cargando = false, error = "Sin conexión con el servidor")
            }
        }
    }

    fun aceptarTutoria(id: Int) = actualizarEstado { token -> RetrofitClient.api.aceptarTutoria(token, id) }

    fun completarTutoria(id: Int) = actualizarEstado { token -> RetrofitClient.api.completarTutoria(token, id) }

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

    fun cerrarSesion(alTerminar: () -> Unit) {
        viewModelScope.launch {
            almacenSesion.cerrarSesion()
            alTerminar()
        }
    }
}
