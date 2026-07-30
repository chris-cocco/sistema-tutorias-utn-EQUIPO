package com.utn.sistematutorias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.utn.sistematutorias.ui.navigation.AppNavigation
import com.utn.sistematutorias.ui.theme.SistemaTutoriasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SistemaTutoriasTheme {
                AppNavigation()
            }
        }
    }
}
