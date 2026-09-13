package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val use24HourClock: Boolean = false,
    val emergencyDismissText: String = "I am wide awake and ready to start my day",
    val defaultMuteGraceSeconds: Int = 30,
    val defaultAlarmVolume: Int = 100,
    val lockVolumeButtons: Boolean = true,
    val qrOnlyDismiss: Boolean = true,
    val hasCompletedPermissionSetup: Boolean = false,
    val defaultMaxMuteCount: Int = 3 // 0 = disabled, 1, 2, 3, 5, -1 = unlimited
)
