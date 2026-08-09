package com.example.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
@Entity(tableName = "cached_services")
data class Service(
    @PrimaryKey
    @ColumnInfo(name = "id") @Json(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "name") @Json(name = "name") val name: String = "",
    @ColumnInfo(name = "duration_minutes") @Json(name = "duration_minutes") val durationMinutes: Int = 30,
    @ColumnInfo(name = "duration_slots") @Json(name = "duration_slots") val durationSlots: Int = 1, // 1=30min, 2=60min
    @ColumnInfo(name = "price") @Json(name = "price") val price: Double = 0.0,
    @ColumnInfo(name = "is_active") @Json(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") @Json(name = "created_at") val createdAt: String? = null
)
