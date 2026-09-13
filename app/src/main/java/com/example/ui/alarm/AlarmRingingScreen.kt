package com.example.ui.alarm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AlarmEntity
import com.example.data.model.AppSettingsEntity
import com.example.data.model.SavedBarcodeEntity
import com.example.service.AlarmRingingService
import com.example.ui.components.CameraBarcodeScanner
import com.example.util.TimeFormatter
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AlarmRingingScreen(
    alarm: AlarmEntity,
    targetBarcode: SavedBarcodeEntity?,
    allSavedBarcodes: List<SavedBarcodeEntity>,
    settings: AppSettingsEntity,
    activeRingingState: AlarmRingingService.ActiveRingingState?,
    isPreviewMode: Boolean = false,
    onMute30sClicked: () -> Unit,
    onSnoozeClicked: () -> Unit,
    onDismissSuccess: () -> Unit,
    onExitPreview: () -> Unit = {}
) {
    var currentTimeString by remember { mutableStateOf("") }
    var showCameraScanner by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var emergencyInputText by remember { mutableStateOf("") }
    var manualCodeInput by remember { mutableStateOf("") }
    var scanErrorMessage by remember { mutableStateOf<String?>(null) }
    var scanSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Live clock update
    LaunchedEffect(settings.use24HourClock) {
        val pattern = if (settings.use24HourClock) "HH:mm:ss" else "hh:mm:ss a"
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        while (true) {
            currentTimeString = sdf.format(Date())
            delay(1000L)
        }
    }

    val isMuted = activeRingingState?.isMutedGrace == true
    val remainingGrace = activeRingingState?.remainingGraceSeconds ?: 0

    // Snooze availability check
    val canSnooze = alarm.snoozeDurationMinutes > 0 &&
            (alarm.maxSnoozeCount == -1 || alarm.currentSnoozeCount < alarm.maxSnoozeCount)

    fun verifyScannedCode(code: String): Boolean {
        val trimmed = code.trim()
        val isTargetMatch = if (targetBarcode != null) {
            trimmed.equals(targetBarcode.code.trim(), ignoreCase = true)
        } else if (allSavedBarcodes.isNotEmpty()) {
            allSavedBarcodes.any { it.code.trim().equals(trimmed, ignoreCase = true) }
        } else {
            // Any scanned barcode dismisses if no specific barcode configured
            trimmed.isNotBlank()
        }

        if (isTargetMatch) {
            scanSuccessMessage = "QR / Barcode Verified! Alarm Dismissed."
            scanErrorMessage = null
            return true
        } else {
            scanErrorMessage = "Scanned code ($trimmed) did not match required barcode (${targetBarcode?.name ?: "saved codes"}). Try again!"
            return false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E1B4B),
                        Color(0xFF090D16)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (showCameraScanner) {
            // Full screen camera scanner overlay
            Box(modifier = Modifier.fillMaxSize()) {
                CameraBarcodeScanner(
                    modifier = Modifier.fillMaxSize(),
                    targetBarcodeHint = targetBarcode?.name,
                    onBarcodeScanned = { text, _ ->
                        if (verifyScannedCode(text)) {
                            showCameraScanner = false
                            onDismissSuccess()
                        }
                    }
                )

                // Close Camera button
                IconButton(
                    onClick = { showCameraScanner = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("close_camera_scanner")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Camera",
                        tint = Color.White
                    )
                }

                // Error feedback bar if scanned wrong code
                if (scanErrorMessage != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 96.dp, start = 24.dp, end = 24.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = scanErrorMessage ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            // Main Alarm Ringing View
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (isPreviewMode) {
                    Surface(
                        color = Color(0xFF6366F1).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF818CF8)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color(0xFFA5B4FC),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Test Preview Active",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Test sound, volume & scanner safely",
                                        color = Color(0xFFC7D2FE),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Button(
                                onClick = onExitPreview,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("exit_preview_button")
                            ) {
                                Text("Exit Preview")
                            }
                        }
                    }
                }

                // Top Header: Alarm Tag & Urgency Status
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = if (isMuted) Color(0xFF0284C7).copy(alpha = 0.25f) else Color(0xFFEF4444).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isMuted) Color(0xFF38BDF8) else Color(0xFFF87171)
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (isMuted) Color(0xFF38BDF8) else Color(0xFFF87171),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isMuted) "MUTED: ${remainingGrace}s LEFT" else "ALARM RINGING",
                                color = if (isMuted) Color(0xFF38BDF8) else Color(0xFFF87171),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentTimeString.ifEmpty {
                            TimeFormatter.formatTime(alarm.hour, alarm.minute, settings.use24HourClock)
                        },
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif
                        ),
                        color = Color.White
                    )

                    Text(
                        text = alarm.title.ifBlank { "Morning Wake Up" },
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Center Card: Required QR / Barcode Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1E293B).copy(alpha = 0.85f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Dismiss Challenge",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = targetBarcode?.let { "Scan \"${it.name}\"" }
                                ?: "Scan Any Registered QR / Barcode",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        if (targetBarcode != null) {
                            Text(
                                text = "Code: ${targetBarcode.code}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        if (scanErrorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = scanErrorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Action Controls Section
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Primary Action: Launch Camera Scanner
                    Button(
                        onClick = {
                            scanErrorMessage = null
                            showCameraScanner = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("scan_qr_to_dismiss_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Scan QR to Dismiss",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // 2. Mute for a while (with limit support)
                    val maxMute = activeRingingState?.maxMuteCount ?: alarm.maxMuteCount
                    val currentMute = activeRingingState?.currentMuteCount ?: 0
                    val isMuteDisabled = maxMute == 0
                    val isMuteLimitReached = maxMute > 0 && currentMute >= maxMute

                    if (!isMuteDisabled) {
                        FilledTonalButton(
                            onClick = onMute30sClicked,
                            enabled = isMuted || !isMuteLimitReached,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("mute_for_a_while_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (isMuted) Color(0xFF0369A1) else Color(0xFF334155),
                                disabledContainerColor = Color(0xFF1E293B)
                            )
                        ) {
                            Icon(
                                imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val buttonText = when {
                                isMuted -> "Muted: ${remainingGrace}s left"
                                isMuteLimitReached -> "Mute Limit Reached ($currentMute/$maxMute)"
                                maxMute > 0 -> "Mute for a while ($currentMute/$maxMute used)"
                                else -> "Mute for a while"
                            }
                            Text(
                                text = buttonText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // 3. Snooze Button (respects snooze limit and duration)
                    if (canSnooze) {
                        val snoozeInfo = if (alarm.maxSnoozeCount > 0) {
                            "${alarm.snoozeDurationMinutes}m (${alarm.currentSnoozeCount}/${alarm.maxSnoozeCount} used)"
                        } else {
                            "${alarm.snoozeDurationMinutes}m"
                        }
                        OutlinedButton(
                            onClick = onSnoozeClicked,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("snooze_alarm_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFCD34D)
                            )
                        ) {
                            Icon(Icons.Default.Snooze, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Snooze $snoozeInfo",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else if (alarm.snoozeDurationMinutes > 0) {
                        Surface(
                            color = Color.White.copy(alpha = 0.07f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Snooze limit reached for this alarm cycle",
                                color = Color.White.copy(alpha = 0.5f),
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    // 4. Secondary Row: Always available Emergency Text & Manual Input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                emergencyInputText = ""
                                showEmergencyDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("emergency_text_dismiss_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF87171)
                            )
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Emergency Text", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                manualCodeInput = ""
                                showManualInputDialog = true
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("manual_code_input_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White.copy(alpha = 0.85f)
                            )
                        ) {
                            Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Manual Code", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    // Emergency Text Dismiss Dialog
    if (showEmergencyDialog) {
        val targetEmergencyText = settings.emergencyDismissText.ifBlank {
            "I am wide awake and ready to start my day"
        }
        val cleanInput = emergencyInputText.trim().replace("\\s+".toRegex(), " ")
        val cleanTarget = targetEmergencyText.trim().replace("\\s+".toRegex(), " ")
        val isMatch = cleanInput.equals(cleanTarget, ignoreCase = true)

        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency Text Dismiss")
                }
            },
            text = {
                Column {
                    Text(
                        text = "Type the emergency phrase below to turn off the alarm without scanning:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = targetEmergencyText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = emergencyInputText,
                        onValueChange = { emergencyInputText = it },
                        label = { Text("Type emergency phrase") },
                        placeholder = { Text("Type accurately...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("emergency_text_input_field"),
                        singleLine = false,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isMatch) "Phrase matched! Ready to dismiss." else "Progress: ${cleanInput.length}/${cleanTarget.length} characters (not case-sensitive)",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isMatch) Color(0xFF22C55E) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isMatch) {
                            showEmergencyDialog = false
                            onDismissSuccess()
                        }
                    },
                    enabled = isMatch,
                    colors = ButtonDefaults.buttonColors(containerColor = if (isMatch) Color(0xFF16A34A) else MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("submit_emergency_text_button")
                ) {
                    Text("Dismiss Alarm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmergencyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Manual Code / Test Simulator Dialog
    if (showManualInputDialog) {
        AlertDialog(
            onDismissRequest = { showManualInputDialog = false },
            title = { Text("Manual Barcode / QR Input") },
            text = {
                Column {
                    Text(
                        text = "If your camera is unavailable or in complete darkness, enter the barcode or QR code string manually:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (targetBarcode != null) {
                        Text(
                            text = "Target code for \"${targetBarcode.name}\": ${targetBarcode.code}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { manualCodeInput = targetBarcode.code },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Fill Target Code (Test Simulator)")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = manualCodeInput,
                        onValueChange = { manualCodeInput = it },
                        label = { Text("Barcode/QR Code String") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_barcode_input_field"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (verifyScannedCode(manualCodeInput)) {
                            showManualInputDialog = false
                            onDismissSuccess()
                        }
                    },
                    enabled = manualCodeInput.isNotBlank(),
                    modifier = Modifier.testTag("submit_manual_barcode_button")
                ) {
                    Text("Verify & Dismiss")
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualInputDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
