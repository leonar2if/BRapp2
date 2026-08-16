package com.example.ui.admin

import androidx.compose.ui.draw.clip
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.models.Appointment
import com.example.ui.viewmodels.AdminViewModel
import com.example.utils.DateFormatter

/**
 * Pestaña "HOY" del administrador — ESTADO ACTIVO. Lista de los 12 turnos
 * del día con su estado visual (libre / ocupado / actual / pasado /
 * cancelado); al tocar una card se abre la galería de detalle en carrusel
 * (swipe) sobre todos los turnos del día, con las acciones de cada cita.
 */
@Composable
fun AdminTurnScreen(
    viewModel: AdminViewModel,
    onBackClick: () -> Unit
) {
    val todayAppts by viewModel.todayAppointments.collectAsState()
    val services by viewModel.allServices.collectAsState()
    val activeSlots by viewModel.activeSlots.collectAsState()

    var showBlockDialog by remember { mutableStateOf(false) }
    var showEndDayDialog by remember { mutableStateOf(false) }
    var galleryStartIndex by remember { mutableStateOf<Int?>(null) }

    // Refresco del "ahora" cada 30s para que el turno ACTUAL avance sin
    // tener que salir y volver a entrar a la pantalla.
    var nowTick by remember { mutableStateOf(DateFormatter.getNowTimeString()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            nowTick = DateFormatter.getNowTimeString()
        }
    }

    val items = remember(todayAppts, nowTick, services, activeSlots) { TodaySlotBuilder.build(todayAppts, nowTick, services, activeSlots) }

    if (galleryStartIndex != null) {
        androidx.activity.compose.BackHandler { galleryStartIndex = null }
        TodayGallery(
            items = items,
            startIndex = galleryStartIndex!!,
            services = services,
            viewModel = viewModel,
            onClose = { galleryStartIndex = null }
        )
        return
    }

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                        Text(
                            text = "Hoy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = { showBlockDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = "Tomarse libre el resto del día",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showEndDayDialog = true },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = Color.White
            ) {
                Text("FINALIZAR DÍA", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 96.dp)
        ) {
            itemsIndexed(items) { index, item ->
                TodaySlotRow(
                    item = item,
                    onCheckClick = {
                        item.appointment?.let { viewModel.finalizeAppointment(it.id) }
                    },
                    onCardClick = { galleryStartIndex = index }
                )
            }
        }
    }

    if (showBlockDialog) {
        val today = remember { DateFormatter.getTodayDateString() }
        BlockRestOfDayDialog(
            timeSlots = activeSlots,
            currentTime = DateFormatter.getNowTimeString(),
            onDismiss = { showBlockDialog = false },
            onConfirm = { fromTime ->
                viewModel.blockRestOfDay(today, fromTime)
                showBlockDialog = false
            }
        )
    }

    if (showEndDayDialog) {
        val attended = todayAppts.count { it.status == "attended" }
        val canceled = todayAppts.count { it.status == "canceled" }
        val rescheduled = todayAppts.count { it.isRescheduled }
        val unattended = todayAppts.count { it.status == "confirmed" || it.status == "in_progress" }
        val estRevenue = todayAppts.filter { it.status == "attended" }.sumOf { appt ->
            services.find { it.id == appt.serviceId }?.price ?: 15.0
        }

        AlertDialog(
            onDismissRequest = { showEndDayDialog = false },
            title = { Text("Resumen del Día", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column {
                    if (unattended > 0) {
                        Text(
                            text = "⚠️ Quedan $unattended turnos sin atender.",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text("📊 Total citas: ${todayAppts.size}")
                    Text("✅ Atendidos: $attended")
                    Text("❌ Cancelados: $canceled")
                    Text("📅 Reprogramados: $rescheduled")
                    Text("💰 Ingresos estimados: $estRevenue €", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndDayDialog = false
                        onBackClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("Confirmar y Cerrar Día")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDayDialog = false }) {
                    Text("Volver")
                }
            }
        )
    }
}

@Composable
private fun TodaySlotRow(
    item: TodaySlotItem,
    onCheckClick: () -> Unit,
    onCardClick: () -> Unit
) {
    val (tagText, tagColor) = when (item.tag) {
        TagKind.LIBRE -> "LIBRE" to Color(0xFF2E7D32)
        TagKind.OCUPADO -> if (item.isContinuation) "OCUPADO (cont.)" to Color(0xFFC62828) else "OCUPADO" to Color(0xFFC62828)
        TagKind.ACTUAL -> "ACTUAL" to Color(0xFFF9A825)
        TagKind.PASADO -> "PASADO" to Color.Gray
        TagKind.CANCELADO -> "CANCELADO" to Color.Gray
    }

    val cardModifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
        .let {
            if (item.isCurrent) it.border(2.dp, Color(0xFFF9A825), RoundedCornerShape(10.dp)) else it
        }
        .clickable(enabled = item.appointment != null) { onCardClick() }

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Izquierda: hora + ícono de estado
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.time,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                when (item.icon) {
                    IconKind.PENDING_CLOCK -> Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Pendiente de confirmar",
                        tint = Color(0xFFF9A825),
                        modifier = Modifier.size(20.dp)
                    )
                    IconKind.CONFIRMED_CHECK -> Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Confirmado",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(20.dp)
                    )
                    IconKind.CANCELED_X -> Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancelado",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(20.dp)
                    )
                    IconKind.NONE -> {}
                }
                if (item.appointment != null) {
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.appointment.fullName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            // Derecha: tag de color
            Surface(
                color = tagColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = tagText,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = tagColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Extremo derecho: botón check de finalizar
            if (item.showCheckButton) {
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(onClick = onCheckClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Marcar atendido",
                        tint = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodayGallery(
    items: List<TodaySlotItem>,
    startIndex: Int,
    services: List<com.example.data.models.Service>,
    viewModel: AdminViewModel,
    onClose: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { items.size })

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver a la lista")
                }
                Text(
                    text = "Turno ${pagerState.currentPage + 1}/${items.size}",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            if (item.appointment != null) {
                OccupiedTurnDetail(
                    appointment = item.appointment,
                    serviceName = services.find { it.id == item.appointment.serviceId }?.name ?: "Servicio de Barbería",
                    viewModel = viewModel
                )
            } else {
                FreeTurnDetail(
                    label = TodaySlotBuilder.nextAppointmentLabel(items, item.time),
                    isCanceled = item.canceledAppointment != null
                )
            }
        }
    }
}

@Composable
private fun OccupiedTurnDetail(
    appointment: Appointment,
    serviceName: String,
    viewModel: AdminViewModel
) {
    val context = LocalContext.current
    val elapsedMap by viewModel.elapsedByAppointment.collectAsState()
    val elapsed = elapsedMap[appointment.id] ?: 0L
    var isFinalized by remember(appointment.id) { mutableStateOf(appointment.status == "attended") }
    var isTimerRunning by remember(appointment.id) { mutableStateOf(viewModel.isCardTimerRunning(appointment.id)) }

    val clientDisplayName = buildString {
        append(appointment.fullName)
        if (!appointment.lastName1.isNullOrBlank()) append(" ${appointment.lastName1}")
        if (!appointment.lastName2.isNullOrBlank()) append(" ${appointment.lastName2}")
    }.trim()

    // Contadores de visitas/faltas del cliente (pequeños, discretos, sección de contadores).
    var clientProfile by remember(appointment.phone) { mutableStateOf<com.example.data.models.Profile?>(null) }
    LaunchedEffect(appointment.phone) {
        if (appointment.phone.isNotBlank()) {
            clientProfile = viewModel.getClientProfile(appointment.phone)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "${appointment.appointmentTime.take(5)} · Ticket #${appointment.ticketNumber}",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onPrimary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (clientDisplayName.isNotEmpty()) clientDisplayName else "Cliente",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        if (appointment.phone.isNotBlank()) {
            Text(
                text = appointment.phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        clientProfile?.let { profile ->
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("${profile.visitCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(10.dp))
                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(2.dp))
                Text("${profile.noShowCount}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.ContentCut, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = serviceName,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (appointment.isAnnexed) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⚠️ Reserva anexada para otra persona",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val cleanPhone = appointment.phone.replace(" ", "").replace("+", "")
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanPhone")))
                },
                modifier = Modifier.weight(1f),
                enabled = appointment.phone.isNotBlank() && !isFinalized
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Llamar")
            }
            OutlinedButton(
                onClick = { viewModel.cancelAppointment(appointment.id) },
                modifier = Modifier.weight(1f),
                enabled = !isFinalized,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cancelar")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Notas del cliente: se acumulan entre visitas (secciones 4/5/6).
        ClientNotesSection(
            clientPhone = appointment.phone,
            clientName = clientDisplayName,
            viewModel = viewModel
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Cronómetro propio de este turno
        val mins = elapsed / 60
        val secs = elapsed % 60
        val timeStr = "%02d:%02d".format(mins, secs)

        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(14.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(timeStr, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.width(16.dp))
                OutlinedButton(
                    onClick = {
                        if (isTimerRunning) {
                            viewModel.pauseCardTimer(appointment.id)
                        } else {
                            viewModel.startCardTimer(appointment.id)
                        }
                        isTimerRunning = !isTimerRunning
                    },
                    enabled = !isFinalized
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isTimerRunning) "Pausar" else "Iniciar cronómetro")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                    viewModel.finalizeAppointment(appointment.id)
                    isFinalized = true
                    isTimerRunning = false
                },
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isFinalized,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "FINALIZADO",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Botón chico y discreto: el cliente NO vino (suma a su contador de
            // faltas). Aparte del FINALIZADO de arriba, que ya existe.
            var noShowConfirmed by remember(appointment.id) { mutableStateOf(false) }
            IconButton(
                onClick = {
                    viewModel.markAsNoShow(appointment.id)
                    noShowConfirmed = true
                },
                enabled = !isFinalized && !noShowConfirmed,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFC62828).copy(alpha = if (!isFinalized && !noShowConfirmed) 0.12f else 0.05f))
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "No vino",
                    tint = Color(0xFFC62828),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FreeTurnDetail(label: String, isCanceled: Boolean) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isCanceled) "Este turno fue cancelado" else "Este turno está libre",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Diálogo del botón ⊘: elegir "Desde ahora" (todos los turnos restantes) o
 * "Desde un turno" (turno elegido en adelante), con confirmación final antes
 * de aplicar. Se movió aquí (vista Activo) según lo pedido.
 */
@Composable
private fun BlockRestOfDayDialog(
    timeSlots: List<String>,
    currentTime: String,
    onDismiss: () -> Unit,
    onConfirm: (fromTime: String?) -> Unit
) {
    var selectedOption by remember { mutableStateOf("now") } // "now" | "fromSlot"
    var selectedSlot by remember { mutableStateOf(timeSlots.firstOrNull { it > currentTime } ?: timeSlots.last()) }
    var showConfirmStep by remember { mutableStateOf(false) }

    if (!showConfirmStep) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Tomarse libre el resto del día", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(selected = selectedOption == "now", onClick = { selectedOption = "now" })
                        Text("Desde ahora", style = MaterialTheme.typography.bodyLarge)
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(selected = selectedOption == "fromSlot", onClick = { selectedOption = "fromSlot" })
                        Text("Desde un turno", style = MaterialTheme.typography.bodyLarge)
                    }

                    if (selectedOption == "fromSlot") {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.heightIn(max = 220.dp)) {
                            items(timeSlots) { slot ->
                                val index = timeSlots.indexOf(slot) + 1
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedSlot == slot, onClick = { selectedSlot = slot })
                                    Text("Turno $index — $slot", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showConfirmStep = true }) {
                    Text("Continuar")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        )
    } else {
        val fromTime = if (selectedOption == "now") null else selectedSlot
        val slotIndex = if (fromTime != null) timeSlots.indexOf(fromTime) + 1 else null
        AlertDialog(
            onDismissRequest = { showConfirmStep = false },
            title = { Text("Confirmar", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Text(
                    if (fromTime == null) {
                        "¿Quieres marcar como libre el resto del día desde ahora?"
                    } else {
                        "¿Quieres marcar como libre el resto del día desde el turno $slotIndex — $fromTime?"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = { onConfirm(fromTime) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmStep = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Notas simples y acumulables del cliente (secciones 4/5). Cada nota nueva se
 * agrega a la lista, ninguna reemplaza a las anteriores. Se guardan en Room de
 * inmediato (ClientNoteRepository) y se suben a Supabase recién al cerrar
 * sesión (AdminViewModel.uploadPendingNotesBeforeLogout).
 */
@Composable
private fun ClientNotesSection(
    clientPhone: String,
    clientName: String,
    viewModel: AdminViewModel
) {
    if (clientPhone.isBlank()) return

    val notes by viewModel.getNotesForClient(clientPhone).collectAsState(initial = emptyList())
    var newNoteText by remember(clientPhone) { mutableStateOf("") }
    val noteDateFormat = remember { java.text.SimpleDateFormat("d MMM, HH:mm", java.util.Locale("es", "ES")) }

    Column {
        Text(
            text = "Notas del cliente" + if (notes.isNotEmpty()) " (${notes.size})" else "",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (notes.isEmpty()) {
            Text(
                text = "Sin notas todavía.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 180.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                notes.forEach { n ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(n.note, style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = noteDateFormat.format(java.util.Date(n.createdAt)) + if (!n.synced) " · pendiente de subir" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = newNoteText,
                onValueChange = { newNoteText = it },
                placeholder = { Text("Nueva nota sobre este cliente...") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (newNoteText.isNotBlank()) {
                        viewModel.addClientNote(clientPhone, clientName, newNoteText)
                        newNoteText = ""
                    }
                },
                enabled = newNoteText.isNotBlank()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar nota")
            }
        }
    }
}
