package com.example.mamzleintencje

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

class UserService: IUserService.Stub() {
    override fun destroy() {
        exitProcess(0)
    }
    override fun runCommand(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            val output = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }

            var errorLine: String?
            while (errorReader.readLine().also { errorLine = it } != null) {
                output.append("[ERROR]: ").append(errorLine).append("\n")
            }

            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            "Błąd wykonania w UserService: ${e.message}"
        }
    }
}