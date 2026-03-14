package com.example.agentconsole.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ExecutionHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun executionHistoryDao(): ExecutionHistoryDao
}
