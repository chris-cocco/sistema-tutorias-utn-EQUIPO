package com.utn.sistematutorias.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Streaming

interface ApiService {

    @POST("api/login")
    suspend fun login(@Body datos: LoginRequest): Response<LoginResponse>

    @GET("api/perfil")
    suspend fun perfil(@Header("Authorization") token: String): Response<PerfilResponse>

    @GET("api/alumno/tutorias")
    suspend fun tutoriasAlumno(@Header("Authorization") token: String): Response<TutoriasResponse>

    @POST("api/alumno/tutorias")
    suspend fun solicitarTutoria(
        @Header("Authorization") token: String,
        @Body datos: SolicitarTutoriaRequest
    ): Response<MensajeResponse>

    @GET("api/tutor/tutorias")
    suspend fun tutoriasTutor(@Header("Authorization") token: String): Response<TutoriasResponse>

    @POST("api/tutor/tutorias/{id}/aceptar")
    suspend fun aceptarTutoria(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MensajeResponse>

    @POST("api/tutor/tutorias/{id}/completar")
    suspend fun completarTutoria(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<MensajeResponse>

    @POST("api/tutor/tutorias/{id}/editar")
    suspend fun editarTutoria(
        @Header("Authorization") token: String,
        @Path("id") id: Int,
        @Body datos: EditarTutoriaRequest
    ): Response<MensajeResponse>

    @GET("api/tutor/alumnos")
    suspend fun alumnosDelTutor(@Header("Authorization") token: String): Response<AlumnosResponse>

    @POST("api/tutor/tutorias")
    suspend fun crearTutoriaTutor(
        @Header("Authorization") token: String,
        @Body datos: CrearTutoriaTutorRequest
    ): Response<MensajeResponse>

    @POST("api/tutor/horario")
    suspend fun actualizarHorario(
        @Header("Authorization") token: String,
        @Body datos: HorarioRequest
    ): Response<MensajeResponse>

    @GET("api/coordinador/resumen")
    suspend fun resumenCoordinador(@Header("Authorization") token: String): Response<ResumenCoordinadorResponse>

    @GET("api/coordinador/usuarios")
    suspend fun usuariosCoordinador(@Header("Authorization") token: String): Response<UsuariosResponse>

    @POST("api/coordinador/usuarios")
    suspend fun crearUsuario(
        @Header("Authorization") token: String,
        @Body datos: CrearUsuarioRequest
    ): Response<MensajeResponse>

    @POST("api/coordinador/usuarios/{idAlumno}/asignar-tutor")
    suspend fun asignarTutor(
        @Header("Authorization") token: String,
        @Path("idAlumno") idAlumno: Int,
        @Body datos: AsignarTutorRequest
    ): Response<MensajeResponse>

    @POST("api/coordinador/usuarios/{id}/estado")
    suspend fun cambiarEstadoUsuario(
        @Header("Authorization") token: String,
        @Path("id") id: Int
    ): Response<EstadoUsuarioResponse>

    @GET("api/coordinador/respaldos")
    suspend fun respaldos(@Header("Authorization") token: String): Response<RespaldosResponse>

    @POST("api/coordinador/respaldos")
    suspend fun crearRespaldoManual(@Header("Authorization") token: String): Response<MensajeResponse>

    @POST("api/coordinador/respaldos/restaurar")
    suspend fun restaurarRespaldo(
        @Header("Authorization") token: String,
        @Body datos: RestaurarRequest
    ): Response<MensajeResponse>

    @POST("api/coordinador/config-respaldos")
    suspend fun configRespaldos(
        @Header("Authorization") token: String,
        @Body datos: ConfigRespaldosRequest
    ): Response<MensajeResponse>

    @GET("api/coordinador/auditoria")
    suspend fun auditoria(@Header("Authorization") token: String): Response<AuditoriaResponse>

    @Streaming
    @GET("reporte-alumno-pdf")
    suspend fun reporteAlumnoPdf(@Header("Authorization") token: String): Response<ResponseBody>

    @Streaming
    @GET("reporte-tutor-pdf")
    suspend fun reporteTutorPdf(@Header("Authorization") token: String): Response<ResponseBody>

    @Streaming
    @GET("reporte-general-pdf")
    suspend fun reporteGeneralPdf(@Header("Authorization") token: String): Response<ResponseBody>
}
