package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Contact
import com.example.data.model.Interaction
import com.example.data.model.Project
import com.example.data.model.ProjectContactLink

@Database(
    entities = [
        Contact::class,
        Interaction::class,
        Project::class,
        ProjectContactLink::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "contact_ledger_database"
                )
                .fallbackToDestructiveMigration() // safe for local dev prototyping
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
