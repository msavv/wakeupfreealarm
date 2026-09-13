package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_barcodes")
data class SavedBarcodeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val code: String,
    val format: String = "QR_CODE", // QR_CODE, EAN_13, CODE_128, UPC_A, etc.
    val createdTimestamp: Long = System.currentTimeMillis()
)
