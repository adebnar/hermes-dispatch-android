package co.hermesdispatch.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand palette — a consistent indigo/blue identity (matches the launcher icon),
// so the app looks intentional rather than adapting to the wallpaper.
private val Brand = Color(0xFF3D5AFE)
private val BrandLight = Color(0xFFB4C3FF)

private val DarkColors = darkColorScheme(
    primary = BrandLight,
    onPrimary = Color(0xFF06164B),
    primaryContainer = Color(0xFF26356E),
    onPrimaryContainer = Color(0xFFDCE2FF),
    secondary = Color(0xFF9FB0D6),
    background = Color(0xFF0E1014),
    onBackground = Color(0xFFE3E6EC),
    surface = Color(0xFF14171D),
    onSurface = Color(0xFFE3E6EC),
    surfaceVariant = Color(0xFF272C36),
    onSurfaceVariant = Color(0xFFBFC6D2),
    outline = Color(0xFF3A4150),
)

private val LightColors = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDFE3FF),
    onPrimaryContainer = Color(0xFF00134C),
    secondary = Color(0xFF5A6480),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF1A1C22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C22),
    surfaceVariant = Color(0xFFE9ECF5),
    onSurfaceVariant = Color(0xFF454B59),
    outline = Color(0xFFC2C7D2),
)

@Composable
fun HermesDispatchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
