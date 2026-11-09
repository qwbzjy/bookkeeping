package com.wenbo.bookkeeping.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        SettingsEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class BookkeepingDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun settingsDao(): SettingsDao

    companion object {

        /** v2：新增 settings 键值表（大模型配置）。显式迁移，避免破坏用户已有数据。 */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `settings` " +
                        "(`key` TEXT NOT NULL, `value` TEXT NOT NULL, PRIMARY KEY(`key`))"
                )
            }
        }

        fun create(context: Context): BookkeepingDatabase {
            // 兜底：个别设备/模拟器上 databases 目录不会自动创建，Room 打开时直接抛 SQLITE_CANTOPEN
            context.getDatabasePath("bookkeeping.db").parentFile?.mkdirs()
            return Room.databaseBuilder(context, BookkeepingDatabase::class.java, "bookkeeping.db")
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration(dropAllTables = true) // 兜底策略，发布前移除
                .build()
        }
    }
}
