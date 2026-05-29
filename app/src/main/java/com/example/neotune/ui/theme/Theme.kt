package com.example.neotune.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun NeotuneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeStyle: String = "material",
    amoledAccent: String = "purple",
    content: @Composable () -> Unit
) {
    val amoledPrimary = when (amoledAccent) {
        "green" -> Color(0xFFA5D6A7)
        "blue" -> Color(0xFF90CAF9)
        "coral" -> Color(0xFFF48FB1)
        "orange" -> Color(0xFFFFCC80)
        else -> Purple80
    }

    val amoledColorScheme = darkColorScheme(
        primary = amoledPrimary,
        secondary = amoledPrimary,
        secondaryContainer = amoledPrimary.copy(alpha = 0.2f),
        onSecondaryContainer = amoledPrimary,
        tertiary = Pink80,
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color(0xFF121212),
        onBackground = Color.White,
        onSurface = Color.White,
        onSurfaceVariant = Color.LightGray
    )

    val colorScheme = when {
        themeStyle == "material" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        themeStyle == "amoled" -> amoledColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.navigationBarColor = if (themeStyle == "amoled") {
                android.graphics.Color.BLACK
            } else {
                Color.Transparent.toArgb()
            }
            val wic = WindowCompat.getInsetsController(window, view)
            wic.isAppearanceLightStatusBars = !darkTheme
            wic.isAppearanceLightNavigationBars = (themeStyle != "amoled" && !darkTheme)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}