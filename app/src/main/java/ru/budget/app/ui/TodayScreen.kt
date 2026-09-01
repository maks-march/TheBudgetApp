package ru.budget.app.ui

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.budget.app.data.Categories
import ru.budget.app.data.TxEntity
import ru.budget.app.domain.BudgetEngine
import ru.budget.app.domain.Envelope
import java.time.LocalDate

@Composable
fun TodayScreen(state: AppState, year: Int, month: Int, onEdit: (TxEntity) -> Unit) {
    val engine = state.engine
    val key = BudgetEngine.monthKey(year, month)
    val totals = engine.totals(key)
    val totalEnv = engine.totalEnvelope(year, month)
    val deltaAcc = engine.deltaChain(year, month)[month] ?: 0.0
    val pct = if (totalEnv.available > 0.0) {
        (totalEnv.spent / totalEnv.available).toFloat().coerceIn(0f, 1f)
    } else 0f

    val today = LocalDate.now()
    val perDay: Double? = if (year == today.year && month == today.monthValue - 1 && totalEnv.left > 0.0) {
        val daysLeft = today.lengthOfMonth() - today.dayOfMonth + 1
        totalEnv.left / daysLeft
    } else null

    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { DeltaCard(deltaAcc, totals.net) }
        item { EnvelopeCard(totalEnv, pct, perDay) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Доходы", Fmt.money(totals.income), Green, Modifier.weight(1f))
                StatCard("Расходы", Fmt.money(totals.expense), Orange, Modifier.weight(1f))
            }
        }
        item {
            Text(
                "ПОСЛЕДНИЕ ОПЕРАЦИИ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMut,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp)
            )
        }
        val ops = engine.txsOfMonth(key).take(8)
        if (ops.isEmpty()) {
            item { EmptyCard("Операций пока нет. Нажмите кнопку «+», чтобы добавить первую трату.") }
        } else {
            items(ops, key = { it.id }) { tx -> OpRow(tx, onEdit) }
        }
    }
}

@Composable
private fun DeltaCard(deltaAcc: Double, netMonth: Double) {
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GreenCard)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "ДЕЛЬТА — СВОБОДНЫЕ ДЕНЬГИ, НАКОПИТЕЛЬНО",
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Bold,
                color = GreenSoft
            )
            Text(
                Fmt.signed(deltaAcc),
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (deltaAcc >= 0) Green else Red
            )
            Text(
                "за месяц ${Fmt.signed(netMonth)} · доход минус расход, с переносом остатка",
                fontSize = 11.sp,
                color = TextMut,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun EnvelopeCard(total: Envelope, pct: Float, perDay: Double?) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceContainer)) {
        Column(Modifier.padding(14.dp)) {
            Text("КОНВЕРТЫ МЕСЯЦА", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMut)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("доступно (план + перенос)", fontSize = 11.sp, color = TextDim)
                    Text(Fmt.money(total.available), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("перенос далее", fontSize = 11.sp, color = TextDim)
                    Text(
                        Fmt.signed(total.left),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (total.left >= 0) Green else Red
                    )
                }
            }
            LinearProgressIndicator(
                progress = { pct },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                color = Green,
                trackColor = SurfaceHigh
            )
            Text(
                "потрачено ${Fmt.money(total.spent)} из ${Fmt.money(total.available)}"
                    + if (total.carryIn != 0.0) " · перенос с прошлого ${Fmt.signed(total.carryIn)}" else "",
                fontSize = 11.sp,
                color = TextDim,
                modifier = Modifier.padding(top = 6.dp)
            )
            if (perDay != null) {
                Text(
                    "примерно ${Fmt.money(perDay)} в день до конца месяца",
                    fontSize = 12.sp,
                    color = Yellow,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = SurfaceContainer)) {
        Column(Modifier.padding(12.dp)) {
            Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextDim)
            Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
fun OpRow(tx: TxEntity, onEdit: (TxEntity) -> Unit) {
    val cat = Categories.byId[tx.categoryId]
    val income = tx.type == BudgetEngine.TYPE_IN
    Row(
        Modifier.fillMaxWidth().clickable { onEdit(tx) }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (cat != null) GroupDot(cat.group) else Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f).padding(start = 10.dp)) {
            Text(cat?.name ?: "Операция", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            val notePart = if (tx.note.isNotBlank()) " · ${tx.note}" else ""
            Text(dayLabel(tx.date) + notePart, fontSize = 10.sp, color = TextDim)
        }
        Text(
            (if (income) "+" else "−") + Fmt.plain(tx.amount) + " ₽",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (income) Green else Red
        )
    }
}

@Composable
fun EmptyCard(text: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceContainer)) {
        Text(
            text,
            fontSize = 12.sp,
            color = TextDim,
            modifier = Modifier.padding(20.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = 17.sp
        )
    }
}
