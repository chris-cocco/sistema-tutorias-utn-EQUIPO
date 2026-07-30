package com.utn.sistematutorias.ui.coordinador

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.utn.sistematutorias.data.local.AlmacenSesion
import com.utn.sistematutorias.data.remote.ResumenCoordinadorResponse
import com.utn.sistematutorias.data.remote.RetrofitClient
import com.utn.sistematutorias.data.remote.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class CoordinadorUiState(
    val nombre: String = "",
    val resumen: ResumenCoordinadorResponse? = null,
    val usuarios: List<Usuario> = emptyList(),
    val cargando: Boolean = true,
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

                if (respuestaResumen.isSuccessful && respuestaUsuarios.isSuccessful) {
                    _uiState.value = _uiState.value.copy(
                        nombre = sesion.nombre,
                        resumen = respuestaResumen.body(),
                        usuarios = respuestaUsuarios.body()?.usuarios ?: emptyList(),
                        cargando = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(cargando = false, error = "No se pudieron cargar los datos")
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(cargando = false, error = "Sin conexión con el servidor")
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
