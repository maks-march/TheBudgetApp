package ru.budget.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ru.budget.app.data.Categories
import ru.budget.app.data.Db
import ru.budget.app.data.PlanEntity
import ru.budget.app.data.TxEntity
import ru.budget.app.domain.BudgetEngine
import java.time.LocalDate

data class AppState(
    val txs: List<TxEntity>,
    val engine: BudgetEngine
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = Db.build(app).dao()

    val state: StateFlow<AppState?> = combine(dao.txFlow(), dao.plansFlow()) { txs, plans ->
        val planMap = plans
            .groupBy({ it.month }, { it.categoryId to it.amount })
            .mapValues { (_, list) -> list.toMap() }
        AppState(txs, BudgetEngine(txs, planMap))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch {
            if (dao.txCount() == 0 && dao.plansCount() == 0) seedDefaults()
        }
    }

    /** Планы из исходной таблицы — на месяцы с текущего до конца года (без фиктивных переносов за прошлое). */
    private suspend fun seedDefaults() {
        val now = LocalDate.now()
        for (m in now.monthValue - 1 until 12) {
            val key = BudgetEngine.monthKey(now.year, m)
            for ((catId, amount) in Categories.defaultPlan) {
                dao.upsertPlan(PlanEntity(key, catId, amount))
            }
        }
    }

    fun saveTx(id: Long, type: String, categoryId: String, amount: Double, dateISO: String, note: String) {
        viewModelScope.launch {
            val tx = TxEntity(
                id = id,
                type = type,
                categoryId = categoryId,
                amount = amount,
                date = dateISO,
                note = note
            )
            if (id == 0L) dao.insertTx(tx) else dao.updateTx(tx)
        }
    }

    fun deleteTx(tx: TxEntity) {
        viewModelScope.launch { dao.deleteTx(tx) }
    }

    fun setPlan(month: String, categoryId: String, amount: Double) {
        viewModelScope.launch { dao.upsertPlan(PlanEntity(month, categoryId, amount)) }
    }
}
