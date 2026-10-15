package com.example.ui.medication

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.data.model.ScheduleType
import com.example.ui.MainViewModel
import com.example.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditMedicationScreen(
    mainViewModel: MainViewModel,
    medicationId: String? = null,
    onNavigateBack: () -> Unit,
    addEditViewModel: AddEditMedicationViewModel = viewModel()
) {
    val context = LocalContext.current
    val activeProfileId by mainViewModel.activeProfileId.collectAsStateWithLifecycle()
    val profiles by mainViewModel.allProfiles.collectAsStateWithLifecycle()
    val activeProfile = profiles.find { it.id == activeProfileId }

    val medName by addEditViewModel.medName.collectAsStateWithLifecycle()
    val doseDesc by addEditViewModel.doseDesc.collectAsStateWithLifecycle()
    val scheduleType by addEditViewModel.scheduleType.collectAsStateWithLifecycle()
    val times by addEditViewModel.times.collectAsStateWithLifecycle()
    val selectedDays by addEditViewModel.selectedDays.collectAsStateWithLifecycle()
    val startDate by addEditViewModel.startDate.collectAsStateWithLifecycle()
    val endDate by addEditViewModel.endDate.collectAsStateWithLifecycle()
    val trackSupply by addEditViewModel.trackSupply.collectAsStateWithLifecycle()
    val remainingSupply by addEditViewModel.remainingSupply.collectAsStateWithLifecycle()
    val lowSupplyThreshold by addEditViewModel.lowSupplyThreshold.collectAsStateWithLifecycle()
    val suggestions by addEditViewModel.suggestions.collectAsStateWithLifecycle()
    val isSaving by addEditViewModel.isSaving.collectAsStateWithLifecycle()

    LaunchedEffect(medicationId) {
        if (medicationId != null) {
            addEditViewModel.loadMedication(medicationId)
        }
    }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (medicationId == null) stringResource(R.string.add_medication) else stringResource(R.string.edit_medication),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cancel)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // Medicine Name with Bangladesh Autocomplete
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = medName,
                    onValueChange = { addEditViewModel.onNameChanged(it) },
                    label = { Text(stringResource(R.string.med_name_label)) },
                    placeholder = { Text(stringResource(R.string.med_name_placeholder)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Medication, contentDescription = null, tint = TealPrimary)
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("med_name_input")
                )

                // Autocomplete suggestion pills
                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            suggestions.take(4).forEach { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { addEditViewModel.selectSuggestion(item) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${item.generic} • ${item.company}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = item.form,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TealPrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dose Description
            OutlinedTextField(
                value = doseDesc,
                onValueChange = { addEditViewModel.onDoseDescChanged(it) },
                label = { Text(stringResource(R.string.dose_desc_label)) },
                placeholder = { Text(stringResource(R.string.dose_desc_placeholder)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dose_desc_input")
            )

            // Schedule Pattern Selector
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.schedule_type_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FilterChip(
                            selected = scheduleType == ScheduleType.FIXED_TIMES,
                            onClick = { addEditViewModel.setScheduleType(ScheduleType.FIXED_TIMES) },
                            label = { Text(stringResource(R.string.schedule_fixed)) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("schedule_fixed_chip"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )

                        FilterChip(
                            selected = scheduleType == ScheduleType.SPECIFIC_DAYS,
                            onClick = { addEditViewModel.setScheduleType(ScheduleType.SPECIFIC_DAYS) },
                            label = { Text(stringResource(R.string.schedule_days)) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("schedule_days_chip"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        )
                    }

                    // Specific Days Picker
                    if (scheduleType == ScheduleType.SPECIFIC_DAYS) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Select Days",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val daysMap = listOf(
                            1 to "Sun",
                            2 to "Mon",
                            3 to "Tue",
                            4 to "Wed",
                            5 to "Thu",
                            6 to "Fri",
                            7 to "Sat"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            daysMap.forEach { (dayInt, dayLabel) ->
                                val isSelected = selectedDays.contains(dayInt)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) TealPrimary else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { addEditViewModel.toggleDay(dayInt) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dayLabel.first().toString(),
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Times per day
                    Text(
                        text = stringResource(R.string.times_per_day),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        times.forEachIndexed { index, timeStr ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AccessTime,
                                        contentDescription = null,
                                        tint = TealPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = timeStr,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (times.size > 1) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = TealPrimary,
                                            modifier = Modifier
                                                .size(14.dp)
                                                .clickable { addEditViewModel.removeTime(index) }
                                        )
                                    }
                                }
                            }
                        }

                        // Add Time Button
                        IconButton(
                            onClick = {
                                val cal = Calendar.getInstance()
                                TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        val formatted = String.format(Locale.US, "%02d:%02d", hourOfDay, minute)
                                        addEditViewModel.addTime(formatted)
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add_dose_time), modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // Supply Tracking (Low-Supply Nudge)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Inventory2, contentDescription = null, tint = TealPrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.track_supply_label),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Switch(
                            checked = trackSupply,
                            onCheckedChange = { addEditViewModel.toggleTrackSupply(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = TealPrimary)
                        )
                    }

                    if (trackSupply) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = remainingSupply,
                                onValueChange = { addEditViewModel.onRemainingSupplyChanged(it) },
                                label = { Text(stringResource(R.string.remaining_supply_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("remaining_supply_input")
                            )

                            OutlinedTextField(
                                value = lowSupplyThreshold,
                                onValueChange = { addEditViewModel.onThresholdChanged(it) },
                                label = { Text(stringResource(R.string.low_supply_threshold_label)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("threshold_input")
                            )
                        }
                    }
                }
            }

            // Save Button
            Button(
                onClick = {
                    if (activeProfileId != null) {
                        addEditViewModel.save(
                            profileId = activeProfileId!!,
                            profileName = activeProfile?.displayName,
                            onSuccess = onNavigateBack
                        )
                    }
                },
                enabled = medName.isNotBlank() && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("save_medication_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                } else {
                    Text(
                        text = stringResource(R.string.save_medication),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Optional Delete Button if editing
            if (medicationId != null) {
                OutlinedButton(
                    onClick = {
                        addEditViewModel.delete(medicationId, onSuccess = onNavigateBack)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = stringResource(R.string.delete_medication),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
