package com.example.service

import com.example.data.models.BlockedSlot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gestiona los turnos bloqueados por el administrador (botón ⊘ "Libre el
 * resto del día", sección 8 del prompt maestro). Sigue el mismo patrón que
 * el resto de servicios de la app (AppointmentService, ProductService).
 */
class BlockedSlotService {
    private val api = SupabaseClient.api

    suspend fun getBlockedSlotsByDate(date: String): List<BlockedSlot> = withContext(Dispatchers.IO) {
        try {
            api.getBlockedSlotsByDate("eq.$date")
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Bloquea todos los `times` indicados para `date`. Usado tanto por
     * "Desde ahora" (todos los turnos restantes) como "Desde un turno"
     * (turno elegido en adelante).
     */
    suspend fun blockSlots(date: String, times: List<String>, blockedBy: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val slots = times.map { time ->
                BlockedSlot(blockDate = date, blockTime = time, blockedBy = blockedBy)
            }
            api.createBlockedSlots(slots)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Revierte el bloqueo del día completo (por si el admin se equivoca). */
    suspend fun clearDayBlocks(date: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            api.deleteBlockedSlotsByDate("eq.$date")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
