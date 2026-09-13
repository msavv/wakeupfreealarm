package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.AlarmDao
import com.example.data.dao.AppSettingsDao
import com.example.data.dao.SavedBarcodeDao
import com.example.data.model.AlarmEntity
import com.example.data.model.AppSettingsEntity
import com.example.data.model.SavedBarcodeEntity

@Database(
    entities = [AlarmEntity::class, SavedBarcodeEntity::class, AppSettingsEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
    abstract fun savedBarcodeDao(): SavedBarcodeDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "qr_alarm_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
