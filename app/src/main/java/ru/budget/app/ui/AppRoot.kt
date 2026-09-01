package ru.budget.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.budget.app.data.TxEntity
import ru.budget.app.domain.BudgetEngine
import java.time.LocalDate

@Composable
fun AppRoot(vm: AppViewModel) {
    val state by vm.state.collectAsState()
    val st = state
    if (st == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var year by rememberSaveable { mutableIntStateOf(LocalDate.now().year) }
    var month by rememberSaveable { mutableIntStateOf(LocalDate.now().monthValue - 1) }
    var sheetOpen by remember { mutableStateOf(false) }
    var editTx by remember { mutableStateOf<TxEntity?>(null) }

    fun shiftMonth(dir: Int) {
        month += dir
        if (month > 11) { month = 0; year += 1 }
        if (month < 0) { month = 11; year -= 1 }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = NavBg, tonalElevation = 0.dp) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Сегодня") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.List, contentDescription = null) },
                    label = { Text("Бюджет") }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editTx = null; sheetOpen = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить операцию")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            MonthSwitcher(year, month) { shiftMonth(it) }
            when (tab) {
                0 -> TodayScreen(st, year, month) { tx -> editTx = tx; sheetOpen = true }
                else -> BudgetScreen(st, year, month) { catId, amount ->
                    vm.setPlan(BudgetEngine.monthKey(year, month), catId, amount)
                }
            }
        }
    }

    if (sheetOpen) {
        AddSheet(
            edit = editTx,
            onDismiss = { sheetOpen = false },
            onSave = { type, catId, amount, dateISO, note ->
                vm.saveTx(editTx?.id ?: 0L, type, catId, amount, dateISO, note)
                sheetOpen = false
            },
            onDelete = {
                editTx?.let(vm::deleteTx)
                sheetOpen = false
            }
        )
    }
}

@Composable
private fun MonthSwitcher(year: Int, month: Int, onShift: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = { onShift(-1) }) {
            Icon(Icons.Filled.KeyboardArrowLeft, contentDescription = "Предыдущий месяц")
        }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("${MONTHS_FULL[month]} $year", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            val today = LocalDate.now()
            if (year == today.year && month == today.monthValue - 1) {
                val daysLeft = today.lengthOfMonth() - today.dayOfMonth + 1
                Text("до конца месяца: $daysLeft дн.", fontSize = 11.sp, color = TextDim)
            }
        }
        IconButton(onClick = { onShift(1) }) {
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = "Следующий месяц")
        }
    }
}
