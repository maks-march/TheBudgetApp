package ru.budget.app.domain

import ru.budget.app.data.CategoryEntity
import ru.budget.app.data.TxEntity
import java.util.Locale

/**
 * Конверт категории за месяц.
 *
 * available = plan + carryIn        (доступно = план + перенос с прошлого месяца)
 * left      = available - spent     (остаток, переносится дальше; может быть отрицательным)
 */
data class Envelope(val plan: Double, val carryIn: Double, val spent: Double) {
    val available: Double get() = plan + carryIn
    val left: Double get() = available - spent

    operator fun plus(other: Envelope) =
        Envelope(plan + other.plan, carryIn + other.carryIn, spent + other.spent)
}

data class MonthTotals(val income: Double, val expense: Double) {
    val net: Double get() = income - expense
}

/**
 * Чистое расчётное ядро без Android-зависимостей.
 * Факт (операции) — единственный источник истины; категории передаются снаружи.
 */
class BudgetEngine(
    private val txs: List<TxEntity>,
    private val plans: Map<String, Map<String, Double>>,
    private val expenseCats: List<CategoryEntity>
) {

    private val byMonth: Map<String, List<TxEntity>> = txs.groupBy { it.date.take(7) }

    fun spentIn(month: String): Map<String, Double> =
        byMonth[month].orEmpty().asSequence()
            .filter { it.type == TYPE_EXP }
            .groupBy({ it.categoryId }, { it.amount })
            .mapValues { (_, list) -> list.sum() }

    fun incomeIn(month: String): Map<String, Double> =
        byMonth[month].orEmpty().asSequence()
            .filter { it.type == TYPE_IN }
            .groupBy({ it.categoryId }, { it.amount })
            .mapValues { (_, list) -> list.sum() }

    /** Конверты месяцев января..upToMonth с переносом остатков. */
    fun envelopes(year: Int, upToMonth: Int = 11): Map<Int, Map<String, Envelope>> {
        val result = mutableMapOf<Int, Map<String, Envelope>>()
        var prev: Map<String, Envelope> = emptyMap()
        for (m in 0..upToMonth.coerceIn(0, 11)) {
            val key = monthKey(year, m)
            val spent = spentIn(key)
            val current = expenseCats.associate { cat ->
                cat.id to Envelope(
                    plan = plans[key]?.get(cat.id) ?: 0.0,
                    carryIn = prev[cat.id]?.left ?: 0.0,
                    spent = spent[cat.id] ?: 0.0
                )
            }
            result[m] = current
            prev = current
        }
        return result
    }

    fun envelopesOfMonth(year: Int, month: Int): Map<String, Envelope> =
        envelopes(year, month).getValue(month)

    fun totalEnvelope(year: Int, month: Int): Envelope =
        envelopesOfMonth(year, month).values.reduce { acc, e -> acc + e }

    fun totals(month: String): MonthTotals {
        var income = 0.0
        var expense = 0.0
        for (t in byMonth[month].orEmpty()) {
            when (t.type) {
                TYPE_IN -> income += t.amount
                TYPE_EXP -> expense += t.amount
            }
        }
        return MonthTotals(income, expense)
    }

    fun txsOfMonth(month: String, type: String? = null): List<TxEntity> =
        byMonth[month].orEmpty()
            .filter { type == null || it.type == type }
            .sortedWith(compareByDescending<TxEntity> { it.date }.thenByDescending { it.id })

    /** Средний доход в месяц по месяцам года (до выбранного включительно), где был доход. */
    fun avgIncome(year: Int, upToMonth: Int): Double {
        var sum = 0.0
        var months = 0
        for (m in 0..upToMonth.coerceIn(0, 11)) {
            val inc = totals(monthKey(year, m)).income
            if (inc > 0.0) { sum += inc; months++ }
        }
        return if (months > 0) sum / months else 0.0
    }

    companion object {
        const val TYPE_EXP = "exp"
        const val TYPE_IN = "in"

        fun monthKey(year: Int, month: Int): String =
            String.format(Locale.US, "%04d-%02d", year, month + 1)
    }
}
