package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarViewDay
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.models.Service
import com.example.ui.client.CalendarScreen
import com.example.ui.components.AppointmentCard
import com.example.ui.components.PhoneField
import com.example.ui.components.TimeSlotWidget
import com.example.ui.viewmodels.AdminViewModel
import com.example.utils.DateFormatter
import com.example.utils.SlotSchedule

@Composable
fun AdminCalendarScreen(
    viewModel: AdminViewModel
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dateAppts by viewModel.selectedDateAppointments.collectAsState()
    val blockedSlots by viewModel.selectedDateBlockedSlots.collectAsState()
    val services by viewModel.allServices.collectAsState()
    val workingDaysCsv by viewModel.workingDaysCsv.collectAsState()
    val context = LocalContext.current

    var showDayOffDialog by remember { mutableStateOf(false) }
    var sendNotification by remember { mutableStateOf(true) }
    var clientStyleView by remember { mutableStateOf(true) }
    var showQuickBookingFor by remember { mutableStateOf<String?>(null) }

    val workingDays = remember(workingDaysCsv) {
        SlotSchedule.parseWorkingDaysCsv(workingDaysCsv)
    }

    val blockedTimes = blockedSlots
        .map { it.blockTime.take(5) }
        .toSet()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Agenda",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.weight(1f, fill = false),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            showDayOffDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(
                            horizontal = 12.dp,
                            vertical = 8.dp
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )

                        Spacer(modifier = Modifier.width(4.dp))

                        Text(
                            text = "Día Libre",
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = clientStyleView,
                        onClick = {
                            clientStyleView = true
                        },
                        label = {
                            Text(
                                text = "Vista turnos",
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarViewDay,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )

                    FilterChip(
                        selected = !clientStyleView,
                        onClick = {
                            clientStyleView = false
                        },
                        label = {
                            Text(
                                text = "Vista administrativa",
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.ViewList,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                CalendarScreen(
                    selectedDate = selectedDate,
                    onDateSelected = { date ->
                        viewModel.selectDate(date)
                    },
                    getDayStatus = { dateStr ->
                        val today = DateFormatter.getTodayDateString()

                        if (dateStr < today) {
                            "gray"
                        } else if (!SlotSchedule.isWorkingDay(
                                dateStr,
                                workingDays
                            )
                        ) {
                            "gray"
                        } else {
                            "green"
                        }
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Reservas para ${
                        DateFormatter.formatDateForDisplay(selectedDate)
                    } (${dateAppts.count { it.status != "canceled" }}):",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    )
                )
            }

            if (clientStyleView) {
                val activeSlots by viewModel.activeSlots.collectAsState()
                items(activeSlots) { slot ->
                    // Igual que ClientViewModel.isSlotOccupied: una cita ocupa
                    // service.durationSlots turnos consecutivos desde su inicio, no
                    // solo el turno en el que empieza (sección 3.3 / 12).
                    val apptForSlot = dateAppts.firstOrNull { appt ->
                        if (appt.status == "canceled") return@firstOrNull false
                        val durationSlots = services.find { it.id == appt.serviceId }?.durationSlots ?: 1
                        val range = SlotSchedule.slotRangeFor(appt.appointmentTime.take(5), durationSlots, activeSlots)
                            ?: listOf(appt.appointmentTime.take(5))
                        slot in range
                    }

                    TimeSlotWidget(
                        time = slot,
                        isOccupied = apptForSlot != null,
                        isBlocked = apptForSlot == null &&
                            slot in blockedTimes,
                        onClick = {
                            if (
                                apptForSlot == null &&
                                slot !in blockedTimes
                            ) {
                                showQuickBookingFor = slot
                            }
                        }
                    )
                }
            } else {
                if (dateAppts.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No hay reservas registradas en este día.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(dateAppts) { appt ->
                        val sName = services
                            .find { it.id == appt.serviceId }
                            ?.name
                            ?: "Barbería"

                        AppointmentCard(
                            appointment = appt,
                            serviceName = sName,
                            isAdmin = true,
                            onCallClick = {
                                val cleanPhone = appt.phone
                                    .replace(" ", "")
                                    .replace("+", "")

                                val intent = Intent(
                                    Intent.ACTION_DIAL,
                                    Uri.parse("tel:$cleanPhone")
                                )

                                context.startActivity(intent)
                            },
                            onCancelClick = {
                                viewModel.cancelAppointment(appt.id)
                            },
                            onAttendClick = {
                                viewModel.markAsAttended(appt.id)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showDayOffDialog) {
        val activeApptsCount = dateAppts.count {
            it.status == "confirmed" ||
                it.status == "in_progress"
        }

        AlertDialog(
            onDismissRequest = {
                showDayOffDialog = false
            },
            title = {
                Text(
                    text = "Marcar Día Libre",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            },
            text = {
                Column {
                    Text(
                        text = "¿Deseas marcar el día ${
                            DateFormatter.formatDateForDisplay(selectedDate)
                        } como día libre/inactivo?"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (activeApptsCount > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "⚠️ Hay $activeApptsCount reservas confirmadas para este día que serán canceladas.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = sendNotification,
                                onCheckedChange = {
                                    sendNotification = it
                                }
                            )

                            Text(
                                text = "Enviar notificación de cancelación a los clientes",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markDayOff(
                            selectedDate,
                            sendNotification
                        )
                        showDayOffDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Confirmar y Cancelar Citas")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDayOffDialog = false
                    }
                ) {
                    Text("Volver")
                }
            }
        )
    }

    val quickBookingTime = showQuickBookingFor

    if (quickBookingTime != null) {
        QuickAdminBookingDialog(
            date = selectedDate,
            time = quickBookingTime,
            services = services,
            onDismiss = {
                showQuickBookingFor = null
            },
            onConfirm = { service, name, phone ->
                viewModel.createQuickAdminAppointment(
                    selectedDate,
                    quickBookingTime,
                    service,
                    name,
                    phone
                )
                showQuickBookingFor = null
            }
        )
    }
}

@Composable
private fun QuickAdminBookingDialog(
    date: String,
    time: String,
    services: List<Service>,
    onDismiss: () -> Unit,
    onConfirm: (
        service: Service,
        name: String,
        phone: String
    ) -> Unit
) {
    var selectedService by remember {
        mutableStateOf<Service?>(services.firstOrNull())
    }

    var name by remember {
        mutableStateOf("")
    }

    var phone by remember {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Reservar $time — ${
                    DateFormatter.formatDateForDisplay(date)
                }",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
        },
        text = {
            Column {
                Text(
                    text = "Servicio",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                LazyColumn(
                    modifier = Modifier.heightIn(max = 160.dp)
                ) {
                    items(services) { service ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedService?.id == service.id,
                                onClick = {
                                    selectedService = service
                                }
                            )

                            Text(
                                text = "${service.name} (${service.durationSlots} turno${
                                    if (service.durationSlots > 1) "s" else ""
                                })",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Datos del cliente (opcional)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    label = {
                        Text("Nombre (opcional)")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                PhoneField(
                    value = phone,
                    onValueChange = {
                        phone = it
                    },
                    label = "Teléfono (opcional)",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedService?.let {
                        onConfirm(
                            it,
                            name,
                            phone
                        )
                    }
                },
                enabled = selectedService != null
            ) {
                Text("Reservar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
        }
    )
}