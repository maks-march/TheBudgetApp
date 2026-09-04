package ru.budget.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import ru.budget.app.data.QuickEntity
import ru.budget.app.data.TxEntity
import ru.budget.app.domain.BudgetEngine
import ru.budget.app.domain.BudgetEngine.TYPE_EXP
import java.time.LocalDate

/**
 * Главный экран «Траты» — макет С1 (рев. 4): диаграмма категорий с тапом,
 * легенда без процентов, быстрые траты, лента по дням со стрелками вниз.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExpensesScreen(
    state: AppState,
    year: Int,
    month: Int,
    onEdit: (TxEntity) -> Unit,
    onAddQuick: () -> Unit,
    onRecordQuick: (QuickEntity) -> Unit,
    onDeleteQuick: (QuickEntity) -> Unit
) {
    val engine = state.engine
    val key = BudgetEngine.monthKey(year, month)
    val spent = engine.spentIn(key)

    // сегменты диаграммы: категории с тратами, по убыванию
    val segments = remember(state, key) {
        spent.mapNotNull { (catId, v) ->
            state.cat(catId)?.let { cat ->
                DonutSeg(
                    id = catId, label = cat.name, value = v,
                    color = donutColor(FlowGroup.valueOf(cat.group), cat.colorIdx)
                )
            }
        }.sortedByDescending { it.value }
    }
    val totalSpent = segments.sumOf { it.value }
    var selected by remember(key) { mutableStateOf<Int?>(null) }

    var confirmQuick by remember { mutableStateOf<QuickEntity?>(null) }

    // лента по дням
    val feed = remember(state, key) { buildFeed(engine.txsOfMonth(key, TYPE_EXP)) }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {

        // диаграмма
        item {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceContainer)
            ) {
                Column(Modifier.padding(12.dp, 12.dp, 12.dp, 10.dp)) {
                    DonutChart(
                        segments = segments,
                        selectedIdx = selected,
                        empty = totalSpent <= 0.0,
                        onSelect = { selected = if (selected == it) null else it },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val sel = selected?.let { segments.getOrNull(it) }
                        if (sel != null) {
                            Text(
                                "${Math.round(sel.value / totalSpent * 100)}%",
                                color = GreenSoft, fontSize = 13.sp, fontWeight = FontWeight.Bold
                            )
                            Text(Fmt.money(sel.value), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(
                                sel.label.uppercase(), fontSize = 7.5.sp, color = TextDim,
                                fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                            )
                        } else if (totalSpent > 0.0) {
                            Text(Fmt.money(totalSpent), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text("ЗА МЕСЯЦ", fontSize = 7.5.sp, color = TextDim, fontWeight = FontWeight.Bold)
                        } else {
                            Text("0 ₽", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDim)
                            Text("ТРАТ ПОКА НЕТ", fontSize = 7.5.sp, color = TextDim, fontWeight = FontWeight.Bold)
                        }
                    }

                    // легенда в 2 колонки, без процентов; приглушается при выборе
                    if (segments.isNotEmpty()) {
                        Column(
                            Modifier.padding(top = 10.dp).alpha(if (selected != null) 0.35f else 1f)
                        ) {
                            segments.chunked(2).forEach { rowSegs ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    rowSegs.forEach { seg ->
                                        Row(
                                            Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            GroupDot(seg.color)
                                            Text(
                                                seg.label,
                                                Modifier.weight(1f).padding(start = 7.dp),
                                                color = TextMut, fontSize = 10.5.sp,
                                                maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                Fmt.plain(seg.value),
                                                fontSize = 10.5.sp, fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    if (rowSegs.size == 1) Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    } else {
                        Text(
                            "Диаграмма покажет, на что уходят деньги в этом месяце.\nПервая трата — кнопка «+» внизу справа",
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            color = TextDim, fontSize = 11.sp, textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // быстрые траты
        item {
            Text(
                "БЫСТРАЯ ТРАТА",
                Modifier.padding(start = 2.dp, bottom = 6.dp),
                fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextDim
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                state.quicks.forEach { q ->
                    state.cat(q.categoryId)?.let { cat ->
                        QuickChip(
                            text = "${cat.name} ${Fmt.plain(q.amount)}",
                            onClick = { onRecordQuick(q) },
                            onLongClick = { confirmQuick = q }
                        )
                    }
                }
                QuickChip(text = "＋", plus = true, onClick = onAddQuick, onLongClick = {})
            }
        }

        // лента по дням
        item {
            Spacer(Modifier.height(2.dp))
        }
        feed.forEach { item ->
            when (item) {
                is FeedItem.Header -> item {
                    Row(
                        Modifier.fillMaxWidth().padding(start = 2.dp, top = 6.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(item.title, Modifier.weight(1f), fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold, color = TextDim)
                        Text(Fmt.plain(item.sum) + " ₽", fontSize = 11.sp, color = TextMut, fontWeight = FontWeight.Bold)
                    }
                }
                is FeedItem.Op -> item {
                    val cat = state.cat(item.tx.categoryId)
                    val group = cat?.let { runCatching { FlowGroup.valueOf(it.group) }.getOrNull() }
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { onEdit(item.tx) }
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (group != null) ArrowBox(group, up = false) else Spacer(Modifier.size(22.dp))
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(cat?.name ?: "Операция", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            if (item.tx.note.isNotBlank()) {
                                Text(item.tx.note, fontSize = 10.sp, color = TextDim)
                            }
                        }
                        Text("−${Fmt.plain(item.tx.amount)} ₽", fontSize = 13.sp,
                            fontWeight = FontWeight.Bold, color = Red)
                    }
                }
            }
        }

        if (feed.isNotEmpty()) {
            item {
                Text(
                    "Вся история за месяц ›",
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    textAlign = TextAlign.Center, color = TextDim,
                    fontSize = 11.sp, fontWeight = FontWeight.Bold
                )
            }
        } else {
            item {
                Text(
                    "Здесь появятся траты по дням",
                    Modifier.fillMaxWidth().padding(top = 10.dp),
                    textAlign = TextAlign.Center, color = TextDim, fontSize = 11.sp
                )
            }
        }
    }

    // подтверждение удаления быстрой траты
    confirmQuick?.let { q ->
        val cat = state.cat(q.categoryId)
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { confirmQuick = null },
            title = { Text("Удалить быструю трату?") },
            text = { Text("«${cat?.name ?: ""} ${Fmt.plain(q.amount)} ₽» исчезнет с главного экрана. Операции не удалятся.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    onDeleteQuick(q); confirmQuick = null
                }) { Text("Удалить") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { confirmQuick = null }) { Text("Отмена") }
            }
        )
    }
}

sealed interface FeedItem {
    data class Header(val title: String, val sum: Double) : FeedItem
    data class Op(val tx: TxEntity) : FeedItem
}

/** Группировка операций по дням с заголовком и суммой дня (как «Итого в день» в таблице). */
fun buildFeed(txs: List<TxEntity>): List<FeedItem> {
    val today = LocalDate.now()
    val byDate = txs.groupBy { it.date }.toSortedMap(compareByDescending { it })
    val items = mutableListOf<FeedItem>()
    for ((date, ops) in byDate) {
        val d = LocalDate.parse(date)
        val title = when {
            date == today.toString() -> "СЕГОДНЯ · ${d.dayOfMonth} ${MONTHS_SHORT[d.monthValue - 1].uppercase()}"
            else -> "${d.dayOfMonth} ${MONTHS_SHORT[d.monthValue - 1].uppercase()}"
        }
        items += FeedItem.Header(title, ops.sumOf { it.amount })
        ops.forEach { items += FeedItem.Op(it) }
    }
    return items
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickChip(
    text: String,
    plus: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Text(
        text,
        Modifier
            .background(SurfaceContainer, RoundedCornerShape(13.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = if (plus) TextDim else TextMain
    )
}
