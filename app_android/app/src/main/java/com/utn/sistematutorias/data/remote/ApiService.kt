package com.utn.sistematutorias.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

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

    @GET("api/coordinador/resumen")
    suspend fun resumenCoordinador(@Header("Authorization") token: String): Response<ResumenCoordinadorResponse>

    @GET("api/coordinador/usuarios")
    suspend fun usuariosCoordinador(@Header("Authorization") token: String): Response<UsuariosResponse>
}
