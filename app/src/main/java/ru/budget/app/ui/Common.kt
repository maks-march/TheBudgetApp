package ru.budget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

object Fmt {
    private val nf: DecimalFormat = DecimalFormat(
        "#,##0.##",
        DecimalFormatSymbols(Locale.forLanguageTag("ru"))
    )

    /** 1234.5 -> "1 234,5" (без валюты) */
    fun plain(v: Double): String = nf.format(v)

    /** 1234.5 -> "1 234,5 ₽" */
    fun money(v: Double): String = nf.format(v) + " ₽"

    /** 1234.5 -> "+1 234,5 ₽" / -1234.5 -> "−1 234,5 ₽" */
    fun signed(v: Double): String =
        (if (v >= 0) "+" else "−") + nf.format(kotlin.math.abs(v)) + " ₽"
}

fun groupColor(group: FlowGroup): Color = when (group) {
    FlowGroup.ACTIVE -> Green
    FlowGroup.PASSIVE -> GreyBlue
    FlowGroup.FIXED -> Orange
    FlowGroup.VARIABLE -> Yellow
}

/** Цветная точка вместо эмодзи — визуальная метка группы */
@Composable
fun GroupDot(group: FlowGroup, modifier: Modifier = Modifier) {
    Box(modifier.size(10.dp).background(groupColor(group), CircleShape))
}

fun dayLabel(isoDate: String): String {
    val d = LocalDate.parse(isoDate)
    return "${d.dayOfMonth} ${MONTHS_GEN[d.monthValue - 1]}"
}
