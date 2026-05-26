package com.example.testapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import rikka.shizuku.Shizuku
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
class MainActivity : ComponentActivity() {

    private val SHIZUKU_CODE = 1001

    // Zmienne stanu dla symulacji nasłuchu
    private var malwareReceiver: BroadcastReceiver? = null
    private var isListening = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == SHIZUKU_CODE && grantResult == PackageManager.PERMISSION_GRANTED) {
                Log.i("IntentGen", "Uprawnienia Shizuku zostały przyznane.")
            }
        }

        setContent {
            IntentControlPanel()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener { _, _ -> }
        // Sprzątanie pamięci - kluczowe, aby nie wywołać wycieku (Memory Leak)
        if (isListening.value) {
            unregisterReceiver(malwareReceiver)
        }
    }

    // --- MODUŁ DYNAMICZNEGO NASŁUCHU (SYMULACJA MALWARE) ---
    private val SMS_REQ_CODE = 1002

    private fun toggleMalwareListener() {
        if (isListening.value) {
            unregisterReceiver(malwareReceiver)
            malwareReceiver = null
            isListening.value = false
            Log.i("IntentGen", "Zatrzymano dynamiczny nasłuch (Malware OFF).")
        } else {
            // Weryfikacja uprawnienia RECEIVE_SMS w czasie wykonywania
            if (checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
                Log.w("IntentGen", "Brak uprawnienia SMS. Żądanie od użytkownika...")
                requestPermissions(arrayOf(android.Manifest.permission.RECEIVE_SMS), SMS_REQ_CODE)
                return // Przerwij włączanie nasłuchu, dokończy po przyznaniu uprawnienia
            }

            malwareReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    Log.w("IntentGen", "MALWARE PRZECHWYCIŁ INTENCJĘ: ${intent?.action}")

                    // Wymuszenie wykonania na wątku UI, aby pokazać Toast
                    runOnUiThread {
                        android.widget.Toast.makeText(
                            this@MainActivity,
                            "Przechwycono: ${intent?.action?.split(".")?.last()}",
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
            Log.w("IntentGen", "Rozpoczęto dynamiczny nasłuch (Malware ON).")
        }
    }

    private fun executeShellCommand(command: String) {
        if (!Shizuku.pingBinder()) return

        // Uruchomienie w wątku tła (IO)
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

                process.waitFor()
                Log.i("IntentGen", "Wykonano komendę systemową: $command")
            } catch (e: Exception) {
                Log.e("IntentGen", "Błąd wywołania komendy przez Shizuku: ${e.message}")
            }
        }
    }

    @Composable
    fun IntentControlPanel() {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // =========================================================
            // SEKCJA 1: INTENCJE ZWYKŁE (Aktywności interfejsu)
            // =========================================================
            Text("1. Intencje Zwykłe (Aktywności UI)", fontWeight = FontWeight.Bold, color = Color.Blue, modifier = Modifier.padding(bottom = 8.dp))

            Button(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://przykladowy-phishing.com"))
                startActivity(intent)
            }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Wyślij ACTION_VIEW (Phishing)")
            }

            Button(onClick = {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:67"))
                startActivity(intent)
            }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Wyślij ACTION_DIAL (Wymuszenie połączenia)")
            }

            Button(onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, "Poufne dane 2FA")
                }
                startActivity(Intent.createChooser(intent, "Udostępnij dane"))
            }, modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Text("Wyślij ACTION_SEND (Eksfiltracja)")
            }

            Divider(modifier = Modifier.padding(bottom = 16.dp), color = Color.Gray, thickness = 2.dp)


            // =========================================================
            // SEKCJA 2: NASŁUCH DYNAMICZNY (Zdarzenia sprzętowe/środowisko)
            // =========================================================
            Text("2. Nasłuch Dynamiczny (Rejestrowany z RAM)", fontWeight = FontWeight.Bold, color = Color.Red, modifier = Modifier.padding(bottom = 8.dp))

            Button(
                onClick = { toggleMalwareListener() }, // Funkcja z poprzedniego kroku (z uprawnieniami SMS)
                colors = ButtonDefaults.buttonColors(containerColor = if (isListening.value) Color.Red else Color.DarkGray),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(if (isListening.value) "Zatrzymaj Nasłuch Dynamiczny" else "Rozpocznij Nasłuch Dynamiczny")
            }
            Text("Po wykryciu intencji wyświetli się komunikat toast oraz zapis w LogCat", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))


            // Poniższe polecenia używają sprzętowej symulacji, aby ominąć filtry Androida
            Button(onClick = {
                executeShellCommand("input keyevent 26")
            }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Zmień stan ekranu (Generuje SCREEN_ON/OFF)")
            }

            Button(onClick = {
                executeShellCommand("dumpsys battery reset")
            }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Odłącz zasilanie (Generuje POWER_DISCONNECTED)")
            }

            Button(onClick = {
                executeShellCommand("dumpsys battery set ac 1")
            }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Podłącz zasilanie (Generuje POWER_CONNECTED)")
            }
            Text("Nasłuch jeszcze wykrywa SMS_RECEIVED, należy wysłać SMS ręcznie (lub w emulatorze)", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))


            Divider(modifier = Modifier.padding(bottom = 16.dp), color = Color.Gray, thickness = 2.dp)


            // =========================================================
            // SEKCJA 3: NASŁUCH STATYCZNY (Zadeklarowane w Manifeście)
            // =========================================================
            Text("3. Nasłuch Statyczny (Manifest / Autostart)", fontWeight = FontWeight.Bold, color = Color(0xFF006400), modifier = Modifier.padding(bottom = 8.dp))

            Text("Nasłuch działa zawsze, nawet bez klikania przycisku 'Rozpocznij Nasłuch' w Sekcji 2. Działa w tle więc nie ma powiadomień toast, o wykryciu informuje tylko LogCat", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))

            // Używamy flagi -p, aby zmusić system do dostarczenia sztucznego polecenia do naszego Static Receivera
            Button(onClick = {
                // Wywołanie testowe do StaticMalwareReceiver (wymaga zdefiniowania w manifeście)
                executeShellCommand("am broadcast -a com.example.testapp.TRIGGER_MALWARE -n com.example.testapp/.StaticMalwareReceiver --include-stopped-packages")
            }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Wyślij TEST_EVENT (Statyczny)")
            }
            Text("Nasłuch wykrywa również DEVICE_ADMIN_ENABLED, BOOT_COMPLETED, PACKAGE_ADDED", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, modifier = Modifier.padding(bottom = 8.dp))

        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == SMS_REQ_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i("IntentGen", "Uprawnienie SMS przyznane. Uruchamiam nasłuch...")
            toggleMalwareListener() // Ponowna próba włączenia
        }
    }
}
