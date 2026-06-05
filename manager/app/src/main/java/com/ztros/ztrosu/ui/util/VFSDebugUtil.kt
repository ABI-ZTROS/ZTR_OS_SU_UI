package com.ztros.ztrosu.ui.util

import android.util.Log
import com.ztros.ztrosu.ui.util.VFSNetlinkListener.VFSEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// UI-Only Mode: All sysfs reads return mock data. No root operations.

private const val TAG = "VFSDebugUtil"

private const val VFS_SYSFS_PATH = "/sys/kernel/ztrosu/vfs"
private const val VFS_DEBUGFS_PATH = "/sys/kernel/debug/ztrosu/vfs"
private const val VFS_USERSPACE_PATH = "/data/adb/ksu/vfs_monitor"

data class VFSStats(
    val openCount: Long = 0,
    val readCount: Long = 0,
    val writeCount: Long = 0,
    val closeCount: Long = 0,
    val deniedCount: Long = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class VFSPolicy(
    val enabled: Boolean = false,
    val logLevel: Int = 0,
    val defaultAction: String = "allow",
    val rules: List<String> = emptyList()
)

enum class VFSBackend {
    KERNEL_SYSFS,
    KERNEL_DEBUGFS,
    USERSPACE,
    MOCK
}

object VFSDebugUtil {

    // UI-Only: always use MOCK backend
    private var backend: VFSBackend = VFSBackend.MOCK
    private var useMockData: Boolean = true

    fun detectBackend(): VFSBackend {
        // UI-Only Mode: always return MOCK
        return VFSBackend.MOCK
    }

    fun isAvailable(): Boolean {
        // UI-Only: report as available (mock)
        return true
    }

    suspend fun initUserspaceBackend(): Boolean = withContext(Dispatchers.IO) {
        // UI-Only: no-op
        true
    }

    private fun readFile(path: String): String {
        // UI-Only: no-op, return empty
        Log.d(TAG, "[UI-Only] readFile: $path (no-op)")
        return ""
    }

    private fun writeFile(path: String, content: String): Boolean {
        // UI-Only: no-op, return true
        Log.d(TAG, "[UI-Only] writeFile: $path (no-op, returning true)")
        return true
    }

    suspend fun getVFSStats(): VFSStats = withContext(Dispatchers.IO) {
        getMockStats()
    }

    private fun getKernelStats(statsPath: String): VFSStats = getMockStats()

    private fun getUserspaceStats(): VFSStats = getMockStats()

    suspend fun getVFSPolicy(): VFSPolicy = withContext(Dispatchers.IO) {
        getMockPolicy()
    }

    private fun getKernelPolicy(basePath: String): VFSPolicy = getMockPolicy()

    private fun getUserspacePolicy(): VFSPolicy = getMockPolicy()

    suspend fun setVFSPolicy(policy: VFSPolicy): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] setVFSPolicy: enabled=${policy.enabled} logLevel=${policy.logLevel} (no-op)")
        true
    }

    private suspend fun setKernelPolicy(basePath: String, policy: VFSPolicy): Boolean {
        Log.d(TAG, "[UI-Only] setKernelPolicy (no-op)")
        return true
    }

    private fun setUserspacePolicy(policy: VFSPolicy): Boolean {
        Log.d(TAG, "[UI-Only] setUserspacePolicy (no-op)")
        return true
    }

    fun validatePolicy(policy: VFSPolicy): Pair<Boolean, String> {
        if (policy.logLevel !in 0..5) return Pair(false, "日志级别必须在 0-5 之间")
        if (policy.defaultAction !in listOf("allow", "deny")) return Pair(false, "默认动作必须是 allow 或 deny")
        policy.rules.forEachIndexed { index, rule ->
            val parts = rule.split(":")
            if (parts.size < 3) return Pair(false, "规则 ${index + 1} 格式错误")
            if (parts[0] !in listOf("allow", "deny")) return Pair(false, "规则 ${index + 1} 的动作无效")
        }
        return Pair(true, "")
    }

    suspend fun resetStats(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] resetStats (no-op)")
        true
    }

    fun forceMockMode(enabled: Boolean) {
        useMockData = enabled
        backend = if (enabled) VFSBackend.MOCK else VFSBackend.MOCK
    }

    fun startEventStream(callback: (VFSEvent) -> Unit) {
        Log.d(TAG, "[UI-Only] startEventStream (no-op)")
    }

    fun stopEventStream() {
        Log.d(TAG, "[UI-Only] stopEventStream (no-op)")
    }

    private fun getMockStats(): VFSStats {
        return VFSStats(
            openCount = 1234,
            readCount = 5678,
            writeCount = 901,
            closeCount = 1230,
            deniedCount = 5
        )
    }

    private fun getMockPolicy(): VFSPolicy {
        return VFSPolicy(
            enabled = true,
            logLevel = 2,
            defaultAction = "allow",
            rules = listOf(
                "deny:/data/data/*/databases/:rw",
                "allow:/sdcard/:r",
                "deny:/system/:w"
            )
        )
    }
}
