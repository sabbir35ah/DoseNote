package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.adherence.AdherenceScreen
import com.example.ui.caregiver.CaregiverScreen
import com.example.ui.components.PaywallBottomSheet
import com.example.ui.home.HomeScreen
import com.example.ui.medication.AddEditMedicationScreen
import com.example.ui.onboarding.OnboardingScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.TealPrimary

enum class AppTab(
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
) {
    TODAY(R.string.nav_today, Icons.Filled.Home, Icons.Outlined.Home, "tab_today"),
    ADHERENCE(R.string.nav_adherence, Icons.Filled.Assessment, Icons.Outlined.Assessment, "tab_adherence"),
    CAREGIVER(R.string.nav_caregivers, Icons.Filled.SupervisorAccount, Icons.Outlined.SupervisorAccount, "tab_caregiver"),
    SETTINGS(R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseNoteApp(
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val showPaywall by viewModel.showPaywall.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(AppTab.TODAY) }
    var isAddingMedication by remember { mutableStateOf(false) }
    var editingMedicationId by remember { mutableStateOf<String?>(null) }

    // Request Notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPerm) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    if (currentUser == null) {
        // Show Onboarding
        OnboardingScreen(
            viewModel = viewModel,
            onComplete = {
                currentTab = AppTab.TODAY
            }
        )
    } else {
        // Main App with bottom tabs
        val paywallSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        Scaffold(
            bottomBar = {
                if (!isAddingMedication) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        AppTab.values().forEach { tab ->
                            val selected = currentTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { currentTab = tab },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.titleRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.titleRes),
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = TealPrimary,
                                    selectedTextColor = TealPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                ),
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isAddingMedication) {
                    AddEditMedicationScreen(
                        mainViewModel = viewModel,
                        medicationId = editingMedicationId,
                        onNavigateBack = {
                            isAddingMedication = false
                            editingMedicationId = null
                        }
                    )
                } else {
                    when (currentTab) {
                        AppTab.TODAY -> HomeScreen(
                            viewModel = viewModel,
                            onNavigateToAddMedication = {
                                isAddingMedication = true
                                editingMedicationId = null
                            }
                        )
                        AppTab.ADHERENCE -> AdherenceScreen(viewModel = viewModel)
                        AppTab.CAREGIVER -> CaregiverScreen(viewModel = viewModel)
                        AppTab.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            onSignOut = {
                                currentTab = AppTab.TODAY
                            }
                        )
                    }
                }
            }

            // Global Family Plan Paywall Bottom Sheet
            if (showPaywall) {
                PaywallBottomSheet(
                    sheetState = paywallSheetState,
                    onDismiss = { viewModel.closePaywall() },
                    onSubscribe = { viewModel.subscribeFamilyPlan() }
                )
            }
        }
    }
}
