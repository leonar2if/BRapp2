package com.example.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.components.AppointmentCard
import com.example.ui.components.PhoneField
import com.example.ui.viewmodels.AdminViewModel
import com.example.ui.viewmodels.AuthViewModel
import com.example.utils.DateFormatter
import com.example.utils.Validators

@Composable
fun AdminSettingsScreen(
    adminViewModel: AdminViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val allAppts by adminViewModel.allAppointments.collectAsState()
    val services by adminViewModel.allServices.collectAsState()
    val managerName by adminViewModel.managerName.collectAsState()
    val managerPhone by adminViewModel.managerPhone.collectAsState()
    val storeHours by adminViewModel.storeHours.collectAsState()
    val isDarkMode by authViewModel.isDarkMode.collectAsState()

    var showHistoryDialog by remember { mutableStateOf(false) }
    var isEditingContact by remember { mutableStateOf(false) }
    var isEditingHours by remember { mutableStateOf(false) }

    var nameInput by remember(managerName) { mutableStateOf(managerName) }
    var phoneInput by remember(managerPhone) { mutableStateOf(Validators.toLocalDisplay(managerPhone)) }
    var hoursInput by remember(storeHours) { mutableStateOf(storeHours) }
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showWorkingDaysDialog by remember { mutableStateOf(false) }
    val workingDaysCsv by adminViewModel.workingDaysCsv.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Text(
                text = "Configuración y Administración",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Registro Histórico Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showHistoryDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Registro Histórico de Citas", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Ver todas las citas con filtros y detalles", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Icon(Icons.Default.Search, contentDescription = "Ver", tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Contacto del gestor
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Contacto del Gestor", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        if (!isEditingContact) {
                            IconButton(onClick = { isEditingContact = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isEditingContact) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nombre del gestor") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PhoneField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = "Teléfono WhatsApp",
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { isEditingContact = false; nameInput = managerName; phoneInput = Validators.toLocalDisplay(managerPhone) }) {
                                Text("Cancelar")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    adminViewModel.saveManagerContact(nameInput, Validators.cleanPhoneNumber(phoneInput))
                                    isEditingContact = false
                                },
                                enabled = Validators.isValidLocalPhone(phoneInput)
                            ) {
                                Text("Guardar")
                            }
                        }
                    } else {
                        Text("👤 Nombre: $managerName", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("📞 WhatsApp: ${Validators.COUNTRY_CODE} ${Validators.toLocalDisplay(managerPhone)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Horarios de apertura
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Horarios de la Barbería", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        if (!isEditingHours) {
                            IconButton(onClick = { isEditingHours = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isEditingHours) {
                        OutlinedTextField(
                            value = hoursInput,
                            onValueChange = { hoursInput = it },
                            label = { Text("Horarios (e.g. Lun-Sab 10:00 - 20:00)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { isEditingHours = false; hoursInput = storeHours }) {
                                Text("Cancelar")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = {
                                adminViewModel.saveStoreHours(hoursInput)
                                isEditingHours = false
                            }) {
                                Text("Guardar")
                            }
                        }
                    } else {
                        Text(text = storeHours, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Días laborables (sección 16/17 del prompt maestro): configurable,
        // no hardcodeado a Lun-Vie. Se guarda en settings.working_days y lo
        // consumen tanto el calendario del cliente como el del admin
        // (SlotSchedule.isWorkingDay).
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Días Laborables", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        IconButton(onClick = { showWorkingDaysDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val dayLabels = mapOf(
                        "MON" to "Lun", "TUE" to "Mar", "WED" to "Mié", "THU" to "Jue",
                        "FRI" to "Vie", "SAT" to "Sáb", "SUN" to "Dom"
                    )
                    val activeDays = workingDaysCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    Text(
                        text = if (activeDays.isEmpty()) "Sin configurar" else activeDays.mapNotNull { dayLabels[it] }.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Dark Mode Toggle
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Brightness4, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Modo Oscuro", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { authViewModel.setDarkMode(it) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Logout
        item {
            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            }
        }
    }

    if (showLogoutConfirm) {
        com.example.ui.components.LogoutConfirmDialog(
            onConfirm = {
                showLogoutConfirm = false
                onLogout()
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }

    if (showHistoryDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var filterDate by remember { mutableStateOf("") }

        val filteredAppts = remember(allAppts, searchQuery, filterDate) {
            allAppts.filter { appt ->
                val matchesQuery = if (searchQuery.isBlank()) true else {
                    appt.fullName.contains(searchQuery, ignoreCase = true) || appt.phone.contains(searchQuery) || appt.ticketNumber.toString().contains(searchQuery)
                }
                val matchesDate = if (filterDate.isBlank()) true else {
                    appt.appointmentDate == filterDate
                }
                matchesQuery && matchesDate
            }
        }

        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Histórico de Citas (${filteredAppts.size})", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column(modifier = Modifier.heightIn(max = 400.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Buscar por nombre, teléfono o ticket") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = filterDate,
                        onValueChange = { filterDate = it },
                        label = { Text("Filtrar por fecha (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filteredAppts) { appt ->
                            val sName = services.find { it.id == appt.serviceId }?.name ?: "Barbería"
                            AppointmentCard(appointment = appt, serviceName = sName, isAdmin = false)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showHistoryDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }

    if (showWorkingDaysDialog) {
        val dayOptions = listOf(
            "MON" to "Lunes", "TUE" to "Martes", "WED" to "Miércoles", "THU" to "Jueves",
            "FRI" to "Viernes", "SAT" to "Sábado", "SUN" to "Domingo"
        )
        var selected by remember {
            mutableStateOf(workingDaysCsv.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableSet())
        }

        AlertDialog(
            onDismissRequest = { showWorkingDaysDialog = false },
            title = { Text("Días Laborables", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column {
                    dayOptions.forEach { (code, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                            Checkbox(
                                checked = code in selected,
                                onCheckedChange = { checked ->
                                    selected = (if (checked) selected + code else selected - code).toMutableSet()
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.saveWorkingDays(selected)
                        showWorkingDaysDialog = false
                    },
                    enabled = selected.isNotEmpty()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showWorkingDaysDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
