package com.wenbo.bookkeeping.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenbo.bookkeeping.data.backup.BackupManager
import com.wenbo.bookkeeping.data.llm.LlmClient
import com.wenbo.bookkeeping.data.llm.LlmConfig
import com.wenbo.bookkeeping.data.llm.LlmProvider
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

/** 设置页 header 编辑行的 UI 模型（name/value 都可即时改，点保存才落库）。 */
data class LlmHeaderUi(val name: String = "", val value: String = "")

data class SettingsUiState(
    val busy: Boolean = false,
    val pendingImportRows: List<com.wenbo.bookkeeping.data.backup.BackupRow>? = null, // 二次确认前暂存
    // 大模型识别配置（表单即时编辑，点保存才落库）
    val llmProvider: LlmProvider = LlmProvider.OPENAI,
    val llmBaseUrl: String = "",
    val llmApiKey: String = "",
    val llmModel: String = "",
    val llmHeaders: List<LlmHeaderUi> = emptyList(),
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val backupManager: BackupManager = container.backupManager
    private val llmSettings = container.llmSettings

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            val config = llmSettings.load()
            _state.update {
                it.copy(
                    llmProvider = config.provider,
                    llmBaseUrl = config.baseUrl,
                    llmApiKey = config.apiKey,
                    llmModel = config.model,
                    llmHeaders = config.customHeaders.map { (k, v) -> LlmHeaderUi(k, v) },
                )
            }
        }
    }

    /** 导出文件默认名：bookkeeping-backup-yyyyMMdd.csv。 */
    fun suggestFileName(): String =
        "bookkeeping-backup-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv"

    // ---- 大模型配置 ----

    fun updateLlmProvider(provider: LlmProvider) = _state.update { it.copy(llmProvider = provider) }
    fun updateLlmBaseUrl(text: String) = _state.update { it.copy(llmBaseUrl = text) }
    fun updateLlmApiKey(text: String) = _state.update { it.copy(llmApiKey = text.trim()) }
    fun updateLlmModel(text: String) = _state.update { it.copy(llmModel = text.trim()) }

    fun addLlmHeader() = _state.update { it.copy(llmHeaders = it.llmHeaders + LlmHeaderUi()) }

    fun removeLlmHeader(index: Int) =
        _state.update { it.copy(llmHeaders = it.llmHeaders.filterIndexed { i, _ -> i != index }) }

    fun updateLlmHeaderName(index: Int, text: String) =
        _state.update { s ->
            s.copy(llmHeaders = s.llmHeaders.mapIndexed { i, h ->
                if (i == index) h.copy(name = text.trim()) else h
            })
        }

    fun updateLlmHeaderValue(index: Int, text: String) =
        _state.update { s ->
            s.copy(llmHeaders = s.llmHeaders.mapIndexed { i, h ->
                if (i == index) h.copy(value = text) else h
            })
        }

    private fun buildConfig(s: SettingsUiState) = LlmConfig(
        provider = s.llmProvider,
        baseUrl = s.llmBaseUrl,
        apiKey = s.llmApiKey,
        model = s.llmModel,
        // 名字为空的行丢弃；保存时同名后者覆盖前者
        customHeaders = s.llmHeaders
            .filter { it.name.isNotBlank() }
            .associate { it.name to it.value.trim() },
    )

    /** 保存当前表单为大模型配置。 */
    fun saveLlmConfig() {
        viewModelScope.launch {
            llmSettings.save(buildConfig(_state.value))
            _events.send("配置已保存")
        }
    }

    /** 连通性测试：用当前表单配置（未保存也能测）发一条纯文本请求。 */
    fun testLlmConnection() {
        val config = buildConfig(_state.value)
        if (config.apiKey.isBlank() || config.model.isBlank()) {
            _events.trySend("请先填写 API Key 与模型名")
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            try {
                val reply = LlmClient(config).ping()
                _events.send("连接正常，模型回复：${reply.take(20)}")
            } catch (e: Exception) {
                _events.send("连接失败：${e.message}")
            } finally {
                _state.update { it.copy(busy = false) }
            }
        }
    }

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
