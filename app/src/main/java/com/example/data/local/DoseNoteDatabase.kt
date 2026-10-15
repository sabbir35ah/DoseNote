package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.CaregiverLink
import com.example.data.model.DoseLog
import com.example.data.model.Medication
import com.example.data.model.Profile
import com.example.data.model.User

@Database(
    entities = [
        User::class,
        Profile::class,
        Medication::class,
        DoseLog::class,
        CaregiverLink::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DoseNoteDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun profileDao(): ProfileDao
    abstract fun medicationDao(): MedicationDao
    abstract fun doseLogDao(): DoseLogDao
    abstract fun caregiverLinkDao(): CaregiverLinkDao

    companion object {
        @Volatile
        private var INSTANCE: DoseNoteDatabase? = null

        fun getDatabase(context: Context): DoseNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DoseNoteDatabase::class.java,
                    "dosenote_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
