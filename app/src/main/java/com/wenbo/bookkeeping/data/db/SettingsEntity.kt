package com.wenbo.bookkeeping.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 通用键值配置表：当前存大模型服务配置（provider/baseUrl/apiKey/model）。 */
@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val key: String,
    val value: String,
)
