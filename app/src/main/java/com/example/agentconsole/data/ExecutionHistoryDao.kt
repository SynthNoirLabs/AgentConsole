package com.example.agentconsole.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionHistoryDao {
    @Insert
    suspend fun insert(entry: ExecutionHistory)

    @Query("SELECT * FROM execution_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ExecutionHistory>>

    @Query("DELETE FROM execution_history WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)
}
