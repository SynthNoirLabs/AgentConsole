package com.example.agentconsole

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agentconsole.data.ExecutionHistory
import com.example.agentconsole.data.ExecutionHistoryDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val dao: ExecutionHistoryDao
) : ViewModel() {

    val history: StateFlow<List<ExecutionHistory>> = dao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
