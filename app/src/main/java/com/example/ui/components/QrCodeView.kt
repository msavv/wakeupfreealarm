package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter

@Composable
fun QrCodeView(
    content: String,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 180.dp,
    format: BarcodeFormat = BarcodeFormat.QR_CODE
) {
    val bitmap = remember(content, format) {
        generateBarcodeBitmap(content, format, 400, 400)
    }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "QR Code Preview for $content",
                modifier = Modifier.size(sizeDp - 16.dp)
            )
        }
    }
}

private fun generateBarcodeBitmap(
    text: String,
    format: BarcodeFormat,
    width: Int,
    height: Int
): Bitmap? {
    if (text.isEmpty()) return null
    return try {
        val bitMatrix = MultiFormatWriter().encode(text, format, width, height)
        val matrixWidth = bitMatrix.width
        val matrixHeight = bitMatrix.height
        val pixels = IntArray(matrixWidth * matrixHeight)
        for (y in 0 until matrixHeight) {
            val offset = y * matrixWidth
            for (x in 0 until matrixWidth) {
                pixels[offset + x] = if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            }
        }
        val bitmap = Bitmap.createBitmap(matrixWidth, matrixHeight, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, matrixWidth, 0, 0, matrixWidth, matrixHeight)
        bitmap
    } catch (e: Exception) {
        null
    }
}
