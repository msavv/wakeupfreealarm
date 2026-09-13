package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.media.Image
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.util.BarcodeDecoder
import com.example.util.VibrationHelper
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalGetImage::class)
@Composable
fun CameraBarcodeScanner(
    modifier: Modifier = Modifier,
    onBarcodeScanned: (text: String, format: String) -> Unit,
    targetBarcodeHint: String? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isScanningLocked = remember { AtomicBoolean(false) }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VideocamOff,
                    contentDescription = "Camera Permission Needed",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Access Required",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Camera is required to scan QR codes and physical barcodes to verify and dismiss your alarm.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("request_camera_permission_button")
                ) {
                    Text("Grant Camera Permission")
                }
            }
        } else {
            // Camera Preview
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (isScanningLocked.get()) {
                                    imageProxy.close()
                                    return@setAnalyzer
                                }

                                val mediaImage: Image? = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(inputImage)
                                        .addOnSuccessListener { barcodes ->
                                            if (barcodes.isNotEmpty() && !isScanningLocked.get()) {
                                                val firstBarcode = barcodes[0]
                                                val raw = firstBarcode.rawValue ?: firstBarcode.displayValue
                                                if (!raw.isNullOrBlank()) {
                                                    if (isScanningLocked.compareAndSet(false, true)) {
                                                        val formatName = getFormatName(firstBarcode.format)
                                                        // Tactile feedback
                                                        VibrationHelper.vibrateOneShot(ctx, 120L)
                                                        ContextCompat.getMainExecutor(ctx).execute {
                                                            onBarcodeScanned(raw, formatName)
                                                        }
                                                    }
                                                }
                                            } else if (!isScanningLocked.get()) {
                                                // If ML Kit found nothing in this frame, try ZXing decoder immediately
                                                try {
                                                    val result = BarcodeDecoder.decodeImageProxy(imageProxy)
                                                    if (result != null && !isScanningLocked.get()) {
                                                        if (isScanningLocked.compareAndSet(false, true)) {
                                                            VibrationHelper.vibrateOneShot(ctx, 120L)
                                                            ContextCompat.getMainExecutor(ctx).execute {
                                                                onBarcodeScanned(result.text, result.format)
                                                            }
                                                        }
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                        }
                                        .addOnFailureListener {
                                            if (!isScanningLocked.get()) {
                                                try {
                                                    val result = BarcodeDecoder.decodeImageProxy(imageProxy)
                                                    if (result != null && !isScanningLocked.get()) {
                                                        if (isScanningLocked.compareAndSet(false, true)) {
                                                            VibrationHelper.vibrateOneShot(ctx, 120L)
                                                            ContextCompat.getMainExecutor(ctx).execute {
                                                                onBarcodeScanned(result.text, result.format)
                                                            }
                                                        }
                                                    }
                                                } catch (_: Exception) {}
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    // Fallback to ZXing
                                    val result = BarcodeDecoder.decodeImageProxy(imageProxy)
                                    if (result != null && !isScanningLocked.get()) {
                                        if (isScanningLocked.compareAndSet(false, true)) {
                                            VibrationHelper.vibrateOneShot(ctx, 120L)
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                onBarcodeScanned(result.text, result.format)
                                            }
                                        }
                                    }
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Scanning Viewfinder Overlay
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val boxSize = (minOf(maxWidth, maxHeight) * 0.72f).coerceIn(220.dp, 320.dp)

                // Laser scan animation
                val infiniteTransition = rememberInfiniteTransition(label = "laser")
                val laserProgress by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "laser_pos"
                )

                Box(
                    modifier = Modifier
                        .size(boxSize)
                        .clip(RoundedCornerShape(20.dp))
                        .border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(20.dp))
                ) {
                    // Scanning laser line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .offset(y = (boxSize - 6.dp) * laserProgress)
                            .background(MaterialTheme.colorScheme.tertiary)
                    )
                }

                // Instructions & Target Hint at top of scanner
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 52.dp, start = 20.dp, end = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = if (!targetBarcodeHint.isNullOrEmpty()) {
                                "Target: $targetBarcodeHint"
                            } else {
                                "Point at any QR code or barcode"
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Torch Toggle Button at bottom
                FilledIconButton(
                    onClick = {
                        val newTorch = !isTorchOn
                        camera?.cameraControl?.enableTorch(newTorch)
                        isTorchOn = newTorch
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (isTorchOn) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.65f),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                        .size(56.dp)
                        .testTag("torch_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Toggle Flashlight",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

private fun getFormatName(format: Int): String {
    return when (format) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_ITF -> "ITF"
        else -> "BARCODE"
    }
}
