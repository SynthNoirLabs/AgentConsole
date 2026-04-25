package com.example.agentconsole

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agentconsole.data.ExecutionHistory
import com.example.agentconsole.data.ExecutionHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val dao: ExecutionHistoryDao
) : ViewModel() {

    val history: StateFlow<List<ExecutionHistory>> =
        dao.getRecent(ExecutionHistoryDao.DEFAULT_HISTORY_LIMIT)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

    init {
        viewModelScope.launch {
            try {
                val cutoff = System.currentTimeMillis() -
                    TimeUnit.DAYS.toMillis(ExecutionHistoryDao.DEFAULT_RETENTION_DAYS)
                dao.deleteOlderThan(cutoff)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to prune old history entries", e)
            }
        }
    }

    companion object {
        private const val TAG = "HistoryViewModel"
    }
}
