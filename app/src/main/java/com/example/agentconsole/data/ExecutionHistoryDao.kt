package com.example.agentconsole.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionHistoryDao {
    @Insert
    suspend fun insert(entry: ExecutionHistory)

    @Query("SELECT * FROM execution_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ExecutionHistory>>

    @Query("SELECT * FROM execution_history ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ExecutionHistory>>

    @Query("DELETE FROM execution_history WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    companion object {
        const val DEFAULT_HISTORY_LIMIT = 200
        const val DEFAULT_RETENTION_DAYS = 30L
    }
}
