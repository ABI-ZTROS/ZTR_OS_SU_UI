package com.ztros.ztrosu.ui.util

import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ZTR_OS SU UI-Only Mode - Shell Utilities (Mocked)
 * All shell commands return simulated values for UI testing.
 * No actual shell/root commands are executed.
 */
object ShellUtils {
    private const val TAG = "ZTR_OS_Shell"

    /**
     * Execute a command and return output as string (MOCKED)
     * Returns realistic fake data without executing any real commands.
     */
    fun fastCmd(cmd: String): String {
        Log.d(TAG, "UI-Only mock: fastCmd('$cmd')")
        return when {
            cmd.contains("getenforce") -> "Enforcing"
            cmd.contains("getprop ro.product.model") -> Build.MODEL
            cmd.contains("getprop ro.build.version.release") -> Build.VERSION.RELEASE
            cmd.contains("getprop ro.build.version.security_patch") -> "2025-01-05"
            cmd.contains("getprop ro.product.cpu.abi") -> Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
            cmd.contains("sestatus") -> ""
            cmd.contains("cat /sys/fs/selinux") -> ""
            cmd.contains("stat -c") -> "N/A"
            cmd.contains("uname") -> "5.10.218-ztr_os"
            else -> ""
        }
    }

    /**
     * Execute a command and return success/failure (MOCKED)
     * Always returns success for UI-Only mode.
     */
    fun fastCmdResult(cmd: String): Boolean {
        Log.d(TAG, "UI-Only mock: fastCmdResult('$cmd') -> true")
        // For reboot commands, return true (simulated)
        // For setenforce, return true (simulated)
        // For killall, return true (simulated)
        return true
    }

    /**
     * Execute command with su prefix (MOCKED)
     * Returns simulated success without executing any real commands.
     */
    suspend fun execSu(cmd: String): Pair<Int, String> = withContext(Dispatchers.IO) {
        Log.d(TAG, "UI-Only mock: execSu('$cmd')")
        // Simulate a brief delay for realism
        kotlinx.coroutines.delay(50)
        Pair(0, "")
    }
}
