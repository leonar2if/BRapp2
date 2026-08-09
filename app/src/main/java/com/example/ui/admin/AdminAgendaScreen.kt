package com.example.ui.admin

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.models.Appointment
import com.example.data.models.Service
import com.example.ui.components.AppointmentCard
import com.example.ui.viewmodels.AdminViewModel
import com.example.utils.DateFormatter

@Composable
fun AdminAgendaScreen(
    viewModel: AdminViewModel,
    onStartDayClick: () -> Unit
) {
    val todayAppts by viewModel.todayAppointments.collectAsState()
    val services by viewModel.allServices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    val timeSlots = remember { DateFormatter.generateTimeSlots(10, 18, 30) }

    Column(modifier = Modifier.fillMaxSize()) {
        // INICIAR DÍA Button Header
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
                Column {
                    Text(
                        text = "Agenda de Hoy",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = DateFormatter.formatDayName(DateFormatter.getTodayDateString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        viewModel.startTurnExecution()
                        onStartDayClick()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar Día", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("INICIAR DÍA", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                items(timeSlots) { slot ->
                    val apptsForSlot = todayAppts.filter { it.appointmentTime.startsWith(slot.take(5)) && it.status != "canceled" }
                    
                    if (apptsForSlot.isNotEmpty()) {
                        apptsForSlot.forEach { appt ->
                            val serviceName = services.find { it.id == appt.serviceId }?.name ?: "Servicio de Barbería"
                            AppointmentCard(
                                appointment = appt,
                                serviceName = serviceName,
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
                    } else {
                        // Libre slot item
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 12.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = slot,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Surface(
                                    color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "LIBRE",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
