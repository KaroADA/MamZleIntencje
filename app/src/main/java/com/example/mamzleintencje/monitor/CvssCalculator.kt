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
        val score: Double,
        val suspiciousReceivers: List<String> = emptyList(),
        val blameSender: Boolean = true,
        val reasons: List<String> = emptyList()
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

    private val APP_FAMILIES = listOf(
        setOf("pl.tablica", "com.naspers"),
        setOf("com.facebook", "com.instagram", "com.whatsapp", "com.oculus"),
        setOf("com.google", "app.revanced")
    )

    private val CONSUMER_APPS = setOf(
        "com.facebook.orca",
        "com.facebook.katana",
        "com.facebook.services",
        "com.instagram.android",
        "com.discord",
        "pl.tablica",
        "com.naspers.",
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
        "SCREEN_OFF",
        "ACTION_POWER_CONNECTED",
        "ACTION_POWER_DISCONNECTED"
    )

    private val BENIGN_ACTIONS = setOf(
        "RSSI_CHANGED",
        "TIME_TICK",
        "BATTERY_CHANGED",
        "BATTERY_LEVEL_CHANGED",
        "SCAN_RESULTS",
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
        "DREAMING_STARTED",
        "PUSH_DISMISSED",
        "ANALYTICS",
        "METRICS"
    )

    private val DANGEROUS_PERMISSIONS = setOf(
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CONTACTS",
        "android.permission.READ_CALL_LOG",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.READ_EXTERNAL_STORAGE"
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
        val riskReasons = mutableListOf<String>()

        // 1. Classify Action First
        val isInformational = BENIGN_ACTIONS.any { actUpper.contains(it) } ||
                actUpper.contains("TELEMETRY") || actUpper.contains("STATUS_BAR") || actUpper.contains("DIAGMON")
        val isPersistence = PERSISTENCE_ACTIONS.any { actUpper.contains(it) }
        val isCritical = CRITICAL_ACTIONS.any { actUpper.contains(it) }
        val isCustomAction = act.contains(".") && !isTrusted(act.substringBeforeLast('.'))

        // Shell/Root Heuristic
        val isShellOrRoot = callerUid == 2000 || callerUid == 0
        val isStandardSystemAction = actUpper.startsWith("ANDROID.") || actUpper.startsWith("COM.ANDROID.") || isInformational || isPersistence

        if (isInformational && !actUpper.contains("POWER") && !isShellOrRoot) {
            return result(act, callerPackage, callerUid, requiredPermissions, extrasSize, deliveryStatus, deliveredReceivers, "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N", 0.0, reasons = listOf("Benign informational action"))
        }

        if (isShellOrRoot && !isStandardSystemAction) riskReasons.add("Arbitrary shell/root broadcast detected")
        if (isCritical) riskReasons.add("Critical system action")
        if (isPersistence) riskReasons.add("Persistence-related action")

        // 2. Identify Trusted Entities & Anti-Spoofing
        val isSystemUid = callerUid != null && (callerUid < SYSTEM_UID_THRESHOLD || callerUid == -1)
        val isTrustedPkg = callerPackage != null && isTrusted(callerPackage)

        val isTrustedCaller = isSystemUid || (isTrustedPkg && !isCustomAction)
        if (!isTrustedCaller && !isShellOrRoot) riskReasons.add("Untrusted caller")

        val receivers = deliveredReceivers?.split(Regex("[,\\s\\n\\r]+"))
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() } ?: emptyList()

        val suspiciousReceiversList = mutableListOf<String>()
        val baseCallerPkg = callerPackage?.substringBefore(':')

        for (receiver in receivers) {
            val pkg = receiver.substringBefore('/')
            val baseReceiverPkg = pkg.substringBefore(':')

            if (baseReceiverPkg != baseCallerPkg && !isSameOrg(baseCallerPkg, baseReceiverPkg) && !isTrusted(baseReceiverPkg) && !CONSUMER_APPS.any { baseReceiverPkg.startsWith(it) }) {
                suspiciousReceiversList.add(pkg)
            }
        }
        val hasSuspiciousReceiver = suspiciousReceiversList.isNotEmpty()
        if (hasSuspiciousReceiver) riskReasons.add("Suspicious receivers detected")

        // 3. Boundary Crossing (Caller vs Receivers)
        var crossedBorders = false
        var allInternal = true
        if (receivers.isEmpty()) allInternal = false

        for (receiver in receivers) {
            val recPkg = receiver.substringBefore('/').substringBefore(':')

            if (!isSameOrg(baseCallerPkg, recPkg) && recPkg != "system" && recPkg != "android") {
                crossedBorders = true
                allInternal = false
            }
        }

        if (crossedBorders) riskReasons.add("Organization boundary crossing")
        if (allInternal && receivers.isNotEmpty()) riskReasons.add("Internal app communication")

        // 4. Supplemental Heuristics
        val hasDangerousPermission = requiredPermissions?.split(",")?.any { perm ->
            DANGEROUS_PERMISSIONS.contains(perm.trim())
        } ?: false
        if (hasDangerousPermission) riskReasons.add("Sensitive permissions required")

        if (extrasSize > 10) riskReasons.add("Large data payload")

        // 5. Decision Engine
        var av = "L"; var ac = "L"; var pr = "N"; var ui = "N"
        var s = "U"; var c = "N"; var i = "N"; var a = "N"

        when {
            // Unrestricted Shell/Root Broadcasts
            isShellOrRoot && !isStandardSystemAction -> {
                s = "C"; c = "H"; i = "H"; a = "L"
            }

            // Passive Sniffing
            (isInformational || isPersistence) && hasSuspiciousReceiver -> {
                c = "L"
                if (isPersistence) {
                    i = "L"
                    if (!isTrustedCaller) s = "C"
                }
                pr = if (isTrustedCaller) "N" else "L"
            }

            // Trusted Caller Routine
            isTrustedCaller && !hasSuspiciousReceiver && !isCritical -> {
                return result(act, callerPackage, callerUid, requiredPermissions, extrasSize, deliveryStatus, deliveredReceivers, "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N", 0.0, reasons = listOf("Trusted system traffic"))
            }

            // Custom IPC Risk
            !isTrustedCaller && isCustomAction && (crossedBorders || hasSuspiciousReceiver) -> {
                s = "C"; c = "H"; i = "H"
            }

            // Behavior-Based Malware Traps (Catches attempts even if blocked by OS)
            !isTrustedCaller && (hasDangerousPermission || extrasSize > 10) -> {
                s = "U"
                c = if (hasDangerousPermission) "H" else "L"
                i = if (hasDangerousPermission) "H" else "N"
                if (deliveryStatus == "SKIPPED" || receivers.isEmpty()) {
                    riskReasons.add("Attempted exploit blocked by OS sandbox")
                }
            }

            // Internal Communication Mitigation
            allInternal && !isCritical -> {
                c = if (extrasSize > 5) "L" else "N"
                s = "U"
            }

            // Critical System Events
            isCritical -> {
                c = "H"; i = "L"
                if (hasSuspiciousReceiver) s = "C"
            }

            else -> {
                if (!hasSuspiciousReceiver && !crossedBorders) {
                    val msg = if (deliveryStatus == "SKIPPED") "Benign skipped traffic" else "Benign local traffic"
                    return result(act, callerPackage, callerUid, requiredPermissions, extrasSize, deliveryStatus, deliveredReceivers, "CVSS:3.1/AV:L/AC:L/PR:N/UI:N/S:U/C:N/I:N/A:N", 0.0, reasons = listOf(msg))
                }
                c = "L"
                if (!isTrustedCaller) i = "L"
            }
        }

        val vector = "CVSS:3.1/AV:$av/AC:$ac/PR:$pr/UI:$ui/S:$s/C:$c/I:$i/A:$a"
        val score = calculateScore(av, ac, pr, ui, s, c, i, a)

        val blameSender = !isTrustedCaller || isCustomAction || isCritical || (isShellOrRoot && !isStandardSystemAction) || hasDangerousPermission

        return result(act, callerPackage, callerUid, requiredPermissions, extrasSize, deliveryStatus, deliveredReceivers, vector, score, suspiciousReceiversList, blameSender, riskReasons)
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

        val baseP1 = p1.substringBefore(':')
        val baseP2 = p2.substringBefore(':')
        if (baseP1 == baseP2) return true

        for (family in APP_FAMILIES) {
            val p1InFamily = family.any { baseP1.startsWith(it) }
            val p2InFamily = family.any { baseP2.startsWith(it) }
            if (p1InFamily && p2InFamily) return true
        }

        val dot1 = baseP1.indexOf('.'); if (dot1 == -1) return false
        val dot2 = baseP1.indexOf('.', dot1 + 1)
        val org = if (dot2 == -1) baseP1 else baseP1.substring(0, dot2)
        return baseP2.startsWith(org)
    }

    private fun result(act: String, pkg: String?, uid: Int?, perms: String?, extras: Int, status: String?, receivers: String?, vector: String, score: Double, suspicious: List<String> = emptyList(), blameSender: Boolean = true, reasons: List<String> = emptyList()): CvssResult {
        try {
            Log.d(TAG, "Action: $act, Caller: $pkg ($uid), Perms: $perms, Extras: $extras, Status: $status, Receivers: $receivers -> $vector ($score) [Reasons: $reasons]")
        } catch (e: Throwable) {
            // Probably in unit test
        }
        return CvssResult(vector, score, suspicious, blameSender, reasons)
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