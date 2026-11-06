记账本（bookkeeping）
=====================

个人记账 Android 应用：记录日常收入与支出，查看统计图表，管理每月预算。

功能
----
- 明细：按月查看收支时间线，按天分组（含当日小计），点行编辑、长按删除
- 记一笔：收/支切换 + 18 个预置分类（emoji 图标）+ 金额/日期/备注，
  金额以"分"存储杜绝精度问题，支持编辑已有记录
- 统计：月/年切换，收支结余汇总，分类占比环形图 + 图例，近 6 个月收支趋势柱状图
  （图表为 Compose Canvas 自绘，无第三方依赖）
- 预算：每月常设的分类预算上限 + 总预算，进度条实时显示当月已用，超支变红
- 备份：导出/导入 CSV（含 BOM，Excel 打开中文不乱码），导入为整体替换、带二次确认

技术栈
------
- Kotlin 2.3.21（JVM 17）+ Jetpack Compose（BOM 2026.02.00）+ Material 3
- Room 2.8.5（KSP）本地数据库，schema 导出于 app/schemas/
- Navigation Compose type-safe 路由（@Serializable route）
- MVVM：StateFlow UiState + Channel 一次性事件；手写 DI（AppContainer + CompositionLocal）
- minSdk 26 / targetSdk 35 / compileSdk 36.1，仅浅色主题
- 数据全部仅存本机，无网络权限

目录结构
--------
app/src/main/java/com/wenbo/bookkeeping/
  BookkeepingApp.kt / MainActivity.kt    入口
  di/AppContainer.kt                     手写 DI 容器 + SAF 流收口
  navigation/                             type-safe 路由 + 底栏骨架（4 Tab + FAB）
  core/amount/AmountFormat.kt            分↔元换算、输入过滤、金额缩写（纯函数）
  core/time/MonthUtils.kt                epochDay 换算、月范围、标签（纯函数）
  domain/model/Models.kt                 TransactionType、TxCategory 预置分类
  domain/StatsAggregator.kt              统计聚合（纯函数，可单测）
  domain/ChartScale.kt                   y 轴刻度归整（纯函数）
  data/db/                               Room：TransactionEntity / BudgetEntity / Dao / Database
  data/backup/                           CSV 编解码（CsvCodec）+ 备份管理（BackupManager）
  ui/theme/                              颜色（语义命名）+ 主题（仅浅色）
  ui/components/                         DonutChart / MonthlyBarChart / MonthSwitcher 等
  feature/home|edit|stats|budget|settings/   每屏 XxxScreen.kt + XxxViewModel.kt
app/src/test/                            纯函数单测（金额/日期/聚合/刻度/CSV）

构建
----
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home
./gradlew assembleDebug        # 构建 APK（app/build/outputs/apk/debug/）
./gradlew test                 # 运行单元测试
./gradlew assembleRelease      # Release 构建（不混淆）

local.properties 中的 sdk.dir 指向 Android SDK（/Users/wenbo/Library/Android/sdk）。

数据模型
--------
- transactions：id / type(EXPENSE|INCOME) / amount_minor(分) / category / date(epochDay) / note / created_at
- budgets：category(主键，"__all__" 为总预算) / amount_minor(每月上限，不绑定年月)
- 分类为硬编码枚举 TxCategory（支出 12 + 收入 6），Room 存枚举 name 字符串
