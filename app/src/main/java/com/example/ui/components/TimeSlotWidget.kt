package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.DateFormatter

@Composable
fun TimeSlotWidget(
    time: String,
    isOccupied: Boolean,
    isSelected: Boolean = false,
    isBlocked: Boolean = false, // turno bloqueado por el admin (⊘), sin cita
    isPast: Boolean = false, // turno de HOY cuya hora ya pasó (punto 3)
    useAbbreviatedLabels: Boolean = false, // true en la vista del cliente: "N.D." en vez de "NO DISPONIBLE" para que quepa
    onClick: () -> Unit
) {
    val effectivelyUnavailable = isOccupied || isBlocked || isPast

    val borderColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isPast || isBlocked -> Color.Gray
        isOccupied -> Color(0xFFC62828) // Red
        else -> Color(0xFF2E7D32) // Green
    }

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isPast || isBlocked -> Color.Gray.copy(alpha = 0.12f)
        isOccupied -> Color(0xFFC62828).copy(alpha = 0.1f)
        else -> Color(0xFF2E7D32).copy(alpha = 0.1f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(enabled = !effectivelyUnavailable) { onClick() }
            .minimumInteractiveComponentSize(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = DateFormatter.formatTimeForDisplay(time),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                ),
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )

            Surface(
                color = when {
                    isPast -> Color.Gray
                    isBlocked -> Color.Gray
                    isOccupied -> Color(0xFFC62828)
                    else -> Color(0xFF2E7D32)
                },
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = when {
                        isPast -> if (useAbbreviatedLabels) "N.D." else "PASADO"
                        isBlocked -> if (useAbbreviatedLabels) "N.D." else "NO DISPONIBLE"
                        isOccupied -> "LLENO"
                        else -> "LIBRE"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
