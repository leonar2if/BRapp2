package com.example.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
@Entity(tableName = "cached_settings")
data class Settings(
    @PrimaryKey
    @ColumnInfo(name = "id") @Json(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "key") @Json(name = "key") val key: String = "",
    @ColumnInfo(name = "value") @Json(name = "value") val value: String = "",
    @ColumnInfo(name = "updated_at") @Json(name = "updated_at") val updatedAt: String? = null
)
