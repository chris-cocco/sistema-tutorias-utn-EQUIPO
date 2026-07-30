package com.utn.sistematutorias.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.utn.sistematutorias.data.local.AlmacenSesion
import com.utn.sistematutorias.data.remote.ErrorResponse
import com.utn.sistematutorias.data.remote.LoginRequest
import com.utn.sistematutorias.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val credencial: String = "",
    val contrasena: String = "",
    val cargando: Boolean = false,
    val error: String? = null
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val almacenSesion = AlmacenSesion(application)

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun actualizarCredencial(valor: String) {
        _uiState.value = _uiState.value.copy(credencial = valor, error = null)
    }

    fun actualizarContrasena(valor: String) {
        _uiState.value = _uiState.value.copy(contrasena = valor, error = null)
    }

    fun iniciarSesion(alExito: (rol: String) -> Unit) {
        val estadoActual = _uiState.value
        if (estadoActual.credencial.isBlank() || estadoActual.contrasena.isBlank()) {
            _uiState.value = estadoActual.copy(error = "Ingresa tu credencial y contraseña")
            return
        }

        _uiState.value = estadoActual.copy(cargando = true, error = null)

        viewModelScope.launch {
            try {
                val respuesta = RetrofitClient.api.login(
                    LoginRequest(estadoActual.credencial.trim(), estadoActual.contrasena)
                )
                if (respuesta.isSuccessful && respuesta.body() != null) {
                    val cuerpo = respuesta.body()!!
                    almacenSesion.guardarSesion(cuerpo.token, cuerpo.rol, cuerpo.nombre)
                    _uiState.value = LoginUiState()
                    alExito(cuerpo.rol)
                } else {
                    val mensajeError = leerMensajeError(respuesta.errorBody()?.string())
                    _uiState.value = _uiState.value.copy(cargando = false, error = mensajeError)
                }
            } catch (excepcion: Exception) {
                _uiState.value = _uiState.value.copy(
                    cargando = false,
                    error = "No se pudo conectar con el servidor. Revisa tu conexión e inténtalo de nuevo."
                )
            }
        }
    }

    private fun leerMensajeError(cuerpoError: String?): String {
        return try {
            if (cuerpoError.isNullOrBlank()) "Credenciales incorrectas"
            else Gson().fromJson(cuerpoError, ErrorResponse::class.java).error
        } catch (excepcion: Exception) {
            "Credenciales incorrectas"
        }
    }
}
