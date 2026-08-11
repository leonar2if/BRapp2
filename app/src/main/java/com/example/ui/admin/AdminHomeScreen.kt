package com.example.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CustomTopBar
import com.example.ui.viewmodels.AdminViewModel
import com.example.ui.viewmodels.AuthViewModel

@Composable
fun AdminHomeScreen(
    adminViewModel: AdminViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf(0) } // 0=HOY, 1=SERVICIOS, 2=CATÁLOGO, 3=AGENDA, 4=AJUSTES
    var isManagingTurns by remember { mutableStateOf(false) }

    val isDarkMode by authViewModel.isDarkMode.collectAsState()

    if (isManagingTurns) {
        AdminTurnScreen(
            viewModel = adminViewModel,
            onBackClick = { isManagingTurns = false }
        )
        return
    }

    Scaffold(
        topBar = {
            CustomTopBar(
                title = "Panel de Administración",
                subtitle = "Rodríguez Barbería",
                onThemeToggle = { authViewModel.setDarkMode(!isDarkMode) },
                isDarkMode = isDarkMode,
                onLogoutClick = { onLogout() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Hoy") },
                    label = { Text("HOY", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)) }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.ContentCut, contentDescription = "Servicios") },
                    label = { Text("SERVICIOS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)) }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Catálogo") },
                    label = { Text("CATÁLOGO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)) }
                )
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Agenda") },
                    label = { Text("AGENDA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)) }
                )
                NavigationBarItem(
                    selected = currentTab == 4,
                    onClick = { currentTab = 4 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("AJUSTES", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)) }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentTab) {
                0 -> {
                    // Pestaña "HOY": operación del día actual (antes llamada "Agenda").
                    // Archivo físico AdminAgendaScreen.kt sin renombrar para no romper
                    // referencias; el nombre visible al usuario ya es "Hoy".
                    AdminAgendaScreen(
                        viewModel = adminViewModel,
                        onStartDayClick = { isManagingTurns = true }
                    )
                }
                1 -> {
                    AdminServicesScreen(viewModel = adminViewModel)
                }
                2 -> {
                    AdminCatalogScreen(viewModel = adminViewModel)
                }
                3 -> {
                    // Pestaña "AGENDA": calendario mensual + turnos de un día
                    // (antes llamada "Calendario"). Archivo físico
                    // AdminCalendarScreen.kt sin renombrar por el mismo motivo.
                    AdminCalendarScreen(viewModel = adminViewModel)
                }
                4 -> {
                    AdminSettingsScreen(
                        adminViewModel = adminViewModel,
                        authViewModel = authViewModel,
                        onLogout = onLogout
                    )
                }
            }
        }
    }
}
