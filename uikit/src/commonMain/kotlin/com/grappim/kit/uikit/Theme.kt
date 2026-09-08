package com.grappim.kit.uikit

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import com.grappim.kit.uikit.widgets.topbar.LocalTopBarConfig
import com.grappim.kit.uikit.widgets.topbar.TopBarController

/**
 * Brand colors and typography stay with the consuming app (a shared library has nothing brand
 * -specific to ship); this composable owns only the structure every app needs around them: the
 * light/dark switch, [SafeUriHandler] hardening, and the `Surface` that paints
 * `colorScheme.surface` and provides `LocalContentColor` for a screen with no `Scaffold` of its
 * own (e.g. a login screen) — without it, the visible background is the *window's*, and
 * Compose's default wins for any text that doesn't name a colour, which in dark mode is black
 * on black.
 */
@Composable
fun KitTheme(
    lightColorScheme: ColorScheme,
    darkColorScheme: ColorScheme,
    typography: Typography,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme else lightColorScheme,
        typography = typography
    ) {
        CompositionLocalProvider(LocalUriHandler provides SafeUriHandler(LocalUriHandler.current)) {
            Surface(modifier = Modifier.fillMaxSize(), content = content)
        }
    }
}

/**
 * The theme every `@Preview` goes through. It adds only the composition local the shell would
 * otherwise provide that this module itself owns: [LocalTopBarConfig] — any screen that declares
 * its own top bar reads it and would crash without it. An app with its own additional locals
 * (an offline banner, a snackbar host, ...) wraps this in its own preview theme rather than this
 * module growing app-specific wiring.
 */
@Composable
fun KitPreviewTheme(
    lightColorScheme: ColorScheme,
    darkColorScheme: ColorScheme,
    typography: Typography,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    KitTheme(lightColorScheme, darkColorScheme, typography, darkTheme) {
        CompositionLocalProvider(LocalTopBarConfig provides remember { TopBarController() }) {
            content()
        }
    }
}
