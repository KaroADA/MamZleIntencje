package com.example.mamzleintencje.monitor

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import rikka.shizuku.Shizuku

class ShizukuClient(
    private val context: Context,
    private val onReady: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val SHIZUKU_CODE = 1001
    private var userService: IUserService? = null

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
        if (requestCode == SHIZUKU_CODE) {
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                bindService()
            } else {
                onError("Błąd: Użytkownik odrzucił uprawnienia dla Shizuku.")
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (binder != null && binder.pingBinder()) {
                userService = IUserService.Stub.asInterface(binder)
                onReady()
            } else {
                onError("Błąd: Nie udało się połączyć z usługą systemową.")
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            userService = null
        }
    }

    fun start() {
        Shizuku.addRequestPermissionResultListener(permissionListener)

        if (!Shizuku.pingBinder()) {
            onError("Błąd: Usługa Shizuku nie działa. Uruchom aplikację Shizuku na telefonie.")
            return
        }

        if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindService()
        } else {
            Shizuku.requestPermission(SHIZUKU_CODE)
        }
    }

    fun destroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener)
        if (userService != null) {
            val serviceArgs = Shizuku.UserServiceArgs(ComponentName(context.packageName, UserService::class.java.name))
            Shizuku.unbindUserService(serviceArgs, serviceConnection, true)
            userService = null
        }
    }

    fun execute(command: String): String {
        val service = userService ?: return "ERROR: Service disconnected"
        return service.runCommand(command)
    }

    private fun bindService() {
        val serviceArgs = Shizuku.UserServiceArgs(ComponentName(context.packageName, UserService::class.java.name))
            .daemon(false)
            .processNameSuffix("service")
            .debuggable(true)
            .version(1)
        Shizuku.bindUserService(serviceArgs, serviceConnection)
    }
}