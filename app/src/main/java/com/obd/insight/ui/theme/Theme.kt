package com.obd.insight.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
private val DarkColorScheme = darkColorScheme(primary = Blue80, secondary = BlueGrey80, tertiary = Teal80)
private val LightColorScheme = lightColorScheme(primary = Blue40, secondary = BlueGrey40, tertiary = Teal40)
@Composable
fun ObdInsightTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}