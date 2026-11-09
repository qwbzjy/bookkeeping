package com.wenbo.bookkeeping.feature.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenbo.bookkeeping.core.amount.AmountFormat
import com.wenbo.bookkeeping.data.db.TransactionEntity
import com.wenbo.bookkeeping.data.llm.LlmClient
import com.wenbo.bookkeeping.data.llm.ReceiptParse
import com.wenbo.bookkeeping.di.AppContainer
import com.wenbo.bookkeeping.domain.model.TransactionType
import com.wenbo.bookkeeping.domain.model.TxCategory
import java.time.LocalDate
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 记一笔表单页状态。txId == -1L 为新建，否则为编辑该笔交易。 */
data class EditUiState(
    val txId: Long = -1L,
    val type: TransactionType = TransactionType.EXPENSE,
    val category: TxCategory? = null,
    val amountText: String = "", // 元字符串，经 AmountFormat 过滤
    val dateEpochDay: Long = LocalDate.now().toEpochDay(),
    val note: String = "",
    val canSave: Boolean = false,
    val saved: Boolean = false, // 保存成功信号，Screen 观察后 popBackStack
    val scanning: Boolean = false, // 票据识别进行中
    val scannedItems: List<ReceiptParse> = emptyList(), // 识别出的明细，待确认
) {
    val isEdit: Boolean get() = txId != -1L
}

class EditViewModel(
    private val container: AppContainer,
    private val txId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(EditUiState(txId = txId))
    val state = _state.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        if (txId != -1L) {
            viewModelScope.launch {
                val tx = container.database.transactionDao().getById(txId)
                if (tx != null) {
                    _state.update {
                        it.copy(
                            type = tx.type,
                            category = TxCategory.byNameOrDefault(tx.category, tx.type),
                            amountText = AmountFormat.formatMinorToYuan(tx.amountMinor),
                            dateEpochDay = tx.date,
                            note = tx.note,
                            canSave = true,
                        )
                    }
                } else {
                    _events.send("记录不存在")
                    _state.update { it.copy(saved = true) }
                }
            }
        }
    }

    fun selectType(type: TransactionType) {
        _state.update {
            // 切换收支时清空已选分类（分类列表会整体换成另一类型）
            it.copy(type = type, category = null, canSave = false)
        }
    }

    fun selectCategory(category: TxCategory) {
        _state.update { s ->
            val next = s.copy(category = category)
            next.copy(canSave = refreshCanSave(next))
        }
    }

    fun updateAmount(raw: String) {
        _state.update { s ->
            val sanitized = AmountFormat.sanitizeInput(raw)
            val next = s.copy(amountText = sanitized)
            next.copy(canSave = refreshCanSave(next))
        }
    }

    fun updateDate(epochDay: Long) {
        _state.update { it.copy(dateEpochDay = epochDay) }
    }

    fun updateNote(raw: String) {
        _state.update { it.copy(note = raw.take(50)) }
    }

    fun save() {
        val s = _state.value
        val minor = AmountFormat.parseYuanToMinor(s.amountText) ?: 0L
        val category = s.category ?: return
        if (minor <= 0L) return
        viewModelScope.launch {
            val entity = TransactionEntity(
                id = if (s.isEdit) s.txId else 0L,
                type = s.type,
                amountMinor = minor,
                category = category.name,
                date = s.dateEpochDay,
                note = s.note,
            )
            val dao = container.database.transactionDao()
            if (s.isEdit) dao.update(entity) else dao.insert(entity)
            _state.update { it.copy(saved = true) }
        }
    }

    /**
     * 票据图片识别：相册选图 → 压缩 base64 → 调大模型 → 弹确认对话框逐条保存。
     * 识别结果只入确认队列，用户勾选后才落库。
     */
    fun scanReceipt(uri: android.net.Uri) {
        if (_state.value.scanning) return
        viewModelScope.launch {
            _state.update { it.copy(scanning = true) }
            try {
                val config = container.llmSettings.load()
                if (!config.isConfigured) {
                    _events.send("请先在「设置」中配置大模型服务")
                    return@launch
                }
                val image = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    container.readImageBase64(uri)
                }
                if (image == null) {
                    _events.send("无法读取图片")
                    return@launch
                }
                val items = LlmClient(config).analyzeReceipt(image.first, image.second)
                _state.update { it.copy(scannedItems = items) }
            } catch (e: Exception) {
                _events.send("识别失败：${e.message}")
            } finally {
                _state.update { it.copy(scanning = false) }
            }
        }
    }

    /** 关闭确认对话框，放弃本次识别结果。 */
    fun clearScannedItems() {
        _state.update { it.copy(scannedItems = emptyList()) }
    }

    /** 批量保存勾选的明细，成功后关闭页面（明细页自动刷新）。 */
    fun saveScannedItems(items: List<ReceiptParse>) {
        if (items.isEmpty()) return
        viewModelScope.launch {
            try {
                val entities = items.map {
                    TransactionEntity(
                        type = it.type,
                        amountMinor = it.amountMinor,
                        category = it.category.name,
                        date = it.dateEpochDay,
                        note = it.note,
                    )
                }
                val dao = container.database.transactionDao()
                entities.forEach { dao.insert(it) }
                _state.update { it.copy(saved = true) }
            } catch (e: Exception) {
                _events.send("保存失败：${e.message}")
            }
        }
    }

    private fun refreshCanSave(state: EditUiState): Boolean {
        val minor = AmountFormat.parseYuanToMinor(state.amountText) ?: 0L
        return state.category != null && minor > 0L
    }
}
