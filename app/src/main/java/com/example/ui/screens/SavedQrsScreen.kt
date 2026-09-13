package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SavedBarcodeEntity
import com.example.ui.components.CameraBarcodeScanner
import com.example.ui.components.QrCodeView

@Composable
fun SavedQrsScreen(
    savedBarcodes: List<SavedBarcodeEntity>,
    onSaveBarcode: (SavedBarcodeEntity) -> Unit,
    onDeleteBarcode: (SavedBarcodeEntity) -> Unit
) {
    val context = LocalContext.current
    var showCameraScanner by remember { mutableStateOf(false) }
    var showAddManualDialog by remember { mutableStateOf(false) }
    var editingBarcode by remember { mutableStateOf<SavedBarcodeEntity?>(null) }
    var viewingBarcode by remember { mutableStateOf<SavedBarcodeEntity?>(null) }

    var newBarcodeName by remember { mutableStateOf("") }
    var newBarcodeCode by remember { mutableStateOf("") }
    var newBarcodeFormat by remember { mutableStateOf("QR_CODE") }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showCameraScanner) {
            Box(modifier = Modifier.fillMaxSize()) {
                CameraBarcodeScanner(
                    modifier = Modifier.fillMaxSize(),
                    onBarcodeScanned = { text, format ->
                        showCameraScanner = false
                        val defaultName = "Scanned ${format.replace('_', ' ')}"
                        val entity = SavedBarcodeEntity(
                            name = defaultName,
                            code = text,
                            format = format
                        )
                        onSaveBarcode(entity)
                        Toast.makeText(context, "Saved: $defaultName", Toast.LENGTH_LONG).show()
                    }
                )
                IconButton(
                    onClick = { showCameraScanner = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .testTag("close_qr_scanner")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Screen Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Saved Codes",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage QR and barcodes for your alarms",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                newBarcodeName = ""
                                newBarcodeCode = ""
                                newBarcodeFormat = "QR_CODE"
                                showAddManualDialog = true
                            },
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                                .testTag("add_manual_barcode_button")
                        ) {
                            Icon(Icons.Default.Keyboard, contentDescription = "Manual Code")
                        }
                    }
                }

                if (savedBarcodes.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Saved QR Codes",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Scan a physical QR code or barcode (like coffee, shampoo, or bathroom item) to use it to shut off your alarm!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showCameraScanner = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("scan_first_qr_button")
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Scan Code with Camera")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { showAddManualDialog = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("enter_manual_qr_button")
                            ) {
                                Icon(Icons.Default.Keyboard, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enter Code Manually")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    val testCode = SavedBarcodeEntity(
                                        name = "Sample Bathroom QR",
                                        code = "QR_WAKEUP_TEST_123",
                                        format = "QR_CODE"
                                    )
                                    onSaveBarcode(testCode)
                                    Toast.makeText(context, "Added sample test QR code!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text("+ Add Sample Test QR Code")
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(savedBarcodes, key = { it.id }) { barcode ->
                            BarcodeItemCard(
                                barcode = barcode,
                                onViewClick = { viewingBarcode = barcode },
                                onEditClick = { editingBarcode = barcode },
                                onDeleteClick = { onDeleteBarcode(barcode) }
                            )
                        }
                    }
                }
            }

            // Extended FAB for Scan New Code
            ExtendedFloatingActionButton(
                onClick = { showCameraScanner = true },
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                text = { Text("Scan New Code") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("scan_new_qr_fab")
            )
        }
    }

    // Manual Entry Dialog
    if (showAddManualDialog) {
        AlertDialog(
            onDismissRequest = { showAddManualDialog = false },
            title = { Text("Add Code Manually") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newBarcodeName,
                        onValueChange = { newBarcodeName = it },
                        label = { Text("Friendly Name (e.g. Morning Coffee)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_name_field"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = newBarcodeCode,
                        onValueChange = { newBarcodeCode = it },
                        label = { Text("Code / Text Content") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_code_field"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newBarcodeCode.isNotBlank()) {
                            val name = newBarcodeName.ifBlank { "Custom QR" }
                            onSaveBarcode(
                                SavedBarcodeEntity(
                                    name = name,
                                    code = newBarcodeCode.trim(),
                                    format = "QR_CODE"
                                )
                            )
                            showAddManualDialog = false
                        }
                    },
                    enabled = newBarcodeCode.isNotBlank(),
                    modifier = Modifier.testTag("save_manual_code_confirm")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddManualDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Name Dialog
    if (editingBarcode != null) {
        var editName by remember { mutableStateOf(editingBarcode!!.name) }
        AlertDialog(
            onDismissRequest = { editingBarcode = null },
            title = { Text("Rename Code") },
            text = {
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Code Label") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveBarcode(editingBarcode!!.copy(name = editName.ifBlank { editingBarcode!!.name }))
                        editingBarcode = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBarcode = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // View / Print QR Dialog
    if (viewingBarcode != null) {
        val barcode = viewingBarcode!!
        AlertDialog(
            onDismissRequest = { viewingBarcode = null },
            title = { Text(barcode.name, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Type: ${barcode.format}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    QrCodeView(
                        content = barcode.code,
                        sizeDp = 200.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = barcode.code,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewingBarcode = null }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
private fun BarcodeItemCard(
    barcode: SavedBarcodeEntity,
    onViewClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewClick)
            .testTag("barcode_item_${barcode.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Small QR Icon / preview
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(50.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = barcode.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${barcode.format} • ${barcode.code}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
