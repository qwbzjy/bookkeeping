package com.wenbo.bookkeeping.di

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.staticCompositionLocalOf
import com.wenbo.bookkeeping.data.backup.BackupManager
import com.wenbo.bookkeeping.data.db.BookkeepingDatabase
import com.wenbo.bookkeeping.data.llm.ImageCodec
import com.wenbo.bookkeeping.data.llm.LlmSettingsStore
import java.io.InputStream
import java.io.OutputStream

/** 手动 DI 容器：依赖是少量封闭集合，全部单例，在 Application 中构造一次。 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: BookkeepingDatabase = BookkeepingDatabase.create(appContext)
    val backupManager = BackupManager(database)
    val llmSettings = LlmSettingsStore(database.settingsDao())

    /** SAF 流收口：备份读写的 uri 授权在会话内一次性消费，不做持久化。 */
    fun openBackupInput(uri: Uri): InputStream? = appContext.contentResolver.openInputStream(uri)

    fun openBackupOutput(uri: Uri): OutputStream? =
        appContext.contentResolver.openOutputStream(uri, "wt")

    /** 相册图片 → (base64, mediaType)，供票据识别。 */
    fun readImageBase64(uri: Uri): Pair<String, String>? =
        ImageCodec.readBase64(appContext, uri)
}

/** 在 MainActivity setContent 时提供，Composable 层通过它获取容器。 */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

/** 备份文件 MIME 类型。 */
const val BACKUP_MIME = "text/csv"
