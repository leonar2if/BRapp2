package com.example.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.utils.Validators

/**
 * Campo de teléfono reutilizable con el prefijo "+53" siempre visible y fijo,
 * de forma que el usuario solo escribe el número local (8 dígitos, debe
 * comenzar con 5). Ver sección 13 del prompt maestro.
 *
 * `value` / `onValueChange` manejan SOLO la parte local (sin +53). Quien use
 * este componente debe unir "+53" + value si necesita el número completo
 * para mostrarlo, o usar Validators.cleanPhoneNumber(value) para guardarlo.
 */
@Composable
fun PhoneField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Número de teléfono",
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            // Solo dígitos, máximo 8 (número local cubano).
            val digitsOnly = input.filter { it.isDigit() }.take(8)
            onValueChange(digitsOnly)
        },
        label = { Text(label) },
        leadingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 4.dp)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null)
                Text(
                    text = "  ${Validators.COUNTRY_CODE} |",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        modifier = modifier,
        singleLine = true,
        isError = isError,
        shape = RoundedCornerShape(12.dp)
    )
}
