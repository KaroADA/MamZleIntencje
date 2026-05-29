package com.example.testapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rikka.shizuku.Shizuku
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class MainActivity : ComponentActivity() {

    private val SHIZUKU_CODE = 1001
    private var malwareReceiver: BroadcastReceiver? = null
    private var isListening = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("IntentGen", "Activity created")

        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
                Log.i("IntentGen", "Shizuku permission granted")
            } else if (requestCode == SHIZUKU_CODE) {
                Log.e("IntentGen", "Shizuku permission denied")
            }
        }

        if (Shizuku.pingBinder()) {
            Log.d("IntentGen", "Shizuku binder active")
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Log.d("IntentGen", "Requesting Shizuku permission")
                Shizuku.requestPermission(SHIZUKU_CODE)
            }
        } else {
            Log.e("IntentGen", "Shizuku binder inactive")
        }

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    IntentControlPanel()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("IntentGen", "Activity destroyed")
        Shizuku.removeRequestPermissionResultListener { _, _ -> }
        if (isListening.value) {
            unregisterReceiver(malwareReceiver)
        }
    }

    private val SMS_REQ_CODE = 1002

    private fun toggleMalwareListener() {
        if (isListening.value) {
            Log.d("IntentGen", "Stopping dynamic receiver")
            unregisterReceiver(malwareReceiver)
            malwareReceiver = null
            isListening.value = false
        } else {
            if (checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("IntentGen", "Requesting SMS permission")
                requestPermissions(arrayOf(android.Manifest.permission.RECEIVE_SMS), SMS_REQ_CODE)
                return
            }

            malwareReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action
                    Log.d("IntentGen", "Dynamic receiver caught action: $action")
                    runOnUiThread {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Intercepted: ${action?.substringAfterLast(".")}",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction("android.provider.Telephony.SMS_RECEIVED")
            }

            registerReceiver(malwareReceiver, filter, Context.RECEIVER_EXPORTED)
            isListening.value = true
            Log.d("IntentGen", "Dynamic receiver started")
        }
    }

    private fun executeShellCommand(command: String) {
        if (!Shizuku.pingBinder()) {
            Log.e("IntentGen", "Shizuku not ready for command: $command")
            return
        }

        Log.d("IntentGen", "Shell execute: $command")
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                newProcessMethod.isAccessible = true

                val process = newProcessMethod.invoke(
                    null,
                    arrayOf("sh", "-c", command),
                    null,
                    null
                ) as Process

                val stdOut = BufferedReader(InputStreamReader(process.inputStream))
                val stdErr = BufferedReader(InputStreamReader(process.errorStream))

                var line: String?
                while (stdOut.readLine().also { line = it } != null) {
                    Log.d("IntentGen", "STDOUT ($command): $line")
                }
                while (stdErr.readLine().also { line = it } != null) {
                    Log.e("IntentGen", "STDERR ($command): $line")
                }

                val exitCode = process.waitFor()
                Log.d("IntentGen", "Process exit code $exitCode: $command")
            } catch (e: Exception) {
                Log.e("IntentGen", "Execution failed: $command", e)
            }
        }
    }

    @Composable
    fun IntentControlPanel() {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column {
                Text(
                    text = "GENERATOR",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Intent Test Harness",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            TestSection(title = "Threat Simulation") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                Log.d("IntentGen", "Button clicked: Simulate Shell Intrusion")
                                executeShellCommand("am broadcast -a com.example.testapp.SHELL_INTRUSION --es payload 'root_exploit' --include-stopped-packages")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simulate Shell Intrusion")
                        }

                        Button(
                            onClick = {
                                Log.d("IntentGen", "Button clicked: Dynamic Boundary Crossing")

                                val pm = packageManager
                                val receivers = pm.queryBroadcastReceivers(Intent(Intent.ACTION_BOOT_COMPLETED), 0)

                                val victim = receivers.firstOrNull {
                                    val pkg = it.activityInfo.packageName
                                    pkg != packageName &&
                                            !pkg.startsWith("com.android.") &&
                                            !pkg.startsWith("android") &&
                                            !pkg.startsWith("com.samsung.") &&
                                            !pkg.startsWith("com.google.")
                                }

                                if (victim != null) {
                                    val targetPkg = victim.activityInfo.packageName
                                    val targetClass = victim.activityInfo.name

                                    Log.d("IntentGen", "Targeting victim: $targetPkg/$targetClass")

                                    val intent = Intent("com.example.mamzleintencje.TRIGGER_ALARM").apply {
                                        setClassName(targetPkg, targetClass)
                                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                                        putExtra("token", "stolen_session_key")
                                    }

                                    sendBroadcast(intent)
                                    Log.d("IntentGen", "Sent boundary crossing broadcast to $targetPkg")
                                } else {
                                    Log.e("IntentGen", "No suitable victim receiver found!")
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Broadcast Boundary Crossing")
                        }

                        Button(
                            onClick = {
                                Log.d("IntentGen", "Button clicked: Broadcast Suspicious Payload")
                                val intent = Intent("com.example.testapp.EXFILTRATE_DATA").apply {
                                    setPackage("com.example.testapp")
                                    for (i in 1..15) {
                                        putExtra("stolen_contacts_chunk_$i", "stolen_data_block")
                                    }
                                }
                                sendBroadcast(intent, android.Manifest.permission.READ_CONTACTS)
                                Log.d("IntentGen", "Sent suspicious payload broadcast")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Broadcast Suspicious Payload")
                        }

                        Button(
                            onClick = {
                                Log.d("IntentGen", "Button clicked: Trigger Static Receiver")
                                executeShellCommand("am broadcast -a com.example.testapp.TRIGGER_MALWARE -n com.example.testapp/.StaticMalwareReceiver --include-stopped-packages")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Trigger Static Receiver")
                        }
                    }
                }
            }

            TestSection(title = "Hardware Simulation") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { toggleMalwareListener() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (isListening.value) "Stop Dynamic Receiver" else "Start Dynamic Receiver")
                        }

                        Button(
                            onClick = {
                                Log.d("IntentGen", "Button clicked: Toggle Screen State")
                                executeShellCommand("input keyevent 26")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Toggle Screen State")
                        }

                        Button(
                            onClick = {
                                Log.d("IntentGen", "Button clicked: Simulate Battery Unplug")
                                executeShellCommand("dumpsys battery reset")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simulate Battery Unplug")
                        }

                        Button(
                            onClick = {
                                Log.d("IntentGen", "Button clicked: Simulate Battery Plug")
                                executeShellCommand("dumpsys battery set ac 1")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Simulate Battery Plug")
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun TestSection(title: String, content: @Composable () -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
            content()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_REQ_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            toggleMalwareListener()
        }
    }
}