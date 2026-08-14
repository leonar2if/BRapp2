package com.example.data.repository

import com.example.data.database.ClientNoteDao
import com.example.data.models.ClientNoteEntity
import com.example.service.SupabaseClient
import kotlinx.coroutines.flow.Flow

/**
 * Notas simples del administrador sobre un cliente. Sin estados de
 * sincronización complejos: cada nota se guarda localmente al crearla y queda
 * marcada synced=false hasta que se sube. uploadPendingNotes() se llama al
 * cerrar sesión (ver AdminViewModel) - si falla (sin conexión), las notas
 * simplemente quedan como estaban, listas para reintentar la próxima vez.
 */
class ClientNoteRepository(private val dao: ClientNoteDao) {

    fun getNotesForClient(phone: String): Flow<List<ClientNoteEntity>> =
        dao.getNotesForClient(phone)

    suspend fun addNote(clientPhone: String, clientName: String, text: String) {
        if (text.isBlank()) return
        dao.insert(
            ClientNoteEntity(
                clientPhone = clientPhone,
                clientName = clientName,
                note = text.trim(),
                createdAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Sube todas las notas pendientes a Supabase. Devuelve true si no quedó
     * ninguna pendiente al terminar (todas subidas u originalmente no había
     * ninguna). Si algo falla a mitad de camino, las notas ya subidas quedan
     * marcadas synced y las que fallaron simplemente se reintentan la próxima vez.
     */
    suspend fun uploadPendingNotes(adminId: String): Boolean {
        val pending = dao.getUnsynced()
        if (pending.isEmpty()) return true

        val uploadedIds = mutableListOf<Long>()
        for (n in pending) {
            try {
                SupabaseClient.api.createClientNote(
                    mapOf(
                        "client_phone" to n.clientPhone,
                        "client_name" to n.clientName,
                        "note" to n.note,
                        "admin_id" to adminId
                    )
                )
                uploadedIds.add(n.id)
            } catch (e: Exception) {
                // Sin conexión / error de red: esta nota (y las que falten) se
                // quedan pendientes, no se pierden ni se borran.
            }
        }
        if (uploadedIds.isNotEmpty()) {
            dao.markSynced(uploadedIds)
        }
        return uploadedIds.size == pending.size
    }
}
