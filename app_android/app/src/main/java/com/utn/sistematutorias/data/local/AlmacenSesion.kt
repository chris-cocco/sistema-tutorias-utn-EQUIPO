package com.utn.sistematutorias.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStoreSesion by preferencesDataStore(name = "sesion")

data class DatosSesion(
    val token: String,
    val rol: String,
    val nombre: String
)

/**
 * Guarda el token JWT y los datos del usuario en disco (DataStore) para que la
 * sesión persista aunque se cierre la app, tal como pide el punto 1 de la lista
 * de cotejo (persistencia de sesión).
 */
class AlmacenSesion(private val context: Context) {

    private companion object {
        val CLAVE_TOKEN = stringPreferencesKey("token")
        val CLAVE_ROL = stringPreferencesKey("rol")
        val CLAVE_NOMBRE = stringPreferencesKey("nombre")
    }

    val sesion: Flow<DatosSesion?> = context.dataStoreSesion.data.map { preferencias ->
        val token = preferencias[CLAVE_TOKEN]
        if (token.isNullOrEmpty()) {
            null
        } else {
            DatosSesion(
                token = token,
                rol = preferencias[CLAVE_ROL] ?: "",
                nombre = preferencias[CLAVE_NOMBRE] ?: ""
            )
        }
    }

    suspend fun guardarSesion(token: String, rol: String, nombre: String) {
        context.dataStoreSesion.edit { preferencias ->
            preferencias[CLAVE_TOKEN] = token
            preferencias[CLAVE_ROL] = rol
            preferencias[CLAVE_NOMBRE] = nombre
        }
    }

    suspend fun cerrarSesion() {
        context.dataStoreSesion.edit { it.clear() }
    }
}
