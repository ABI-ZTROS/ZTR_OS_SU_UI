package com.ztros.ztrosu.ui.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ZTR_OS SU UI-Only Mode - Shell Utilities
 * Provides shell command execution for UI testing without kernel
 */
object ShellUtils {
    private const val TAG = "ZTR_OS_Shell"

    /**
     * Execute a command and return output as string
     */
    fun fastCmd(cmd: String): String {
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            reader.close()
            output.toString().trim()
        } catch (e: Exception) {
            Log.e(TAG, "fastCmd error: $cmd", e)
            ""
        }
    }

    /**
     * Execute a command and return success/failure
     */
    fun fastCmdResult(cmd: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            Log.e(TAG, "fastCmdResult error: $cmd", e)
            false
        }
    }

    /**
     * Execute command with su prefix (for UI-Only mode, just runs command directly)
     */
    suspend fun execSu(cmd: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            // Remove su prefix if present
            val actualCmd = when {
                cmd.startsWith("su -c '") -> cmd.removePrefix("su -c '").removeSuffix("'")
                cmd.startsWith("su -c ") -> cmd.removePrefix("su -c ")
                cmd.startsWith("su ") -> cmd.removePrefix("su ")
                else -> cmd
            }
            val process = Runtime.getRuntime().exec(actualCmd)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.waitFor()
            Pair(process.exitValue(), output.trim())
        } catch (e: Exception) {
            Log.e(TAG, "execSu error: $cmd", e)
            Pair(-1, "error: ${e.message}")
        }
    }
}
