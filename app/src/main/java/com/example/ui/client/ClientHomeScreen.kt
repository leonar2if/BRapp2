package com.example.ui.client

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.models.Product
import com.example.ui.components.CustomTopBar
import com.example.ui.components.RefreshToast
import com.example.ui.components.rememberRefreshFeedbackState
import com.example.ui.viewmodels.AuthViewModel
import com.example.ui.viewmodels.ClientViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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

    // Pull-to-refresh + toast + punto de frescura (sección 1). Un solo estado
    // compartido para todo esto, disparado tanto por el gesto de swipe como por
    // cambios de día dentro de la reserva (sección 2.4).
    val refreshFeedback = rememberRefreshFeedbackState()
    var isPullRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    val productForDetail = selectedProductForDetail
    if (productForDetail != null) {
        androidx.activity.compose.BackHandler { selectedProductForDetail = null }
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
                onLogoutClick = { onLogout() },
                isDataFresh = refreshFeedback.isFresh
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
            PullToRefreshBox(
                isRefreshing = isPullRefreshing,
                onRefresh = {
                    coroutineScope.launch {
                        isPullRefreshing = true
                        if (com.example.utils.NetworkUtils.isOnline(context)) {
                            clientViewModel.refreshDataAwait()
                            isPullRefreshing = false
                            refreshFeedback.notifyRefreshed(message = "Actualizado", freshDurationMs = 60_000)
                        } else {
                            isPullRefreshing = false
                            refreshFeedback.notifyRefreshFailed()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
            when (currentTab) {
                0 -> {
                    BookAppointmentScreen(
                        viewModel = clientViewModel,
                        onNavigateBackToHome = { currentTab = 0 },
                        onDaySlotsRefreshed = { refreshFeedback.notifyRefreshed(message = "Actualizado", freshDurationMs = 60_000) }
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
                        currentBirthday = clientViewModel.userBirthday.collectAsState().value,
                        onUpdatePhone = { newPhone ->
                            clientViewModel.updatePhone(newPhone)
                        },
                        onUpdateBirthday = { birthday ->
                            clientViewModel.saveBirthday(birthday)
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
            RefreshToast(refreshFeedback.toastMessage, isError = refreshFeedback.isError)
        }
    }

    val cancellationNotice by clientViewModel.cancellationNotice.collectAsState()
    if (cancellationNotice != null) {
        val appt = cancellationNotice!!
        AlertDialog(
            onDismissRequest = { clientViewModel.dismissCancellationNotice() },
            title = { Text("Tu turno fue cancelado") },
            text = {
                Text(
                    "Lamentamos las molestias: tu turno del ${com.example.utils.DateFormatter.formatDateForDisplay(appt.appointmentDate)} " +
                        "a las ${com.example.utils.DateFormatter.formatTimeForDisplay(appt.appointmentTime)} fue cancelado por la barbería. " +
                        "Podés reservar otro horario cuando quieras."
                )
            },
            confirmButton = {
                Button(onClick = { clientViewModel.dismissCancellationNotice() }) {
                    Text("Entendido")
                }
            }
        )
    }

    if (showChangePasswordDialog) {
        var newPass by remember { mutableStateOf("") }
        var confirmPass by remember { mutableStateOf("") }
        var errorText by remember { mutableStateOf<String?>(null) }
        var successText by remember { mutableStateOf<String?>(null) }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showChangePasswordDialog = false },
            title = { Text("Cambiar Contraseña", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
            text = {
                Column {
                    if (successText != null) {
                        Text(successText!!, color = Color(0xFF2E7D32))
                    } else {
                        Text("Introduce tu nueva contraseña:")
                        Spacer(modifier = Modifier.height(8.dp))
                        com.example.ui.components.PasswordField(
                            value = newPass,
                            onValueChange = { newPass = it; errorText = null },
                            label = "Nueva contraseña",
                            showLockIcon = false
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        com.example.ui.components.PasswordField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it; errorText = null },
                            label = "Confirmar contraseña",
                            showLockIcon = false
                        )
                        if (errorText != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorText!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                if (successText != null) {
                    Button(onClick = { showChangePasswordDialog = false }) { Text("Listo") }
                } else {
                    Button(
                        onClick = {
                            if (newPass != confirmPass) {
                                errorText = "Las contraseñas no coinciden."
                                return@Button
                            }
                            isSaving = true
                            authViewModel.changePassword(newPass) { error ->
                                isSaving = false
                                if (error == null) {
                                    successText = "Contraseña actualizada correctamente."
                                } else {
                                    errorText = error
                                }
                            }
                        },
                        enabled = !isSaving && newPass.length >= 6 && confirmPass.length >= 6
                    ) {
                        Text(if (isSaving) "Guardando..." else "Guardar")
                    }
                }
            },
            dismissButton = {
                if (successText == null) {
                    TextButton(onClick = { showChangePasswordDialog = false }, enabled = !isSaving) {
                        Text("Cancelar")
                    }
                }
            }
        )
    }
}
