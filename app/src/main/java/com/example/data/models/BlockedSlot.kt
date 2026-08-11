package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

/**
 * Corresponde a la tabla `blocked_slots` (ver supabase_update_bloque1.sql).
 * Representa un turno bloqueado por el administrador (botón ⊘ "Libre el
 * resto del día") que NO tiene cliente ni servicio asociado.
 */
@Serializable
@JsonClass(generateAdapter = true)
data class BlockedSlot(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "block_date") val blockDate: String = "", // YYYY-MM-DD
    @Json(name = "block_time") val blockTime: String = "", // HH:mm
    @Json(name = "reason") val reason: String? = null,
    @Json(name = "blocked_by") val blockedBy: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)
