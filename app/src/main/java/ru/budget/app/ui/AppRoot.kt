package ru.budget.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.budget.app.data.TxEntity
import ru.budget.app.domain.BudgetEngine
import java.time.LocalDate

/**
 * Каркас приложения — макеты С1/С3/С4/С7:
 * нижняя навигация Траты · Доходы · План-факт, FAB «+» над панелью справа,
 * настройки по иконке в шапке. С2 (План-факт) — текущая реализация конвертов.
 */
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

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var year by rememberSaveable { mutableIntStateOf(LocalDate.now().year) }
    var month by rememberSaveable { mutableIntStateOf(LocalDate.now().monthValue - 1) }
    var showSettings by remember { mutableStateOf(false) }

    var sheetOpen by remember { mutableStateOf(false) }
    var sheetEdit by remember { mutableStateOf<TxEntity?>(null) }
    var sheetQuick by remember { mutableStateOf(false) }

    fun shiftMonth(dir: Int) {
        month += dir
        if (month > 11) { month = 0; year += 1 }
        if (month < 0) { month = 11; year -= 1 }
    }

    val tabTitle = when (tab) { 0 -> "Траты"; 1 -> "Доходы"; else -> "План-факт" }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (showSettings) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showSettings = false }) {
                        Text("‹", fontSize = 20.sp, color = TextMut)
                    }
                    Text("Настройки", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Row(
                    Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tabTitle,
                        Modifier.weight(1f),
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold
                    )
                    if (tab != 2) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                Icons.Filled.Settings,
                                contentDescription = "Настройки",
                                tint = TextDim
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = NavBg,
                tonalElevation = 0.dp,
                contentColor = TextDim
            ) {
                NavigationBarItem(
                    selected = !showSettings && tab == 0,
                    onClick = { showSettings = false; tab = 0 },
                    icon = { Icon(Icons.Filled.Home, contentDescription = null) },
                    label = { Text("Траты") },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = !showSettings && tab == 1,
                    onClick = { showSettings = false; tab = 1 },
                    icon = { Icon(Icons.Filled.ArrowUpward, contentDescription = null) },
                    label = { Text("Доходы") },
                    colors = navItemColors()
                )
                NavigationBarItem(
                    selected = !showSettings && tab == 2,
                    onClick = { showSettings = false; tab = 2 },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("План-факт") },
                    colors = navItemColors()
                )
            }
        },
        floatingActionButton = {
            if (!showSettings) {
                FloatingActionButton(
                    onClick = { sheetEdit = null; sheetQuick = false; sheetOpen = true },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(15.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    elevation = FloatingActionButtonDefaults.elevation(6.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Добавить операцию")
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (!showSettings) {
                MonthSwitcher(year, month) { shiftMonth(it) }
                when (tab) {
                    0 -> ExpensesScreen(
                        state = st,
                        year = year,
                        month = month,
                        onEdit = { tx -> sheetEdit = tx; sheetOpen = true },
                        onAddQuick = { sheetEdit = null; sheetQuick = true; sheetOpen = true },
                        onRecordQuick = { q ->
                            vm.recordQuick(q.categoryId, q.amount) { id ->
                                scope.launch {
                                    val res = snackbar.showSnackbar(
                                        "Записано · ${Fmt.money(q.amount)}", actionLabel = "Отменить"
                                    )
                                    if (res == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                        vm.deleteTxById(id)
                                    }
                                }
                            }
                        },
                        onDeleteQuick = { vm.deleteQuick(it) }
                    )
                    1 -> IncomeScreen(
                        state = st,
                        year = year,
                        month = month,
                        onEdit = { tx -> sheetEdit = tx; sheetOpen = true }
                    )
                    else -> BudgetScreen(
                        state = st,
                        year = year,
                        month = month,
                        onSetPlan = { catId, amount ->
                            vm.setPlan(BudgetEngine.monthKey(year, month), catId, amount)
                        }
                    )
                }
            } else {
                SettingsScreen(
                    vm = vm,
                    onBack = { showSettings = false },
                    onMessage = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
                )
            }
        }
    }

    if (sheetOpen) {
        AddSheet(
            state = st,
            edit = sheetEdit,
            initialType = if (tab == 1) BudgetEngine.TYPE_IN else BudgetEngine.TYPE_EXP,
            quickMode = sheetQuick,
            onDismiss = { sheetOpen = false },
            onSave = { type, catId, amount, dateISO, saveQuick ->
                vm.saveTx(sheetEdit?.id ?: 0L, type, catId, amount, dateISO)
                if (saveQuick) vm.addQuickTemplate(catId, amount)
                sheetOpen = false
            },
            onDelete = {
                sheetEdit?.let(vm::deleteTx)
                sheetOpen = false
            },
            onAddCategory = { name, group, onDone -> vm.addCategory(name, group, onDone) },
            onRemoveCategory = { vm.removeOrHideCategory(it) }
        )
    }
}

@Composable
private fun navItemColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Green,
    selectedTextColor = Green,
    unselectedIconColor = TextDim,
    unselectedTextColor = TextDim,
    indicatorColor = Green.copy(alpha = 0.14f)
)
