package com.ztros.ztrosu.ui.util

import android.util.Log
import com.ztros.ztrosu.ui.util.VFSNetlinkListener.VFSEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// UI-Only Mode: All kernel interface calls return mock data. No sysfs/pipe/root operations.

private const val TAG = "VFSKernelInterface"

private const val VFS_SYSFS_PATH = "/sys/kernel/ztrosu/vfs"

object VFSKernelInterface {

    enum class CommChannel { PIPE, SYSFS, USERSPACE }

    @Volatile
    private var cachedChannel: CommChannel? = null

    suspend fun detectBestChannel(): CommChannel = withContext(Dispatchers.IO) {
        // UI-Only: always return PIPE (mock)
        CommChannel.PIPE
    }

    fun resetChannelCache() {
        cachedChannel = null
    }

    fun startEventListening(callback: (VFSEvent) -> Unit) {
        Log.d(TAG, "[UI-Only] startEventListening (no-op)")
    }

    fun stopEventListening() {
        Log.d(TAG, "[UI-Only] stopEventListening (no-op)")
    }

    suspend fun getVersion(): Int? = withContext(Dispatchers.IO) {
        // UI-Only: return mock version 2
        2
    }

    suspend fun isV2Supported(): Boolean = true

    enum class HookMode(val value: Int) {
        MONITOR_ONLY(0), INTERCEPT_READ(1), INTERCEPT_WRITE(2), INTERCEPT_ALL(3);
        companion object {
            fun fromValue(value: Int): HookMode = values().find { it.value == value } ?: MONITOR_ONLY
            fun fromString(str: String): HookMode = when (str.uppercase()) {
                "MONITOR_ONLY" -> MONITOR_ONLY; "INTERCEPT_READ" -> INTERCEPT_READ
                "INTERCEPT_WRITE" -> INTERCEPT_WRITE; "INTERCEPT_ALL" -> INTERCEPT_ALL; else -> MONITOR_ONLY
            }
        }
    }

    enum class HookType { PID, PACKAGE }

    data class HookTarget(
        val type: HookType,
        val identifier: String,
        val uid: Int,
        val mode: HookMode,
        val enabled: Boolean = true
    )

    suspend fun addHookTarget(target: HookTarget): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] addHookTarget: ${target.identifier} (no-op)")
        true
    }

    suspend fun removeHookTarget(type: HookType, identifier: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] removeHookTarget: $identifier (no-op)")
        true
    }

    suspend fun getHookList(): List<HookTarget> = withContext(Dispatchers.IO) {
        // UI-Only: return mock hook list
        listOf(
            HookTarget(HookType.PID, "1234", 10086, HookMode.MONITOR_ONLY, true),
            HookTarget(HookType.PACKAGE, "com.example.app", 10087, HookMode.INTERCEPT_ALL, true)
        )
    }

    suspend fun batchAddHookTargets(targets: List<HookTarget>): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] batchAddHookTargets: ${targets.size} targets (no-op)")
        true
    }

    suspend fun clearAllHooks(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] clearAllHooks (no-op)")
        true
    }

    suspend fun clearRules(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] clearRules (no-op)")
        true
    }

    suspend fun addRulesBatch(rules: List<String>): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] addRulesBatch: ${rules.size} rules (no-op)")
        true
    }

    private fun readFile(path: String): String {
        Log.d(TAG, "[UI-Only] readFile: $path (returning empty)")
        return ""
    }

    private fun writeFile(path: String, content: String): Boolean {
        Log.d(TAG, "[UI-Only] writeFile: $path (no-op, returning true)")
        return true
    }

    private fun appendFile(path: String, content: String): Boolean {
        Log.d(TAG, "[UI-Only] appendFile: $path (no-op, returning true)")
        return true
    }

    suspend fun getModuleStatus(): ModuleStatus = withContext(Dispatchers.IO) {
        ModuleStatus(
            version = 2,
            stats = VFSDebugUtil.getVFSStats(),
            policy = VFSDebugUtil.getVFSPolicy(),
            hooks = getHookList(),
            isV2 = true
        )
    }

    data class ModuleStatus(
        val version: Int,
        val stats: VFSStats,
        val policy: VFSPolicy,
        val hooks: List<HookTarget>,
        val isV2: Boolean
    )

    suspend fun applyFullConfig(policy: VFSPolicy, hooks: List<HookTarget>? = null): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] applyFullConfig (no-op)")
        true
    }
}
