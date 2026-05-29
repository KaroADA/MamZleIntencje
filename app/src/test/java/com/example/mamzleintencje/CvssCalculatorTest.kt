package com.example.mamzleintencje

import com.example.mamzleintencje.monitor.CvssCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CvssCalculatorTest {

    @Test
    fun testScreenOnBlame() {
        // android sending SCREEN_ON to a malicious app
        val result = CvssCalculator.calculate(
            action = "android.intent.action.SCREEN_ON",
            callerPackage = "android",
            callerUid = 1000,
            requiredPermissions = null,
            extrasSize = 0,
            deliveryStatus = "DELIVERED",
            deliveredReceivers = "com.malicious.app/com.malicious.app.Receiver"
        )

        assertFalse("Android should not be blamed for SCREEN_ON", result.blameSender)
        assertTrue("Malicious app should be in suspicious receivers", result.suspiciousReceivers.contains("com.malicious.app"))
        assertTrue("Score should be non-zero", result.score > 0)
    }

    @Test
    fun testCustomActionBlame() {
        // Malicious app sending a custom intent
        val result = CvssCalculator.calculate(
            action = "com.malicious.app.CUSTOM_ACTION",
            callerPackage = "com.malicious.app",
            callerUid = 10123,
            requiredPermissions = null,
            extrasSize = 0,
            deliveryStatus = "DELIVERED",
            deliveredReceivers = "com.another.app/com.another.app.Receiver"
        )

        assertTrue("Malicious sender should be blamed", result.blameSender)
        assertTrue("Score should be non-zero", result.score > 0)
    }

    @Test
    fun testTrustedCommunicationNoBlame() {
        // android sending to com.android.settings
        val result = CvssCalculator.calculate(
            action = "android.intent.action.SCREEN_ON",
            callerPackage = "android",
            callerUid = 1000,
            requiredPermissions = null,
            extrasSize = 0,
            deliveryStatus = "DELIVERED",
            deliveredReceivers = "com.android.settings/com.android.settings.Receiver"
        )

        assertEquals("Score should be 0.0 for trusted communication", 0.0, result.score, 0.0)
    }

    @Test
    fun testAliExpressSelfCommunication() {
        // AliExpress communicating with itself
        val result = CvssCalculator.calculate(
            action = "com.alibaba.aliexpresshd.SOME_ACTION",
            callerPackage = "com.alibaba.aliexpresshd",
            callerUid = 10378,
            requiredPermissions = null,
            extrasSize = 0,
            deliveryStatus = "DELIVERED",
            deliveredReceivers = "com.alibaba.aliexpresshd:channel/Receiver, com.alibaba.aliexpresshd/Receiver"
        )

        assertTrue("Should have Unchanged scope (S:U) for internal communication", result.vector.contains("S:U"))
        assertTrue("Score should be lower than cross-app CRITICAL", result.score < 8.0)
        assertTrue("Reasons should include internal communication", result.reasons.contains("Internal app communication"))
    }

    @Test
    fun testMaliciousInternalCommunication() {
        // App sending sensitive data to itself
        val result = CvssCalculator.calculate(
            action = "com.example.testapp.EXFILTRATE_DATA",
            callerPackage = "com.example.testapp",
            callerUid = 10566,
            requiredPermissions = "android.permission.READ_SMS",
            extrasSize = 20,
            deliveryStatus = "DELIVERED",
            deliveredReceivers = "com.example.testapp.ExfiltrateReceiver"
        )

        assertTrue("Should have non-zero score for sensitive internal action", result.score > 5.0)
        assertTrue("Reasons should include sensitive permissions", result.reasons.contains("Sensitive permissions required"))
        assertTrue("Reasons should include large payload", result.reasons.contains("Large data payload"))
        assertTrue("Scope remains Unchanged (S:U) as it is internal", result.vector.contains("S:U"))
    }
}
