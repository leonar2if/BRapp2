package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class Profile(
    @Json(name = "id") val id: String = "",
    @Json(name = "phone") val phone: String = "",
    @Json(name = "full_name") val fullName: String = "",
    @Json(name = "role") val role: String = "client", // 'client' or 'admin'
    @Json(name = "birthday") val birthday: String? = null, // "yyyy-MM-dd"
    @Json(name = "visit_count") val visitCount: Int = 0,
    @Json(name = "no_show_count") val noShowCount: Int = 0,
    @Json(name = "created_at") val createdAt: String? = null
)