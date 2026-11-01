package dev.playground.musicplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val Amber = Color(0xFFFFA800)
private val Navy = Color(0xFF0A0A2C)
private val Purple = Color(0xFF3A1263)

private val Scheme = darkColorScheme(
    primary = Amber,
    onPrimary = Color.Black,
    background = Navy,
    surface = Navy,
    onSurface = Color.White,
    onBackground = Color.White,
    surfaceVariant = Color(0x33FFFFFF),
    onSurfaceVariant = Color(0xB3FFFFFF),
)

val Gradient = Brush.linearGradient(
    colors = listOf(Navy, Color(0xFF1E0F45), Purple),
    start = androidx.compose.ui.geometry.Offset(0f, 0f),
    end = androidx.compose.ui.geometry.Offset(900f, 2400f),
)

/** Vivid pink-to-blue backdrop for the now-playing screen. */
val NowPlayingGradient = Brush.linearGradient(
    colors = listOf(Color(0xFFC2185B), Color(0xFF6A3A8E), Color(0xFF0D3B8E)),
    start = androidx.compose.ui.geometry.Offset(0f, 0f),
    end = androidx.compose.ui.geometry.Offset(300f, 2400f),
)

/** Always-dark theme with the navy-to-purple gradient behind every screen. */
@Composable
fun MusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme) {
        Box(Modifier.fillMaxSize().background(Gradient)) { content() }
    }
}
