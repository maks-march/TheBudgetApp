package ru.budget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.budget.app.data.FlowGroup
import ru.budget.app.data.TxEntity
import ru.budget.app.domain.BudgetEngine

/**
 * Экран «Доходы» — макет С4: зеркало главного (пончик с тапом, легенда, лента
 * со стрелками вверх) + инфострока активный / пассивный / средний в месяц.
 */
@Composable
fun IncomeScreen(
    state: AppState,
    year: Int,
    month: Int,
    onEdit: (TxEntity) -> Unit
) {
    val engine = state.engine
    val key = BudgetEngine.monthKey(year, month)
    val income = engine.incomeIn(key)
    val totals = engine.totals(key)
    val avg = engine.avgIncome(year, month)

    val active = income.entries.filter { state.group(it.key) == FlowGroup.ACTIVE }.sumOf { it.value }
    val passive = totals.income - active
    val segments = remember(state, key) {
        income.mapNotNull { (catId, v) ->
            state.cat(catId)?.let { cat ->
                val g = FlowGroup.valueOf(cat.group)
                DonutSeg(catId, cat.name, v, donutColor(g, cat.colorIdx))
            }
        }.sortedByDescending { it.value }
    }
    val totalIncome = segments.sumOf { it.value }
    var selected by remember(key) { mutableStateOf<Int?>(null) }

    val feed = remember(state, key) { buildFeed(engine.txsOfMonth(key, BudgetEngine.TYPE_IN)) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {

        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
            ) {
                Column(Modifier.padding(12.dp, 12.dp, 12.dp, 10.dp)) {
                    DonutChart(
                        segments = segments,
                        selectedIdx = selected,
                        empty = totalIncome <= 0.0,
                        onSelect = { selected = if (selected == it) null else it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val sel = selected?.let { segments.getOrNull(it) }
                        if (sel != null) {
                            Text(
                                "${Math.round(sel.value / totalIncome * 100)}%",
                                color = GreenSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            Text(Fmt.money(sel.value), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(sel.label.uppercase(), fontSize = 7.5.sp, color = TextDim,
                                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        } else if (totalIncome > 0.0) {
                            Text(Fmt.money(totalIncome), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("ДОХОД ЗА МЕСЯЦ", fontSize = 7.5.sp, color = TextDim, fontWeight = FontWeight.Bold)
                        } else {
                            Text("0 ₽", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDim)
                            Text("ДОХОДОВ ПОКА НЕТ", fontSize = 7.5.sp, color = TextDim, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (segments.isNotEmpty()) {
                        Column(Modifier.padding(top = 10.dp).alpha(if (selected != null) 0.35f else 1f)) {
                            segments.chunked(2).forEach { rowSegs ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    rowSegs.forEach { seg ->
                                        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            GroupDot(seg.color)
                                            Text(seg.label, Modifier.weight(1f).padding(start = 7.dp),
                                                color = TextMut, fontSize = 10.5.sp,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(Fmt.plain(seg.value), fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    if (rowSegs.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        Text(
                            "Запишите зарплату или аванс — средний доход и диаграмма появятся сами",
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            color = TextDim, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp
                        )
                    }

                    // инфострока
                    Column(Modifier.padding(top = 10.dp)) {
                        Row(Modifier.fillMaxWidth().background(Outline.copy(alpha = 0.5f)).height(1.dp)) {}
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            InfoCell("АКТИВНЫЙ", if (active > 0.0) Fmt.money(active) else "—", Green, totals.income > 0.0, Modifier.weight(1f))
                            InfoCell("ПАССИВНЫЙ", if (passive > 0.0) Fmt.money(passive) else "—", GreyBlue, totals.income > 0.0, Modifier.weight(1f))
                            InfoCell("СРЕДНИЙ/МЕС", if (avg > 0.0) Fmt.money(avg) else "—", TextMain, totals.income > 0.0, Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        feed.forEach { item ->
            when (item) {
                is FeedItem.Header -> item {
                    Row(Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp, bottom = 4.dp)) {
                        Text(item.title, Modifier.weight(1f), fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold, color = TextDim)
                        Text("+${Fmt.plain(item.sum)} ₽", fontSize = 11.sp, color = TextMut, fontWeight = FontWeight.Bold)
                    }
                }
                is FeedItem.Op -> item {
                    val cat = state.cat(item.tx.categoryId)
                    val group = cat?.let { runCatching { FlowGroup.valueOf(it.group) }.getOrNull() }
                    Row(
                        Modifier.fillMaxWidth().clickable { onEdit(item.tx) }.padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (group != null) ArrowBox(group, up = true) else Spacer(Modifier.size(22.dp))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(cat?.name ?: "Операция", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            if (group == FlowGroup.PASSIVE) {
                                Text("пассивный", fontSize = 10.sp, color = TextDim)
                            }
                        }
                        Text("+${Fmt.plain(item.tx.amount)} ₽", fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, color = Green)
                    }
                }
            }
        }

        if (feed.isEmpty()) {
            item {
                Text(
                    "Здесь появятся доходы по дням",
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    textAlign = TextAlign.Center, color = TextDim, fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun InfoCell(label: String, value: String, color: androidx.compose.ui.graphics.Color, visible: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextDim)
        Text(
            value, fontSize = 11.5.sp, fontWeight = FontWeight.Bold,
            color = if (visible) color else TextDim.copy(alpha = 0.5f),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
