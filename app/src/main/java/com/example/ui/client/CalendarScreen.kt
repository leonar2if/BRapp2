package com.example.ui.client

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.utils.DateFormatter
import com.example.utils.SlotSchedule
import java.util.Calendar

@Composable
fun CalendarScreen(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    getDayStatus: (String) -> String
) {
    val navigableMonths = remember {
        SlotSchedule.navigableMonths()
    }

    val selectedCalendar = remember(selectedDate) {
        DateFormatter.stringToDate(selectedDate)?.let { date ->
            Calendar.getInstance().apply {
                time = date
            }
        }
    }

    var monthIndex by remember(selectedDate) {
        mutableStateOf(
            navigableMonths.indexOfFirst { (year, month) ->
                selectedCalendar?.get(Calendar.YEAR) == year &&
                    selectedCalendar.get(Calendar.MONTH) == month
            }.takeIf { it >= 0 } ?: 0
        )
    }

    if (navigableMonths.isEmpty()) {
        return
    }

    val safeMonthIndex = monthIndex.coerceIn(
        0,
        navigableMonths.lastIndex
    )

    val (year, month) = navigableMonths[safeMonthIndex]

    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val daysInMonth = calendar.getActualMaximum(
        Calendar.DAY_OF_MONTH
    )

    val firstDayOffset =
        (calendar.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7

    val today = DateFormatter.getTodayDateString()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = {
                        if (monthIndex > 0) {
                            monthIndex--
                        }
                    },
                    enabled = monthIndex > 0
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronLeft,
                        contentDescription = "Mes anterior"
                    )
                }

                Text(
                    text = DateFormatter.formatMonthYear(calendar),
                    style = MaterialTheme.typography.titleMedium
                )

                IconButton(
                    onClick = {
                        if (monthIndex < navigableMonths.lastIndex) {
                            monthIndex++
                        }
                    },
                    enabled = monthIndex < navigableMonths.lastIndex
                ) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Mes siguiente"
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val dayNames = listOf(
                "L",
                "M",
                "X",
                "J",
                "V",
                "S",
                "D"
            )

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                dayNames.forEach { day ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            val totalCells =
                ((firstDayOffset + daysInMonth + 6) / 7) * 7

            for (rowStart in 0 until totalCells step 7) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    for (column in 0 until 7) {
                        val cell = rowStart + column
                        val day = cell - firstDayOffset + 1

                        if (day !in 1..daysInMonth) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .size(44.dp)
                            )
                        } else {
                            val date = Calendar.getInstance().apply {
                                set(Calendar.YEAR, year)
                                set(Calendar.MONTH, month)
                                set(Calendar.DAY_OF_MONTH, day)
                            }

                            val dateString =
                                DateFormatter.dateToString(date.time)

                            val status =
                                getDayStatus(dateString)

                            val isSelected =
                                dateString == selectedDate

                            val isPast =
                                dateString < today

                            val isWorkingDay =
                                SlotSchedule.isWorkingDay(dateString)

                            /*
                             * green = disponible
                             * red   = lleno
                             * gray  = pasado/no laborable
                             */
                            val enabled =
                                !isPast &&
                                    isWorkingDay &&
                                    status == "green"

                            val backgroundColor = when {
                                isSelected ->
                                    MaterialTheme.colorScheme.primary

                                enabled ->
                                    MaterialTheme.colorScheme.primaryContainer

                                else ->
                                    MaterialTheme.colorScheme.surfaceVariant
                            }

                            val textColor = when {
                                isSelected ->
                                    MaterialTheme.colorScheme.onPrimary

                                enabled ->
                                    MaterialTheme.colorScheme.onPrimaryContainer

                                else ->
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .size(44.dp)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(backgroundColor)
                                    .clickable(
                                        enabled = enabled
                                    ) {
                                        onDateSelected(dateString)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = textColor,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}