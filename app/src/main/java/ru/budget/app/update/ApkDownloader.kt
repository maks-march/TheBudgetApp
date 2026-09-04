package ru.budget.app.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import ru.budget.app.BuildConfig
import java.io.File

/**
 * Скачивает APK приложения системным [DownloadManager] и предлагает установить его.
 * (Перенесён из TheSleepTracker.)
 *
 * Ссылка ведёт на файл в репозитории, поэтому качается именно та сборка,
 * которая там лежит.
 */
object ApkDownloader {

    private const val FILE_NAME = "Budget.apk"
    private const val MIME_APK = "application/vnd.android.package-archive"

    /** Состояние загрузки для UI. */
    sealed interface Status {
        data object Idle : Status
        data object Running : Status
        data object Done : Status

        /** Скачано, но система не разрешает ставить APK из этого источника. */
        data object NeedsInstallPermission : Status
        data class Failed(val reason: String?) : Status
    }

    /**
     * Может ли приложение самостоятельно запустить установку.
     * С Android 8 для этого нужно отдельное разрешение пользователя.
     */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    /** Открывает системный экран «Установка неизвестных приложений» для нашего пакета. */
    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // на некоторых прошивках экрана нет — открываем общие настройки
            try {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (ignored: Exception) {
                // ничего не поделать
            }
        }
    }

    /**
     * Ставит загрузку в очередь.
     *
     * @param onStatus вызывается при изменении состояния
     * @return id загрузки или null, если сервис недоступен
     */
    fun enqueue(context: Context, onStatus: (Status) -> Unit): Long? {
        val app = context.applicationContext
        val manager = app.getSystemService(DownloadManager::class.java) ?: return null

        // старый файл мешает: DownloadManager добавит суффикс -1 к имени
        File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), FILE_NAME).delete()

        val request = DownloadManager.Request(Uri.parse(BuildConfig.APK_URL))
            .setTitle(FILE_NAME)
            .setMimeType(MIME_APK)
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            .setDestinationInExternalFilesDir(app, Environment.DIRECTORY_DOWNLOADS, FILE_NAME)
            .setAllowedOverMetered(true)

        val id = try {
            manager.enqueue(request)
        } catch (e: Exception) {
            onStatus(Status.Failed(e.message))
            return null
        }

        onStatus(Status.Running)
        registerCompletionReceiver(app, id, onStatus)
        return id
    }

    private fun registerCompletionReceiver(
        app: Context,
        id: Long,
        onStatus: (Status) -> Unit,
    ) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val finishedId =
                    intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (finishedId != id) return

                try {
                    app.unregisterReceiver(this)
                } catch (ignored: Exception) {
                    // уже снят
                }

                val manager = app.getSystemService(DownloadManager::class.java)
                if (manager == null) {
                    onStatus(Status.Failed(null))
                    return
                }

                val (status, reason) = queryStatus(manager, id)
                if (status != DownloadManager.STATUS_SUCCESSFUL) {
                    onStatus(Status.Failed(reason))
                    return
                }

                onStatus(Status.Done)

                if (canInstall(app)) {
                    install(app)
                } else {
                    // без разрешения установщик молча не откроется — говорим об этом
                    onStatus(Status.NeedsInstallPermission)
                    openInstallPermissionSettings(app)
                }
            }
        }

        ContextCompat.registerReceiver(
            app,
            receiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED,
        )
    }

    private fun queryStatus(manager: DownloadManager, id: Long): Pair<Int, String?> {
        var cursor: Cursor? = null
        return try {
            cursor = manager.query(DownloadManager.Query().setFilterById(id))
            if (cursor != null && cursor.moveToFirst()) {
                val status =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val reason =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                status to reason.toString()
            } else {
                DownloadManager.STATUS_FAILED to null
            }
        } catch (e: Exception) {
            DownloadManager.STATUS_FAILED to e.message
        } finally {
            cursor?.close()
        }
    }

    /** Открывает системный установщик для скачанного файла. */
    fun install(app: Context) {
        val file = File(
            app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            FILE_NAME,
        )
        if (!file.exists()) return

        val uri: Uri = FileProvider.getUriForFile(
            app,
            "${app.packageName}.fileprovider",
            file,
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, MIME_APK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            app.startActivity(intent)
        } catch (e: Exception) {
            // установщик недоступен — файл всё равно лежит в Downloads
        }
    }

    /** Уже скачанный файл — чтобы предложить установку повторно. */
    fun downloadedFileExists(app: Context): Boolean =
        File(app.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), FILE_NAME).exists()
}
