package ru.budget.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.budget.app.data.Categories
import ru.budget.app.data.Category
import ru.budget.app.data.FlowGroup
import ru.budget.app.domain.Envelope

/**
 * Экран план-факта: конвертный бюджет с переносом остатков.
 * Тап по категории — изменить план месяца.
 */
@Composable
fun BudgetScreen(
    state: AppState,
    year: Int,
    month: Int,
    onSetPlan: (categoryId: String, amount: Double) -> Unit
) {
    val engine = state.engine
    val envs = engine.envelopesOfMonth(year, month)
    val total = engine.totalEnvelope(year, month)
    var planCat by remember { mutableStateOf<Category?>(null) }

    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 4.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SummaryCard(total) }
        item { GroupCard(FlowGroup.FIXED, envs) { planCat = it } }
        item { GroupCard(FlowGroup.VARIABLE, envs) { planCat = it } }
        item {
            Text(
                "Остаток каждого конверта автоматически переносится на следующий месяц. " +
                    "Перерасход уменьшает доступное в следующем месяце. " +
                    "Нажмите на категорию, чтобы изменить план.",
                fontSize = 11.5.sp,
                color = TextDim,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }

    planCat?.let { cat ->
        PlanDialog(
            cat = cat,
            month = month,
            env = envs[cat.id] ?: Envelope(0.0, 0.0, 0.0),
            onDismiss = { planCat = null },
            onSave = { amount ->
                onSetPlan(cat.id, amount)
                planCat = null
            }
        )
    }
}

@Composable
private fun SummaryCard(total: Envelope) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceContainer)) {
        Column(Modifier.padding(14.dp)) {
            Text("ИТОГО ЗА МЕСЯЦ", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = TextMut)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "${Fmt.money(total.spent)}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        "потрачено из ${Fmt.money(total.available)} доступных",
                        fontSize = 11.sp,
                        color = TextDim
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        Fmt.signed(total.left),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (total.left >= 0) Green else Red
                    )
                    Text(
                        if (total.left >= 0) "переходит в следующий месяц" else "перерасход",
                        fontSize = 10.sp,
                        color = TextDim
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupCard(group: FlowGroup, envs: Map<String, Envelope>, onClick: (Category) -> Unit) {
    val cats = Categories.expenses.filter { it.group == group }
    val sum = cats.sumOf { envs[it.id]?.available ?: 0.0 }
    Column {
        Row(
            Modifier.fillMaxWidth().padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                group.title.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMut,
                modifier = Modifier.weight(1f)
            )
            Text(Fmt.money(sum), fontSize = 12.sp, color = TextDim)
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceContainer)) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                cats.forEach { cat ->
                    EnvelopeRow(cat, envs[cat.id] ?: Envelope(0.0, 0.0, 0.0)) { onClick(cat) }
                }
            }
        }
    }
}

@Composable
private fun EnvelopeRow(cat: Category, env: Envelope, onClick: () -> Unit) {
    val frac = if (env.available > 0.0) {
        (env.spent / env.available).toFloat().coerceIn(0f, 1f)
    } else if (env.spent > 0.0) 1f else 0f
    val over = env.left < 0.0

    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GroupDot(cat.group)
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(cat.name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                val sub = buildString {
                    append("план ${Fmt.plain(env.plan)}")
                    if (env.carryIn != 0.0) append(" · перенос ${Fmt.signed(env.carryIn)}")
                    if (env.spent != 0.0) append(" · потрачено ${Fmt.plain(env.spent)}")
                }
                Text(sub, fontSize = 10.sp, color = TextDim, modifier = Modifier.padding(top = 1.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    Fmt.signed(env.left),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        over -> Red
                        env.left > 0.0 -> Green
                        else -> TextMut
                    }
                )
                Text(
                    if (over) "перерасход" else "доступно ${Fmt.plain(env.available)}",
                    fontSize = 9.sp,
                    color = TextDim
                )
            }
        }
        LinearProgressIndicator(
            progress = { frac },
            modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
            color = if (over) Red else groupColor(cat.group),
            trackColor = SurfaceHigh
        )
    }
}

@Composable
private fun PlanDialog(
    cat: Category,
    month: Int,
    env: Envelope,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var text by remember {
        mutableStateOf(if (env.plan == 0.0) "" else Fmt.plain(env.plan))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${cat.name} — ${MONTHS_FULL[month].lowercase()}") },
        text = {
            Column {
                Text(
                    "Потрачено ${Fmt.money(env.spent)} · перенос ${Fmt.signed(env.carryIn)} · " +
                        "доступно ${Fmt.money(env.available)}",
                    fontSize = 12.sp,
                    color = TextMut
                )
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    label = { Text("План на месяц, руб.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { text.replace(',', '.').toDoubleOrNull()?.let(onSave) },
                enabled = text.isNotBlank()
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
