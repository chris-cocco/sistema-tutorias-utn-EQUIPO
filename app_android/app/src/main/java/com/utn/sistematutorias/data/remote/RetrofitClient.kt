package com.utn.sistematutorias.data.remote

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    // Mismo backend que usa el sitio web (sistema_tutorias en Flask), desplegado en Render.
    // El plan gratis de Render "duerme" tras inactividad, por eso el timeout es largo:
    // la primera petición después de un rato puede tardar hasta ~50s en responder.
    private const val URL_BASE = "https://sistema-tutorias-utn-equipo.onrender.com/"

    val api: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val cliente = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(URL_BASE)
            .client(cliente)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
