package com.example.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.DateFormatter
import java.util.Calendar

@Composable
fun CalendarScreen(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    getDayStatus: (String) -> String // returns "green", "red", "gray"
) {
    val currentCalendar = remember { Calendar.getInstance() }
    var year by remember { mutableStateOf(currentCalendar.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(currentCalendar.get(Calendar.MONTH)) }

    val calendarInstance = remember(year, month) {
        Calendar.getInstance().apply { set(year, month, 1) }
    }

    val daysInMonth = remember(year, month) {
        val maxDays = calendarInstance.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayOfWeek = calendarInstance.get(Calendar.DAY_OF_WEEK) // 1=Sunday
        val offset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2 // Make Monday day 0

        val list = mutableListOf<String?>()
        for (i in 0 until offset) {
            list.add(null)
        }
        for (day in 1..maxDays) {
            val mStr = (month + 1).toString().padStart(2, '0')
            val dStr = day.toString().padStart(2, '0')
            list.add("$year-$mStr-$dStr")
        }
        list
    }

    val weekDays = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Month Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (month == 0) {
                            month = 11; year -= 1
                        } else {
                            month -= 1
                        }
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Mes anterior")
                }

                Text(
                    text = DateFormatter.formatMonthYear(calendarInstance),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(
                    onClick = {
                        if (month == 11) {
                            month = 0; year += 1
                        } else {
                            month += 1
                        }
                    },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Mes siguiente")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Weekday labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekDays.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Days Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(daysInMonth) { dateStr ->
                    if (dateStr == null) {
                        Box(modifier = Modifier.aspectRatio(1f))
                    } else {
                        val dayNum = dateStr.substringAfterLast("-").toInt()
                        val status = getDayStatus(dateStr)
                        val isSelected = dateStr == selectedDate

                        val textColor = when (status) {
                            "green" -> Color(0xFF2E7D32)
                            "red" -> Color(0xFFC62828)
                            else -> Color.Gray
                        }

                        val bgColor = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            Color.Transparent
                        }

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(bgColor)
                                .clickable(enabled = status != "gray") {
                                    onDateSelected(dateStr)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayNum.toString(),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (isSelected || status != "gray") FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 15.sp
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else textColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                LegendItem(color = Color(0xFF2E7D32), label = "Turnos libres")
                LegendItem(color = Color(0xFFC62828), label = "Ocupado")
                LegendItem(color = Color.Gray, label = "Inactivo")
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
