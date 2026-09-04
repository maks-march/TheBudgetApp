package ru.budget.app.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ru.budget.app.BuildConfig
import ru.budget.app.update.ApkDownloader
import ru.budget.app.update.UpdateChecker

/**
 * Настройки — макет С7 (рев. 3): только не финансовое.
 * Данные: экспорт в Excel, резервная копия, очистка. Приложение: обновления, о программе.
 */
@Composable
fun SettingsScreen(
    vm: AppViewModel,
    onBack: () -> Unit,
    onMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var update by remember { mutableStateOf<UpdateChecker.Result?>(null) }
    var dlStatus by remember { mutableStateOf<ApkDownloader.Status?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmClear2 by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.readText() ?: ""
                vm.importBackup(text) { ok ->
                    onMessage(if (ok) "Данные восстановлены" else "Не удалось прочитать файл")
                }
            }.onFailure { onMessage("Не удалось прочитать файл") }
        }
    }

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        item { Cap("ДАННЫЕ") }
        item {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceContainer)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                    SetRow("↓", "Экспорт в Excel", "все операции в .xlsx, папка «Загрузки»") {
                        vm.exportXlsx { saved ->
                            onMessage(
                                if (saved != null) "Файл ${saved.fileName} сохранён в «Загрузках»"
                                else "Нет операций для экспорта"
                            )
                        }
                    }
                    SetRow("⇄", "Резервная копия", "сохранить JSON в «Загрузки»") {
                        vm.exportBackup { name ->
                            onMessage(
                                if (name != null) "Копия $name сохранена в «Загрузках»"
                                else "Не удалось создать копию"
                            )
                        }
                    }
                    SetRow("⇠", "Восстановить из копии", "выбрать JSON-файл") {
                        importLauncher.launch(arrayOf("*/*"))
                    }
                    SetRow("×", "Очистить все данные", "операции, планы, шаблоны", danger = true) {
                        confirmClear = true
                    }
                }
            }
        }

        item { Cap("ПРИЛОЖЕНИЕ") }
        item {
            update?.takeIf { it.isNewer }?.let { res ->
                Card(
                    Modifier.fillMaxWidth().padding(bottom = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = GreenCard)
                ) {
                    Column(Modifier.padding(13.dp, 11.dp, 13.dp, 11.dp)) {
                        Text(
                            "Доступна версия ${res.versionName}",
                            color = GreenSoft, fontSize = 13.5.sp, fontWeight = FontWeight.Bold
                        )
                        if (res.notes.isNotBlank()) {
                            Text(res.notes, color = TextMut, fontSize = 11.sp,
                                lineHeight = 15.sp, modifier = Modifier.padding(top = 3.dp))
                        }
                        Row(Modifier.padding(top = 9.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .background(SurfaceHigh, RoundedCornerShape(10.dp))
                                    .clickable { update = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Позже", color = TextMut, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(34.dp)
                                    .background(Green, RoundedCornerShape(10.dp))
                                    .clickable {
                                        if (dlStatus !is ApkDownloader.Status.Running) {
                                            ApkDownloader.enqueue(context) { dlStatus = it }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (dlStatus is ApkDownloader.Status.Running) "Скачивание…" else "Обновить",
                                    color = GreenOn, fontSize = 12.sp, fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SurfaceContainer)) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 2.dp)) {
                    SetRow("↻", "Проверить обновления", "загрузка с GitHub") {
                        scope.launch {
                            val res = UpdateChecker.check()
                            update = res
                            onMessage(
                                if (res == null || !res.isNewer) "У вас последняя версия"
                                else "Доступна версия ${res.versionName}"
                            )
                        }
                    }
                    SetRow("i", "О приложении", null, value = BuildConfig.VERSION_NAME) {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, android.net.Uri.parse(BuildConfig.GITHUB_URL))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Очистить все данные?") },
            text = { Text("Будут удалены все операции, планы и быстрые траты. Действие необратимо.") },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; confirmClear2 = true }) { Text("Продолжить") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Отмена") } }
        )
    }
    if (confirmClear2) {
        AlertDialog(
            onDismissRequest = { confirmClear2 = false },
            title = { Text("Точно очистить?") },
            text = { Text("Второе подтверждение. После очистки данные нельзя восстановить (если не было копии).") },
            confirmButton = {
                TextButton(onClick = {
                    vm.clearAll(); confirmClear2 = false; onMessage("Данные очищены")
                }) { Text("Очистить", color = Red) }
            },
            dismissButton = { TextButton(onClick = { confirmClear2 = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun Cap(text: String) {
    Text(
        text, Modifier.padding(start = 2.dp, top = 6.dp, bottom = 2.dp),
        fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = TextDim
    )
}

@Composable
private fun SetRow(
    icon: String,
    name: String,
    sub: String?,
    danger: Boolean = false,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(30.dp)
                .background(
                    if (danger) Red.copy(alpha = 0.12f) else SurfaceHigh,
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, color = if (danger) Red else TextMut, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
        Column(Modifier.weight(1f).padding(start = 11.dp)) {
            Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (danger) Red else TextMain)
            if (sub != null) Text(sub, fontSize = 10.sp, color = TextDim, modifier = Modifier.padding(top = 1.dp))
        }
        if (value != null) Text(value, fontSize = 11.sp, color = TextDim, fontWeight = FontWeight.Bold)
        else Text("›", color = TextDim, fontSize = 13.sp)
    }
}
