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
                callerUid = 10999,
                requiredPermissions = "android.permission.RECEIVE_BOOT_COMPLETED",
                extrasSize = 0,
                extrasDump = null,
                dispatchTime = now + 10,
                finishTime = now + 250,
                deliveryStatus = "DELIVERED",
                skipReasons = null,
                totalReceiverCount = 1,
                deliveredReceivers = "com.evil.spyware.dummy",
                skippedReceivers = null,
                cvssVector = "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H",
                cvssBaseScore = 9.3
            ),
            IntentRecord(
                id = "b",
                timestamp = now - 5000,
                action = "android.intent.action.SMS_RECEIVED",
                callerPackage = "com.example.flashlight",
                callerUid = 10245,
                requiredPermissions = "android.permission.RECEIVE_SMS",
                extrasSize = 2,
                extrasDump = "Bundle[{pdus=[data_bytes], format=3gpp}]",
                dispatchTime = now - 4995,
                finishTime = now - 4950,
                deliveryStatus = "PARTIALLY_SKIPPED",
                skipReasons = "Background execution not allowed",
                totalReceiverCount = 2,
                deliveredReceivers = "com.android.phone",
                skippedReceivers = "com.example.flashlight",
                cvssVector = "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:N",
                cvssBaseScore = 7.9
            ),
            IntentRecord(
                id = "c",
                timestamp = now - 15000,
                action = "com.example.malicious.TRIGGER_PHISHING",
                callerPackage = "com.example.mamzleintencje",
                callerUid = 10211,
                requiredPermissions = null,
                extrasSize = 1,
                extrasDump = "Bundle[{url=http://phishing-test.com}]",
                dispatchTime = now - 14999,
                finishTime = now - 14995,
                deliveryStatus = "DELIVERED",
                skipReasons = null,
                totalReceiverCount = 1,
                deliveredReceivers = "com.android.browser",
                skippedReceivers = null,
                cvssVector = "CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:H/I:L/A:N",
                cvssBaseScore = 6.1
            ),
            IntentRecord(
                id = "d",
                timestamp = now - 60000,
                action = "android.intent.action.SCREEN_OFF",
                callerPackage = "com.android.systemui",
                callerUid = 1000,
                requiredPermissions = null,
                extrasSize = 0,
                extrasDump = null,
                dispatchTime = now - 59998,
                finishTime = now - 59980,
                deliveryStatus = "DELIVERED",
                skipReasons = null,
                totalReceiverCount = 12,
                deliveredReceivers = "com.android.systemui, com.google.android.gms, com.example.mamzleintencje",
                skippedReceivers = null,
                cvssVector = "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N",
                cvssBaseScore = 0.0
            )
        )
    }
}