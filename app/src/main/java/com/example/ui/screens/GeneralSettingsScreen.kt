package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettingsEntity
import com.example.util.TimeFormatter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GeneralSettingsScreen(
    settings: AppSettingsEntity,
    onUpdateSettings: (AppSettingsEntity) -> Unit,
    onOpenPermissionGuide: () -> Unit = {}
) {
    var showEmergencyEditDialog by remember { mutableStateOf(false) }
    var showEmergencyTestPracticeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp)
        ) {
            Text(
                text = "General Settings",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Clock format, emergency bypass, and wake-up behaviors",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Clock Format Section: 12hr vs 24hr
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "24-Hour Clock",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (settings.use24HourClock) "Example: 19:45" else "Example: 07:45 PM",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = settings.use24HourClock,
                                onCheckedChange = { checked ->
                                    onUpdateSettings(settings.copy(use24HourClock = checked))
                                },
                                modifier = Modifier.testTag("toggle_24hr_clock")
                            )
                        }
                    }
                }
            }

            // Emergency Text Bypass Section
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.EditNote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Emergency Text Bypass",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Allows disabling alarm without QR if camera or code is unavailable",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "\"${settings.emergencyDismissText}\"",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showEmergencyEditDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("edit_emergency_text_button")
                            ) {
                                Text("Edit Phrase")
                            }

                            OutlinedButton(
                                onClick = { showEmergencyTestPracticeDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("practice_emergency_text_button")
                            ) {
                                Text("Practice Typing")
                            }
                        }
                    }
                }
            }

            // Mute for a while Setting
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeMute,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Mute for a while",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Temporarily silences alarm sound for a short grace period while walking to the code",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Grace Period Duration:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(15, 30, 45, 60).forEach { seconds ->
                                FilterChip(
                                    selected = settings.defaultMuteGraceSeconds == seconds,
                                    onClick = {
                                        onUpdateSettings(settings.copy(defaultMuteGraceSeconds = seconds))
                                    },
                                    label = { Text("${seconds}s", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Default Mute Limit (Presses allowed):",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val muteLimits = listOf(
                                "0 (Disabled)" to 0,
                                "1 time" to 1,
                                "2 times" to 2,
                                "3 times" to 3,
                                "5 times" to 5,
                                "Unlimited" to -1
                            )
                            muteLimits.forEach { (label, count) ->
                                FilterChip(
                                    selected = settings.defaultMaxMuteCount == count,
                                    onClick = {
                                        onUpdateSettings(settings.copy(defaultMaxMuteCount = count))
                                    },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }
                }
            }

            // Default Volume & Anti-Bypass Hardening
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Volume & Anti-Bypass Defaults",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Prevent snoozing by muting keys or swiping away",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Default Volume Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VolumeUp,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Default Alarm Volume", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Text(
                                text = "${settings.defaultAlarmVolume}%",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = settings.defaultAlarmVolume.toFloat(),
                            onValueChange = { onUpdateSettings(settings.copy(defaultAlarmVolume = it.toInt())) },
                            valueRange = 10f..100f,
                            steps = 17,
                            modifier = Modifier.fillMaxWidth().testTag("default_volume_slider")
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Lock Volume Buttons Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Disable Volume Keys while Ringing",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Physical volume buttons cannot lower or mute alarm sound",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = settings.lockVolumeButtons,
                                onCheckedChange = { onUpdateSettings(settings.copy(lockVolumeButtons = it)) },
                                modifier = Modifier.testTag("toggle_settings_lock_volume")
                            )
                        }
                    }
                }
            }

            // Permissions & Reliability Checklist
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Alarm Reliability & Permissions",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ensure Camera, Overlays, and Exact Alarms are enabled",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = onOpenPermissionGuide,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("open_permission_guide_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Configure Required Permissions")
                        }
                    }
                }
            }

            // Sleep Hygiene & Wakeup Tips
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "QR Alarm Sleep Hygiene Tips",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Place your QR code in your bathroom or kitchen so you must get out of bed to dismiss it.\n" +
                                    "• Use Mute for a while to walk quietly without disturbing household members.\n" +
                                    "• Set a 1 to 2 snooze limit during work weekdays to eliminate groggy oversleeping.\n" +
                                    "• Enable volume fade-in (30s to 2min) for a gentle, cortisol-reducing morning wake up.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }

    // Edit Emergency Text Dialog
    if (showEmergencyEditDialog) {
        var tempEmergencyText by remember { mutableStateOf(settings.emergencyDismissText) }

        AlertDialog(
            onDismissRequest = { showEmergencyEditDialog = false },
            title = { Text("Configure Emergency Phrase") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Enter a deliberate phrase you will be forced to type in full to dismiss the alarm without scanning (e.g. if camera is broken or phone is found):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = tempEmergencyText,
                        onValueChange = { tempEmergencyText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emergency_phrase_input"),
                        singleLine = false,
                        maxLines = 4,
                        supportingText = {
                            Text("${tempEmergencyText.length} characters")
                        }
                    )
                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    val presets = listOf(
                        "Alphabet (lowercase)" to "abcdefghijklmnopqrstuvwxyz",
                        "Alphabet (UPPERCASE)" to "ABCDEFGHIJKLMNOPQRSTUVWXYZ",
                        "50 Letters of Gibberish" to "xK9mQ2vL8wR4tP1jZ6bC3nF5yH0sD8gT2vX4mP9wQ7tL1zR6j8",
                        "Mindful Wake-up" to "I am wide awake and ready to start my day",
                        "Alert & Focused" to "Awake alert focused and ready"
                    )

                    presets.forEach { (label, preset) ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { tempEmergencyText = preset }
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = preset,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
                            val gibberish = (1..50).map { chars.random() }.joinToString("")
                            tempEmergencyText = gibberish
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate 50 Letters of Gibberish", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (tempEmergencyText.isNotBlank()) {
                            onUpdateSettings(settings.copy(emergencyDismissText = tempEmergencyText.trim()))
                            showEmergencyEditDialog = false
                        }
                    },
                    modifier = Modifier.testTag("save_emergency_phrase_button")
                ) {
                    Text("Save Phrase")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyEditDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Practice Typing Dialog
    if (showEmergencyTestPracticeDialog) {
        var practiceInput by remember { mutableStateOf("") }
        val target = settings.emergencyDismissText
        val isExactMatch = practiceInput.trim() == target.trim()

        AlertDialog(
            onDismissRequest = { showEmergencyTestPracticeDialog = false },
            title = { Text("Practice Emergency Typing") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Target phrase:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = target,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                    OutlinedTextField(
                        value = practiceInput,
                        onValueChange = { practiceInput = it },
                        label = { Text("Type here to test") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                    if (isExactMatch) {
                        Text(
                            text = "Match verified! You can type this accurately.",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showEmergencyTestPracticeDialog = false }
                ) {
                    Text("Done")
                }
            }
        )
    }
}
