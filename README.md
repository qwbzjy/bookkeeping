# 记账本（bookkeeping）

一款纯本地化的个人记账 Android 应用：记录日常收入与支出，查看统计图表，管理每月预算。数据仅保存在本机，无需登录、无网络权限。

## 功能

### ✍️ 记一笔
- 支出 / 收入一键切换，18 个预置分类（emoji 图标）
- 金额以「分」为单位存储（Long），杜绝浮点精度问题
- 日期可选（默认今天），备注选填（50 字内）
- 支持编辑已有记录、长按删除

### 📋 明细
- 按月浏览，按天分组的时间线，每日小计（支 / 收）
- 月度收支结余汇总卡
- 左右切换月份，跨年自然衔接

### 📊 统计
- 月 / 年两种维度切换
- 收支结余三卡汇总
- 分类占比环形图 + 图例（金额、百分比），支出 / 收入维度可切换
- 近 6 个月收支趋势柱状图（无记录月份自动补零）
- 所有图表为 Compose Canvas 自绘，零第三方图表依赖

### 🎯 预算
- 每月常设的分类预算上限 + 可选总预算
- 进度条实时显示当月已用比例，超支满格变红
- 清空并保存即取消该分类预算

### 💾 备份
- 导出 CSV（UTF-8 带 BOM，Excel 打开中文不乱码；RFC 4180 转义）
- 导入为整体替换，带二次确认；坏行自动跳过并计数，未知分类降级为「其他」
- 备份内容包含全部交易与预算设置

## 技术栈

| 项 | 选型 |
|---|---|
| 语言 | Kotlin 2.3.21（JVM 17） |
| UI | Jetpack Compose（BOM 2026.02.00）+ Material 3，仅浅色主题 |
| 数据库 | Room 2.8.5（KSP 编译，schema 导出于 `app/schemas/`） |
| 导航 | Navigation Compose type-safe 路由（`@Serializable` route） |
| 架构 | MVVM（StateFlow UiState + Channel 一次性事件）+ 手写 DI（AppContainer + CompositionLocal） |
| SDK | minSdk 26 / targetSdk 35 / compileSdk 36.1 |
| 权限 | 无（数据全部仅存本机） |

## 构建与运行

```bash
# 指定 JDK 17+（本机示例为 JDK 21）
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home

./gradlew assembleDebug   # 构建 Debug APK（输出于 app/build/outputs/apk/debug/）
./gradlew test            # 运行单元测试（50 个，全部为纯函数测试）
./gradlew assembleRelease # Release 构建（不混淆）
```

`local.properties` 中的 `sdk.dir` 需指向本机 Android SDK。

安装到已连接设备：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> 注：当前 release 构建临时关闭了 lintVital（`app/build.gradle.kts` 中 `lint { checkReleaseBuilds = false }`，因构建机访问 Maven 仓库的 TLS 握手不稳定）。网络恢复后可移除该配置。

## 数据模型

- **transactions**：`id` / `type`(EXPENSE\|INCOME) / `amount_minor`(分) / `category` / `date`(epochDay) / `note` / `created_at`，`date` 建索引
- **budgets**：`category`(主键，`__all__` 为总预算) / `amount_minor`(每月上限，不绑定年月)
- 分类为硬编码枚举 `TxCategory`（支出 12 + 收入 6），Room 存枚举 name 字符串

## 项目结构

```
app/src/main/java/com/wenbo/bookkeeping/
├── BookkeepingApp.kt / MainActivity.kt   # 入口
├── di/AppContainer.kt                    # 手写 DI 容器 + SAF 流收口
├── navigation/                            # type-safe 路由 + 底栏骨架（4 Tab + FAB）
├── core/
│   ├── amount/AmountFormat.kt             # 分↔元换算、输入过滤、金额缩写（纯函数）
│   └── time/MonthUtils.kt                 # epochDay 换算、月范围、标签（纯函数）
├── domain/
│   ├── model/Models.kt                    # TransactionType、TxCategory 预置分类
│   ├── StatsAggregator.kt                 # 统计聚合（纯函数，可单测）
│   └── ChartScale.kt                      # 柱状图 y 轴刻度归整（纯函数）
├── data/
│   ├── db/                                # Room：Entity / Dao / Database
│   └── backup/                            # CsvCodec 编解码 + BackupManager
├── ui/
│   ├── theme/                             # 语义化颜色 + 仅浅色主题
│   └── components/                        # DonutChart / MonthlyBarChart / MonthSwitcher 等
└── feature/
    ├── home / edit / stats / budget / settings   # 每屏 XxxScreen.kt + XxxViewModel.kt
app/src/test/                              # 纯函数单测（金额/日期/聚合/刻度/CSV）
```

## 设计要点

- **金额分存**：全链路 Long（分），仅在 UI 输入/展示层做字符串转换，转换逻辑收口在 `AmountFormat` 且可单测
- **日期存 epochDay**：月查询即两个数字的闭区间，无时区歧义
- **统计不写 SQL**：数据量小，全量拉取后内存聚合（`StatsAggregator` 纯函数），聚合逻辑可 JUnit 直测
- **CSV 容错**：解析为逐字符状态机（正确处理引号内逗号/换行/翻倍引号），单行脏数据不致整体失败
- **SAF 收口**：备份读写 uri 经 `AppContainer.openBackupInput/Output` 统一处理，会话内一次性消费
