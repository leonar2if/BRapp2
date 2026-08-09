package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.models.Appointment

@Composable
fun AppointmentCard(
    appointment: Appointment,
    serviceName: String = "Servicio de Barbería",
    isAdmin: Boolean = false,
    onCallClick: (() -> Unit)? = null,
    onCancelClick: (() -> Unit)? = null,
    onAttendClick: (() -> Unit)? = null,
    onCardClick: (() -> Unit)? = null
) {
    var isExpanded by remember { mutableStateOf(false) }

    val statusColor = when (appointment.status) {
        "attended" -> Color(0xFF2E7D32) // Green
        "canceled" -> Color(0xFFC62828) // Red
        "in_progress" -> Color(0xFFEF6C00) // Orange
        else -> MaterialTheme.colorScheme.primary // Blue/Default
    }

    val statusText = when (appointment.status) {
        "attended" -> "Atendido"
        "canceled" -> "Cancelado"
        "in_progress" -> "En curso"
        else -> "Confirmado"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp, horizontal = 12.dp)
            .clickable {
                if (isAdmin) isExpanded = !isExpanded else onCardClick?.invoke()
            },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Ticket #${appointment.ticketNumber}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = appointment.appointmentTime.take(5),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = statusColor
                    )
                    if (isAdmin) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Expandir",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = serviceName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )

            val clientDisplayName = buildString {
                append(appointment.fullName)
                if (!appointment.lastName1.isNullOrBlank()) append(" ${appointment.lastName1}")
                if (!appointment.lastName2.isNullOrBlank()) append(" ${appointment.lastName2}")
            }.trim()

            Text(
                text = "Cliente: ${if (clientDisplayName.isNotEmpty()) clientDisplayName else "Cliente"}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (appointment.isAnnexed) {
                Text(
                    text = "⚠️ Reserva anexada para otra persona",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            AnimatedVisibility(visible = isAdmin && isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                ) {
                    Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Teléfono: ${appointment.phone}", style = MaterialTheme.typography.bodySmall)
                    if (appointment.createdByAdmin) {
                        Text(text = "Reservado por: Administrador", style = MaterialTheme.typography.bodySmall)
                    } else {
                        Text(text = "Reservado por: Cliente (App)", style = MaterialTheme.typography.bodySmall)
                    }
                    if (!appointment.notes.isNullOrBlank()) {
                        Text(text = "Notas: ${appointment.notes}", style = MaterialTheme.typography.bodySmall)
                    }

                    if (appointment.status == "confirmed" || appointment.status == "in_progress") {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            if (onCallClick != null) {
                                OutlinedButton(
                                    onClick = onCallClick,
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Llamar", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Llamar")
                                }
                            }
                            if (onCancelClick != null) {
                                OutlinedButton(
                                    onClick = onCancelClick,
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancelar", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancelar")
                                }
                            }
                            if (onAttendClick != null) {
                                Button(
                                    onClick = onAttendClick,
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Atendido", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Atendido")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
