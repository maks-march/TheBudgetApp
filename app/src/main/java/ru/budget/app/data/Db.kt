package ru.budget.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** Операция: трата ("exp") или доход ("in"). Дата — yyyy-MM-dd. Заметка убрана из UI, поле оставлено для совместимости. */
@Entity(tableName = "transactions")
data class TxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val categoryId: String,
    val amount: Double,
    val date: String,
    val note: String = ""
)

/** План на месяц: конверт категории. */
@Entity(tableName = "plans", primaryKeys = ["month", "categoryId"])
data class PlanEntity(
    val month: String,
    val categoryId: String,
    val amount: Double
)

/** Категория: системная или своя; скрытая не видна в интерфейсе, история целостна. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val group: String,           // FlowGroup.name
    val colorIdx: Int,           // индекс в палитре диаграмм
    val isCustom: Boolean = false,
    val hidden: Boolean = false,
    val sortOrder: Int = 0
)

/** Шаблон быстрой траты с главного экрана. */
@Entity(tableName = "quick_expenses")
data class QuickEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: String,
    val amount: Double
)

@Dao
interface BudgetDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun txFlow(): Flow<List<TxEntity>>

    @Query("SELECT * FROM plans")
    fun plansFlow(): Flow<List<PlanEntity>>

    @Query("SELECT * FROM categories ORDER BY sortOrder")
    fun catsFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM quick_expenses")
    fun quicksFlow(): Flow<List<QuickEntity>>

    @Insert
    suspend fun insertTx(tx: TxEntity): Long

    @Update
    suspend fun updateTx(tx: TxEntity)

    @Delete
    suspend fun deleteTx(tx: TxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(plan: PlanEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCat(cat: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCat(id: String)

    @Insert
    suspend fun insertQuick(q: QuickEntity)

    @Delete
    suspend fun deleteQuick(q: QuickEntity)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun txCount(): Int

    @Query("SELECT COUNT(*) FROM plans")
    suspend fun plansCount(): Int

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun catsCount(): Int

    @Query("SELECT MAX(colorIdx) FROM categories")
    suspend fun maxColorIdx(): Int?

    @Query("SELECT * FROM transactions ORDER BY date ASC, id ASC")
    suspend fun getAllOnce(): List<TxEntity>

    @Query("SELECT * FROM plans")
    suspend fun getPlansOnce(): List<PlanEntity>

    @Query("SELECT * FROM categories")
    suspend fun getCatsOnce(): List<CategoryEntity>

    @Query("SELECT * FROM quick_expenses")
    suspend fun getQuicksOnce(): List<QuickEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllTx(txs: List<TxEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPlans(plans: List<PlanEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCats(cats: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllQuicks(quicks: List<QuickEntity>)

    @Query("DELETE FROM transactions")
    suspend fun clearTxs()

    @Query("DELETE FROM plans")
    suspend fun clearPlans()

    @Query("DELETE FROM categories")
    suspend fun clearCats()

    @Query("DELETE FROM quick_expenses")
    suspend fun clearQuicks()
}

@Database(
    entities = [TxEntity::class, PlanEntity::class, CategoryEntity::class, QuickEntity::class],
    version = 2,
    exportSchema = false
)
abstract class Db : RoomDatabase() {

    abstract fun dao(): BudgetDao

    companion object {
        fun build(context: Context): Db =
            Room.databaseBuilder(context, Db::class.java, "budget.db")
                // до релиза: смена схемы без миграций
                .fallbackToDestructiveMigration()
                .build()
    }
}
