package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SavedBarcodeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedBarcodeDao {
    @Query("SELECT * FROM saved_barcodes ORDER BY createdTimestamp DESC")
    fun getAllSavedBarcodes(): Flow<List<SavedBarcodeEntity>>

    @Query("SELECT * FROM saved_barcodes WHERE id = :id")
    suspend fun getBarcodeById(id: Long): SavedBarcodeEntity?

    @Query("SELECT * FROM saved_barcodes WHERE code = :code LIMIT 1")
    suspend fun getBarcodeByCode(code: String): SavedBarcodeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBarcode(barcode: SavedBarcodeEntity): Long

    @Update
    suspend fun updateBarcode(barcode: SavedBarcodeEntity)

    @Delete
    suspend fun deleteBarcode(barcode: SavedBarcodeEntity)

    @Query("SELECT COUNT(*) FROM saved_barcodes")
    suspend fun getCount(): Int
}
