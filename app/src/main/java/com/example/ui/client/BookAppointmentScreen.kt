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

    /*
     * IMPORTANTE:
     * Estos estados y efectos deben estar en el contexto Composable
     * principal y NO dentro del bloque LazyColumn/LazyListScope.
     */
    val isDaySlotsLoading by viewModel.isDaySlotsLoading.collectAsState()

    var wasDaySlotsLoading by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(isDaySlotsLoading) {
        if (wasDaySlotsLoading && !isDaySlotsLoading) {
            onDaySlotsRefreshed()
        }

        wasDaySlotsLoading = isDaySlotsLoading
    }

    val timeSlots = remember {
        SlotSchedule.DEFAULT_SLOTS
    }

    val slotsChunked = remember(timeSlots) {
        timeSlots.chunked(2)
    }

    val activeAppts = remember(myAppts) {
        myAppts.filter {
            it.status == "confirmed" || it.status == "in_progress"
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        /*
         * Header para los pasos 2 y 3.
         *
         * El paso 1 no muestra esta barra para evitar ocupar
         * espacio innecesariamente.
         */
        if (step in 2..3) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(
                    alpha = 0.5f
                ),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 8.dp,
                            vertical = 8.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    IconButton(
                        onClick = {
                            viewModel.setBookingStep(step - 1)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Anterior"
                        )
                    }

                    Text(
                        text = when (step) {
                            1 -> {
                                "Seleccionar Hora " +
                                    "(${DateFormatter.formatDateForDisplay(selectedDate)})"
                            }

                            2 -> "Seleccionar Servicio"

                            3 -> "Confirmar Reserva"

                            else -> "Reserva"
                        },
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        when (step) {

            /*
             * ============================================================
             * PASO 0 / 1
             * Selección de fecha y hora
             * ============================================================
             */
            0, 1 -> {

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(
                        bottom = 80.dp
                    )
                ) {

                    /*
                     * Reservas activas del cliente.
                     */
                    if (activeAppts.isNotEmpty()) {

                        item {

                            Spacer(
                                modifier = Modifier.height(12.dp)
                            )

                            Text(
                                text = "Mis Reservas Activas (${activeAppts.size}/2)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(
                                modifier = Modifier.height(4.dp)
                            )
                        }

                        items(activeAppts) { appt ->

                            val serviceName =
                                services
                                    .find {
                                        it.id == appt.serviceId
                                    }
                                    ?.name
                                    ?: "Servicio de Barbería"

                            AppointmentCard(
                                appointment = appt,
                                serviceName = serviceName,
                                isAdmin = false
                            )
                        }

                        item {
                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )
                        }
                    }

                    /*
                     * Calendario.
                     */
                    item {

                        Text(
                            text = "1. Elige un día en el calendario:",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.padding(
                                top = 8.dp
                            )
                        )

                        CalendarScreen(
                            selectedDate = selectedDate,

                            onDateSelected = { date ->
                                viewModel.selectDate(date)
                                viewModel.setBookingStep(1)
                            },

                            getDayStatus = { dateStr ->
                                viewModel.getDayStatus(dateStr)
                            }
                        )
                    }

                    /*
                     * Horarios del día seleccionado.
                     */
                    if (
                        step == 1 ||
                        selectedDate.isNotBlank()
                    ) {

                        item {

                            Spacer(
                                modifier = Modifier.height(16.dp)
                            )

                            Text(
                                text = "2. Horarios para ${
                                    DateFormatter.formatDayName(
                                        selectedDate
                                    )
                                }:",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )
                        }

                        /*
                         * Indicador de carga.
                         */
                        if (isDaySlotsLoading) {

                            item {

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = 24.dp
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {

                                    CircularProgressIndicator()
                                }
                            }

                        } else {

                            /*
                             * Horarios divididos en filas de 2.
                             */
                            items(slotsChunked) { rowSlots ->

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(
                                        8.dp
                                    )
                                ) {

                                    rowSlots.forEach { slot ->

                                        Box(
                                            modifier = Modifier