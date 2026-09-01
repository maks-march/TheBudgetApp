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

data class Category(val id: String, val name: String, val group: FlowGroup)

object Categories {

    val all: List<Category> = listOf(
        // Активный доход
        Category("advance", "Аванс", FlowGroup.ACTIVE),
        Category("salary", "Зарплата", FlowGroup.ACTIVE),
        Category("transfers_in", "Переводы входящие", FlowGroup.ACTIVE),
        Category("sales", "Продажи (Авито)", FlowGroup.ACTIVE),
        // Пассивный доход
        Category("deposit_int", "Проценты по вкладу", FlowGroup.PASSIVE),
        Category("coupons", "Купоны по облигациям", FlowGroup.PASSIVE),
        Category("capitalization", "Капитализация НС", FlowGroup.PASSIVE),
        Category("cashback", "Кэшбек", FlowGroup.PASSIVE),
        Category("last_year", "С прошлого года", FlowGroup.PASSIVE),
        Category("other_income", "Другой доход", FlowGroup.PASSIVE),
        // Постоянные расходы
        Category("rent", "Аренда / ипотека", FlowGroup.FIXED),
        Category("groceries", "Продукты", FlowGroup.FIXED),
        Category("transport", "Транспорт", FlowGroup.FIXED),
        Category("telecom", "Связь", FlowGroup.FIXED),
        Category("credit", "Кредит", FlowGroup.FIXED),
        Category("subscriptions", "Подписки", FlowGroup.FIXED),
        Category("taxes", "Налоги", FlowGroup.FIXED),
        // Переменные расходы
        Category("eating_out", "Еда вне дома", FlowGroup.VARIABLE),
        Category("coffee", "Кофе", FlowGroup.VARIABLE),
        Category("entertainment", "Развлечения, отдых", FlowGroup.VARIABLE),
        Category("hobby", "Хобби", FlowGroup.VARIABLE),
        Category("clothes", "Одежда, обувь", FlowGroup.VARIABLE),
        Category("household", "Бытовые товары", FlowGroup.VARIABLE),
        Category("health", "Здоровье", FlowGroup.VARIABLE),
        Category("pets", "Домашние животные", FlowGroup.VARIABLE),
        Category("gifts", "Подарки", FlowGroup.VARIABLE),
        Category("education", "Обучение", FlowGroup.VARIABLE),
        Category("misc", "Прочее / непредвиденное", FlowGroup.VARIABLE),
        Category("transfers_out", "Переводы исходящие", FlowGroup.VARIABLE),
        Category("withdrawal", "Снятие наличных", FlowGroup.VARIABLE)
    )

    val byId: Map<String, Category> = all.associateBy { it.id }
    val expenses: List<Category> = all.filter { it.group == FlowGroup.FIXED || it.group == FlowGroup.VARIABLE }
    val incomes: List<Category> = all.filter { it.group == FlowGroup.ACTIVE || it.group == FlowGroup.PASSIVE }

    /** Планы по умолчанию (из исходной таблицы пользователя), засеиваются при первом запуске */
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
