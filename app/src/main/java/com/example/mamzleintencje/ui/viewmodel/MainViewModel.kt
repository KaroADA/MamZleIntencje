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
    val intentRecords = combine(
        dao.getAllRecords(),
        _filterState
    ) { records, filter ->
        records.filter { record ->
            // 1. Wyszukiwarka (szuka w nazwie akcji lub paczce)
            val matchesSearch = filter.searchQuery.isBlank() ||
                    record.action?.contains(filter.searchQuery, ignoreCase = true) == true ||
                    record.callerPackage?.contains(filter.searchQuery, ignoreCase = true) == true

            // 2. Ukrywanie aplikacji systemowych
            val isSystemApp = record.callerPackage == "system_server" ||
                    record.callerPackage?.startsWith("com.android") == true ||
                    record.callerPackage?.startsWith("android") == true
            val matchesSystem = if (filter.hideSystemApps) !isSystemApp else true

            // 3. Poziom ryzyka (CVSS)
            val matchesCvss = record.cvssBaseScore >= filter.minCvss

            // 4. Obiekty Extras
            val matchesExtras = if (filter.hasExtras) record.extrasSize > 0 else true

            // 5. Wymagane uprawnienia
            val matchesPermissions = when (filter.requiresPermission) {
                true -> !record.requiredPermissions.isNullOrBlank()
                false -> record.requiredPermissions.isNullOrBlank()
                null -> true
            }

            // 6. Statusy dostarczenia
            val recordStatus = record.deliveryStatus ?: "UNKNOWN"
            val matchesStatus = filter.allowedStatuses.contains(recordStatus)

            // 7. Typ Intentu - ZIGNOROWANY (zawsze zwraca true)
            val matchesType = true

            // Zwracamy rekord tylko, gdy spełnia WSZYSTKIE powyższe warunki
            matchesSearch && matchesSystem && matchesCvss && matchesExtras && matchesPermissions && matchesStatus && matchesType
        }
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