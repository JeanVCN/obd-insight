package com.obd.insight.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    onPrimary = Ink,
    secondary = BlueGrey80,
    tertiary = Teal80,
    background = Night,
    surface = Panel,
    surfaceVariant = Color(0xFF1C2940),
    onSurface = Color(0xFFE8EEF8),
    onSurfaceVariant = Color(0xFFAAB8CB)
)
private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = Teal40,
    background = Color(0xFFF7F9FC),
    surface = Color.White,
    surfaceVariant = SoftPanel,
    onSurface = Ink,
    onSurfaceVariant = Color(0xFF5B6B80)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

@Composable
fun ObdInsightTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, shapes = AppShapes, content = content)
}
