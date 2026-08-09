package com.example.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.models.Product
import com.example.ui.components.CustomTopBar
import com.example.ui.viewmodels.AuthViewModel
import com.example.ui.viewmodels.ClientViewModel

@Composable
fun ClientHomeScreen(
    clientViewModel: ClientViewModel,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    var currentTab by remember { mutableStateOf(0) } // 0=RESERVAR, 1=CATÁLOGO, 2=AJUSTES
    var selectedProductForDetail by remember { mutableStateOf<Product?>(null) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }

    val userPhone by authViewModel.userPhone.collectAsState()
    val isDarkMode by authViewModel.isDarkMode.collectAsState()
    val products by clientViewModel.activeProducts.collectAsState()
    val managerPhone by clientViewModel.managerPhone.collectAsState()

    val productForDetail = selectedProductForDetail
    if (productForDetail != null) {
        ProductDetailScreen(
            product = productForDetail,
            managerPhone = managerPhone,
            onBackClick = { selectedProductForDetail = null }
        )
        return
    }

    Scaffold(
        topBar = {
            CustomTopBar(
                title = "- Rodríguez -",
                subtitle = "Barbería & Estilo",
                onThemeToggle = { authViewModel.setDarkMode(!isDarkMode) },
                isDarkMode = isDarkMode,
                onLogoutClick = { onLogout() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Reservar") },
                    label = { Text("RESERVAR", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Catálogo") },
                    label = { Text("CATÁLOGO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("AJUSTES", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)) }
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
                    BookAppointmentScreen(
                        viewModel = clientViewModel,
                        onNavigateBackToHome = { currentTab = 0 }
                    )
                }
                1 -> {
                    CatalogScreen(
                        products = products,
                        onProductClick = { product ->
                            selectedProductForDetail = product
                        }
                    )
                }
                2 -> {
                    SettingsScreen(
                        currentPhone = userPhone,
                        isDarkMode = isDarkMode,
                        onUpdatePhone = { newPhone ->
                            clientViewModel.updatePhone(newPhone)
                        },
                        onToggleDarkMode = { enabled ->
                            authViewModel.setDarkMode(enabled)
                        },
                        onChangePasswordClick = {
                            showChangePasswordDialog = true
                        },
                        onLogoutClick = {
                            onLogout()
                        }
                    )
                }
            }
        }
    }

    if (showChangePasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Cambiar Contraseña", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column {
                    Text("Introduce tu nueva contraseña:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("Nueva contraseña") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // In demo/basic mode, close dialog
                        showChangePasswordDialog = false
                    },
                    enabled = newPass.length >= 6
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
