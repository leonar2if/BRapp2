package com.example.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
@Entity(tableName = "cached_products")
data class Product(
    @PrimaryKey
    @ColumnInfo(name = "id") @Json(name = "id") val id: Long = 0L,
    @ColumnInfo(name = "name") @Json(name = "name") val name: String = "",
    @ColumnInfo(name = "description") @Json(name = "description") val description: String = "",
    @ColumnInfo(name = "price") @Json(name = "price") val price: Double = 0.0,
    @ColumnInfo(name = "currency") @Json(name = "currency") val currency: String = "MN", // "MN" o "USD"
    @ColumnInfo(name = "image_url1") @Json(name = "image_url1") val imageUrl1: String? = null,
    @ColumnInfo(name = "image_url2") @Json(name = "image_url2") val imageUrl2: String? = null,
    @ColumnInfo(name = "is_active") @Json(name = "is_active") val isActive: Boolean = true,
    @ColumnInfo(name = "created_at") @Json(name = "created_at") val createdAt: String? = null
)
