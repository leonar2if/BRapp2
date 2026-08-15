package com.example.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodels.AdminViewModel
import com.example.utils.DateFormatter

/**
 * Pestaña "HOY" del administrador — ESTADO INACTIVO (por defecto). Pantalla
 * simple: "Hoy" grande, fecha debajo, cantidad de turnos pendientes del día,
 * y el botón INICIAR DÍA que lleva al estado activo (AdminTurnScreen, ahora
 * la galería de turnos). Archivo físico AdminAgendaScreen.kt sin renombrar
 * para no romper referencias existentes.
 */
@Composable
fun AdminAgendaScreen(
    viewModel: AdminViewModel,
    onStartDayClick: () -> Unit
) {
    val todayAppts by viewModel.todayAppointments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val today = remember { DateFormatter.getTodayDateString() }
    val pendingCount = remember(todayAppts) {
        todayAppts.count { it.status == "confirmed" || it.status == "in_progress" }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Hoy",
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = DateFormatter.formatDayName(today),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(40.dp))

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "$pendingCount",
                            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold, fontSize = 56.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (pendingCount == 1) "turno pendiente para hoy" else "turnos pendientes para hoy",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))

                Button(
                    onClick = onStartDayClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar Día", tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("INICIAR DÍA", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
                }
            }
        }
    }
}
