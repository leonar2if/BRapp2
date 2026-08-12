package com.example.ui.client

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AppointmentCard
import com.example.ui.components.PhoneField
import com.example.ui.components.ServiceCard
import com.example.ui.components.TimeSlotWidget
import com.example.ui.viewmodels.ClientViewModel
import com.example.utils.DateFormatter
import com.example.utils.SlotSchedule
import com.example.utils.Validators

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookAppointmentScreen(
    viewModel: ClientViewModel,
    onNavigateBackToHome: () -> Unit,
    onDaySlotsRefreshed: () -> Unit = {}
) {
    val step by viewModel.bookingStep.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedTime by viewModel.selectedTime.collectAsState()
    val selectedService by viewModel.selectedService.collectAsState()
    val services by viewModel.activeServices.collectAsState()
    val myAppts by viewModel.clientAppointments.collectAsState()
    val lastBooked by viewModel.lastBookedAppointment.collectAsState()
    val error by viewModel.bookingError.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val timeSlots = remember { SlotSchedule.DEFAULT_SLOTS }

    Column(modifier = Modifier.fillMaxSize()) {
        // Top header bar depending on step. Se omite en el paso 1 (selección de
        // fecha/hora): antes quedaba una barra fija "Seleccionar Hora (fecha)"
        // ocupando espacio permanentemente - el calendario y los turnos ya se ven
        // sin necesidad de esa barra, y la navegación hacia atrás la sigue dando
        // el CustomTopBar general de la pantalla (sección 2.1).
        if (step in 2..3) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { viewModel.setBookingStep(step - 1) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Anterior")
                    }
                    Text(
                        text = when (step) {
                            1 -> "Seleccionar Hora (${DateFormatter.formatDateForDisplay(selectedDate)})"
                            2 -> "Seleccionar Servicio"
                            3 -> "Confirmar Reserva"
                            else -> "Reserva"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        when (step) {
            0, 1 -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    // Show client's active reservations
                    val activeAppts = myAppts.filter { it.status == "confirmed" || it.status == "in_progress" }
                    if (activeAppts.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Mis Reservas Activas (${activeAppts.size}/2)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        items(activeAppts) { appt ->
                            val sName = services.find { it.id == appt.serviceId }?.name ?: "Servicio de Barbería"
                            AppointmentCard(appointment = appt, serviceName = sName, isAdmin = false)
                        }
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }

                    item {
                        Text(
                            text = "1. Elige un día en el calendario:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        CalendarScreen(
                            selectedDate = selectedDate,
                            onDateSelected = { date ->
                                viewModel.selectDate(date)
                                viewModel.setBookingStep(1)
                            },
                            getDayStatus = { dateStr -> viewModel.getDayStatus(dateStr) }
                        )
                    }

                    if (step == 1 || selectedDate.isNotBlank()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "2. Horarios para ${DateFormatter.formatDayName(selectedDate)}:",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        val isDaySlotsLoading by viewModel.isDaySlotsLoading.collectAsState()
                        var wasDaySlotsLoading by remember { mutableStateOf(false) }
                        LaunchedEffect(isDaySlotsLoading) {
                            if (wasDaySlotsLoading && !isDaySlotsLoading) onDaySlotsRefreshed()
                            wasDaySlotsLoading = isDaySlotsLoading
                        }
                        if (isDaySlotsLoading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else {
                            val slotsChunked = timeSlots.chunked(2)
                            items(slotsChunked) { rowSlots ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rowSlots.forEach { slot ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            val isOccupied = viewModel.isSlotOccupied(slot)
                                            TimeSlotWidget(
                                                time = slot,
                                                isOccupied = isOccupied,
                                                isSelected = selectedTime == slot,
                                                onClick = { viewModel.selectTime(slot) }
                                            )
                                        }
                                    }
                                    if (rowSlots.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> { // Selector de Servicios
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
                ) {
                    item {
                        Text(
                            text = "Selecciona un servicio de la lista:",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                    items(services) { service ->
                        ServiceCard(
                            service = service,
                            isAdmin = false,
                            isSelected = selectedService?.id == service.id,
                            onSelect = { viewModel.selectService(service) }
                        )
                    }
                }
            }

            3 -> { // Confirmación de reserva
                var isForOther by remember { mutableStateOf(false) }
                var otherName by remember { mutableStateOf("") }
                var otherLastName1 by remember { mutableStateOf("") }
                var otherLastName2 by remember { mutableStateOf("") }
                var otherPhone by remember { mutableStateOf("") }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Resumen de Cita",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("📅 Día: ${DateFormatter.formatDayName(selectedDate)}", style = MaterialTheme.typography.bodyLarge)
                                Text("⏰ Hora: $selectedTime", style = MaterialTheme.typography.bodyLarge)
                                Text("💈 Servicio: ${selectedService?.name}", style = MaterialTheme.typography.bodyLarge)
                                Text("💰 Precio: ${selectedService?.price} €", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            text = "¿Para quién es la reserva?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Para mí
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isForOther = false }
                                    .border(
                                        width = if (!isForOther) 2.dp else 1.dp,
                                        color = if (!isForOther) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp)),
                                color = if (!isForOther) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Box(
                                    modifier = Modifier.padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Para mí",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (!isForOther) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Para otra persona
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isForOther = true }
                                    .border(
                                        width = if (isForOther) 2.dp else 1.dp,
                                        color = if (isForOther) MaterialTheme.colorScheme.primary else Color.Gray,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clip(RoundedCornerShape(12.dp)),
                                color = if (isForOther) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                            ) {
                                Box(
                                    modifier = Modifier.padding(14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Para otra persona",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isForOther) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = isForOther) {
                            Column(modifier = Modifier.padding(top = 16.dp)) {
                                Text(
                                    text = "Datos de la otra persona:",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = otherName,
                                    onValueChange = { otherName = it },
                                    label = { Text("Nombre") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = otherLastName1,
                                        onValueChange = { otherLastName1 = it },
                                        label = { Text("Primer apellido") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 4.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = otherLastName2,
                                        onValueChange = { otherLastName2 = it },
                                        label = { Text("Segundo apellido") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 4.dp),
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                PhoneField(
                                    value = otherPhone,
                                    onValueChange = { otherPhone = it },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        val currentError = error
                        if (currentError != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = currentError,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = {
                                viewModel.confirmBooking(
                                    isForOther = isForOther,
                                    otherName = otherName,
                                    otherLastName1 = otherLastName1,
                                    otherLastName2 = otherLastName2,
                                    otherPhone = otherPhone
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            enabled = !isLoading,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Text("Confirmar Reserva", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }
            }

            4 -> { // Ticket de confirmación
                val appt = lastBooked
                if (appt != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Éxito",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(72.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "¡Reserva Confirmada!",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF2E7D32))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    // El cliente NO debe ver el ticket/ID interno de la
                                    // reserva (sección 14 del prompt maestro). El número
                                    // sigue existiendo en appt.ticketNumber y el
                                    // administrador puede seguir usándolo internamente.
                                    Text(
                                        text = "✓ RESERVA CONFIRMADA",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(text = "📅 ${DateFormatter.formatDayName(appt.appointmentDate)}", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "⏰ Hora: ${appt.appointmentTime.take(5)}", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "💈 Servicio: ${selectedService?.name ?: "Barbería"}", style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = "👤 Para: ${appt.fullName}", style = MaterialTheme.typography.bodyMedium)

                                Spacer(modifier = Modifier.height(20.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "⚠️ Debes estar 5 minutos antes de tu cita.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFC62828),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = {
                                viewModel.resetBookingFlow()
                                onNavigateBackToHome()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Volver al inicio", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }
    }
}
