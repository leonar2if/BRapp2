package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.client.CalendarScreen
import com.example.ui.components.AppointmentCard
import com.example.ui.viewmodels.AdminViewModel
import com.example.utils.DateFormatter

@Composable
fun AdminCalendarScreen(
    viewModel: AdminViewModel
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dateAppts by viewModel.selectedDateAppointments.collectAsState()
    val services by viewModel.allServices.collectAsState()
    val context = LocalContext.current

    var showDayOffDialog by remember { mutableStateOf(false) }
    var sendNotification by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Agenda",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Button(
                    onClick = { showDayOffDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Marcar Día Libre", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
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
                    onDateSelected = { date -> viewModel.selectDate(date) },
                    getDayStatus = { dateStr ->
                        val today = DateFormatter.getTodayDateString()
                        if (dateStr < today) "gray" else "green"
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reservas para ${DateFormatter.formatDateForDisplay(selectedDate)} (${dateAppts.size}):",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (dateAppts.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No hay reservas registradas en este día.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(dateAppts) { appt ->
                    val sName = services.find { it.id == appt.serviceId }?.name ?: "Barbería"
                    AppointmentCard(
                        appointment = appt,
                        serviceName = sName,
                        isAdmin = true,
                        onCallClick = {
                            val cleanPhone = appt.phone.replace(" ", "").replace("+", "")
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone"))
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

    if (showDayOffDialog) {
        val activeApptsCount = dateAppts.count { it.status == "confirmed" || it.status == "in_progress" }

        AlertDialog(
            onDismissRequest = { showDayOffDialog = false },
            title = { Text("Marcar Día Libre", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column {
                    Text("¿Deseas marcar el día ${DateFormatter.formatDateForDisplay(selectedDate)} como día libre/inactivo?")
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = sendNotification, onCheckedChange = { sendNotification = it })
                            Text("Enviar notificación de cancelación a los clientes", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.markDayOff(selectedDate, sendNotification)
                        showDayOffDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmar y Cancelar Citas")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDayOffDialog = false }) {
                    Text("Volver")
                }
            }
        )
    }
}
