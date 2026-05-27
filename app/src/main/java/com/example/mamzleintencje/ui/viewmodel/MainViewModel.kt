package com.example.mamzleintencje.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.mamzleintencje.MonitorState
import com.example.mamzleintencje.data.IntentRecord
import com.example.mamzleintencje.data.IntentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MainViewModel : ViewModel() {
    private fun getMockIntents(): List<IntentRecord> {
        val now = System.currentTimeMillis()
        return listOf(
            IntentRecord(
                id = 1,
                timestamp = now,
                action = "android.intent.action.BOOT_COMPLETED",
                callerPackage = "com.evil.spyware.dummy",
                targetComponent = null,
                intentType = IntentType.BROADCAST,
                extrasSize = 0,
                extrasDump = null,
                threatScore = 95
            ),
            IntentRecord(
                id = 2,
                timestamp = now - 5000,
                action = "android.intent.action.SMS_RECEIVED",
                callerPackage = "com.example.flashlight",
                targetComponent = null,
                intentType = IntentType.BROADCAST,
                extrasSize = 124,
                extrasDump = "pdus data inside",
                threatScore = 88
            ),
            IntentRecord(
                id = 3,
                timestamp = now - 15000,
                action = "android.intent.action.VIEW",
                callerPackage = "com.example.mamzleintencje",
                targetComponent = "com.android.browser",
                intentType = IntentType.IMPLICIT,
                extrasSize = 42,
                extrasDump = "url: http://phishing-test.com",
                threatScore = 60
            ),
            IntentRecord(
                id = 4,
                timestamp = now - 60000,
                action = "android.intent.action.SCREEN_OFF",
                callerPackage = "com.android.systemui",
                targetComponent = null,
                intentType = IntentType.BROADCAST,
                extrasSize = 0,
                extrasDump = null,
                threatScore = 5
            )
        )
    }
}