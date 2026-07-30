package com.utn.sistematutorias.data.remote

data class LoginRequest(
    val credencial: String,
    val contrasena: String
)

data class LoginResponse(
    val token: String,
    val uid: Int,
    val rol: String,
    val nombre: String
)

data class PerfilResponse(
    val uid: Int,
    val rol: String,
    val nombre: String
)

data class Tutoria(
    val id: Int,
    val fecha: String,
    val tema: String,
    val estado: String,
    val observaciones: String,
    val alumno: String?
)

data class TutoriasResponse(
    val tutorias: List<Tutoria>
)

data class SolicitarTutoriaRequest(
    val fecha: String,
    val tema: String
)

data class MensajeResponse(
    val mensaje: String,
    val tutoria: Tutoria? = null
)

data class ResumenTutorias(
    val total: Int,
    val solicitadas: Int,
    val confirmadas: Int,
    val realizadas: Int,
    val asignadas: Int
)

data class ResumenUsuarios(
    val alumnos: Int,
    val tutores: Int,
    val activos: Int,
    val bloqueados: Int
)

data class ResumenCoordinadorResponse(
    val tutorias: ResumenTutorias,
    val usuarios: ResumenUsuarios
)

data class Usuario(
    val id: Int,
    val tipo: String,
    val credencial: String,
    val nombre: String,
    val bloqueado: Boolean
)

data class UsuariosResponse(
    val usuarios: List<Usuario>
)

data class ErrorResponse(
    val error: String
)
