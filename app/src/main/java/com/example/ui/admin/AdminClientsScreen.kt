package com.example.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.models.Profile
import com.example.ui.viewmodels.AdminViewModel
import com.example.utils.DateFormatter

/**
 * Directorio completo de clientes (Ajustes -> Clientes). Muestra todos los
 * datos de cada cliente menos la contraseña (que ni siquiera se guarda acá,
 * vive en auth.users de Supabase). Avisa si hoy es el cumpleaños de alguno.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminClientsScreen(
    viewModel: AdminViewModel,
    onBackClick: () -> Unit
) {
    val allClients by viewModel.allClients.collectAsState()
    val birthdayToday by viewModel.clientsWithBirthdayToday.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedClient by remember { mutableStateOf<Profile?>(null) }

    LaunchedEffect(Unit) { viewModel.loadAllClients() }

    if (selectedClient != null) {
        androidx.activity.compose.BackHandler { selectedClient = null }
        ClientProfileDetail(profile = selectedClient!!, onBackClick = { selectedClient = null })
        return
    }

    val filtered = allClients.filter {
        searchQuery.isBlank() ||
            it.fullName.contains(searchQuery, ignoreCase = true) ||
            it.phone.contains(searchQuery)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Clientes (${allClients.size})") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Buscar por nombre o teléfono") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            if (birthdayToday.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Cake, contentDescription = null, tint = Color(0xFF8A6D00))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "¡Hoy cumple años!",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF8A6D00)
                            )
                        }
                        birthdayToday.forEach {
                            Text("🎂 ${it.fullName.ifBlank { it.phone }}", color = Color(0xFF8A6D00))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "Sin clientes todavía.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                items(filtered) { client ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { selectedClient = client },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    client.fullName.ifBlank { "Sin nombre" },
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    com.example.utils.Validators.COUNTRY_CODE + " " + client.phone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                                Text(" ${client.visitCount}  ", style = MaterialTheme.typography.labelMedium)
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                                Text(" ${client.noShowCount}", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientProfileDetail(profile: Profile, onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(profile.fullName.ifBlank { "Cliente" }) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp)
        ) {
            ProfileRow(icon = Icons.Default.Person, label = "Nombre", value = profile.fullName.ifBlank { "-" })
            ProfileRow(label = "Teléfono", value = com.example.utils.Validators.COUNTRY_CODE + " " + profile.phone)
            ProfileRow(
                icon = Icons.Default.Cake,
                label = "Cumpleaños",
                value = profile.birthday?.let { DateFormatter.formatDateForDisplay(it) } ?: "No configurado"
            )
            ProfileRow(label = "Rol", value = if (profile.role == "admin") "Administrador" else "Cliente")
            ProfileRow(icon = Icons.Default.Check, label = "Veces que ha venido", value = "${profile.visitCount}")
            ProfileRow(icon = Icons.Default.Close, label = "Veces que faltó", value = "${profile.noShowCount}")
            profile.createdAt?.let {
                ProfileRow(label = "Cliente desde", value = DateFormatter.formatDateForDisplay(it.take(10)))
            }
        }
    }
}

@Composable
private fun ProfileRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
        }
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
    }
    HorizontalDivider()
}
