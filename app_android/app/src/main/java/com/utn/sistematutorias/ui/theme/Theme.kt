package com.utn.sistematutorias.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// El sitio web no tiene modo oscuro (siempre se ve igual), así que la app
// usa un único esquema de colores que coincide con el verde de la web,
// en vez de cambiar con el tema del sistema o el fondo de pantalla.
private val ColorSchemeUt = lightColorScheme(
    primary = VerdeUt,
    onPrimary = Color.White,
    primaryContainer = VerdeSuave,
    onPrimaryContainer = VerdeUt,
    secondary = VerdeSec,
    onSecondary = Color.White,
    secondaryContainer = VerdeSuave,
    onSecondaryContainer = VerdeSec,
    tertiary = AmbarAviso,
    background = VerdeClaro,
    onBackground = TextoPrincipal,
    surface = Color.White,
    onSurface = TextoPrincipal,
    surfaceVariant = VerdeSuave,
    onSurfaceVariant = TextoSecundario,
    outline = BordeSuave,
    // El sitio web pinta las tarjetas (.card) en blanco puro sobre el fondo
    // verde claro; sin esto, Material3 usa tonos grisáceos por defecto.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color.White,
    error = RojoError,
    onError = Color.White,
    errorContainer = RojoErrorSuave,
    onErrorContainer = RojoError
)

// Las tarjetas y botones del sitio web tienen esquinas bien redondeadas
// (border-radius: 12-18px), así que se ajustan las formas por defecto de
// Material para que las Card/Button de la app se vean igual de suaves.
private val FormasUt = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(18.dp)
)

@Composable
fun SistemaTutoriasTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ColorSchemeUt,
        typography = Typography,
        shapes = FormasUt,
        content = content
    )
}
