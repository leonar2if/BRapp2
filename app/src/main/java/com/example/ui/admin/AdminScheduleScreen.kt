package com.example.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodels.AdminViewModel
import com.example.utils.DateFormatter
import com.example.utils.SlotSchedule

/**
 * Pestaña "Horario" en Ajustes. Reemplaza el sistema de turnos fijo por uno
 * que el admin controla de verdad:
 * - Prender/apagar cualquier día de la semana (afecta calendario y estado del
 *   día en toda la app - misma fuente de datos que ya usan cliente y admin).
 * - Agregar/quitar turnos individuales, sin límite de cantidad ni duración
 *   fija: si quiere 10 turnos con más descanso, o turnos de 1h, puede.
 *
 * Por defecto se mantienen los 12 turnos oficiales actuales hasta que el
 * admin toque algo acá.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScheduleScreen(
    viewModel: AdminViewModel,
    onBackClick: () -> Unit
) {
    val workingDaysCsv by viewModel.workingDaysCsv.collectAsState()
    val activeSlots by viewModel.activeSlots.collectAsState()

    var selectedDays by remember(workingDaysCsv) {
        mutableStateOf(workingDaysCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet())
    }
    var slots by remember(activeSlots) { mutableStateOf(activeSlots.toMutableList()) }
    var newSlotTime by remember { mutableStateOf("") }
    var slotError by remember { mutableStateOf<String?>(null) }

    val hasChanges = selectedDays != workingDaysCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet() ||
        slots != activeSlots

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Horario") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        selectedDays = mutableSetOf("MON", "TUE", "WED", "THU", "FRI")
                        slots = SlotSchedule.DEFAULT_SLOTS.toMutableList()
                        slotError = null
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Restaurar")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Text(
                    "Días laborables",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Si apagás un día, el calendario y las reservas de clientes lo reflejan al instante en toda la app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Column {
                    SlotSchedule.ALL_DAY_CODES_IN_ORDER.forEach { code ->
                        val checked = code in selectedDays
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(SlotSchedule.dayLabel(code), style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = checked,
                                onCheckedChange = { isOn ->
                                    selectedDays = (if (isOn) selectedDays + code else selectedDays - code).toMutableSet()
                                }
                            )
                        }
                    }
                    if (selectedDays.isEmpty()) {
                        Text(
                            "Tiene que quedar al menos un día laborable.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                Text(
                    "Turnos del día (${slots.size})",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Agregá o quitá los turnos que quiera: cantidad, horarios y descansos, todo libre. No hay un número fijo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (slots.isEmpty()) {
                    Text("Sin turnos configurados.", color = MaterialTheme.colorScheme.error)
                } else {
                    SlotSchedule.sortSlots(slots).forEach { slot ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${DateFormatter.formatTimeForDisplay(slot)}  ($slot)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(onClick = { slots = (slots - slot).toMutableList() }) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar turno $slot", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSlotTime,
                        onValueChange = {
                            newSlotTime = it.filter { c -> c.isDigit() || c == ':' }.take(5)
                            slotError = null
                        },
                        label = { Text("Nuevo turno (HH:mm, ej. 09:15)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        isError = slotError != null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        val timeRegex = Regex("^([01][0-9]|2[0-3]):[0-5][0-9]$")
                        when {
                            !timeRegex.matches(newSlotTime) -> slotError = "Formato inválido"
                            newSlotTime in slots -> slotError = "Ese turno ya existe"
                            else -> {
                                slots = SlotSchedule.sortSlots(slots + newSlotTime).toMutableList()
                                newSlotTime = ""
                                slotError = null
                            }
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar turno")
                    }
                }
                if (slotError != null) {
                    Text(slotError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.saveWorkingDays(selectedDays)
                        viewModel.saveActiveSlots(slots)
                        onBackClick()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = hasChanges && selectedDays.isNotEmpty() && slots.isNotEmpty()
                ) {
                    Text("Guardar horario", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
