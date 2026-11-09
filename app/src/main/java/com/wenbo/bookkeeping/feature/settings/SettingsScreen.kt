package com.wenbo.bookkeeping.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.wenbo.bookkeeping.data.llm.LlmProvider
import com.wenbo.bookkeeping.di.BACKUP_MIME
import com.wenbo.bookkeeping.di.LocalAppContainer
import com.wenbo.bookkeeping.ui.components.ConfirmDialog
import com.wenbo.bookkeeping.ui.theme.TextSecondary

/**
 * 设置页：CSV 导出 / 导入（覆盖确认）+ 版本信息。
 * SAF 选择器由 ActivityResultContracts 提供，uri 流经 AppContainer 收口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val container = LocalAppContainer.current
    val viewModel: SettingsViewModel = viewModel { SettingsViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { snackbarHostState.showSnackbar(it) }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(BACKUP_MIME),
    ) { uri -> uri?.let(viewModel::exportTo) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::requestImportFrom) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "大模型识别",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
            )
            Text(
                text = "配置后在「记一笔」页可从票据图片自动识别分类与金额",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = state.llmProvider == LlmProvider.OPENAI,
                    onClick = { viewModel.updateLlmProvider(LlmProvider.OPENAI) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                ) { Text("OpenAI 兼容") }
                SegmentedButton(
                    selected = state.llmProvider == LlmProvider.ANTHROPIC,
                    onClick = { viewModel.updateLlmProvider(LlmProvider.ANTHROPIC) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                ) { Text("Anthropic") }
            }
            OutlinedTextField(
                value = state.llmBaseUrl,
                onValueChange = viewModel::updateLlmBaseUrl,
                label = { Text("接口地址（留空用官方默认）") },
                placeholder = {
                    Text(
                        if (state.llmProvider == LlmProvider.OPENAI) {
                            "https://api.openai.com/v1"
                        } else {
                            "https://api.anthropic.com"
                        },
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.llmApiKey,
                onValueChange = viewModel::updateLlmApiKey,
                label = { Text("API Key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.llmModel,
                onValueChange = viewModel::updateLlmModel,
                label = { Text("模型名") },
                placeholder = {
                    Text(
                        if (state.llmProvider == LlmProvider.OPENAI) {
                            "如 gpt-4o-mini"
                        } else {
                            "如 claude-sonnet-4-5"
                        },
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = viewModel::saveLlmConfig,
                    modifier = Modifier.weight(1f),
                ) { Text("保存配置") }
                OutlinedButton(
                    onClick = viewModel::testLlmConnection,
                    enabled = !state.busy,
                    modifier = Modifier.weight(1f),
                ) { Text(if (state.busy) "测试中…" else "测试连接") }
            }

            HorizontalDivider()

            Text(
                text = "备份",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
            )
            Button(
                onClick = { exportLauncher.launch(viewModel.suggestFileName()) },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("导出 CSV 备份") }
            OutlinedButton(
                onClick = {
                    importLauncher.launch(arrayOf(BACKUP_MIME, "text/comma-separated-values", "text/plain"))
                },
                enabled = !state.busy,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("从 CSV 导入（覆盖现有数据）") }

            HorizontalDivider()

            Text(
                text = "说明",
                style = MaterialTheme.typography.titleSmall,
                color = TextSecondary,
            )
            Text(
                text = "· 导出包含全部交易与预算设置，可用表格软件打开查看\n" +
                    "· 导入会整体替换现有数据，请先导出当前数据再操作\n" +
                    "· 所有数据仅保存在本机",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "记账本 v1.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                )
            }
        }
    }

    state.pendingImportRows?.let { rows ->
        ConfirmDialog(
            title = "确认导入？",
            text = "将导入 ${rows.size} 行数据，并覆盖现有全部记录。此操作不可撤销。",
            confirmLabel = "覆盖导入",
            onConfirm = viewModel::confirmImport,
            onDismiss = viewModel::cancelImport,
        )
    }
}
