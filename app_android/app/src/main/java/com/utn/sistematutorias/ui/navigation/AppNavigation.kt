package com.utn.sistematutorias.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.utn.sistematutorias.data.local.AlmacenSesion
import com.utn.sistematutorias.ui.alumno.AlumnoScreen
import com.utn.sistematutorias.ui.coordinador.CoordinadorScreen
import com.utn.sistematutorias.ui.login.LoginScreen
import com.utn.sistematutorias.ui.tutor.TutorScreen
import kotlinx.coroutines.flow.first

private const val RUTA_LOGIN = "login"
private const val RUTA_ALUMNO = "alumno"
private const val RUTA_TUTOR = "tutor"
private const val RUTA_COORDINADOR = "coordinador"

@Composable
fun AppNavigation() {
    val contexto = LocalContext.current
    val almacenSesion = remember { AlmacenSesion(contexto) }
    val navController = rememberNavController()

    var cargandoSesion by remember { mutableStateOf(true) }
    var destinoInicial by remember { mutableStateOf(RUTA_LOGIN) }

    LaunchedEffect(Unit) {
        val sesion = almacenSesion.sesion.first()
        destinoInicial = when (sesion?.rol) {
            "alumno" -> RUTA_ALUMNO
            "tutor" -> RUTA_TUTOR
            "coordinador" -> RUTA_COORDINADOR
            else -> RUTA_LOGIN
        }
        cargandoSesion = false
    }

    if (cargandoSesion) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    NavHost(navController = navController, startDestination = destinoInicial) {
        composable(RUTA_LOGIN) {
            LoginScreen(
                alIniciarSesionExitoso = { rol ->
                    val destino = when (rol) {
                        "alumno" -> RUTA_ALUMNO
                        "tutor" -> RUTA_TUTOR
                        else -> RUTA_COORDINADOR
                    }
                    navController.navigate(destino) {
                        popUpTo(RUTA_LOGIN) { inclusive = true }
                    }
                }
            )
        }
        composable(RUTA_ALUMNO) {
            AlumnoScreen(alCerrarSesion = { navController.navigate(RUTA_LOGIN) { popUpTo(0) } })
        }
        composable(RUTA_TUTOR) {
            TutorScreen(alCerrarSesion = { navController.navigate(RUTA_LOGIN) { popUpTo(0) } })
        }
        composable(RUTA_COORDINADOR) {
            CoordinadorScreen(alCerrarSesion = { navController.navigate(RUTA_LOGIN) { popUpTo(0) } })
        }
    }
}
