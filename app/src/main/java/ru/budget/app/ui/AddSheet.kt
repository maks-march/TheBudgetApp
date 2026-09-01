package ru.budget.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.budget.app.data.Categories
import ru.budget.app.data.TxEntity
import ru.budget.app.domain.BudgetEngine
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Шторка добавления/редактирования операции: сумма → тип → категория → дата → сохранить.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddSheet(
    edit: TxEntity?,
    onDismiss: () -> Unit,
    onSave: (type: String, categoryId: String, amount: Double, dateISO: String, note: String) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var type by remember { mutableStateOf(edit?.type ?: BudgetEngine.TYPE_EXP) }
    var amountText by remember { mutableStateOf(if (edit != null) Fmt.plain(edit.amount) else "") }
    var note by remember { mutableStateOf(edit?.note ?: "") }
    var dateISO by remember { mutableStateOf(edit?.date ?: LocalDate.now().toString()) }
    var showPicker by remember { mutableStateOf(false) }

    val cats = if (type == BudgetEngine.TYPE_IN) Categories.incomes else Categories.expenses
    var catId by remember(type) {
        val initial = edit?.categoryId?.takeIf { id -> cats.any { it.id == id } }
        mutableStateOf(initial ?: cats.first().id)
    }

    val amount = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {

            Text(
                when {
                    edit != null -> "ИЗМЕНИТЬ ОПЕРАЦИЮ"
                    type == BudgetEngine.TYPE_IN -> "НОВЫЙ ДОХОД"
                    else -> "НОВАЯ ТРАТА"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMut,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            OutlinedTextField(
                value = amountText,
                onValueChange = { v -> amountText = v.filter { it.isDigit() || it == '.' || it == ',' } },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                suffix = { Text("₽", color = TextMut) }
            )

            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = type == BudgetEngine.TYPE_EXP,
                    onClick = { type = BudgetEngine.TYPE_EXP },
                    label = { Text("Расход") }
                )
                FilterChip(
                    selected = type == BudgetEngine.TYPE_IN,
                    onClick = { type = BudgetEngine.TYPE_IN },
                    label = { Text("Доход") }
                )
            }

            Text(
                "Категория",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = TextDim,
                modifier = Modifier.padding(top = 10.dp, start = 4.dp)
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                for (c in cats) {
                    FilterChip(
                        selected = catId == c.id,
                        onClick = { catId = c.id },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(Modifier.size(8.dp).background(groupColor(c.group), CircleShape))
                                Text(c.name, fontSize = 12.sp)
                            }
                        }
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(onClick = { showPicker = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(dayLabel(dateISO))
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Заметка") },
                    singleLine = true
                )
            }

            Button(
                onClick = { onSave(type, catId, amount, dateISO, note) },
                enabled = amount > 0.0,
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(50.dp)
            ) {
                Text(if (edit != null) "Сохранить" else "Добавить", fontWeight = FontWeight.Bold)
            }

            if (edit != null) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text("Удалить операцию", color = Red)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = LocalDate.parse(dateISO)
                .atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { ms ->
                        dateISO = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showPicker = false
                }) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Отмена") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
