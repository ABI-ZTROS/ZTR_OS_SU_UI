package com.ztros.ztrosu.ui.util

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

// UI-Only Mode: All pipe operations return mock success data.
// No actual FIFO pipes, Shell commands, or root operations are performed.

private const val TAG = "VFSPipeComm"

/**
 * 结构化规则数据，用于与内核VFS模块的二进制协议通讯。
 */
data class PipeRuleData(
    val action: Int,    // 0=allow, 1=deny
    val path: String,
    val modeMask: Int   // bit0=read, bit1=write
)

/**
 * VFS Pipe通讯管理器 (UI-Only Mode)
 *
 * 通过FIFO管道与内核VFS模块进行二进制协议通讯。
 * UI-Only Mode: 所有操作返回模拟成功数据。
 */
object VFSPipeComm {

    // ==================== 协议常量 ====================

    const val MAGIC: Int = 0xAF5F
    const val VERSION = 2
    const val PIPE_BASE = "/dev/aurora_vfs_"
    const val PIPE_TIMEOUT_MS = 5000L

    // 命令类型
    const val CMD_ADD_HOOK = 1
    const val CMD_REMOVE_HOOK = 2
    const val CMD_SET_RULES = 3
    const val CMD_CLEAR_RULES = 4
    const val CMD_SET_POLICY = 5
    const val CMD_RESET_STATS = 6
    const val CMD_QUERY_STATUS = 7

    // 响应状态码
    const val RESP_SUCCESS = 0
    const val RESP_ERR_UNKNOWN = 1
    const val RESP_ERR_INVALID_CMD = 2
    const val RESP_ERR_PIPE = 3
    const val RESP_ERR_TIMEOUT = 4
    const val RESP_ERR_PERMISSION = 5

    // 协议头大小: 4 * UInt32 = 16 bytes
    private const val HEADER_SIZE = 16

    // 线程安全锁
    private val commLock = ReentrantLock()

    // ==================== 核心通讯方法 ====================

    /**
     * 发送命令到内核VFS模块 (UI-Only: always returns true)
     */
    fun sendCommand(cmdType: Int, data: ByteArray = ByteArray(0)): Boolean {
        commLock.withLock {
            Log.d(TAG, "[UI-Only] sendCommand type=$cmdType, dataSize=${data.size}")
            return true
        }
    }

    /**
     * 发送命令并获取响应数据 (UI-Only: returns mock response)
     */
    fun sendCommandWithData(cmdType: Int, data: ByteArray = ByteArray(0)): ByteArray? {
        commLock.withLock {
            Log.d(TAG, "[UI-Only] sendCommandWithData type=$cmdType, dataSize=${data.size}")
            return buildMockResponse(cmdType)
        }
    }

    // ==================== 高级命令接口 ====================

    fun addHook(type: Int, identifier: String, uid: Int, mode: Int): Boolean {
        Log.d(TAG, "[UI-Only] addHook type=$type identifier=$identifier uid=$uid mode=$mode")
        return true
    }

    fun removeHook(type: Int, identifier: String): Boolean {
        Log.d(TAG, "[UI-Only] removeHook type=$type identifier=$identifier")
        return true
    }

    fun setStructuredRules(rules: List<PipeRuleData>): Boolean {
        Log.d(TAG, "[UI-Only] setStructuredRules count=${rules.size}")
        return true
    }

    fun setRules(rules: List<String>): Boolean {
        Log.d(TAG, "[UI-Only] setRules count=${rules.size}")
        return true
    }

    fun clearRules(): Boolean {
        Log.d(TAG, "[UI-Only] clearRules")
        return true
    }

    fun setPolicy(enabled: Boolean, logLevel: Int, defaultAction: Int): Boolean {
        Log.d(TAG, "[UI-Only] setPolicy enabled=$enabled logLevel=$logLevel defaultAction=$defaultAction")
        return true
    }

    fun setPolicy(enabled: Boolean, logLevel: Int, defaultAction: String): Boolean {
        Log.d(TAG, "[UI-Only] setPolicy enabled=$enabled logLevel=$logLevel defaultAction=$defaultAction")
        return true
    }

    fun resetStats(): Boolean {
        Log.d(TAG, "[UI-Only] resetStats")
        return true
    }

    fun queryStatus(): ByteArray? {
        Log.d(TAG, "[UI-Only] queryStatus")
        return buildMockResponse(CMD_QUERY_STATUS)
    }

    // ==================== 管道管理 (UI-Only stubs) ====================

    fun createPipe(): String? {
        Log.d(TAG, "[UI-Only] createPipe -> mock path")
        return "/dev/aurora_vfs_mock_ui"
    }

    fun destroyPipe(path: String) {
        Log.d(TAG, "[UI-Only] destroyPipe path=$path (no-op)")
    }

    // ==================== 协议构建与解析 ====================

    private fun buildCommandPacket(cmdType: Int, data: ByteArray): ByteArray {
        val buffer = ByteBuffer.allocate(HEADER_SIZE + data.size)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(MAGIC)
        buffer.putInt(VERSION)
        buffer.putInt(cmdType)
        buffer.putInt(data.size)
        if (data.isNotEmpty()) buffer.put(data)
        return buffer.array()
    }

    private fun buildHookData(type: Int, identifier: String, uid: Int, mode: Int): ByteArray {
        val identifierBytes = identifier.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(1 + 4 + identifierBytes.size + 4 + 1)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(type.toByte())
        buffer.putInt(identifierBytes.size)
        buffer.put(identifierBytes)
        buffer.putInt(uid)
        buffer.put(mode.toByte())
        return buffer.array()
    }

    private fun buildRemoveHookData(type: Int, identifier: String): ByteArray {
        val identifierBytes = identifier.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(1 + 4 + identifierBytes.size)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(type.toByte())
        buffer.putInt(identifierBytes.size)
        buffer.put(identifierBytes)
        return buffer.array()
    }

    private fun buildRulesData(rules: List<PipeRuleData>): ByteArray {
        var totalSize = 4
        for (rule in rules) {
            val pathBytes = rule.path.toByteArray(Charsets.UTF_8)
            totalSize += 1 + 4 + pathBytes.size + 1
        }
        val buffer = ByteBuffer.allocate(totalSize)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(rules.size)
        for (rule in rules) {
            val pathBytes = rule.path.toByteArray(Charsets.UTF_8)
            buffer.put(rule.action.toByte())
            buffer.putInt(pathBytes.size)
            buffer.put(pathBytes)
            buffer.put(rule.modeMask.toByte())
        }
        return buffer.array()
    }

    private fun buildRulesDataFromStrings(rules: List<String>): ByteArray {
        val parsedRules = rules.mapNotNull { ruleStr ->
            val parts = ruleStr.split(":", limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val action = when (parts[0].lowercase()) {
                "allow" -> 0
                "deny" -> 1
                else -> return@mapNotNull null
            }
            val path = parts[1]
            val modeMask = when (parts[2].lowercase()) {
                "r" -> 0x01
                "w" -> 0x02
                "rw" -> 0x03
                else -> return@mapNotNull null
            }
            PipeRuleData(action, path, modeMask)
        }
        return buildRulesData(parsedRules)
    }

    private fun buildPolicyData(enabled: Boolean, logLevel: Int, defaultAction: Int): ByteArray {
        val buffer = ByteBuffer.allocate(4)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(if (enabled) 1 else 0.toByte())
        buffer.put(logLevel.toByte())
        buffer.put(defaultAction.toByte())
        buffer.put(0.toByte())
        return buffer.array()
    }

    private fun buildPolicyData(enabled: Boolean, logLevel: Int, defaultAction: String): ByteArray {
        val actionInt = when (defaultAction.lowercase()) {
            "allow" -> 0
            "deny" -> 1
            else -> 0
        }
        return buildPolicyData(enabled, logLevel, actionInt)
    }

    private fun parseResponse(data: ByteArray): Boolean {
        if (data.size < HEADER_SIZE) return false
        val buffer = ByteBuffer.wrap(data)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val magic = buffer.getInt()
        val version = buffer.getInt()
        val status = buffer.getInt()
        return magic == MAGIC && version == VERSION && status == RESP_SUCCESS
    }

    private fun parseResponseData(data: ByteArray): ByteArray? {
        if (data.size < HEADER_SIZE) return null
        val buffer = ByteBuffer.wrap(data)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        val magic = buffer.getInt()
        val version = buffer.getInt()
        val status = buffer.getInt()
        val respLen = buffer.getInt()
        if (magic != MAGIC || version != VERSION || status != RESP_SUCCESS) return null
        if (respLen <= 0 || data.size < HEADER_SIZE + respLen) return ByteArray(0)
        val responseData = ByteArray(respLen)
        buffer.get(responseData)
        return responseData
    }

    // ==================== Mock helpers ====================

    private fun buildMockResponse(cmdType: Int): ByteArray {
        val mockData = when (cmdType) {
            CMD_QUERY_STATUS -> {
                // Mock status: version=2, enabled=1, hooks=3, rules=5
                byteArrayOf(2, 0, 0, 0, 1, 0, 0, 0, 3, 0, 0, 0, 5, 0, 0, 0)
            }
            else -> ByteArray(0)
        }
        val buffer = ByteBuffer.allocate(HEADER_SIZE + mockData.size)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(MAGIC)
        buffer.putInt(VERSION)
        buffer.putInt(RESP_SUCCESS)
        buffer.putInt(mockData.size)
        if (mockData.isNotEmpty()) buffer.put(mockData)
        return buffer.array()
    }

    private fun generateRandomHex(length: Int): String {
        val random = java.security.SecureRandom()
        val bytes = ByteArray(length / 2 + 1)
        random.nextBytes(bytes)
        return bytes.take(length / 2).joinToString("") { "%02x".format(it) }
    }

    private fun statusToString(status: Int): String {
        return when (status) {
            RESP_SUCCESS -> "SUCCESS"
            RESP_ERR_UNKNOWN -> "UNKNOWN_ERROR"
            RESP_ERR_INVALID_CMD -> "INVALID_COMMAND"
            RESP_ERR_PIPE -> "PIPE_ERROR"
            RESP_ERR_TIMEOUT -> "TIMEOUT"
            RESP_ERR_PERMISSION -> "PERMISSION_DENIED"
            else -> "UNKNOWN($status)"
        }
    }

    fun isAvailable(): Boolean {
        // UI-Only Mode: always report available
        return true
    }

    fun getDebugInfo(): String {
        return buildString {
            appendLine("VFSPipeComm Debug Info (UI-Only Mode):")
            appendLine("  Magic: 0x${MAGIC.toString(16)}")
            appendLine("  Version: $VERSION")
            appendLine("  Pipe Base: $PIPE_BASE")
            appendLine("  Timeout: ${PIPE_TIMEOUT_MS}ms")
            appendLine("  Available: true (mock)")
        }
    }
}
