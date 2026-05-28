package com.example.mamzleintencje.monitor

import kotlin.math.ceil
import kotlin.math.pow
import java.util.Locale

class CvssCalculator {

    data class CvssResult(
        val vector: String,
        val score: Double
    )

    companion object {
        private val SIGNATURE_PERMISSIONS = setOf(
            "android.permission.INTERACT_ACROSS_USERS",
            "android.permission.WRITE_SECURE_SETTINGS",
            "android.permission.BROADCAST_STICKY"
        )

        private val CRITICAL_ACTIONS = setOf(
            "android.provider.Telephony.SMS_RECEIVED",
            "android.intent.action.NEW_OUTGOING_CALL"
        )

        private val PERSISTENCE_ACTIONS = setOf(
            "android.intent.action.BOOT_COMPLETED",
            "android.intent.action.LOCKED_BOOT_COMPLETED",
            "android.intent.action.MY_PACKAGE_REPLACED",
            "android.intent.action.PACKAGE_ADDED",
            "android.intent.action.PACKAGE_REMOVED",
            "android.intent.action.USER_PRESENT",
            "android.intent.action.SCREEN_ON",
            "android.intent.action.SCREEN_OFF",
            "com.samsung.android.ce.SCREEN_ON",
            "com.samsung.android.ce.SCREEN_OFF",
            "android.net.conn.CONNECTIVITY_CHANGE"
        )

        private val INFORMATIONAL_ACTIONS = setOf(
            "android.net.wifi.RSSI_CHANGED",
            "android.intent.action.TIME_TICK",
            "android.intent.action.BATTERY_CHANGED",
            "android.net.wifi.SCAN_RESULTS",
            "com.sec.android.diagmonagent.intent.USE_APP_FEATURE_SURVEY",
            "com.samsung.android.weather.action.WEATHER_DATA_SYNC"
        )

        private val TRUSTED_PREFIXES = listOf(
            "android",
            "com.android.",
            "com.samsung.",
            "com.sec.",
            "com.google.android."
        )

        fun calculate(
            action: String?,
            callerPackage: String?,
            callerUid: Int?,
            requiredPermissions: String?,
            extrasSize: Int,
            deliveryStatus: String?,
            deliveredReceivers: String?
        ): CvssResult {
            if (deliveryStatus == "SKIPPED") {
                return CvssResult("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N", 0.0)
            }

            val isTrustedCaller = (callerUid != null && callerUid == 1000) ||
                    (callerPackage != null && TRUSTED_PREFIXES.any { callerPackage.startsWith(it) } && callerUid != 2000)

            val isPersistenceAction = PERSISTENCE_ACTIONS.any { action?.contains(it) ?: false }
            val isInformationalAction = INFORMATIONAL_ACTIONS.any { action?.contains(it) ?: false }

            val deliveredList = deliveredReceivers?.split(",")?.map { it.trim() } ?: emptyList()
            val hasUntrustedReceiver = deliveredList.any { receiver ->
                val receiverPkg = extractPackage(receiver)
                !isTrustedPackage(receiverPkg)
            }

            val isCustomAction = action != null && !action.startsWith("android.") && !action.startsWith("com.android.")
            val isSuspicious = !isTrustedCaller && (isCustomAction || callerUid == 2000)

            if (isInformationalAction) {
                return CvssResult("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N", 0.0)
            }

            if (extrasSize == 0 && !isSuspicious && !isPersistenceAction) {
                return CvssResult("CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N", 0.0)
            }

            val av = "L"; val ac = "L"; val ui = "N"

            val pr = when {
                requiredPermissions.isNullOrEmpty() -> "N"
                SIGNATURE_PERMISSIONS.contains(requiredPermissions) ||
                        requiredPermissions.contains("SIGNATURE", ignoreCase = true) -> "H"
                else -> "L"
            }

            val crossedBoundary = deliveredList.any { receiver ->
                val receiverPkg = extractPackage(receiver)
                !isSameOrganization(callerPackage, receiverPkg) && receiverPkg != "system"
            }

            val s = if (crossedBoundary && !isTrustedCaller) "C" else "U"

            var c = "N"; var i = "N"; val a = "N"

            when {
                CRITICAL_ACTIONS.any { action?.contains(it) ?: false } -> {
                    c = "H"; i = "N"
                }
                isSuspicious -> {
                    if (callerUid == 2000) {
                        c = "H"; i = "H"
                    } else {
                        c = "L"; i = "L"
                    }
                }
                isPersistenceAction -> {
                    i = if (!isTrustedCaller && hasUntrustedReceiver) "L" else "N"
                }
                isTrustedCaller -> {
                    if (hasUntrustedReceiver && extrasSize > 0) {
                        c = "L"; i = "L"
                    } else {
                        c = "N"; i = "N"
                    }
                }
                s == "C" -> {
                    c = "H"; i = "H"
                }
                else -> {
                    c = "L"; i = "L"
                }
            }

            val vector = "CVSS:3.1/AV:$av/AC:$ac/PR:$pr/UI:$ui/S:$s/C:$c/I:$i/A:$a"
            val score = calculateBaseScore(av, ac, pr, ui, s, c, i, a)

            return CvssResult(vector, score)
        }

        private fun isTrustedPackage(pkg: String?): Boolean {
            if (pkg == null || pkg == "system") return true
            return TRUSTED_PREFIXES.any { pkg.startsWith(it) }
        }

        private fun extractPackage(receiver: String): String = receiver.substringBefore('/')

        private fun isSameOrganization(pkg1: String?, pkg2: String?): Boolean {
            if (pkg1 == null || pkg2 == null) return false
            if (pkg1 == pkg2) return true
            val p1 = pkg1.split("."); val p2 = pkg2.split(".")
            return p1.size >= 2 && p2.size >= 2 && p1[0] == p2[0] && p1[1] == p2[1]
        }

        private fun calculateBaseScore(av: String, ac: String, pr: String, ui: String, s: String, c: String, i: String, a: String): Double {
            val avVal = 0.55; val acVal = 0.77; val uiVal = 0.85
            val prVal = if (s == "U") (when (pr) { "N" -> 0.85; "L" -> 0.62; "H" -> 0.27; else -> 0.0 })
            else (when (pr) { "N" -> 0.85; "L" -> 0.68; "H" -> 0.50; else -> 0.0 })
            val cVal = when (c) { "N" -> 0.0; "L" -> 0.22; "H" -> 0.56; else -> 0.0 }
            val iVal = when (i) { "N" -> 0.0; "L" -> 0.22; "H" -> 0.56; else -> 0.0 }
            val aVal = when (a) { "N" -> 0.0; "L" -> 0.22; "H" -> 0.56; else -> 0.0 }
            val iscBase = 1.0 - ((1.0 - cVal) * (1.0 - iVal) * (1.0 - aVal))
            val exploitability = 8.22 * avVal * acVal * prVal * uiVal
            return if (s == "U") {
                val isc = 6.42 * iscBase
                if (isc <= 0) 0.0 else roundup(minOf(isc + exploitability, 10.0))
            } else {
                val isc = 7.52 * (iscBase - 0.029) - 3.25 * (iscBase - 0.02).pow(15.0)
                if (isc <= 0) 0.0 else roundup(minOf(1.08 * (isc + exploitability), 10.0))
            }
        }

        private fun roundup(input: Double): Double {
            return kotlin.math.ceil(input * 10.0) / 10.0
        }
    }
}