package ru.budget.app.export

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import ru.budget.app.data.CategoryEntity
import ru.budget.app.data.TxEntity
import ru.budget.app.domain.BudgetEngine
import java.io.File
import java.io.OutputStream
import java.time.LocalDate

/**
 * Выгружает все операции в .xlsx прямо в папку «Загрузки».
 * (Схема сохранения перенесена из TheSleepTracker.)
 */
object BudgetExporter {

    private const val MIME_XLSX =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    data class Saved(val fileName: String)

    /**
     * Пишет файл в общую папку Downloads: на Android 10+ через MediaStore
     * (разрешения не нужны), на более старых — напрямую в публичную директорию.
     *
     * @return null, если выгружать нечего или запись не удалась
     */
    fun exportToDownloads(context: Context, txs: List<TxEntity>, cats: List<CategoryEntity>): Saved? {
        if (txs.isEmpty()) return null

        val fileName = buildFileName()

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(context, fileName, txs, cats)
            } else {
                saveToPublicDir(context, fileName, txs, cats)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun saveViaMediaStore(
        context: Context,
        fileName: String,
        txs: List<TxEntity>,
        cats: List<CategoryEntity>,
    ): Saved? {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, MIME_XLSX)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return null

        resolver.openOutputStream(uri)?.use { out -> writeWorkbook(out, txs, cats) }
            ?: return null

        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)

        return Saved(fileName)
    }

    private fun saveToPublicDir(
        context: Context,
        fileName: String,
        txs: List<TxEntity>,
        cats: List<CategoryEntity>,
    ): Saved {
        val dir = Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            .apply { mkdirs() }
        val file = File(dir, fileName)
        file.outputStream().use { out -> writeWorkbook(out, txs, cats) }
        return Saved(fileName)
    }

    private fun writeWorkbook(out: OutputStream, txs: List<TxEntity>, cats: List<CategoryEntity>) {
        val header = listOf("Дата", "Тип", "Категория", "Сумма", "Заметка")

        val rows = txs
            .sortedWith(compareBy({ it.date }, { it.id }))
            .map { t ->
                listOf(
                    XlsxWriter.Cell.Text(t.date),
                    XlsxWriter.Cell.Text(
                        if (t.type == BudgetEngine.TYPE_IN) "Доход" else "Расход"
                    ),
                    XlsxWriter.Cell.Text(cats.find { it.id == t.categoryId }?.name ?: t.categoryId),
                    XlsxWriter.Cell.Number(t.amount),
                    XlsxWriter.Cell.Text(t.note),
                )
            }

        XlsxWriter.write(
            out = out,
            sheetName = "Операции",
            header = header,
            rows = rows,
            columnWidths = listOf(12, 10, 26, 12, 32),
        )
    }

    /** Имя с датой, чтобы выгрузки не затирали друг друга. */
    private fun buildFileName(): String {
        val stamp = LocalDate.now().toString() // yyyy-MM-dd
        return "Budget-$stamp.xlsx"
    }
}
