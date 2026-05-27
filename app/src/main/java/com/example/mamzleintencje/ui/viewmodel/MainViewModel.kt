package com.example.mamzleintencje.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mamzleintencje.data.IntentRecord
import com.example.mamzleintencje.data.IntentType
import com.example.mamzleintencje.monitor.MonitorState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val _monitorState = MutableStateFlow<MonitorState>(MonitorState.Connecting)
    val monitorState = _monitorState.asStateFlow()

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

    data class FilterState(
        val minCvss: Double? = null,
        val intentType: String? = null,
        val hasExtras: Boolean = false,
        val hideSystemApps: Boolean = false
    )

    private val _filterState = MutableStateFlow(FilterState())
    val filterState = _filterState.asStateFlow()

    fun updateFilter(update: (FilterState) -> FilterState) {
        _filterState.value = update(_filterState.value)

        Log.d("MainViewModel", "Filter state changed: ${_filterState.value}")
    }

    fun getMockIntents(): List<IntentRecord> {
        val now = System.currentTimeMillis()
        return listOf(
            IntentRecord(
                id = "a",
                timestamp = now,
                action = "android.intent.action.BOOT_COMPLETED",
                callerPackage = "com.evil.spyware.dummy",
                targetComponent = null,
                intentType = IntentType.BROADCAST,
                extrasSize = 0,
                extrasDump = null,
                cvssVector = "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H",
                cvssBaseScore = 9.3
            ),
            IntentRecord(
                id = "b",
                timestamp = now - 5000,
                action = "android.intent.action.SMS_RECEIVED",
                callerPackage = "com.example.flashlight",
                targetComponent = null,
                intentType = IntentType.BROADCAST,
                extrasSize = 124,
                extrasDump = "pdus data inside",
                cvssVector = "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:N",
                cvssBaseScore = 7.9
            ),
            IntentRecord(
                id = "c",
                timestamp = now - 15000,
                action = "android.intent.action.VIEW",
                callerPackage = "com.example.mamzleintencje",
                targetComponent = "com.android.browser",
                intentType = IntentType.IMPLICIT,
                extrasSize = 42,
                extrasDump = "url: http://phishing-test.com",
                cvssVector = "CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:H/I:L/A:N",
                cvssBaseScore = 6.1
            ),
            IntentRecord(
                id = "d",
                timestamp = now - 60000,
                action = "android.intent.action.SCREEN_OFF",
                callerPackage = "com.android.systemui",
                targetComponent = null,
                intentType = IntentType.BROADCAST,
                extrasSize = 0,
                extrasDump = null,
                cvssVector = "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N",
                cvssBaseScore = 0.0
            )
        )
    }
}