package com.example.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.serialization.Serializable

@Serializable
@JsonClass(generateAdapter = true)
data class Appointment(
    @Json(name = "id") val id: Long = 0L,
    @Json(name = "client_id") val clientId: String = "",
    @Json(name = "service_id") val serviceId: Long = 0L,
    @Json(name = "ticket_number") val ticketNumber: Int = 1,
    @Json(name = "full_name") val fullName: String = "",
    @Json(name = "last_name1") val lastName1: String? = null,
    @Json(name = "last_name2") val lastName2: String? = null,
    @Json(name = "phone") val phone: String = "",
    @Json(name = "is_annexed") val isAnnexed: Boolean = false,
    @Json(name = "main_client_id") val mainClientId: String? = null,
    @Json(name = "appointment_date") val appointmentDate: String = "", // YYYY-MM-DD
    @Json(name = "appointment_time") val appointmentTime: String = "", // HH:mm
    @Json(name = "status") val status: String = "confirmed", // 'confirmed', 'attended', 'canceled', 'in_progress'
    @Json(name = "created_by_admin") val createdByAdmin: Boolean = false,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "is_rescheduled") val isRescheduled: Boolean = false,
    @Json(name = "original_appointment_id") val originalAppointmentId: Long? = null,
    @Json(name = "canceled_by") val canceledBy: String? = null,
    @Json(name = "canceled_at") val canceledAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)
