package com.ztros.ztrosu.ui.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * ZTR_OS SU UI-Only Mode - Shell Utilities
 * Mocked SU support for UI testing without kernel
 */
object ShellUtils {
    private const val TAG = "ZTR_OS_Shell"
    
    // Mock root availability
    private var mockHasRoot = true
    private var mockLastCommand = ""
    
    /**
     * Check if root is available (mocked)
     */
    fun hasRoot(): Boolean = mockHasRoot
    
    /**
     * Set mock root status for testing
     */
    fun setHasRoot(hasRoot: Boolean) {
        mockHasRoot = hasRoot
    }
    
    /**
     * Execute a command WITHOUT root privileges
     */
    fun fastCmd(cmd: String): String {
        mockLastCommand = cmd
        return try {
            val process = Runtime.getRuntime().exec(cmd)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            
            val output = StringBuilder()
            var line: String?
            
            // Read output
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            
            // Read errors
            val errorOutput = StringBuilder()
            while (errorReader.readLine().also { line = it } != null) {
                errorOutput.append(line).append("\n")
            }
            
            process.waitFor()
            reader.close()
            errorReader.close()
            
            if (errorOutput.isNotEmpty() && output.isEmpty()) {
                "error: $errorOutput"
            } else {
                output.toString().trim()
            }
        } catch (e: Exception) {
            Log.e(TAG, "fastCmd error: $cmd", e)
            "error: ${e.message}"
        }
    }
    
    /**
     * Execute a command WITH root privileges (mocked)
     * In UI-Only mode, we simulate root by running commands directly
     */
    fun fastCmdResult(cmd: String): Boolean {
        mockLastCommand = cmd
        return try {
            // For UI-Only mode, try to execute with su first
            // If that fails, fall back to non-root execution
            val suCommand = if (cmd.startsWith("su -c ")) {
                cmd.removePrefix("su -c ")
            } else if (cmd.startsWith("su ")) {
                cmd.removePrefix("su ")
            } else {
                cmd
            }
            
            val process = Runtime.getRuntime().exec(suCommand)
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            Log.e(TAG, "fastCmdResult error: $cmd", e)
            false
        }
    }
    
    /**
     * Execute a command with su (async, for coroutines)
     */
    suspend fun execSu(cmd: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
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
    
    /**
     * Execute a command without su (async, for coroutines)
     */
    suspend fun exec(cmd: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec(cmd)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readText()
            reader.close()
            process.waitFor()
            Pair(process.exitValue(), output.trim())
        } catch (e: Exception) {
            Log.e(TAG, "exec error: $cmd", e)
            Pair(-1, "error: ${e.message}")
        }
    }
    
    /**
     * Get the last executed command (for debugging)
     */
    fun getLastCommand(): String = mockLastCommand
    
    /**
     * Simulate reboot command
     */
    fun reboot(reason: String = "") {
        try {
            val cmd = if (reason.isNotEmpty()) {
                "su -c 'reboot $reason'"
            } else {
                "su -c 'reboot'"
            }
            fastCmd(cmd)
        } catch (e: Exception) {
            Log.e(TAG, "reboot error", e)
        }
    }
    
    /**
     * Kill a process
     */
    fun killProcess(processName: String): Boolean {
        return fastCmdResult("killall $processName")
    }
}
