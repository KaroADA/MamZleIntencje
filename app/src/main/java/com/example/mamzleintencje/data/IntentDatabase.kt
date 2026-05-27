package com.example.mamzleintencje.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [IntentRecord::class], version = 1, exportSchema = false)
abstract class IntentDatabase : RoomDatabase() {
    abstract fun intentRecordDao(): IntentRecordDao

    companion object {
        @Volatile
        private var INSTANCE: IntentDatabase? = null

        fun getDatabase(context: Context): IntentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IntentDatabase::class.java,
                    "intent_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}