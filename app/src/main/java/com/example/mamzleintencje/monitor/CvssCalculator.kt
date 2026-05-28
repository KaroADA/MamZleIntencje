package com.example.mamzleintencje.monitor

import android.util.Log
import kotlin.math.ceil
import kotlin.math.pow

/**
 * Optimized CVSS Calculator for Android IPC Monitoring.
 */
object CvssCalculator {

    data class CvssResult(
        val vector: String,
        val score: Double
    )

    private const val TAG = "CvssCalculator"
    private const val SYSTEM_UID_THRESHOLD = 10000

    private val TRUSTED_PREFIXES = arrayOf(
        "android.",
        "com.android.",
        "com.samsung.",
        "com.sec.",
        "com.google.android.",
        "com.qualcomm.",
        "org.microg.",
        "com.google.firebase.",
        "app.revanced."
    )

    private val CONSUMER_APPS = setOf(
        "com.facebook.orca",
        "com.facebook.katana",
        "com.facebook.services",
        "com.instagram.android",
        "com.discord",
        "pl.tablica",
        "org.kde.kdeconnect_tp",
        "io.homeassistant.companion.android",
        "com.whatsapp",
        "com.google.android.apps.turbo",
        "app.revanced.android.gms",
        "com.microsoft.office.outlook",
        "com.crispim.coverspin"
    )

    private val CRITICAL_ACTIONS = setOf(
        "SMS_RECEIVED",
        "NEW_OUTGOING_CALL",
        "BATTERY_LOW",
        "REBOOT",
        "ACTION_SHUTDOWN"
    )

    private val PERSISTENCE_ACTIONS = setOf(
        "BOOT_COMPLETED",
        "LOCKED_BOOT_COMPLETED",
        "MY_PACKAGE_REPLACED",
        "PACKAGE_ADDED",
        "PACKAGE_REPLACED",
        "USER_PRESENT",
        "SCREEN_ON",
        "SCREEN_OFF"
    )

    private val BENIGN_ACTIONS = setOf(
        "RSSI_CHANGED",
        "TIME_TICK",
        "BATTERY_CHANGED",
        "BATTERY_LEVEL_CHANGED",
        "SCAN_RESULTS",
        "POWER_CONNECTED",
        "POWER_DISCONNECTED",
        "CONNECTIVITY_CHANGE",
        "RECEIVE",
        "CLOSE_SYSTEM_DIALOGS",
        "USE_APP_FEATURE_SURVEY",
        "WEATHER_DATA_SYNC",
        "ANY_DATA_STATE",
        "SIM_STATE_CHANGED",
        "USER_ACTIVITY",
        "INPUTMETHOD_STARTING",
        "RESPONSEAXT9INFO",
        "BADGE_COUNT_UPDATE",
        "DREAMING_STARTED"
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
        val act = action ?: ""
        val actUpper = act.uppercase()

        // 0. Skipped delivery is always 0.0
        if (deliveryStatus == "SKIPPED") {
            return result(act, callerPackage, callerUid, requiredPermissions, extrasSize, deliveryStatus, deliveredReceivers, "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N", 0.0)
        }

        if (BENIGN_ACTIONS.any { actUpper.contains(it) }) {
            return result(act, callerPackage, callerUid, requiredPermissions, extrasSize, deliveryStatus, deliveredReceivers, "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N", 0.0)
        }

        // 1. Identify Trusted Entities
        val isTrustedCaller = (callerUid != null && (callerUid < SYSTEM_UID_THRESHOLD || callerUid == -1)) ||
                (callerPackage != null && isTrusted(callerPackage))

        val receivers = deliveredReceivers?.split(Regex("[,\\s\\n\\r]+"))
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() } ?: emptyList()

        var hasSuspiciousReceiver = false
        for (receiver in receivers) {
            val pkg = receiver.substringBefore('/')
            if (!isTrusted(pkg) && !CONSUMER_APPS.any { pkg.startsWith(it) }) {
                hasSuspiciousReceiver = true
                break
            }
        }

        // 2. Classify Action
        val isInformational = BENIGN_ACTIONS.any { actUpper.contains(it) } ||
                actUpper.contains("TELEMETRY") || actUpper.contains("STATUS_BAR") || actUpper.contains("DIAGMON")
        val isPersistence = PERSISTENCE_ACTIONS.any { actUpper.contains(it) }
        val isCritical = CRITICAL_ACTIONS.any { actUpper.contains(it) }
        val isCustomAction = !act.startsWith("android.") && !act.startsWith("com.android.")

        // 3. Boundary Crossing (Caller vs Receivers)
        var crossedBorders = false
        for (receiver in receivers) {
            val recPkg = receiver.substringBefore('/')
            if (!isSameOrg(callerPackage, recPkg) && recPkg != "system" && recPkg != "android") {
                crossedBorders = true
                break
            }
        }

        // 4. Decision Engine
        var av = "L"; var ac = "L"; var pr = "N"; var ui = "N"
        var s = "U"; var c = "N"; var i = "N"; var a = "N"

        when {
            (isInformational || isPersistence) && hasSuspiciousReceiver -> {
                c = "L"
                if (isPersistence) {
                    i = "L"
                    if (!isTrustedCaller) s = "C"
                }
                pr = if (isTrustedCaller) "N" else "L"
            }

            // Trusted caller + no suspicious receivers = 0.0
            isTrustedCaller && !hasSuspiciousReceiver && !isCritical -> {
                return result(act, callerPackage, callerUid, requiredPermissions, extrasSize, deliveryStatus, deliveredReceivers, "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N", 0.0)
            }

            // Custom IPC Risk
            !isTrustedCaller && isCustomAction && crossedBorders -> {
                s = "C"; c = "H"; i = "H"
            }

            // Shell/Root activity
            callerUid == 2000 -> {
                s = "C"; c = "H"; i = "H"; a = "L"
            }

            // Critical System Events
            isCritical -> {
                c = "H"; i = "L"
                if (hasSuspiciousReceiver) s = "C"
            }

            else -> {
                if (!hasSuspiciousReceiver && !crossedBorders) {
                    return result(act, callerPackage, callerUid, requiredPermissions, extrasSize, deliveryStatus, deliveredReceivers, "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N", 0.0)
                }
                c = "L"
                if (!isTrustedCaller) i = "L"
            }
        }

        val vector = "CVSS:3.1/AV:$av/AC:$ac/PR:$pr/UI:$ui/S:$s/C:$c/I:$i/A:$a"
        val score = calculateScore(av, ac, pr, ui, s, c, i, a)

        return result(act, callerPackage, callerUid, requiredPermissions, extrasSize, deliveryStatus, deliveredReceivers, vector, score)
    }

    private fun isTrusted(pkg: String): Boolean {
        if (pkg == "system" || pkg == "android") return true
        for (prefix in TRUSTED_PREFIXES) {
            if (pkg.startsWith(prefix)) return true
        }
        return false
    }

    private fun isSameOrg(p1: String?, p2: String?): Boolean {
        if (p1 == null || p2 == null) return false
        if (p1 == p2) return true
        val dot1 = p1.indexOf('.'); if (dot1 == -1) return false
        val dot2 = p1.indexOf('.', dot1 + 1)
        val org = if (dot2 == -1) p1 else p1.substring(0, dot2)
        return p2.startsWith(org)
    }

    private fun result(act: String, pkg: String?, uid: Int?, perms: String?, extras: Int, status: String?, receivers: String?, vector: String, score: Double): CvssResult {
        Log.d(TAG, "Action: $act, Caller: $pkg ($uid), Perms: $perms, Extras: $extras, Status: $status, Receivers: $receivers -> $vector ($score)")
        return CvssResult(vector, score)
    }

    private fun calculateScore(av: String, ac: String, pr: String, ui: String, s: String, c: String, i: String, a: String): Double {
        val avV = 0.55; val acV = 0.77; val uiV = 0.85
        val prV = if (s == "U") {
            when (pr) { "N" -> 0.85; "L" -> 0.62; "H" -> 0.27; else -> 0.0 }
        } else {
            when (pr) { "N" -> 0.85; "L" -> 0.68; "H" -> 0.50; else -> 0.0 }
        }

        val cV = when (c) { "H" -> 0.56; "L" -> 0.22; else -> 0.0 }
        val iV = when (i) { "H" -> 0.56; "L" -> 0.22; else -> 0.0 }
        val aV = when (a) { "H" -> 0.56; "L" -> 0.22; else -> 0.0 }

        val iss = 1.0 - ((1.0 - cV) * (1.0 - iV) * (1.0 - aV))
        val exploitability = 8.22 * avV * acV * prV * uiV

        return if (s == "U") {
            if (iss <= 0) 0.0 else roundup(minOf(6.42 * iss + exploitability, 10.0))
        } else {
            if (iss <= 0.029) 0.0 else {
                val base = 7.52 * (iss - 0.029) - 3.25 * (iss - 0.02).pow(15.0)
                roundup(minOf(1.08 * (base + exploitability), 10.0))
            }
        }
    }

    private fun roundup(input: Double): Double = ceil(input * 10.0) / 10.0
}
