package com.wenbo.bookkeeping.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenbo.bookkeeping.data.backup.BackupManager
import com.wenbo.bookkeeping.di.AppContainer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val busy: Boolean = false,
    val pendingImportRows: List<com.wenbo.bookkeeping.data.backup.BackupRow>? = null, // 二次确认前暂存
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val backupManager: BackupManager = container.backupManager

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    /** 导出文件默认名：bookkeeping-backup-yyyyMMdd.csv。 */
    fun suggestFileName(): String =
        "bookkeeping-backup-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv"

    fun exportTo(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val csv = withContext(Dispatchers.IO) { backupManager.export() }
                val ok = withContext(Dispatchers.IO) {
                    container.openBackupOutput(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                        true
                    } ?: false
                }
                _events.send(if (ok) "导出成功" else "导出失败：无法写入文件")
            } catch (e: Exception) {
                _events.send("导出失败：${e.message}")
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    /** 读入用户选择的文件，解析成功后进入二次确认。 */
    fun requestImportFrom(uri: Uri) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val text = withContext(Dispatchers.IO) {
                    container.openBackupInput(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: ""
                }
                val result = com.wenbo.bookkeeping.data.backup.CsvCodec.decode(text)
                if (result.rows.isEmpty()) {
                    _events.send(if (result.skipped > 0) "文件无法识别（$result.skipped 行无效）" else "文件为空")
                } else {
                    _state.update { it.copy(pendingImportRows = result.rows) }
                }
            } catch (e: Exception) {
                _events.send("读取失败：${e.message}")
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

    /** 确认后整体替换导入。 */
    fun confirmImport() {
        val rows = _state.value.pendingImportRows ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val summary = backupManager.importReplace(rows)
                _events.send(summary)
            } catch (e: Exception) {
                _events.send("导入失败：${e.message}")
            } finally {
                _state.update { it.copy(busy = false, pendingImportRows = null) }
            }
        }
    }

    fun cancelImport() {
        _state.update { it.copy(pendingImportRows = null) }
    }
}
