package com.wenbo.bookkeeping.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class BookkeepingDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        fun create(context: Context): BookkeepingDatabase {
            // 兜底：个别设备/模拟器上 databases 目录不会自动创建，Room 打开时直接抛 SQLITE_CANTOPEN
            context.getDatabasePath("bookkeeping.db").parentFile?.mkdirs()
            return Room.databaseBuilder(context, BookkeepingDatabase::class.java, "bookkeeping.db")
                .fallbackToDestructiveMigration(dropAllTables = true) // v1 阶段策略，发布前换显式 Migration
                .build()
        }
    }
}
