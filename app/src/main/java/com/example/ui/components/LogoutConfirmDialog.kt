package com.example.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

/**
 * Diálogo de confirmación reutilizable para "Cerrar Sesión" (sección 21 del
 * prompt maestro). Se usa igual en cliente (SettingsScreen) y administrador
 * (AdminSettingsScreen) para no duplicar una segunda lógica de logout: el
 * logout real sigue siendo el que ya existe en cada pantalla, este componente
 * solo antepone la confirmación.
 */
@Composable
fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Quieres cerrar sesión?", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cerrar sesión")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
