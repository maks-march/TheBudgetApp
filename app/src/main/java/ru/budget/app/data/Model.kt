package ru.budget.app.data

/**
 * Группа денежного потока. Эмодзи не используем — цвет задаётся в ui-слое.
 */
enum class FlowGroup(val title: String) {
    ACTIVE("Активный доход"),
    PASSIVE("Пассивный доход"),
    FIXED("Постоянные расходы"),
    VARIABLE("Переменные расходы")
}

/** Определение категории для засеивания БД при первом запуске. */
data class CategoryDef(val id: String, val name: String, val group: FlowGroup)

object CategorySeed {

    val defaults: List<CategoryDef> = listOf(
        // Активный доход
        CategoryDef("advance", "Аванс", FlowGroup.ACTIVE),
        CategoryDef("salary", "Зарплата", FlowGroup.ACTIVE),
        CategoryDef("transfers_in", "Переводы входящие", FlowGroup.ACTIVE),
        CategoryDef("sales", "Продажи (Авито)", FlowGroup.ACTIVE),
        // Пассивный доход
        CategoryDef("deposit_int", "Проценты по вкладу", FlowGroup.PASSIVE),
        CategoryDef("coupons", "Купоны по облигациям", FlowGroup.PASSIVE),
        CategoryDef("capitalization", "Капитализация НС", FlowGroup.PASSIVE),
        CategoryDef("cashback", "Кэшбек", FlowGroup.PASSIVE),
        CategoryDef("last_year", "С прошлого года", FlowGroup.PASSIVE),
        CategoryDef("other_income", "Другой доход", FlowGroup.PASSIVE),
        // Постоянные расходы
        CategoryDef("rent", "Аренда / ипотека", FlowGroup.FIXED),
        CategoryDef("groceries", "Продукты", FlowGroup.FIXED),
        CategoryDef("transport", "Транспорт", FlowGroup.FIXED),
        CategoryDef("telecom", "Связь", FlowGroup.FIXED),
        CategoryDef("credit", "Кредит", FlowGroup.FIXED),
        CategoryDef("subscriptions", "Подписки", FlowGroup.FIXED),
        CategoryDef("taxes", "Налоги", FlowGroup.FIXED),
        // Переменные расходы
        CategoryDef("eating_out", "Еда вне дома", FlowGroup.VARIABLE),
        CategoryDef("coffee", "Кофе", FlowGroup.VARIABLE),
        CategoryDef("entertainment", "Развлечения, отдых", FlowGroup.VARIABLE),
        CategoryDef("hobby", "Хобби", FlowGroup.VARIABLE),
        CategoryDef("clothes", "Одежда, обувь", FlowGroup.VARIABLE),
        CategoryDef("household", "Бытовые товары", FlowGroup.VARIABLE),
        CategoryDef("health", "Здоровье", FlowGroup.VARIABLE),
        CategoryDef("pets", "Домашние животные", FlowGroup.VARIABLE),
        CategoryDef("gifts", "Подарки", FlowGroup.VARIABLE),
        CategoryDef("education", "Обучение", FlowGroup.VARIABLE),
        CategoryDef("misc", "Прочее / непредвиденное", FlowGroup.VARIABLE),
        CategoryDef("transfers_out", "Переводы исходящие", FlowGroup.VARIABLE),
        CategoryDef("withdrawal", "Снятие наличных", FlowGroup.VARIABLE)
    )

    /** Планы по умолчанию (из исходной таблицы пользователя). */
    val defaultPlan: Map<String, Double> = mapOf(
        "rent" to 4000.0,
        "groceries" to 10000.0,
        "transport" to 500.0,
        "telecom" to 600.0,
        "credit" to 0.0,
        "subscriptions" to 0.0,
        "taxes" to 0.0,
        "eating_out" to 500.0,
        "coffee" to 1000.0,
        "entertainment" to 500.0,
        "hobby" to 0.0,
        "clothes" to 0.0,
        "household" to 0.0,
        "health" to 500.0,
        "pets" to 0.0,
        "gifts" to 0.0,
        "education" to 0.0,
        "misc" to 200.0,
        "transfers_out" to 1000.0,
        "withdrawal" to 0.0
    )
}
