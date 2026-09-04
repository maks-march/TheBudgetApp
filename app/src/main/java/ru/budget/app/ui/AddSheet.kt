package ru.budget.app.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.budget.app.data.CategoryEntity
import ru.budget.app.data.FlowGroup
import ru.budget.app.data.TxEntity
import ru.budget.app.domain.BudgetEngine
import java.time.LocalDate

private fun poolCats(state: AppState, type: String): List<CategoryEntity> {
    val groups = if (type == BudgetEngine.TYPE_IN) listOf(FlowGroup.ACTIVE, FlowGroup.PASSIVE)
    else listOf(FlowGroup.FIXED, FlowGroup.VARIABLE)
    return state.cats.filter { c -> !c.hidden && groups.any { it.name == c.group } }
}

/**
 * Шторка добавления/редактирования операции — макет С3 (рев. 2):
 * сегмент Расход/Доход, свой numpad, категории текстом (частые сверху, «＋» — своя,
 * зажатие — удалить/скрыть), календарь дат, тумблер быстрой траты. Без заметки.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSheet(
    state: AppState,
    edit: TxEntity?,
    initialType: String,
    quickMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (type: String, categoryId: String, amount: Double, dateISO: String, saveQuick: Boolean) -> Unit,
    onDelete: () -> Unit,
    onAddCategory: (String, FlowGroup, (String) -> Unit) -> Unit,
    onRemoveCategory: (CategoryEntity) -> Unit
) {
    val isEdit = edit != null
    var type by remember { mutableStateOf(edit?.type ?: initialType) }
    var amountText by remember { mutableStateOf(if (edit != null) Fmt.plain(edit.amount) else "") }
    var catId by remember { mutableStateOf<String?>(edit?.categoryId) }
    var dateISO by remember { mutableStateOf(edit?.date ?: LocalDate.now().toString()) }
    var showCal by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var quickOn by remember { mutableStateOf(quickMode) }
    var confirmCat by remember { mutableStateOf<CategoryEntity?>(null) }
    var showAddCat by remember { mutableStateOf(false) }

    val pool = remember(state, type) { poolCats(state, type) }
    val usage = remember(state) { state.usageCounts() }
    val sorted = remember(pool, usage) {
        pool.sortedWith(
            compareByDescending<CategoryEntity> { usage[it.id] ?: 0 }.thenBy { it.sortOrder }
        )
    }
    val visible = if (expanded) sorted else sorted.take(6)
    val hiddenCount = sorted.size - visible.size

    // при переключении типа выбранная категория сбрасывается на первую доступную
    LaunchedEffect(type, pool) {
        if (catId == null || pool.none { it.id == catId }) catId = pool.firstOrNull()?.id
    }

    val amount = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
    val daysWithOps = remember(state) { state.txs.map { it.date }.toSet() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {

            if (showCal) {
                SheetTitle("ВЫБОР ДАТЫ")
                CalendarCard(
                    selectedISO = dateISO,
                    daysWithOps = daysWithOps,
                    onPick = { dateISO = it },
                    onDone = { showCal = false }
                )
                Spacer(Modifier.height(24.dp))
            } else {

                SheetTitle(
                    when {
                        isEdit -> "ИЗМЕНИТЬ ОПЕРАЦИЮ"
                        type == BudgetEngine.TYPE_IN -> "НОВЫЙ ДОХОД"
                        else -> "НОВАЯ ОПЕРАЦИЯ"
                    }
                )

                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = type == BudgetEngine.TYPE_EXP,
                        onClick = { type = BudgetEngine.TYPE_EXP },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Расход") }
                    SegmentedButton(
                        selected = type == BudgetEngine.TYPE_IN,
                        onClick = { type = BudgetEngine.TYPE_IN },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Доход") }
                }

                Text(
                    text = (if (amountText.isEmpty()) "0" else amountText) + " ₽",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Numpad { k ->
                    when (k) {
                        "⌫" -> amountText = amountText.dropLast(1)
                        "," -> if (!amountText.contains(',') && !amountText.contains('.')) amountText += ","
                        else -> if (amountText.length < 9) amountText += k
                    }
                }

                if (type == BudgetEngine.TYPE_IN) {
                    CategorySection(
                        caption = "АКТИВНЫЙ ДОХОД",
                        cats = sorted.filter { it.group == FlowGroup.ACTIVE.name },
                        selectedId = catId,
                        onSelect = { catId = it.id },
                        onLongPress = { confirmCat = it },
                        onAdd = { showAddCat = true }
                    )
                } else {
                    CapLabel("КАТЕГОРИЯ", Modifier.padding(top = 12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(top = 6.dp)
                    ) {
                        visible.forEach { c ->
                            CatChip(
                                cat = c,
                                selected = catId == c.id,
                                onClick = { catId = c.id },
                                onLongClick = { confirmCat = c }
                            )
                        }
                        CatChip(
                            cat = CategoryEntity("＋", "", "", 0),
                            selected = false,
                            plus = true,
                            onClick = { showAddCat = true },
                            onLongClick = {}
                        )
                        if (hiddenCount > 0) {
                            CatChip(
                                cat = CategoryEntity("ещё", "", "", 0),
                                selected = false,
                                more = hiddenCount,
                                onClick = { expanded = true },
                                onLongClick = {}
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { showCal = true },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
                ) {
                    val today = LocalDate.now()
                    Text(
                        when (dateISO) {
                            today.toString() -> "Сегодня"
                            else -> dayLabel(dateISO) + if (dateISO.year != today.year) " ${dateISO.year}" else ""
                        },
                        fontSize = 13.sp
                    )
                }

                if (!isEdit && type == BudgetEngine.TYPE_EXP) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .background(SurfaceHigh, RoundedCornerShape(13.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Быстрая трата", fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
                            Text("сохранить шаблон на главный экран", fontSize = 10.sp, color = TextDim)
                        }
                        Switch(
                            checked = quickOn,
                            onCheckedChange = { quickOn = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Green.copy(alpha = 0.4f),
                                checkedThumbColor = Green
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        val c = catId ?: return@Button
                        onSave(
                            type, c, amount, dateISO,
                            quickOn && !isEdit && type == BudgetEngine.TYPE_EXP
                        )
                    },
                    enabled = amount > 0.0 && catId != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = GreenOn),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(46.dp)
                ) {
                    val label = when {
                        isEdit -> "Сохранить"
                        type == BudgetEngine.TYPE_IN -> "Добавить доход"
                        else -> "Добавить"
                    }
                    val sign = if (type == BudgetEngine.TYPE_IN) "+" else "−"
                    Text(
                        if (amount > 0.0) "$label · $sign${Fmt.plain(amount)} ₽" else label,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isEdit) {
                    TextButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) { Text("Удалить операцию", color = Red) }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }

    confirmCat?.let { c ->
        AlertDialog(
            onDismissRequest = { confirmCat = null },
            title = { Text(if (c.isCustom) "Удалить категорию?" else "Скрыть категорию?") },
            text = {
                Text(
                    "«${c.name}». Операции не удалятся" +
                        if (!c.isCustom) " — категория исчезнет из шторки, конвертов и диаграмм" else ""
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onRemoveCategory(c)
                    if (catId == c.id) catId = null
                    confirmCat = null
                }) { Text(if (c.isCustom) "Удалить" else "Скрыть") }
            },
            dismissButton = { TextButton(onClick = { confirmCat = null }) { Text("Отмена") } }
        )
    }

    if (showAddCat) {
        AddCategoryDialog(
            isIncome = type == BudgetEngine.TYPE_IN,
            onDismiss = { showAddCat = false },
            onConfirm = { name, group ->
                onAddCategory(name, group) { id -> catId = id }
                showAddCat = false
            }
        )
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text,
        Modifier.fillMaxWidth().padding(bottom = 10.dp),
        fontSize = 11.5.sp,
        fontWeight = FontWeight.Bold,
        color = TextMut,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun CapLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier.padding(start = 2.dp), fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextDim)
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun CatChip(
    cat: CategoryEntity,
    selected: Boolean,
    plus: Boolean = false,
    more: Int = 0,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    FilterChip(
        selected = selected && !plus && more == 0,
        onClick = onClick,
        label = {
            Text(
                when {
                    plus -> "＋"
                    more > 0 -> "ещё $more ↓"
                    else -> cat.name
                },
                fontSize = 12.sp,
                fontWeight = if (plus) FontWeight.Black else FontWeight.Normal,
                color = when {
                    plus -> TextDim
                    more > 0 -> TextDim
                    else -> TextMain
                }
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.Transparent,
            borderColor = Outline,
            selectedContainerColor = Green.copy(alpha = 0.16f),
            selectedBorderColor = Green.copy(alpha = 0.5f),
            selectedLabelColor = TextMain
        ),
        modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySection(
    caption: String,
    cats: List<CategoryEntity>,
    selectedId: String?,
    onSelect: (CategoryEntity) -> Unit,
    onLongPress: (CategoryEntity) -> Unit,
    onAdd: () -> Unit
) {
    if (cats.isEmpty()) return
    CapLabel(caption, Modifier.padding(top = 12.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(top = 6.dp)
    ) {
        cats.forEach { c ->
            CatChip(
                cat = c,
                selected = selectedId == c.id,
                onClick = { onSelect(c) },
                onLongClick = { onLongPress(c) }
            )
        }
        CatChip(
            cat = CategoryEntity("＋", "", "", 0),
            selected = false,
            plus = true,
            onClick = onAdd,
            onLongClick = {}
        )
    }
}

/** Диалог создания своей категории: название + группа. */
@Composable
private fun AddCategoryDialog(
    isIncome: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String, FlowGroup) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var first by remember { mutableStateOf(true) }
    val group = when {
        isIncome && first -> FlowGroup.ACTIVE
        isIncome -> FlowGroup.PASSIVE
        first -> FlowGroup.FIXED
        else -> FlowGroup.VARIABLE
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новая категория") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = first,
                        onClick = { first = true },
                        label = { Text(if (isIncome) "Активный" else "Постоянная") }
                    )
                    FilterChip(
                        selected = !first,
                        onClick = { first = false },
                        label = { Text(if (isIncome) "Пассивный" else "Переменная") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), group) },
                enabled = name.isNotBlank()
            ) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}
