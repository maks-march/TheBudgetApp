package ru.budget.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Green = Color(0xFF4ADE80)
val GreenOn = Color(0xFF06240F)
val GreenSoft = Color(0xFF8FD6AB)
val GreenCard = Color(0xFF14301F)
val Orange = Color(0xFFFFB376)
val Yellow = Color(0xFFFFE082)
val Purple = Color(0xFFD8B4FE)
val GreyBlue = Color(0xFFC9CEDB)
val Red = Color(0xFFFF8A8A)

val Bg = Color(0xFF0B0D12)
val Surface = Color(0xFF12151C)
val SurfaceContainer = Color(0xFF191D26)
val SurfaceHigh = Color(0xFF222734)
val NavBg = Color(0xFF0D1017)
val Outline = Color(0xFF2B3140)

val TextMain = Color(0xFFE9ECF3)
val TextMut = Color(0xFF98A1B3)
val TextDim = Color(0xFF6B7385)

private val Scheme = darkColorScheme(
    primary = Green,
    onPrimary = GreenOn,
    secondary = Purple,
    background = Bg,
    onBackground = TextMain,
    surface = Surface,
    onSurface = TextMain,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = TextMut,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceHigh,
    surfaceContainerHighest = SurfaceHigh,
    outline = Outline,
    outlineVariant = Outline,
    error = Red
)

@Composable
fun BudgetTheme(content: @Composable () -> Unit) {
    // приложение осознанно тёмное; isSystemInDarkTheme() не используется
    MaterialTheme(colorScheme = Scheme, content = content)
}
