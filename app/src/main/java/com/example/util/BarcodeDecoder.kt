package com.example.util

import androidx.camera.core.ImageProxy
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer
import java.util.EnumMap

object BarcodeDecoder {

    private val reader = MultiFormatReader().apply {
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        hints[DecodeHintType.TRY_HARDER] = java.lang.Boolean.TRUE
        hints[DecodeHintType.CHARACTER_SET] = "UTF-8"
        setHints(hints)
    }

    data class ScanResult(
        val text: String,
        val format: String
    )

    /**
     * Fallback decoder using ZXing with correct rowStride alignment and rotation support.
     */
    fun decodeImageProxy(image: ImageProxy): ScanResult? {
        val planes = image.planes
        if (planes.isEmpty()) return null

        val plane = planes[0]
        val buffer: ByteBuffer = plane.buffer
        val rowStride = plane.rowStride
        val width = image.width
        val height = image.height

        // Copy row by row to eliminate rowStride alignment padding
        val yBytes = ByteArray(width * height)
        val rowBuffer = ByteArray(rowStride)

        val originalPos = buffer.position()
        for (row in 0 until height) {
            buffer.position(originalPos + row * rowStride)
            val bytesToRead = minOf(rowStride, buffer.remaining())
            buffer.get(rowBuffer, 0, bytesToRead)
            System.arraycopy(rowBuffer, 0, yBytes, row * width, width)
        }

        // Apply sensor rotation
        val rotationDegrees = image.imageInfo.rotationDegrees
        val (rotatedData, rotW, rotH) = when (rotationDegrees) {
            90 -> {
                val rotated = ByteArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotated[x * height + (height - 1 - y)] = yBytes[y * width + x]
                    }
                }
                Triple(rotated, height, width)
            }
            180 -> {
                val rotated = ByteArray(width * height)
                val total = width * height
                for (i in 0 until total) {
                    rotated[total - 1 - i] = yBytes[i]
                }
                Triple(rotated, width, height)
            }
            270 -> {
                val rotated = ByteArray(width * height)
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        rotated[(width - 1 - x) * height + y] = yBytes[y * width + x]
                    }
                }
                Triple(rotated, height, width)
            }
            else -> Triple(yBytes, width, height)
        }

        val source = PlanarYUVLuminanceSource(
            rotatedData,
            rotW,
            rotH,
            0,
            0,
            rotW,
            rotH,
            false
        )

        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        return try {
            val result = reader.decodeWithState(binaryBitmap)
            reader.reset()
            ScanResult(
                text = result.text,
                format = result.barcodeFormat.name
            )
        } catch (_: Exception) {
            reader.reset()
            null
        }
    }
}
