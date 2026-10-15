package com.example.ui.caregiver

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.example.R
import com.example.data.model.CaregiverLink
import com.example.data.model.CaregiverLinkStatus
import com.example.ui.MainViewModel
import com.example.ui.theme.StatusTaken
import com.example.ui.theme.TealPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaregiverScreen(
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val profiles by viewModel.allProfiles.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()
    val allCaregiverLinks by viewModel.allCaregiverLinks.collectAsStateWithLifecycle()

    val activeProfile = profiles.find { it.id == activeProfileId }
    val isFamilySubscribed = currentUser?.isFamilyPlanSubscribed == true

    var showInviteDialog by remember { mutableStateOf(false) }
    var caregiverPhone by remember { mutableStateOf("") }
    var caregiverName by remember { mutableStateOf("") }
    var inviteError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.caregiver_sharing_title),
                        fontWeight = FontWeight.Bold
                    )
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
                .testTag("caregiver_screen_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Intro Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SupervisorAccount,
                                    contentDescription = null,
                                    tint = TealPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.caregiver_sharing_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Real-time read-only sync",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = stringResource(R.string.caregiver_sharing_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (isFamilySubscribed) {
                                    showInviteDialog = true
                                } else {
                                    viewModel.openPaywall()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("invite_caregiver_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                        ) {
                            Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.invite_caregiver_btn),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Shared with me profiles (Read-only view)
            val sharedProfiles = profiles.filter { it.isCaregiverShared }
            if (sharedProfiles.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.shared_with_me),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                items(sharedProfiles) { sharedProfile ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sharedProfile.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = stringResource(R.string.read_only_badge),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            OutlinedButton(
                                onClick = { viewModel.setActiveProfile(sharedProfile.id) },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("View")
                            }
                        }
                    }
                }
            }

            // Sent invitations
            item {
                Text(
                    text = stringResource(R.string.sent_invites),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            if (allCaregiverLinks.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.no_caregivers_yet),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(allCaregiverLinks) { link ->
                    CaregiverLinkItem(
                        link = link,
                        onCopyLink = {
                            val shareUrl = "https://dosenote.app/connect?inviteId=${link.id}&phone=${link.caregiverPhone}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("DoseNote Invite", shareUrl))
                            Toast.makeText(context, context.getString(R.string.share_link_copied), Toast.LENGTH_SHORT).show()
                        },
                        onAcceptDemo = {
                            viewModel.acceptCaregiverInvite(
                                inviteId = link.id,
                                profileName = link.caregiverName ?: "Family Member",
                                ownerPhone = link.caregiverPhone,
                                onSuccess = {
                                    Toast.makeText(context, "Caregiver profile linked in Read-Only mode!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    )
                }
            }
        }
    }

    // Invite Dialog
    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = {
                showInviteDialog = false
                inviteError = null
            },
            title = {
                Text(
                    text = stringResource(R.string.invite_caregiver_btn),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Enter mobile number of the caregiver to grant read-only view of ${activeProfile?.displayName ?: "this profile"}.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = caregiverPhone,
                        onValueChange = {
                            caregiverPhone = it.filter { char -> char.isDigit() || char == '+' }
                            inviteError = null
                        },
                        label = { Text(stringResource(R.string.caregiver_phone_label)) },
                        placeholder = { Text("01712345678") },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = TealPrimary)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("caregiver_phone_input")
                    )

                    OutlinedTextField(
                        value = caregiverName,
                        onValueChange = { caregiverName = it },
                        label = { Text("Caregiver Name (Optional)") },
                        placeholder = { Text("e.g. Son, Daughter, Nurse") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (inviteError != null) {
                        Text(
                            text = inviteError!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (caregiverPhone.length < 10) {
                            inviteError = "Please enter a valid phone number"
                        } else if (activeProfileId != null) {
                            viewModel.inviteCaregiver(
                                profileId = activeProfileId!!,
                                phone = caregiverPhone,
                                name = caregiverName.ifBlank { null },
                                onSuccess = {
                                    showInviteDialog = false
                                    caregiverPhone = ""
                                    caregiverName = ""
                                    Toast.makeText(context, context.getString(R.string.invite_sent_success), Toast.LENGTH_SHORT).show()
                                },
                                onPaywallNeeded = {
                                    showInviteDialog = false
                                    viewModel.openPaywall()
                                },
                                onError = { inviteError = it }
                            )
                        }
                    },
                    modifier = Modifier.testTag("submit_caregiver_invite")
                ) {
                    Text(stringResource(R.string.send_invite_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
fun CaregiverLinkItem(
    link: CaregiverLink,
    onCopyLink: () -> Unit,
    onAcceptDemo: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val invitedDateStr = dateFormat.format(Date(link.invitedAt))

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = link.caregiverName ?: link.caregiverPhone,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Invited $invitedDateStr • ${link.permission}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (link.status == CaregiverLinkStatus.ACCEPTED) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (link.status == CaregiverLinkStatus.ACCEPTED) stringResource(R.string.accepted_status)
                        else stringResource(R.string.pending_status),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (link.status == CaregiverLinkStatus.ACCEPTED) TealPrimary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyLink,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Link", fontSize = 12.sp)
                }

                if (link.status == CaregiverLinkStatus.PENDING) {
                    Button(
                        onClick = onAcceptDemo,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Demo Accept", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
