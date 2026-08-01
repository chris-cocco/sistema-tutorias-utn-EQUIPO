package com.utn.sistematutorias

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.utn.sistematutorias.ui.navigation.AppNavigation
import com.utn.sistematutorias.ui.theme.SistemaTutoriasTheme

class MainActivity : ComponentActivity() {

    private val solicitarPermisoNotificaciones =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* sin acción: si se niega, simplemente no se notificará */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pedirPermisoNotificacionesSiHaceFalta()
        enableEdgeToEdge()
        setContent {
            SistemaTutoriasTheme {
                AppNavigation()
            }
        }
    }

    private fun pedirPermisoNotificacionesSiHaceFalta() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val yaConcedido = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!yaConcedido) {
            solicitarPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
