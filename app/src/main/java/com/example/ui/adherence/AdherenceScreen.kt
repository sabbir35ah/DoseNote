package com.example.ui.adherence

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DoseStatus
import com.example.data.repository.MedicationAdherence
import com.example.ui.MainViewModel
import com.example.ui.home.DoseStatusChip
import com.example.ui.theme.StatusTaken
import com.example.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdherenceScreen(
    viewModel: MainViewModel
) {
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()
    val selectedDateMillis by viewModel.selectedAdherenceDateMillis.collectAsStateWithLifecycle()
    val adherenceDayDoses by viewModel.adherenceDayDoses.collectAsStateWithLifecycle()
    val adherenceSummary by viewModel.adherenceSummary.collectAsStateWithLifecycle()

    val activeProfile = profiles.find { it.id == activeProfileId }

    // Generate past 7 days for calendar strip
    val calendarDays = remember {
        val list = mutableListOf<Long>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        for (i in 6 downTo 0) {
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -i)
            list.add(c.timeInMillis)
        }
        list
    }

    val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    val dateFormat = SimpleDateFormat("d", Locale.getDefault())
    val fullDateFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.adherence_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (activeProfile != null) {
                            Text(
                                text = "Profile: ${activeProfile.displayName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("adherence_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 7-day adherence score card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Assessment,
                                        contentDescription = null,
                                        tint = TealPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.weekly_score),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            val score = adherenceSummary?.adherencePercentage ?: 100
                            Text(
                                text = "$score%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (score >= 80) StatusTaken else MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Custom Canvas progress bar for adherence
                        CanvasBar(
                            percentage = adherenceSummary?.adherencePercentage ?: 100,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(14.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val scheduled = adherenceSummary?.totalScheduled ?: 0
                        val taken = adherenceSummary?.totalTaken ?: 0
                        Text(
                            text = stringResource(R.string.adherence_summary, taken, scheduled),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Per Medication Breakdown
            val breakdown = adherenceSummary?.perMedicationBreakdown ?: emptyList()
            if (breakdown.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.per_med_breakdown),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                items(breakdown) { item ->
                    MedicationAdherenceItem(adherence = item)
                }
            }

            // Calendar Strip Header
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.calendar_strip),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            // 7-day horizontal scrollable calendar strip
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(calendarDays) { dayMillis ->
                        val isSelected = selectedDateMillis == dayMillis
                        val date = Date(dayMillis)
                        val dayName = dayFormat.format(date)
                        val dayNumber = dateFormat.format(date)

                        Box(
                            modifier = Modifier
                                .width(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isSelected) TealPrimary else MaterialTheme.colorScheme.surface)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) TealPrimary else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { viewModel.setSelectedAdherenceDate(dayMillis) }
                                .padding(vertical = 12.dp)
                                .testTag("cal_day_$dayNumber"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = dayNumber,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Doses for Selected Date
            item {
                Text(
                    text = fullDateFormat.format(Date(selectedDateMillis)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            if (adherenceDayDoses.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.no_logs_for_day),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(adherenceDayDoses, key = { it.doseLog.id }) { item ->
                    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    val scheduledTimeStr = timeFormat.format(Date(item.doseLog.scheduledAt))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = scheduledTimeStr,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.width(76.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.medication.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = item.medication.doseDescription,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DoseStatusChip(status = item.doseLog.status)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CanvasBar(percentage: Int, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val barColor = if (percentage >= 80) StatusTaken else TealPrimary

    Canvas(modifier = modifier) {
        val cornerRadius = CornerRadius(size.height / 2, size.height / 2)
        // Background track
        drawRoundRect(
            color = trackColor,
            topLeft = Offset.Zero,
            size = size,
            cornerRadius = cornerRadius
        )
        // Progress fill
        val progressWidth = (size.width * (percentage / 100f)).coerceIn(0f, size.width)
        if (progressWidth > 0f) {
            drawRoundRect(
                color = barColor,
                topLeft = Offset.Zero,
                size = Size(progressWidth, size.height),
                cornerRadius = cornerRadius
            )
        }
    }
}

@Composable
fun MedicationAdherenceItem(adherence: MedicationAdherence) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = adherence.medicationName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${adherence.percentage}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (adherence.percentage >= 80) StatusTaken else MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            CanvasBar(
                percentage = adherence.percentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${adherence.taken} / ${adherence.scheduled} doses",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
