package com.ztros.ztrosu.ui.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

// UI-Only Mode: Hooks stored in memory only. No kernel writes or /proc reads.

private const val TAG = "VFSHookManager"

enum class HookType { PID, PACKAGE }

enum class HookMode(val displayName: String, val description: String) {
    MONITOR_ONLY("Monitor Only", "Only monitor, no interception"),
    INTERCEPT_READ("Intercept Read", "Intercept read operations"),
    INTERCEPT_WRITE("Intercept Write", "Intercept write operations"),
    INTERCEPT_ALL("Intercept All", "Intercept all operations")
}

data class VFSHookTarget(
    val id: String,
    val type: HookType,
    val identifier: String,
    val uid: Int,
    val mode: HookMode,
    val enabled: Boolean,
    val createdAt: Long,
    val processName: String = "",
    val lastPid: Int = -1
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("type", type.name); put("identifier", identifier)
        put("uid", uid); put("mode", mode.name); put("enabled", enabled)
        put("createdAt", createdAt); put("processName", processName); put("lastPid", lastPid)
    }
    fun toJSON(): JSONObject = toJson()
    companion object {
        fun fromJson(json: JSONObject): VFSHookTarget = VFSHookTarget(
            id = json.getString("id"), type = HookType.valueOf(json.getString("type")),
            identifier = json.getString("identifier"), uid = json.getInt("uid"),
            mode = HookMode.valueOf(json.getString("mode")), enabled = json.getBoolean("enabled"),
            createdAt = json.getLong("createdAt"), processName = json.optString("processName", ""),
            lastPid = json.optInt("lastPid", -1)
        )
        fun fromJSON(json: JSONObject): VFSHookTarget = fromJson(json)
    }
}

object VFSHookManager {

    private val targets = mutableListOf<VFSHookTarget>()

    suspend fun addPidHook(pid: Int, mode: HookMode = HookMode.MONITOR_ONLY, enabled: Boolean = true): VFSHookTarget? = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] addPidHook pid=$pid mode=$mode")
        val target = VFSHookTarget(
            id = UUID.randomUUID().toString(), type = HookType.PID, identifier = pid.toString(),
            uid = 10086 + pid, mode = mode, enabled = enabled, createdAt = System.currentTimeMillis(),
            processName = "mock_process_$pid", lastPid = pid
        )
        targets.add(target)
        target
    }

    suspend fun addHookByProcessName(processName: String, mode: HookMode = HookMode.MONITOR_ONLY, enabled: Boolean = true): VFSHookTarget? = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] addHookByProcessName name=$processName")
        val mockPid = (1000..9999).random()
        val target = VFSHookTarget(
            id = UUID.randomUUID().toString(), type = HookType.PID, identifier = mockPid.toString(),
            uid = 10086, mode = mode, enabled = enabled, createdAt = System.currentTimeMillis(),
            processName = processName, lastPid = mockPid
        )
        targets.add(target)
        target
    }

    suspend fun removePidHook(id: String): Boolean = withContext(Dispatchers.IO) {
        val removed = targets.removeAll { it.id == id }
        Log.d(TAG, "[UI-Only] removePidHook id=$id removed=$removed")
        removed
    }

    suspend fun addPackageHook(packageName: String, mode: HookMode = HookMode.MONITOR_ONLY, enabled: Boolean = true): VFSHookTarget? = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] addPackageHook pkg=$packageName mode=$mode")
        val target = VFSHookTarget(
            id = UUID.randomUUID().toString(), type = HookType.PACKAGE, identifier = packageName,
            uid = 10086, mode = mode, enabled = enabled, createdAt = System.currentTimeMillis(),
            processName = packageName
        )
        targets.add(target)
        target
    }

    suspend fun removePackageHook(id: String): Boolean = withContext(Dispatchers.IO) {
        targets.removeAll { it.id == id }
    }

    suspend fun getTargets(): List<VFSHookTarget> = withContext(Dispatchers.IO) {
        targets.toList()
    }

    suspend fun getTarget(id: String): VFSHookTarget? = withContext(Dispatchers.IO) {
        targets.find { it.id == id }
    }

    suspend fun getEnabledTargets(): List<VFSHookTarget> = withContext(Dispatchers.IO) {
        targets.filter { it.enabled }
    }

    suspend fun setTargetEnabled(id: String, enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        val index = targets.indexOfFirst { it.id == id }
        if (index >= 0) { targets[index] = targets[index].copy(enabled = enabled); true } else false
    }

    suspend fun clearAllTargets(): Boolean = withContext(Dispatchers.IO) {
        targets.clear()
        Log.d(TAG, "[UI-Only] Cleared all targets")
        true
    }

    suspend fun getTargetCount(): Int = withContext(Dispatchers.IO) { targets.size }

    suspend fun loadFromPersistence(): Boolean = withContext(Dispatchers.IO) {
        try {
            val persisted = VFSPersistenceManager.loadHookTargets()
            targets.clear()
            targets.addAll(persisted)
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveToPersistence(): Boolean = withContext(Dispatchers.IO) {
        VFSPersistenceManager.saveHookTargets(targets)
    }

    suspend fun exportTargets(): JSONArray = withContext(Dispatchers.IO) {
        JSONArray(targets.map { it.toJSON() })
    }

    suspend fun importTargets(jsonArray: JSONArray): Boolean = withContext(Dispatchers.IO) {
        try {
            targets.clear()
            for (i in 0 until jsonArray.length()) targets.add(VFSHookTarget.fromJSON(jsonArray.getJSONObject(i)))
            true
        } catch (e: Exception) { false }
    }

    // Alias for compatibility with VFSDebugScreen
    suspend fun getHookTargets(): List<VFSHookTarget> = getTargets()

    suspend fun toggleHook(id: String, enabled: Boolean): Boolean = setTargetEnabled(id, enabled)

    suspend fun clearAll(): Boolean = clearAllTargets()

    fun getDebugInfo(): String = buildString {
        appendLine("VFSHookManager (UI-Only Mode):")
        appendLine("  Total Targets: ${targets.size}")
        targets.forEach { appendLine("  - [${it.type}] ${it.identifier} (${it.mode.displayName}) enabled=${it.enabled}") }
    }
}
