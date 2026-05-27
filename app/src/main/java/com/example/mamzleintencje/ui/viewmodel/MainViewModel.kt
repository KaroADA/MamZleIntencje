package com.example.mamzleintencje.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.mamzleintencje.data.IntentRecord
import com.example.mamzleintencje.data.IntentType

class MainViewModel : ViewModel() {
    fun getMockIntents(): List<IntentRecord> {
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
                cvssVector = "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:C/C:H/I:H/A:H",
                cvssBaseScore = 9.3
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
                cvssVector = "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:H/I:H/A:N",
                cvssBaseScore = 7.9
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
                cvssVector = "CVSS:3.1/AV:L/AC:L/PR:N/UI:R/S:U/C:H/I:L/A:N",
                cvssBaseScore = 6.1
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
                cvssVector = "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N",
                cvssBaseScore = 0.0
            )
        )
    }
}