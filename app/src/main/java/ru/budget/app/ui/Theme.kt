package ru.budget.app.ui

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
val GreyBlue = Color(0xFF9AA4B5)
val Red = Color(0xFFFF8A8A)

val Bg = Color(0xFF0B0D12)
val Surface = Color(0xFF12151C)
val SurfaceContainer = Color(0xFF161A22)
val SurfaceHigh = Color(0xFF20242F)
val NavBg = Color(0xFF0C0F15)
val Outline = Color(0xFF232836)

val TextMain = Color(0xFFE9ECF3)
val TextMut = Color(0xFF98A1B3)
val TextDim = Color(0xFF666F81)

/** Палитра диаграммы расходов (тёплые + контрастные, читаются на тёмном). */
val ExpensePalette = listOf(
    Color(0xFFFF8F4D), Color(0xFFFFB376), Color(0xFFFDE68A), Color(0xFFA7F3D0),
    Color(0xFFFCD34D), Color(0xFFD8B4FE), Color(0xFF94A3B8), Color(0xFF7DD3FC),
    Color(0xFFF9A8D4), Color(0xFF86EFAC), Color(0xFFFDBA74), Color(0xFFA5B4FC),
    Color(0xFF5EEAD4), Color(0xFFFCA5A5), Color(0xFFE2E8F0), Color(0xFFC4B5FD),
    Color(0xFFFFE082), Color(0xFF93C5FD)
)

/** Активный доход — оттенки зелёного, пассивный — серые. */
val IncomeGreens = listOf(
    Color(0xFF4ADE80), Color(0xFF86EFAC), Color(0xFFBBF7D0),
    Color(0xFF34D399), Color(0xFFA7F3D0), Color(0xFF6EE7B7)
)
val IncomeGrays = listOf(
    Color(0xFF9AA4B5), Color(0xFF94A3B8), Color(0xFFB6C0CF),
    Color(0xFF6E7B8F), Color(0xFFCBD5E1), Color(0xFF8B95A6)
)

private val Scheme = darkColorScheme(
    primary = Green,
    onPrimary = GreenOn,
    outline = Outline,
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
    outlineVariant = Outline,
    error = Red
)

@Composable
fun BudgetTheme(content: @Composable () -> Unit) {
    // приложение осознанно тёмное
    MaterialTheme(colorScheme = Scheme, content = content)
}
