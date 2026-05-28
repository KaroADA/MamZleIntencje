package com.example.mamzleintencje.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mamzleintencje.data.IntentRecord
import com.example.mamzleintencje.data.IntentRecordDao
import com.example.mamzleintencje.data.IntentType
import com.example.mamzleintencje.monitor.MonitorState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application, dao: IntentRecordDao) : AndroidViewModel(application) {
    private val sharedPrefs = application.getSharedPreferences("monitor_settings", Context.MODE_PRIVATE)
    private val _monitorState = MutableStateFlow<MonitorState>(MonitorState.Connecting)
    val monitorState = _monitorState.asStateFlow()

    data class FilterState(
        val minCvss: Double = 0.0,
        val hideSystemApps: Boolean = false,
        val searchQuery: String = "",
        val allowedStatuses: Set<String> = setOf("DELIVERED", "PARTIALLY_SKIPPED", "SKIPPED", "DEFERRED"),
        val hasExtras: Boolean = false,
        val requiresPermission: Boolean? = null,
        val intentType: IntentType? = null
    )

    private val _filterState = MutableStateFlow(FilterState())
    val filterState = _filterState.asStateFlow()

    // --- LOGIKA FILTROWANIA ---
    // Łączymy wszystkie rekordy z bazy z aktualnym stanem filtrów
    val intentRecords = _filterState.flatMapLatest { filter ->
        dao.getFilteredRecords(
            searchQuery = filter.searchQuery,
            minCvss = filter.minCvss,
            hideSystemApps = filter.hideSystemApps,
            hasExtras = filter.hasExtras,
            useStatusFilter = filter.allowedStatuses.isNotEmpty(),
            statusFilters = filter.allowedStatuses.toList()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun updateFilter(update: (FilterState) -> FilterState) {
        _filterState.value = update(_filterState.value)
        Log.d("MainViewModel", "Filter state changed: ${_filterState.value}")
    }

    fun updateMonitorState(state: MonitorState) {
        _monitorState.value = state
    }

    private val _restartSignal = MutableSharedFlow<Unit>()
    val restartSignal = _restartSignal.asSharedFlow()

    fun restartMonitor() {
        viewModelScope.launch {
            _restartSignal.emit(Unit)
        }
    }

    data class MonitorSettings(
        val workInBackground: Boolean = true,
        val fetchPeriodSeconds: Int = 30
    )

    private val _monitorSettings = MutableStateFlow(
        MonitorSettings(
            workInBackground = sharedPrefs.getBoolean("work_in_background", true),
            fetchPeriodSeconds = sharedPrefs.getInt("fetch_period_seconds", 30)
        )
    )
    val monitorSettings = _monitorSettings.asStateFlow()

    fun updateMonitorSettings(update: (MonitorSettings) -> MonitorSettings) {
        val newSettings = update(_monitorSettings.value)
        _monitorSettings.value = newSettings
        sharedPrefs.edit {
            putBoolean("work_in_background", newSettings.workInBackground)
                .putInt("fetch_period_seconds", newSettings.fetchPeriodSeconds)
        }
        Log.d("MainViewModel", "Monitor settings updated and saved: $newSettings")
    }

    companion object {
        fun getMockIntents(): List<IntentRecord> = emptyList()
    }
}