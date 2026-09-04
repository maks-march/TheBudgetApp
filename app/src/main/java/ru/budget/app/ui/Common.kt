package ru.budget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ru.budget.app.data.FlowGroup
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Locale

val MONTHS_FULL = arrayOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

val MONTHS_GEN = arrayOf(
    "января", "февраля", "марта", "апреля", "мая", "июня",
    "июля", "августа", "сентября", "октября", "ноября", "декабря"
)

val MONTHS_SHORT = arrayOf(
    "янв", "фев", "мар", "апр", "май", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек"
)

object Fmt {
    private val nf: DecimalFormat = DecimalFormat(
        "#,##0.##",
        DecimalFormatSymbols(Locale.forLanguageTag("ru"))
    )

    fun plain(v: Double): String = nf.format(v)
    fun money(v: Double): String = nf.format(v) + " ₽"
    fun signed(v: Double): String =
        (if (v >= 0) "+" else "−") + nf.format(kotlin.math.abs(v)) + " ₽"
}

fun groupColor(group: FlowGroup): Color = when (group) {
    FlowGroup.ACTIVE -> Green
    FlowGroup.PASSIVE -> GreyBlue
    FlowGroup.FIXED -> Orange
    FlowGroup.VARIABLE -> Yellow
}

/** Цвет сегмента диаграммы: у расходов — тёплая палитра, у доходов зелёные/серые. */
fun donutColor(group: FlowGroup, colorIdx: Int): Color = when (group) {
    FlowGroup.ACTIVE -> IncomeGreens[colorIdx.mod(IncomeGreens.size)]
    FlowGroup.PASSIVE -> IncomeGrays[colorIdx.mod(IncomeGrays.size)]
    else -> ExpensePalette[colorIdx.mod(ExpensePalette.size)]
}

fun dayLabel(isoDate: String): String {
    val d = LocalDate.parse(isoDate)
    return "${d.dayOfMonth} ${MONTHS_SHORT[d.monthValue - 1]}"
}

/** Стрелка в квадратике цвета группы: вниз — расход, вверх — доход. */
@Composable
fun ArrowBox(group: FlowGroup, up: Boolean, modifier: Modifier = Modifier) {
    val color = groupColor(group)
    val icon: ImageVector = if (up) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward
    Box(
        modifier
            .size(22.dp)
            .background(color.copy(alpha = 0.16f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
    }
}

/** Цветная точка группы (легенда). */
@Composable
fun GroupDot(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(8.dp)
            .background(color, RoundedCornerShape(3.dp))
    )
}

@Composable
fun EmptyDot(modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).background(TextDim, CircleShape))
}
