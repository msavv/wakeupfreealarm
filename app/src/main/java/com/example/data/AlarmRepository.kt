package com.example.data

import com.example.data.dao.AlarmDao
import com.example.data.dao.AppSettingsDao
import com.example.data.dao.SavedBarcodeDao
import com.example.data.model.AlarmEntity
import com.example.data.model.AppSettingsEntity
import com.example.data.model.SavedBarcodeEntity
import kotlinx.coroutines.flow.Flow

class AlarmRepository(
    private val alarmDao: AlarmDao,
    private val savedBarcodeDao: SavedBarcodeDao,
    private val appSettingsDao: AppSettingsDao
) {
    val allAlarms: Flow<List<AlarmEntity>> = alarmDao.getAllAlarms()
    val allSavedBarcodes: Flow<List<SavedBarcodeEntity>> = savedBarcodeDao.getAllSavedBarcodes()
    val settings: Flow<AppSettingsEntity?> = appSettingsDao.getSettings()

    suspend fun getAlarmById(id: Long): AlarmEntity? = alarmDao.getAlarmById(id)

    suspend fun insertAlarm(alarm: AlarmEntity): Long = alarmDao.insertAlarm(alarm)

    suspend fun updateAlarm(alarm: AlarmEntity) = alarmDao.updateAlarm(alarm)

    suspend fun deleteAlarm(alarm: AlarmEntity) = alarmDao.deleteAlarm(alarm)

    suspend fun setAlarmEnabled(id: Long, isEnabled: Boolean) = alarmDao.setAlarmEnabled(id, isEnabled)

    suspend fun updateSnoozeCount(id: Long, count: Int) = alarmDao.updateSnoozeCount(id, count)

    suspend fun getEnabledAlarmsSync(): List<AlarmEntity> = alarmDao.getEnabledAlarmsSync()

    suspend fun getBarcodeById(id: Long): SavedBarcodeEntity? = savedBarcodeDao.getBarcodeById(id)

    suspend fun getBarcodeByCode(code: String): SavedBarcodeEntity? = savedBarcodeDao.getBarcodeByCode(code)

    suspend fun insertBarcode(barcode: SavedBarcodeEntity): Long = savedBarcodeDao.insertBarcode(barcode)

    suspend fun updateBarcode(barcode: SavedBarcodeEntity) = savedBarcodeDao.updateBarcode(barcode)

    suspend fun deleteBarcode(barcode: SavedBarcodeEntity) = savedBarcodeDao.deleteBarcode(barcode)

    suspend fun getSettingsSync(): AppSettingsEntity {
        return appSettingsDao.getSettingsSync() ?: AppSettingsEntity().also {
            appSettingsDao.insertOrUpdateSettings(it)
        }
    }

    suspend fun updateSettings(settings: AppSettingsEntity) {
        appSettingsDao.insertOrUpdateSettings(settings)
    }

    suspend fun seedDefaultsIfEmpty() {
        val existingCount = savedBarcodeDao.getCount()
        if (existingCount == 0) {
            val defaultQr1 = SavedBarcodeEntity(
                name = "Kitchen Coffee Canister",
                code = "WAKE_UP_KITCHEN_COFFEE_2026",
                format = "QR_CODE"
            )
            val defaultQr2 = SavedBarcodeEntity(
                name = "Bathroom Mirror QR",
                code = "GOOD_MORNING_BATHROOM_MIRROR",
                format = "QR_CODE"
            )
            val defaultBarcode = SavedBarcodeEntity(
                name = "Toothpaste Tube Barcode",
                code = "012345678905",
                format = "UPC_A"
            )
            val id1 = savedBarcodeDao.insertBarcode(defaultQr1)
            savedBarcodeDao.insertBarcode(defaultQr2)
            savedBarcodeDao.insertBarcode(defaultBarcode)

            // Seed default morning alarm linked to the kitchen coffee QR
            val defaultAlarm = AlarmEntity(
                title = "Weekday Morning Rise",
                hour = 7,
                minute = 0,
                isEnabled = true,
                daysOfWeek = AlarmEntity.DAYS_WEEKDAYS,
                targetBarcodeId = id1,
                audioTitle = "Gentle Chimes",
                fadeInDurationSeconds = 30,
                vibrateEnabled = true,
                vibrationPattern = "PULSE",
                vibrationIntensity = "MEDIUM",
                snoozeDurationMinutes = 5,
                maxSnoozeCount = 3
            )
            alarmDao.insertAlarm(defaultAlarm)
        }

        if (appSettingsDao.getSettingsSync() == null) {
            appSettingsDao.insertOrUpdateSettings(AppSettingsEntity())
        }
    }
}
