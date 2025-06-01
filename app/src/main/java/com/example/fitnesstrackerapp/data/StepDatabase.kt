package com.example.fitnesstrackerapp.data

import android.content.Context
import androidx.room.*
import com.example.fitnesstrackerapp.model.StepEntry

@Dao
interface StepDao {
    @Insert
    suspend fun insert(entry: StepEntry)

    @Query("SELECT * FROM steps ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatest(): StepEntry?
}

@Database(entities = [StepEntry::class], version = 1)
abstract class StepDatabase : RoomDatabase() {
    abstract fun stepDao(): StepDao

    companion object {
        @Volatile private var INSTANCE: StepDatabase? = null

        fun getDatabase(context: Context): StepDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    StepDatabase::class.java,
                    "step_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}