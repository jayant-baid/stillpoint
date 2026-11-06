package com.stillpoint.app.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.Typeface
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Black and white throughout. `error` is the one colour in the app, and it only ever means
// that time is running out or has run out.
private val Light = lightColorScheme(
    primary = Color(0xFF0A0A0A),
    onPrimary = Color(0xFFFFFFFF),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF0A0A0A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFE6E6E6),
    surfaceContainerHigh = Color(0xFFFFFFFF),
    surfaceContainerHighest = Color(0xFFE6E6E6),
    onSurfaceVariant = Color(0xFF6A6A6A),
    outlineVariant = Color(0xFFE3E3E3),
    outline = Color(0xFF0A0A0A),
    error = Color(0xFFE02D0A),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFF5F5F5),
    onPrimary = Color(0xFF0A0A0A),
    background = Color(0xFF0A0A0A),
    onBackground = Color(0xFFF5F5F5),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFF5F5F5),
    surfaceVariant = Color(0xFF2A2A2A),
    surfaceContainerHigh = Color(0xFF161616),
    surfaceContainerHighest = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFF9A9A9A),
    outlineVariant = Color(0xFF262626),
    outline = Color(0xFFF5F5F5),
    error = Color(0xFFFF4A26),
)

private fun appTypography(context: Context): Typography {
    fun family(typeface: android.graphics.Typeface) = FontFamily(Typeface(typeface))
    val display = family(Fonts.display(context))
    val title = family(Fonts.title(context))
    val text = family(Fonts.text(context))
    val label = family(Fonts.label(context))
    val base = TextStyle(fontSynthesis = FontSynthesis.None)
    return Typography(
        displayLarge = base.copy(fontFamily = display, fontSize = 88.sp, lineHeight = 88.sp),
        headlineLarge = base.copy(fontFamily = display, fontSize = 56.sp, lineHeight = 60.sp),
        titleMedium = base.copy(fontFamily = title, fontSize = 24.sp, lineHeight = 30.sp),
        bodyLarge = base.copy(fontFamily = text, fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = base.copy(
            fontFamily = text,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontFeatureSettings = "tnum",
        ),
        labelLarge = base.copy(fontFamily = label, fontSize = 14.sp, lineHeight = 20.sp),
        labelSmall = base.copy(
            fontFamily = label,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.2.sp,
        ),
    )
}

@Composable
fun StillpointTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val typography = remember { appTypography(context) }
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = typography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(2.dp),
            small = RoundedCornerShape(2.dp),
            medium = RoundedCornerShape(2.dp),
            large = RoundedCornerShape(2.dp),
            extraLarge = RoundedCornerShape(2.dp),
        ),
    ) {
        // Without a Surface, text falls back to black, which is unreadable in dark mode.
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}
