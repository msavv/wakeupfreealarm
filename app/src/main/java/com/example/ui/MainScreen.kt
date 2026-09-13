package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.QrAlarmApplication
import com.example.data.model.AlarmEntity
import com.example.data.model.AppSettingsEntity
import com.example.data.model.SavedBarcodeEntity
import com.example.ui.components.PermissionSetupDialog
import com.example.ui.screens.AlarmEditDialog
import com.example.ui.screens.AlarmsScreen
import com.example.ui.screens.GeneralSettingsScreen
import com.example.ui.screens.SavedQrsScreen
import com.example.util.AlarmScheduler
import kotlinx.coroutines.launch

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val repository = remember { QrAlarmApplication.instance.repository }
    val scope = rememberCoroutineScope()

    val alarms by repository.allAlarms.collectAsStateWithLifecycle(initialValue = emptyList())
    val savedBarcodes by repository.allSavedBarcodes.collectAsStateWithLifecycle(initialValue = emptyList())
    val settingsState by repository.settings.collectAsStateWithLifecycle(initialValue = null)
    val settings = settingsState ?: AppSettingsEntity()

    var currentTab by remember { mutableIntStateOf(0) }
    var editingAlarm by remember { mutableStateOf<AlarmEntity?>(null) }
    var isAddingAlarm by remember { mutableStateOf(false) }

    val prefs = remember { context.getSharedPreferences("qr_alarm_prefs", Context.MODE_PRIVATE) }
    var showPermissionSetupDialog by remember {
        mutableStateOf(!prefs.getBoolean("has_completed_permission_onboarding", false))
    }

    // Request POST_NOTIFICATIONS permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Permission result */ }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav")
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 0) Icons.Filled.Alarm else Icons.Outlined.Alarm,
                            contentDescription = "Alarms"
                        )
                    },
                    label = { Text("Alarms") },
                    modifier = Modifier.testTag("nav_alarms_tab")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 1) Icons.Filled.QrCodeScanner else Icons.Outlined.QrCodeScanner,
                            contentDescription = "Saved QRs"
                        )
                    },
                    label = { Text("Saved Codes") },
                    modifier = Modifier.testTag("nav_saved_qrs_tab")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("nav_settings_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                0 -> AlarmsScreen(
                    alarms = alarms,
                    savedBarcodes = savedBarcodes,
                    use24HourClock = settings.use24HourClock,
                    onToggleAlarm = { alarm, isChecked ->
                        scope.launch {
                            val updated = alarm.copy(isEnabled = isChecked, currentSnoozeCount = 0)
                            repository.updateAlarm(updated)
                            if (isChecked) {
                                AlarmScheduler.scheduleAlarm(context, updated)
                            } else {
                                AlarmScheduler.cancelAlarm(context, alarm.id)
                            }
                        }
                    },
                    onEditAlarm = { alarm ->
                        editingAlarm = alarm
                    },
                    onDeleteAlarm = { alarm ->
                        scope.launch {
                            AlarmScheduler.cancelAlarm(context, alarm.id)
                            repository.deleteAlarm(alarm)
                        }
                    },
                    onAddAlarmClick = {
                        isAddingAlarm = true
                    }
                )
                1 -> SavedQrsScreen(
                    savedBarcodes = savedBarcodes,
                    onSaveBarcode = { barcode ->
                        scope.launch {
                            if (barcode.id == 0L) {
                                repository.insertBarcode(barcode)
                            } else {
                                repository.updateBarcode(barcode)
                            }
                        }
                    },
                    onDeleteBarcode = { barcode ->
                        scope.launch {
                            repository.deleteBarcode(barcode)
                        }
                    }
                )
                2 -> GeneralSettingsScreen(
                    settings = settings,
                    onUpdateSettings = { newSettings ->
                        scope.launch {
                            repository.updateSettings(newSettings)
                        }
                    },
                    onOpenPermissionGuide = {
                        showPermissionSetupDialog = true
                    }
                )
            }
        }

        // Add or Edit Alarm Dialog
        if (isAddingAlarm || editingAlarm != null) {
            AlarmEditDialog(
                initialAlarm = editingAlarm ?: AlarmEntity(
                    volumePercent = settings.defaultAlarmVolume,
                    lockVolumeButtons = settings.lockVolumeButtons,
                    maxMuteCount = settings.defaultMaxMuteCount
                ),
                savedBarcodes = savedBarcodes,
                use24HourClock = settings.use24HourClock,
                onDismiss = {
                    isAddingAlarm = false
                    editingAlarm = null
                },
                onSave = { savedAlarm ->
                    scope.launch {
                        if (savedAlarm.id == 0L) {
                            val id = repository.insertAlarm(savedAlarm)
                            val inserted = savedAlarm.copy(id = id)
                            if (inserted.isEnabled) {
                                AlarmScheduler.scheduleAlarm(context, inserted)
                            }
                        } else {
                            repository.updateAlarm(savedAlarm)
                            if (savedAlarm.isEnabled) {
                                AlarmScheduler.scheduleAlarm(context, savedAlarm)
                            } else {
                                AlarmScheduler.cancelAlarm(context, savedAlarm.id)
                            }
                        }
                        isAddingAlarm = false
                        editingAlarm = null
                    }
                },
                onSaveNewBarcode = { newBarcode ->
                    scope.launch {
                        repository.insertBarcode(newBarcode)
                    }
                }
            )
        }

        // First-Run / Onboarding Permissions Dialog
        if (showPermissionSetupDialog) {
            PermissionSetupDialog(
                onDismiss = {
                    prefs.edit().putBoolean("has_completed_permission_onboarding", true).apply()
                    showPermissionSetupDialog = false
                },
                onComplete = {
                    prefs.edit().putBoolean("has_completed_permission_onboarding", true).apply()
                    showPermissionSetupDialog = false
                }
            )
        }
    }
}
