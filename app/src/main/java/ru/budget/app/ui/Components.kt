package ru.budget.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.budget.app.data.CategoryEntity
import java.time.LocalDate
import kotlin.math.atan2
import kotlin.math.sqrt

/** Сегмент диаграммы. */
data class DonutSeg(
    val id: String,
    val label: String,
    val value: Double,
    val color: Color
)

/**
 * Круговая диаграмма расходов/доходов (макеты С1/С4): тап по сектору выбирает его,
 * повторный тап — сбрасывает. [center] — содержимое дырки (итог или деталь).
 */
@Composable
fun DonutChart(
    segments: List<DonutSeg>,
    selectedIdx: Int?,
    empty: Boolean,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    center: @Composable () -> Unit = {}
) {
    val total = segments.sumOf { it.value }
    Box(
        modifier
            .aspectRatio(1f)
            .pointerInput(segments, empty) {
                if (empty || segments.isEmpty()) return@pointerInput
                detectTapGestures { pos: Offset ->
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val dx = pos.x - c.x
                    val dy = pos.y - c.y
                    val r = sqrt(dx * dx + dy * dy)
                    val outer = size.minDimension / 2f
                    val inner = outer * 0.62f
                    if (r < inner || r > outer) { onSelect(null); return@detectTapGestures }
                    var ang = Math.toDegrees(atan2(dy, dx).toDouble()) + 90.0
                    if (ang < 0) ang += 360.0
                    var acc = 0.0
                    segments.forEachIndexed { i, seg ->
                        val sweep = seg.value / total * 360.0
                        if (ang >= acc && ang < acc + sweep) { onSelect(i); return@detectTapGestures }
                        acc += sweep
                    }
                    onSelect(null)
                }
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val outer = size.minDimension / 2f
            val inner = outer * 0.62f
            val thickness = outer - inner
            val topLeft = Offset(center.x - outer + thickness / 2, center.y - outer + thickness / 2)
            val arcSize = Size(outer * 2 - thickness, outer * 2 - thickness)
            if (empty || total <= 0.0) {
                drawCircle(
                    color = Color(0xFF2C3345),
                    radius = (inner + thickness / 2),
                    style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 12f)))
                )
            } else {
                var start = -90f
                segments.forEachIndexed { i, seg ->
                    val sweep = (seg.value / total * 360f).coerceAtLeast(0.5f)
                    drawArc(
                        color = if (selectedIdx == null || selectedIdx == i) seg.color else seg.color.copy(alpha = 0.35f),
                        startAngle = start,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = thickness, cap = androidx.compose.ui.graphics.StrokeCap.Butt)
                    )
                    start += sweep
                }
            }
        }
        // центр поверх
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                center()
            }
        }
    }
}

/**
 * Собственная числовая панель (С3): системная клавиатура не выезжает.
 */
@Composable
fun Numpad(onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", ",")
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        keys.chunked(3).forEach { rowKeys ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowKeys.forEach { k ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(38.dp)
                            .background(SurfaceHigh, RoundedCornerShape(12.dp))
                            .clickable { onKey(k) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            k,
                            color = if (k == "⌫" || k == ",") TextDim else TextMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Календарь выбора даты (С3, состояние 4): точки на днях с операциями,
 * подсветка выбранного, «Сегодня»/«Готово».
 */
@Composable
fun CalendarCard(
    selectedISO: String,
    daysWithOps: Set<String>,
    onPick: (String) -> Unit,
    onDone: () -> Unit
) {
    val initial = remember { LocalDate.parse(selectedISO) }
    var page by remember { mutableStateOf(initial.withDayOfMonth(1)) }
    val today = remember { LocalDate.now() }
    val sel = remember(selectedISO) { LocalDate.parse(selectedISO) }

    Column(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1F2B), RoundedCornerShape(18.dp))
            .padding(12.dp, 12.dp, 12.dp, 12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp).background(SurfaceHigh, RoundedCornerShape(9.dp)).clickable {
                    page = page.minusMonths(1)
                },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.KeyboardArrowLeft, null, tint = TextMut, modifier = Modifier.size(16.dp)) }
            Text(
                "${MONTHS_FULL[page.monthValue - 1]} ${page.year}",
                Modifier.weight(1f),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Box(
                Modifier.size(28.dp).background(SurfaceHigh, RoundedCornerShape(9.dp)).clickable {
                    page = page.plusMonths(1)
                },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.KeyboardArrowRight, null, tint = TextMut, modifier = Modifier.size(16.dp)) }
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp)) {
            listOf("ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС").forEach {
                Text(
                    it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = TextDim
                )
            }
        }

        val leading = (page.dayOfWeek.value + 6) % 7
        val daysInMonth = page.lengthOfMonth()
        val cells = leading + daysInMonth
        val rows = (cells + 6) / 7
        repeat(rows) { r ->
            Row(Modifier.fillMaxWidth()) {
                repeat(7) { c ->
                    val idx = r * 7 + c
                    val day = idx - leading + 1
                    if (day in 1..daysInMonth) {
                        val date = page.withDayOfMonth(day)
                        val iso = date.toString()
                        val isSel = date == sel
                        val hasOps = iso in daysWithOps
                        Box(Modifier.weight(1f).padding(1.5.dp).aspectRatio(1f)
                            .background(
                                if (isSel) Green else Color.Transparent,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onPick(iso) },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    day.toString(),
                                    color = when {
                                        isSel -> GreenOn
                                        date == today -> Green
                                        else -> TextMut
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel || date == today) FontWeight.Bold else FontWeight.Normal
                                )
                                if (hasOps) Box(
                                    Modifier
                                        .padding(top = 1.dp)
                                        .size(3.dp)
                                        .background(if (isSel) GreenOn else Green, CircleShape)
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.weight(1f).height(36.dp).background(SurfaceHigh, RoundedCornerShape(11.dp))
                    .clickable {
                        val t = LocalDate.now()
                        page = t.withDayOfMonth(1)
                        onPick(t.toString())
                    },
                contentAlignment = Alignment.Center
            ) { Text("Сегодня", color = TextMut, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            Box(
                Modifier.weight(1f).height(36.dp).background(Green, RoundedCornerShape(11.dp))
                    .clickable { onDone() },
                contentAlignment = Alignment.Center
            ) { Text("Готово", color = GreenOn, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Переключатель месяцев под шапкой (все экраны). */
@Composable
fun MonthSwitcher(year: Int, month: Int, onShift: (Int) -> Unit) {
    val today = LocalDate.now()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onShift(-1) }) {
            Icon(Icons.Filled.KeyboardArrowLeft, "Предыдущий месяц", tint = TextMut)
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${MONTHS_FULL[month]} $year",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (year == today.year && month == today.monthValue - 1) {
                val daysLeft = today.lengthOfMonth() - today.dayOfMonth + 1
                Text(
                    if (daysLeft == 1) "последний день месяца" else "$daysLeft дней до конца месяца",
                    fontSize = 9.5.sp, color = TextDim, fontWeight = FontWeight.Bold
                )
            } else {
                Spacer(Modifier.height(2.dp))
            }
        }
        IconButton(onClick = { onShift(1) }) {
            Icon(Icons.Filled.KeyboardArrowRight, "Следующий месяц", tint = TextMut)
        }
    }
}
