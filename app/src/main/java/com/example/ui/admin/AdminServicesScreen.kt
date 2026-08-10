package com.example.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.models.Service
import com.example.ui.components.ServiceCard
import com.example.ui.viewmodels.AdminViewModel

@Composable
fun AdminServicesScreen(
    viewModel: AdminViewModel
) {
    val services by viewModel.allServices.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<Service?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        text = "Gestión de Servicios (${services.size})",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Button(
                        onClick = {
                            editingService = null
                            showDialog = true
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Nuevo", maxLines = 1, softWrap = false)
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                items(services) { service ->
                    ServiceCard(
                        service = service,
                        isAdmin = true,
                        onEdit = {
                            editingService = service
                            showDialog = true
                        },
                        onDelete = {
                            viewModel.deleteService(service.id)
                        }
                    )
                }
            }
        }
    }

    if (showDialog) {
        var name by remember { mutableStateOf(editingService?.name ?: "") }
        var durationSlots by remember { mutableStateOf(editingService?.durationSlots ?: 1) }
        var price by remember { mutableStateOf(editingService?.price?.toString() ?: "15.0") }
        var isActive by remember { mutableStateOf(editingService?.isActive ?: true) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = if (editingService == null) "Agregar Servicio" else "Editar Servicio",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del servicio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Duración en turnos (1 = 30min, 2 = 60min):", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = durationSlots == 1,
                            onClick = { durationSlots = 1 },
                            label = { Text("1 turno (30 min)") }
                        )
                        FilterChip(
                            selected = durationSlots == 2,
                            onClick = { durationSlots = 2 },
                            label = { Text("2 turnos (60 min)") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("Precio (€)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (editingService != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Servicio Activo")
                            Switch(checked = isActive, onCheckedChange = { isActive = it })
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank() && price.toDoubleOrNull() != null) {
                            val durMins = durationSlots * 30
                            val s = Service(
                                id = editingService?.id ?: 0L,
                                name = name,
                                durationMinutes = durMins,
                                durationSlots = durationSlots,
                                price = price.toDoubleOrNull() ?: 15.0,
                                isActive = isActive
                            )
                            viewModel.saveService(s)
                            showDialog = false
                        }
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
