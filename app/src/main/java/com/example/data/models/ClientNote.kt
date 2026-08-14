package com.example.data.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Nota simple del administrador sobre un cliente (secciones 4/5/6). Se guarda
 * localmente en Room de inmediato y se sube a Supabase al cerrar sesión. Las
 * notas SE ACUMULAN - nunca se reemplazan ni se borran al agregar una nueva.
 */
@Entity(tableName = "client_notes_local")
data class ClientNoteEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    @ColumnInfo(name = "client_phone") val clientPhone: String,
    @ColumnInfo(name = "client_name") val clientName: String,
    @ColumnInfo(name = "note") val note: String,
    @ColumnInfo(name = "created_at") val createdAt: Long, // epoch millis
    @ColumnInfo(name = "synced") val synced: Boolean = false
)
