package ru.budget.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import ru.budget.app.data.BudgetDao
import ru.budget.app.data.CategorySeed
import ru.budget.app.data.CategoryEntity
import ru.budget.app.data.Db
import ru.budget.app.data.FlowGroup
import ru.budget.app.data.PlanEntity
import ru.budget.app.data.QuickEntity
import ru.budget.app.data.TxEntity
import ru.budget.app.domain.BudgetEngine
import ru.budget.app.export.BudgetExporter
import java.time.LocalDate

data class AppState(
    val txs: List<TxEntity>,
    val plans: Map<String, Map<String, Double>>,
    val cats: List<CategoryEntity>,
    val quicks: List<QuickEntity>,
    val engine: BudgetEngine
) {
    fun cat(id: String): CategoryEntity? = cats.find { it.id == id }
    fun group(catId: String): FlowGroup? = cat(catId)?.let { runCatching { FlowGroup.valueOf(it.group) }.getOrNull() }

    /** Число использований категорий (для порядка «частые сверху» в шторке). */
    fun usageCounts(): Map<String, Int> = txs.groupingBy { it.categoryId }.eachCount()
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val dao: BudgetDao = Db.build(app).dao()

    val state: StateFlow<AppState?> = combine(
        dao.txFlow(), dao.plansFlow(), dao.catsFlow(), dao.quicksFlow()
    ) { txs, plans, cats, quicks ->
        val planMap = plans
            .groupBy({ it.month }, { it.categoryId to it.amount })
            .mapValues { (_, list) -> list.toMap() }
        val expCats = cats.filter {
            !it.hidden && (it.group == FlowGroup.FIXED.name || it.group == FlowGroup.VARIABLE.name)
        }
        AppState(txs, planMap, cats, quicks, BudgetEngine(txs, planMap, expCats))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        viewModelScope.launch { seedIfNeeded() }
    }

    private suspend fun seedIfNeeded() {
        if (dao.catsCount() == 0) {
            dao.insertAllCats(
                CategorySeed.defaults.mapIndexed { i, d ->
                    CategoryEntity(
                        id = d.id, name = d.name, group = d.group.name,
                        colorIdx = i, sortOrder = i
                    )
                }
            )
        }
        if (dao.txCount() == 0 && dao.plansCount() == 0) {
            val now = LocalDate.now()
            for (m in now.monthValue - 1 until 12) {
                val key = BudgetEngine.monthKey(now.year, m)
                for ((catId, amount) in CategorySeed.defaultPlan) {
                    dao.upsertPlan(PlanEntity(key, catId, amount))
                }
            }
        }
    }

    /* ─────────── операции ─────────── */

    fun saveTx(id: Long, type: String, categoryId: String, amount: Double, dateISO: String) {
        viewModelScope.launch {
            val tx = TxEntity(id = id, type = type, categoryId = categoryId, amount = amount, date = dateISO)
            if (id == 0L) dao.insertTx(tx) else dao.updateTx(tx)
        }
    }

    /** Быстрая трата с главного экрана; возвращает id для «Отменить». */
    fun recordQuick(categoryId: String, amount: Double, onDone: (Long) -> Unit) {
        viewModelScope.launch {
            val id = dao.insertTx(
                TxEntity(
                    type = BudgetEngine.TYPE_EXP, categoryId = categoryId,
                    amount = amount, date = LocalDate.now().toString()
                )
            )
            onDone(id)
        }
    }

    fun deleteTxById(id: Long) {
        viewModelScope.launch {
            dao.getAllOnce().find { it.id == id }?.let { dao.deleteTx(it) }
        }
    }

    fun deleteTx(tx: TxEntity) {
        viewModelScope.launch { dao.deleteTx(tx) }
    }

    /* ─────────── план ─────────── */

    fun setPlan(month: String, categoryId: String, amount: Double) {
        viewModelScope.launch { dao.upsertPlan(PlanEntity(month, categoryId, amount)) }
    }

    /* ─────────── быстрые шаблоны ─────────── */

    fun addQuickTemplate(categoryId: String, amount: Double) {
        viewModelScope.launch { dao.insertQuick(QuickEntity(categoryId = categoryId, amount = amount)) }
    }

    fun deleteQuick(q: QuickEntity) {
        viewModelScope.launch { dao.deleteQuick(q) }
    }

    /* ─────────── категории ─────────── */

    fun addCategory(name: String, group: FlowGroup, onDone: (String) -> Unit = {}) {
        viewModelScope.launch {
            val idx = (dao.maxColorIdx() ?: -1) + 1
            val id = "c${System.currentTimeMillis()}"
            dao.upsertCat(
                CategoryEntity(
                    id = id, name = name, group = group.name,
                    colorIdx = idx, isCustom = true, sortOrder = 1000 + idx
                )
            )
            onDone(id)
        }
    }

    /** Свою — удалить, системную — скрыть. */
    fun removeOrHideCategory(cat: CategoryEntity) {
        viewModelScope.launch {
            if (cat.isCustom) dao.deleteCat(cat.id)
            else dao.upsertCat(cat.copy(hidden = true))
        }
    }

    /* ─────────── экспорт и бэкап ─────────── */

    fun exportXlsx(onDone: (BudgetExporter.Saved?) -> Unit) {
        viewModelScope.launch {
            val st = state.value
            val result = withContext(Dispatchers.IO) {
                if (st == null) null
                else BudgetExporter.exportToDownloads(getApplication(), st.txs, st.cats)
            }
            onDone(result)
        }
    }

    /** JSON-копия всех данных в «Загрузки». */
    fun exportBackup(onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val st = state.value
            val name = withContext(Dispatchers.IO) {
                if (st == null) null else writeBackup(getApplication(), st)
            }
            onDone(name)
        }
    }

    fun importBackup(json: String, onDone: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                val root = JSONObject(json)
                dao.clearTxs(); dao.clearPlans(); dao.clearCats(); dao.clearQuicks()

                val cats = root.optJSONArray("cats") ?: JSONArray()
                for (i in 0 until cats.length()) {
                    val o = cats.getJSONObject(i)
                    dao.upsertCat(
                        CategoryEntity(
                            id = o.getString("id"), name = o.getString("name"),
                            group = o.getString("group"), colorIdx = o.getInt("colorIdx"),
                            isCustom = o.optBoolean("custom"), hidden = o.optBoolean("hidden"),
                            sortOrder = o.optInt("sort")
                        )
                    )
                }
                val plans = root.optJSONArray("plans") ?: JSONArray()
                for (i in 0 until plans.length()) {
                    val o = plans.getJSONObject(i)
                    dao.upsertPlan(PlanEntity(o.getString("m"), o.getString("c"), o.getDouble("a")))
                }
                val txs = root.optJSONArray("txs") ?: JSONArray()
                for (i in 0 until txs.length()) {
                    val o = txs.getJSONObject(i)
                    dao.insertTx(
                        TxEntity(
                            id = 0, type = o.getString("t"), categoryId = o.getString("c"),
                            amount = o.getDouble("a"), date = o.getString("d"),
                            note = o.optString("n", "")
                        )
                    )
                }
                val quicks = root.optJSONArray("quicks") ?: JSONArray()
                for (i in 0 until quicks.length()) {
                    val o = quicks.getJSONObject(i)
                    dao.insertQuick(QuickEntity(categoryId = o.getString("c"), amount = o.getDouble("a")))
                }
                true
            }.getOrDefault(false)
            withContext(Dispatchers.Main) { onDone(ok) }
        }
    }

    private fun writeBackup(
        context: android.content.Context,
        st: AppState
    ): String? = runCatching {
        val root = JSONObject()
        root.put("cats", JSONArray(st.cats.map {
            JSONObject()
                .put("id", it.id).put("name", it.name).put("group", it.group)
                .put("colorIdx", it.colorIdx).put("custom", it.isCustom)
                .put("hidden", it.hidden).put("sort", it.sortOrder)
        }))
        root.put("plans", JSONArray(st.plans.flatMap { (m, cm) ->
            cm.map { (c, a) -> JSONObject().put("m", m).put("c", c).put("a", a) }
        }))
        root.put("txs", JSONArray(st.txs.map {
            JSONObject().put("t", it.type).put("c", it.categoryId)
                .put("a", it.amount).put("d", it.date).put("n", it.note)
        }))
        root.put("quicks", JSONArray(st.quicks.map {
            JSONObject().put("c", it.categoryId).put("a", it.amount)
        }))

        val fileName = "Budget-backup-${LocalDate.now()}.json"
        val resolver = context.contentResolver
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json")
            put(android.provider.MediaStore.Downloads.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            put(android.provider.MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return@runCatching null
        resolver.openOutputStream(uri)?.use { it.write(root.toString().toByteArray()) }
        values.clear()
        values.put(android.provider.MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        fileName
    }.getOrNull()

    /* ─────────── очистка ─────────── */

    fun clearAll() {
        viewModelScope.launch(Dispatchers.IO) {
            dao.clearTxs(); dao.clearPlans(); dao.clearQuicks(); dao.clearCats()
            seedIfNeeded()
        }
    }
}
