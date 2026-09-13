package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayCircle
import com.example.service.AlarmRingingService
import com.example.ui.components.CameraBarcodeScanner
import com.example.data.model.AlarmEntity
import com.example.data.model.SavedBarcodeEntity
import com.example.util.AudioPlayerHelper
import com.example.util.TimeFormatter
import com.example.util.VibrationHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AlarmEditDialog(
    initialAlarm: AlarmEntity?,
    savedBarcodes: List<SavedBarcodeEntity>,
    use24HourClock: Boolean,
    onDismiss: () -> Unit,
    onSave: (AlarmEntity) -> Unit,
    onSaveNewBarcode: ((SavedBarcodeEntity) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val initialHour = initialAlarm?.hour ?: 7
    val initialMinute = initialAlarm?.minute ?: 0

    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = use24HourClock
    )

    var title by remember { mutableStateOf(initialAlarm?.title ?: "Wake Up") }
    var daysOfWeek by remember { mutableIntStateOf(initialAlarm?.daysOfWeek ?: AlarmEntity.DAYS_WEEKDAYS) }
    var targetBarcodeId by remember { mutableStateOf(initialAlarm?.targetBarcodeId) }

    var audioUri by remember { mutableStateOf(initialAlarm?.audioUri) }
    var audioTitle by remember {
        val initialTitle = initialAlarm?.audioTitle ?: "Gentle Chimes"
        val initialUriStr = initialAlarm?.audioUri
        val cleanTitle = if (!initialUriStr.isNullOrEmpty() && initialTitle.startsWith("audio:")) {
            try {
                AudioPlayerHelper.getDisplayName(context, Uri.parse(initialUriStr))
            } catch (_: Exception) {
                initialTitle
            }
        } else {
            initialTitle
        }
        mutableStateOf(cleanTitle)
    }
    var fadeInSeconds by remember { mutableIntStateOf(initialAlarm?.fadeInDurationSeconds ?: 30) }

    var snoozeDurationMinutes by remember { mutableIntStateOf(initialAlarm?.snoozeDurationMinutes ?: 5) }
    var maxSnoozeCount by remember { mutableIntStateOf(initialAlarm?.maxSnoozeCount ?: 3) }
    var maxMuteCount by remember { mutableIntStateOf(initialAlarm?.maxMuteCount ?: 3) }

    var vibrateEnabled by remember { mutableStateOf(initialAlarm?.vibrateEnabled ?: true) }
    var vibrationPattern by remember { mutableStateOf(initialAlarm?.vibrationPattern ?: "PULSE") }
    var vibrationIntensity by remember { mutableStateOf(initialAlarm?.vibrationIntensity ?: "MEDIUM") }

    var volumePercent by remember { mutableIntStateOf(initialAlarm?.volumePercent ?: 100) }
    var lockVolumeButtons by remember { mutableStateOf(initialAlarm?.lockVolumeButtons ?: true) }
    var qrOnlyDismiss by remember { mutableStateOf(false) }

    var isPreviewPlaying by remember { mutableStateOf(false) }
    var showInlineScanner by remember { mutableStateOf(false) }
    var showInlineManualAdd by remember { mutableStateOf(false) }
    var inlineCodeName by remember { mutableStateOf("") }
    var inlineCodeText by remember { mutableStateOf("") }

    fun buildCurrentAlarm(): AlarmEntity {
        return (initialAlarm ?: AlarmEntity()).copy(
            title = title.ifBlank { "Wake Up" },
            hour = timePickerState.hour,
            minute = timePickerState.minute,
            isEnabled = true,
            daysOfWeek = daysOfWeek,
            targetBarcodeId = targetBarcodeId,
            audioTitle = audioTitle,
            audioUri = audioUri,
            fadeInDurationSeconds = fadeInSeconds,
            snoozeDurationMinutes = snoozeDurationMinutes,
            maxSnoozeCount = maxSnoozeCount,
            maxMuteCount = maxMuteCount,
            vibrateEnabled = vibrateEnabled,
            vibrationPattern = vibrationPattern,
            vibrationIntensity = vibrationIntensity,
            volumePercent = volumePercent,
            lockVolumeButtons = lockVolumeButtons,
            qrOnlyDismiss = false
        )
    }

    // SAF file picker for custom audio file
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri.toString()
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            val displayName = AudioPlayerHelper.getDisplayName(context, uri)
            audioTitle = displayName
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            AudioPlayerHelper.stopAudio()
            VibrationHelper.stopVibration(context)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialAlarm == null) "New QR Alarm" else "Edit QR Alarm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Time Picker
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Set Wake-up Time",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            TimePicker(
                                state = timePickerState,
                                colors = TimePickerDefaults.colors()
                            )
                        }
                    }

                    // Label Input
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Alarm Label") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("alarm_title_input"),
                        singleLine = true
                    )

                    // Recurring Schedule: Weekday / Weekend / Custom Chips
                    Column {
                        Text(
                            text = "Recurring Schedule",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Quick Presets
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = daysOfWeek == AlarmEntity.DAYS_WEEKDAYS,
                                onClick = { daysOfWeek = AlarmEntity.DAYS_WEEKDAYS },
                                label = { Text("Weekdays", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = daysOfWeek == AlarmEntity.DAYS_WEEKEND,
                                onClick = { daysOfWeek = AlarmEntity.DAYS_WEEKEND },
                                label = { Text("Weekends", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = daysOfWeek == AlarmEntity.DAYS_EVERYDAY,
                                onClick = { daysOfWeek = AlarmEntity.DAYS_EVERYDAY },
                                label = { Text("Everyday", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = daysOfWeek == AlarmEntity.DAYS_ONCE,
                                onClick = { daysOfWeek = AlarmEntity.DAYS_ONCE },
                                label = { Text("Once", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Individual Day Toggles
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val days = listOf(
                                "M" to AlarmEntity.DAY_MON,
                                "T" to AlarmEntity.DAY_TUE,
                                "W" to AlarmEntity.DAY_WED,
                                "T" to AlarmEntity.DAY_THU,
                                "F" to AlarmEntity.DAY_FRI,
                                "S" to AlarmEntity.DAY_SAT,
                                "S" to AlarmEntity.DAY_SUN
                            )
                            days.forEach { (label, flag) ->
                                val isSelected = AlarmEntity.isDaySelected(daysOfWeek, flag)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable {
                                            daysOfWeek = AlarmEntity.toggleDay(daysOfWeek, flag)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // QR Code / Barcode Picker
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Required Dismiss QR / Barcode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        var barcodeMenuExpanded by remember { mutableStateOf(false) }
                        val selectedBarcode = savedBarcodes.find { it.id == targetBarcodeId }

                        ExposedDropdownMenuBox(
                            expanded = barcodeMenuExpanded,
                            onExpandedChange = { barcodeMenuExpanded = !barcodeMenuExpanded }
                        ) {
                            OutlinedTextField(
                                value = selectedBarcode?.name ?: "Any Registered QR / Barcode",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = barcodeMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                                    .testTag("alarm_qr_selector")
                            )

                            ExposedDropdownMenu(
                                expanded = barcodeMenuExpanded,
                                onDismissRequest = { barcodeMenuExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Any Registered QR / Barcode") },
                                    onClick = {
                                        targetBarcodeId = null
                                        barcodeMenuExpanded = false
                                    }
                                )
                                savedBarcodes.forEach { barcode ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(barcode.name, fontWeight = FontWeight.Bold)
                                                Text(
                                                    "${barcode.format}: ${barcode.code}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        },
                                        onClick = {
                                            targetBarcodeId = barcode.id
                                            barcodeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Quick buttons to scan or add barcode right inside dialog
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showInlineScanner = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("inline_scan_code_button"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan Code", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    inlineCodeName = ""
                                    inlineCodeText = ""
                                    showInlineManualAdd = true
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("inline_add_manual_code_button"),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Type Code", fontSize = 12.sp)
                            }
                        }
                    }

                    // Audio & Fade-In Section
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Audiotrack, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Alarm Tone & Gentle Fade-in",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Audio Tone Selector Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            var soundMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                ExposedDropdownMenuBox(
                                    expanded = soundMenuExpanded,
                                    onExpandedChange = { soundMenuExpanded = !soundMenuExpanded }
                                ) {
                                    OutlinedTextField(
                                        value = audioTitle,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Sound Tone") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = soundMenuExpanded) },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = soundMenuExpanded,
                                        onDismissRequest = { soundMenuExpanded = false }
                                    ) {
                                        AudioPlayerHelper.PRESETS.forEach { preset ->
                                            DropdownMenuItem(
                                                text = { Text(preset) },
                                                onClick = {
                                                    audioTitle = preset
                                                    audioUri = null
                                                    soundMenuExpanded = false
                                                }
                                            )
                                        }
                                        HorizontalDivider()
                                        DropdownMenuItem(
                                            text = { Text("Choose Custom Audio File...", color = MaterialTheme.colorScheme.primary) },
                                            onClick = {
                                                soundMenuExpanded = false
                                                audioPickerLauncher.launch("audio/*")
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // Preview sound button
                            IconButton(
                                onClick = {
                                    if (isPreviewPlaying) {
                                        AudioPlayerHelper.stopAudio()
                                        isPreviewPlaying = false
                                    } else {
                                        isPreviewPlaying = true
                                        AudioPlayerHelper.playPreview(context, audioUri, audioTitle, volumePercent, coroutineScope)
                                    }
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Preview Tone",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Alarm Volume Slider
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
                                Text(
                                    text = "Alarm Volume",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "$volumePercent%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = volumePercent.toFloat(),
                            onValueChange = { volumePercent = it.toInt() },
                            valueRange = 10f..100f,
                            steps = 17,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("alarm_volume_slider")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Volume Fade-in duration chips
                        Text(
                            text = "Volume Fade-In Effect (Gentle Wakeup):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val fadeOptions = listOf(
                                "Instant" to 0,
                                "15s" to 15,
                                "30s" to 30,
                                "1 min" to 60,
                                "2 min" to 120,
                                "5 min" to 300
                            )
                            fadeOptions.forEach { (label, sec) ->
                                FilterChip(
                                    selected = fadeInSeconds == sec,
                                    onClick = { fadeInSeconds = sec },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    // Snooze Hygiene Settings
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Snooze, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Snooze Hygiene Controls",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Snooze Duration
                        Text(
                            text = "Snooze Duration (or disable entirely):",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val durations = listOf(
                                "Disabled" to 0,
                                "3 min" to 3,
                                "5 min" to 5,
                                "10 min" to 10,
                                "15 min" to 15,
                                "20 min" to 20
                            )
                            durations.forEach { (label, mins) ->
                                FilterChip(
                                    selected = snoozeDurationMinutes == mins,
                                    onClick = { snoozeDurationMinutes = mins },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }

                        if (snoozeDurationMinutes > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            // Snooze Limit
                            Text(
                                text = "Snooze Limit (Prevent Oversleeping):",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val limits = listOf(
                                    "1 time" to 1,
                                    "2 times" to 2,
                                    "3 times" to 3,
                                    "5 times" to 5,
                                    "Unlimited" to -1
                                )
                                limits.forEach { (label, count) ->
                                    FilterChip(
                                        selected = maxSnoozeCount == count,
                                        onClick = { maxSnoozeCount = count },
                                        label = { Text(label, fontSize = 12.sp) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Mute for a while Limit
                        Text(
                            text = "Mute for a while Limit (Presses allowed):",
                            style = MaterialTheme.typography.labelMedium,
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
                                    selected = maxMuteCount == count,
                                    onClick = { maxMuteCount = count },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    // Vibration Personalization
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Vibration",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Switch(
                                checked = vibrateEnabled,
                                onCheckedChange = { vibrateEnabled = it },
                                modifier = Modifier.testTag("alarm_vibration_toggle")
                            )
                        }

                        AnimatedVisibility(visible = vibrateEnabled) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Text(
                                    text = "Vibration Pattern:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    val patterns = listOf(
                                        "Pulse" to "PULSE",
                                        "Steady" to "STEADY",
                                        "Heartbeat" to "HEARTBEAT",
                                        "Escalating" to "ESCALATING",
                                        "Staccato" to "STACCATO"
                                    )
                                    patterns.forEach { (label, patternKey) ->
                                        FilterChip(
                                            selected = vibrationPattern == patternKey,
                                            onClick = {
                                                vibrationPattern = patternKey
                                                VibrationHelper.playPreview(context, patternKey, vibrationIntensity)
                                            },
                                            label = { Text(label, fontSize = 12.sp) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "Vibration Intensity:",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val intensities = listOf("SOFT", "MEDIUM", "STRONG")
                                    intensities.forEach { intensity ->
                                        FilterChip(
                                            selected = vibrationIntensity == intensity,
                                            onClick = {
                                                vibrationIntensity = intensity
                                                VibrationHelper.playPreview(context, vibrationPattern, intensity)
                                            },
                                            label = { Text(intensity, fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Anti-Bypass & Wakeup Hardening Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
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
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Anti-Bypass & Hardening",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))

                            // Emergency Phrase Safe-Guard Info Card
                            Surface(
                                color = MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Emergency Phrase Safeguard: An emergency text phrase is always enabled on the alarm ringing screen to ensure you can dismiss the alarm if your barcode is damaged or inaccessible.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 16.sp
                                    )
                                }
                            }

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
                                        text = "Prevents turning down or muting alarm with physical volume keys.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(
                                    checked = lockVolumeButtons,
                                    onCheckedChange = { lockVolumeButtons = it },
                                    modifier = Modifier.testTag("toggle_lock_volume_keys")
                                )
                            }
                        }
                    }
                }

                // Bottom Action Buttons (Preview & Save)
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val previewAlarm = buildCurrentAlarm()
                            AlarmRingingService.startPreview(context, previewAlarm)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("preview_alarm_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preview", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Button(
                        onClick = {
                            onSave(buildCurrentAlarm())
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(52.dp)
                            .testTag("save_alarm_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = if (initialAlarm == null) "Create Alarm" else "Save Changes",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }

    // Inline Camera Scanner Dialog
    if (showInlineScanner) {
        Dialog(
            onDismissRequest = { showInlineScanner = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraBarcodeScanner(
                    modifier = Modifier.fillMaxSize(),
                    onBarcodeScanned = { scannedCode, format ->
                        showInlineScanner = false
                        val friendlyName = "Scanned ${format.replace('_', ' ')}"
                        val newBarcode = SavedBarcodeEntity(
                            name = friendlyName,
                            code = scannedCode,
                            format = format
                        )
                        onSaveNewBarcode?.invoke(newBarcode)
                        Toast.makeText(context, "Saved & Selected: $friendlyName", Toast.LENGTH_SHORT).show()
                    }
                )
                IconButton(
                    onClick = { showInlineScanner = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }
            }
        }
    }

    // Inline Manual Add Code Dialog
    if (showInlineManualAdd) {
        AlertDialog(
            onDismissRequest = { showInlineManualAdd = false },
            title = { Text("Add Barcode / QR Code") },
            text = {
                Column {
                    Text(
                        text = "Enter the text or number of the barcode you want to use:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = inlineCodeName,
                        onValueChange = { inlineCodeName = it },
                        label = { Text("Label (e.g. Bathroom Mirror)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inlineCodeText,
                        onValueChange = { inlineCodeText = it },
                        label = { Text("Code value or number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (inlineCodeText.isNotBlank()) {
                            val name = inlineCodeName.ifBlank { "Custom Code" }
                            val newBarcode = SavedBarcodeEntity(
                                name = name,
                                code = inlineCodeText.trim(),
                                format = "BARCODE"
                            )
                            onSaveNewBarcode?.invoke(newBarcode)
                            Toast.makeText(context, "Saved: $name", Toast.LENGTH_SHORT).show()
                            showInlineManualAdd = false
                        }
                    },
                    enabled = inlineCodeText.isNotBlank()
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showInlineManualAdd = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
