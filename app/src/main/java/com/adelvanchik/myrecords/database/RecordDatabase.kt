package com.adelvanchik.myrecords.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RecordingItem::class], version = 1, exportSchema = false)
abstract class RecordDatabase: RoomDatabase() {

    abstract val recordDatabaseDao: RecordDatabaseDao

    companion object {

        private const val DATABASE_NAME = "record_app_database"

        @Volatile
        private var INSTANCE: RecordDatabase? = null

        fun getInstance(context: Context): RecordDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        RecordDatabase::class.java,
                        DATABASE_NAME,
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance

            }
        }
    }

}