package com.ztros.ztrosu.ui.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// UI-Only Mode: All sysfs reads return mock data. No kernel communication.

private const val TAG = "SecurityAudit"

data class ShellExecStats(
    val totalExecCount: Long = 0,
    val scriptExecCount: Long = 0,
    val interactiveCount: Long = 0,
    val deniedCount: Long = 0,
    val lastInterpreter: String = "",
    val lastCaller: String = "",
    val lastTimestamp: Long = 0
)

data class ShellExecRecord(
    val interpreter: String,
    val callerPid: Int,
    val callerUid: Int,
    val callerName: String,
    val scriptPath: String,
    val execType: String,
    val timestamp: Long
)

data class PartitionStatus(
    val enabled: Boolean = false,
    val autoReject: Boolean = false,
    val alertOnly: Boolean = false,
    val checkInterval: Int = 300,
    val partitions: List<PartitionInfo> = emptyList()
)

data class PartitionInfo(
    val mountPoint: String,
    val isProtected: Boolean = true,
    val isModified: Boolean = false,
    val modificationCount: Long = 0
)

object SecurityAuditInterface {

    private val auditBase = "/sys/kernel/ztrosu/audit"

    suspend fun getShellStats(): ShellExecStats? = withContext(Dispatchers.IO) {
        // UI-Only: return mock stats
        ShellExecStats(
            totalExecCount = 42,
            scriptExecCount = 15,
            interactiveCount = 27,
            deniedCount = 3,
            lastInterpreter = "/system/bin/sh",
            lastCaller = "com.android.shell",
            lastTimestamp = System.currentTimeMillis() - 60000
        )
    }

    suspend fun getRecentShellExecs(): List<ShellExecRecord> = withContext(Dispatchers.IO) {
        // UI-Only: return mock records
        listOf(
            ShellExecRecord("/system/bin/sh", 1234, 2000, "sh", "/data/local/tmp/script.sh", "script", System.currentTimeMillis() - 30000),
            ShellExecRecord("/system/bin/mksh", 5678, 10086, "mksh", "", "interactive", System.currentTimeMillis() - 60000),
            ShellExecRecord("/system/bin/su", 9999, 0, "root", "/system/bin/su", "script", System.currentTimeMillis() - 120000)
        )
    }

    suspend fun clearShellHistory(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] clearShellHistory (no-op)")
        true
    }

    suspend fun getPartitionStatus(): PartitionStatus? = withContext(Dispatchers.IO) {
        // UI-Only: return mock partition status
        PartitionStatus(
            enabled = true,
            autoReject = true,
            alertOnly = false,
            checkInterval = 300,
            partitions = listOf(
                PartitionInfo("/system", true, false, 0),
                PartitionInfo("/vendor", true, false, 0),
                PartitionInfo("/data", false, false, 0),
                PartitionInfo("/boot", true, false, 0),
                PartitionInfo("/recovery", true, false, 0)
            )
        )
    }

    suspend fun setPartitionPolicy(enabled: Boolean, autoReject: Boolean, alertOnly: Boolean, checkInterval: Int): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] setPartitionPolicy enabled=$enabled autoReject=$autoReject (no-op)")
        true
    }

    suspend fun resetPartitionModification(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] resetPartitionModification (no-op)")
        true
    }

    private fun parseShellExecRecord(line: String): ShellExecRecord? {
        val parts = line.split(":")
        if (parts.size < 7) return null
        return try {
            ShellExecRecord(
                interpreter = parts[0], callerPid = parts[1].toIntOrNull() ?: 0,
                callerUid = parts[2].toIntOrNull() ?: 0, callerName = parts[3],
                scriptPath = parts[4], execType = parts[5], timestamp = parts[6].toLongOrNull() ?: 0
            )
        } catch (e: Exception) { null }
    }

    private fun parsePartitionLine(line: String): PartitionInfo? {
        val mountPoint = line.substringBefore(":").trim()
        val isProtected = line.contains("protected=1")
        val isModified = line.contains("modified=1")
        val modsMatch = Regex("mods=(\\d+)").find(line)
        val modCount = modsMatch?.groupValues?.get(1)?.toLongOrNull() ?: 0
        return if (mountPoint.isNotBlank()) PartitionInfo(mountPoint, isProtected, isModified, modCount) else null
    }
}
