package com.example.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DoseItem
import com.example.data.model.DoseStatus
import com.example.data.model.ProfileRelationship
import com.example.ui.MainViewModel
import com.example.ui.components.CreateProfileDialog
import com.example.ui.components.DoseActionBottomSheet
import com.example.ui.components.ProfileSwitchDialog
import com.example.ui.theme.StatusMissed
import com.example.ui.theme.StatusMissedContainer
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPendingContainer
import com.example.ui.theme.StatusSkipped
import com.example.ui.theme.StatusSkippedContainer
import com.example.ui.theme.StatusTaken
import com.example.ui.theme.StatusTakenContainer
import com.example.ui.theme.TealPrimary
import com.example.ui.theme.WarmAmberContainer
import com.example.ui.theme.WarmAmberOnContainer
import com.example.ui.theme.WarmAmberTertiary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToAddMedication: () -> Unit
) {
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()
    val todayDoses by viewModel.todayDoses.collectAsStateWithLifecycle()
    val lowSupplyMeds by viewModel.lowSupplyMeds.collectAsStateWithLifecycle()
    val dismissedLowSupplyIds by viewModel.dismissedLowSupplyIds.collectAsStateWithLifecycle()

    val activeProfile = profiles.find { it.id == activeProfileId } ?: profiles.firstOrNull()
    val isReadOnly = activeProfile?.isCaregiverShared == true

    var showProfileDialog by remember { mutableStateOf(false) }
    var showCreateProfileDialog by remember { mutableStateOf(false) }
    var selectedDoseItemForAction by remember { mutableStateOf<DoseItem?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showProfileDialog = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("profile_switcher_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isReadOnly) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isReadOnly) Icons.Default.Lock else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isReadOnly) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeProfile?.displayName ?: stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = stringResource(R.string.switch_profile),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isReadOnly) {
                                Text(
                                    text = stringResource(R.string.read_only_badge),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            // Only show add FAB if not in read-only caregiver profile
            if (!isReadOnly) {
                FloatingActionButton(
                    onClick = onNavigateToAddMedication,
                    containerColor = TealPrimary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.testTag("add_medication_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.add_medication))
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("home_screen_doses_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Low supply alert banners
            val activeLowSupply = lowSupplyMeds.filter { !dismissedLowSupplyIds.contains(it.id) }
            if (activeLowSupply.isNotEmpty()) {
                items(activeLowSupply) { med ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = WarmAmberContainer),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("low_supply_banner_${med.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarmAmberTertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.low_supply_banner, med.name, med.remainingSupply ?: 0),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = WarmAmberOnContainer,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.dismissLowSupplyBanner(med.id) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.dismiss),
                                    tint = WarmAmberOnContainer,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Read-Only Caregiver Notice Banner
            if (isReadOnly) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.read_only_explanation),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Header: Today's Schedule
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.today_schedule),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    val takenCount = todayDoses.count { it.doseLog.status == DoseStatus.TAKEN }
                    val totalCount = todayDoses.size
                    if (totalCount > 0) {
                        Text(
                            text = "$takenCount / $totalCount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TealPrimary
                        )
                    }
                }
            }

            // Chronological list of doses
            if (todayDoses.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Medication,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.no_medications_yet),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.no_medications_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(todayDoses, key = { it.doseLog.id }) { item ->
                    DoseCardItem(
                        doseItem = item,
                        isReadOnly = isReadOnly,
                        onClick = {
                            selectedDoseItemForAction = item
                        }
                    )
                }
            }
        }
    }

    // Dose Action Bottom Sheet
    selectedDoseItemForAction?.let { item ->
        DoseActionBottomSheet(
            doseItem = item,
            isReadOnly = isReadOnly,
            sheetState = sheetState,
            onDismiss = {
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    selectedDoseItemForAction = null
                }
            },
            onTake = {
                viewModel.markDoseTaken(item.doseLog.id, item.medication.id)
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    selectedDoseItemForAction = null
                }
            },
            onSkip = {
                viewModel.markDoseSkipped(item.doseLog.id)
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    selectedDoseItemForAction = null
                }
            },
            onSnooze = {
                viewModel.snoozeDose(
                    doseLogId = item.doseLog.id,
                    medicationId = item.medication.id,
                    medName = item.medication.name,
                    doseDesc = item.medication.doseDescription,
                    profileName = activeProfile?.displayName
                )
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    selectedDoseItemForAction = null
                }
            }
        )
    }

    // Profile Switch Dialog
    if (showProfileDialog) {
        ProfileSwitchDialog(
            profiles = profiles,
            activeProfileId = activeProfileId,
            onProfileSelected = { viewModel.setActiveProfile(it) },
            onAddProfileClicked = {
                showProfileDialog = false
                viewModel.addProfile(
                    name = "",
                    relationship = ProfileRelationship.PARENT,
                    onSuccess = {},
                    onPaywallNeeded = { viewModel.openPaywall() }
                )
                showCreateProfileDialog = true
            },
            onDismiss = { showProfileDialog = false }
        )
    }

    // Create Profile Dialog
    if (showCreateProfileDialog) {
        CreateProfileDialog(
            onDismiss = { showCreateProfileDialog = false },
            onCreate = { name, relation ->
                viewModel.addProfile(
                    name = name,
                    relationship = relation,
                    onSuccess = { showCreateProfileDialog = false },
                    onPaywallNeeded = {
                        showCreateProfileDialog = false
                        viewModel.openPaywall()
                    }
                )
            }
        )
    }
}

@Composable
fun DoseCardItem(
    doseItem: DoseItem,
    isReadOnly: Boolean,
    onClick: () -> Unit
) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val scheduledTimeStr = timeFormat.format(Date(doseItem.doseLog.scheduledAt))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("dose_item_${doseItem.doseLog.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Time Column
            Column(
                modifier = Modifier.width(76.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = scheduledTimeStr,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Medication info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doseItem.medication.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = doseItem.medication.doseDescription,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Chip
            DoseStatusChip(status = doseItem.doseLog.status)
        }
    }
}

@Composable
fun DoseStatusChip(status: DoseStatus) {
    val (bgColor, textColor, borderColor, icon, labelRes) = when (status) {
        DoseStatus.TAKEN -> StatusChipConfig(
            StatusTakenContainer,
            StatusTaken,
            StatusTaken,
            Icons.Default.Check,
            R.string.status_taken
        )
        DoseStatus.PENDING -> StatusChipConfig(
            StatusPendingContainer,
            StatusPending,
            StatusPending.copy(alpha = 0.5f),
            Icons.Default.AccessTime,
            R.string.status_pending
        )
        DoseStatus.SKIPPED -> StatusChipConfig(
            StatusSkippedContainer,
            StatusSkipped,
            StatusSkipped.copy(alpha = 0.5f),
            Icons.Default.Close,
            R.string.status_skipped
        )
        DoseStatus.MISSED -> StatusChipConfig(
            StatusMissedContainer,
            StatusMissed,
            StatusMissed,
            Icons.Default.ErrorOutline,
            R.string.status_missed
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(labelRes),
                color = textColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private data class StatusChipConfig(
    val bg: androidx.compose.ui.graphics.Color,
    val text: androidx.compose.ui.graphics.Color,
    val border: androidx.compose.ui.graphics.Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int
)
