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

/** Операция: трата ("exp") или доход ("in"). Дата хранится как yyyy-MM-dd. */
@Entity(tableName = "transactions")
data class TxEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val categoryId: String,
    val amount: Double,
    val date: String,
    val note: String = ""
)

/** План на месяц: конверт категории. Ключ — пара (месяц, категория). */
@Entity(tableName = "plans", primaryKeys = ["month", "categoryId"])
data class PlanEntity(
    val month: String,
    val categoryId: String,
    val amount: Double
)

@Dao
interface BudgetDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun txFlow(): Flow<List<TxEntity>>

    @Query("SELECT * FROM plans")
    fun plansFlow(): Flow<List<PlanEntity>>

    @Insert
    suspend fun insertTx(tx: TxEntity): Long

    @Update
    suspend fun updateTx(tx: TxEntity)

    @Delete
    suspend fun deleteTx(tx: TxEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPlan(plan: PlanEntity)

    @Query("SELECT COUNT(*) FROM transactions")
    suspend fun txCount(): Int

    @Query("SELECT COUNT(*) FROM plans")
    suspend fun plansCount(): Int
}

@Database(entities = [TxEntity::class, PlanEntity::class], version = 1, exportSchema = false)
abstract class Db : RoomDatabase() {

    abstract fun dao(): BudgetDao

    companion object {
        fun build(context: Context): Db =
            Room.databaseBuilder(context, Db::class.java, "budget.db").build()
    }
}
