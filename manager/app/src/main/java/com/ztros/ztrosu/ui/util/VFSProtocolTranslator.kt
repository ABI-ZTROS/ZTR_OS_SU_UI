@file:Suppress("TooManyFunctions")

package com.ztros.ztrosu.ui.util

import android.util.Log
import java.nio.ByteBuffer
import java.nio.ByteOrder

// UI-Only Mode: All translations work on mock data. No kernel communication.

/**
 * VFS 内核模块协议双向翻译器 (UI-Only Mode)
 */
object VFSProtocolTranslator {

    private const val TAG = "VFSProtocolTranslator"

    // ==================== 常量 ====================

    const val EVENT_MAGIC: Int = 0xAF5F
    const val ACTION_ALLOW: Byte = 0x00
    const val ACTION_DENY: Byte = 0x01
    const val MODE_READ: Byte = 0x01
    const val MODE_WRITE: Byte = 0x02
    const val HOOK_TYPE_PID: Byte = 0x00
    const val HOOK_TYPE_PACKAGE: Byte = 0x01
    const val HOOK_MODE_MONITOR_ONLY: Byte = 0x00
    const val HOOK_MODE_INTERCEPT_READ: Byte = 0x01
    const val HOOK_MODE_INTERCEPT_WRITE: Byte = 0x02
    const val HOOK_MODE_INTERCEPT_ALL: Byte = 0x03
    const val POLICY_BINARY_SIZE: Int = 4
    private const val EVENT_HEADER_SIZE = 20
    private const val EVENT_TRAILER_SIZE = 12

    // ==================== 数据类 ====================

    data class HookEntry(
        val type: String,
        val identifier: String,
        val uid: Int,
        val mode: String,
        val enabled: Boolean
    )

    data class ValidationResult(
        val valid: Boolean,
        val error: String? = null,
        val parsedFields: Map<String, String>? = null
    )

    // ==================== 规则翻译 ====================

    fun ruleToBinary(ruleString: String): ByteArray? {
        val parts = ruleString.split(":", limit = 3)
        if (parts.size != 3) {
            Log.w(TAG, "ruleToBinary: 格式无效，期望 'action:path:mode'，实际: $ruleString")
            return null
        }

        val actionStr = parts[0].lowercase()
        val path = parts[1]
        val modeStr = parts[2].lowercase()

        val action: Byte = when (actionStr) {
            "allow" -> ACTION_ALLOW
            "deny" -> ACTION_DENY
            else -> {
                Log.w(TAG, "ruleToBinary: 未知动作 '$actionStr'")
                return null
            }
        }

        val modeMask = parseModeMask(modeStr)
        if (modeMask < 0) {
            Log.w(TAG, "ruleToBinary: 无效模式 '$modeStr'")
            return null
        }

        val pathBytes = path.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(1 + 4 + pathBytes.size + 1)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(action)
        buffer.putInt(pathBytes.size)
        buffer.put(pathBytes)
        buffer.put(modeMask.toByte())
        return buffer.array()
    }

    fun ruleToString(data: ByteArray): String? {
        return try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val action = buffer.get()
            val pathLen = buffer.getInt()
            if (pathLen < 0 || pathLen > data.size - 6) return null
            val pathBytes = ByteArray(pathLen)
            buffer.get(pathBytes)
            val path = String(pathBytes, Charsets.UTF_8)
            val modeMask = buffer.get().toInt()
            val actionStr = when (action) {
                ACTION_ALLOW -> "allow"
                ACTION_DENY -> "deny"
                else -> return null
            }
            val modeStr = modeMaskToString(modeMask)
            "$actionStr:$path:$modeStr"
        } catch (e: Exception) {
            null
        }
    }

    fun rulesToBinary(ruleStrings: List<String>): ByteArray? {
        if (ruleStrings.isEmpty()) return null
        val binaryRules = ruleStrings.mapNotNull { ruleToBinary(it) }
        if (binaryRules.isEmpty()) return null
        val totalSize = 4 + binaryRules.sumOf { it.size }
        val buffer = ByteBuffer.allocate(totalSize)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(binaryRules.size)
        binaryRules.forEach { buffer.put(it) }
        return buffer.array()
    }

    fun rulesToString(data: ByteArray): List<String> {
        if (data.size < 4) return emptyList()
        val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        val count = buffer.getInt()
        val rules = mutableListOf<String>()
        for (i in 0 until minOf(count, 100)) {
            if (buffer.remaining() < 2) break
            val action = buffer.get()
            val pathLen = if (buffer.remaining() >= 4) buffer.getInt() else break
            if (pathLen < 0 || pathLen > buffer.remaining() - 1) break
            val pathBytes = ByteArray(pathLen)
            buffer.get(pathBytes)
            val path = String(pathBytes, Charsets.UTF_8)
            if (buffer.remaining() < 1) break
            val modeMask = buffer.get().toInt()
            val actionStr = if (action == 0x00.toByte()) "allow" else "deny"
            rules.add("$actionStr:$path:${modeMaskToString(modeMask)}")
        }
        return rules
    }

    // ==================== 策略翻译 ====================

    fun policyToBinary(enabled: Boolean, logLevel: Int, defaultAction: String): ByteArray? {
        val actionByte: Byte = when (defaultAction.lowercase()) {
            "allow" -> 0x00
            "deny" -> 0x01
            else -> return null
        }
        val buffer = ByteBuffer.allocate(POLICY_BINARY_SIZE)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.put(if (enabled) 1 else 0)
        buffer.put(logLevel.toByte())
        buffer.put(actionByte)
        buffer.put(0) // reserved
        return buffer.array()
    }

    fun policyToString(data: ByteArray): Triple<Boolean, Int, String>? {
        if (data.size < POLICY_BINARY_SIZE) return null
        val enabled = data[0] != 0.toByte()
        val logLevel = data[1].toInt() and 0xFF
        val defaultAction = if (data[2] == 0x00.toByte()) "allow" else "deny"
        return Triple(enabled, logLevel, defaultAction)
    }

    // ==================== Hook 命令翻译 ====================

    fun hookCommandToBinary(command: String): ByteArray? {
        val parts = command.split(":", limit = 5)
        if (parts.isEmpty()) return null

        return when (parts[0].lowercase()) {
            "add" -> {
                if (parts.size != 5) return null
                val typeStr = parts[1].uppercase()
                val identifier = parts[2]
                val uidStr = parts[3]
                val modeStr = parts[4].uppercase()

                val hookType: Byte = when (typeStr) {
                    "PID" -> HOOK_TYPE_PID
                    "PACKAGE" -> HOOK_TYPE_PACKAGE
                    else -> return null
                }
                val uid = uidStr.toIntOrNull() ?: return null
                val hookMode: Byte = when (modeStr) {
                    "MONITOR_ONLY" -> HOOK_MODE_MONITOR_ONLY
                    "INTERCEPT_READ" -> HOOK_MODE_INTERCEPT_READ
                    "INTERCEPT_WRITE" -> HOOK_MODE_INTERCEPT_WRITE
                    "INTERCEPT_ALL" -> HOOK_MODE_INTERCEPT_ALL
                    else -> return null
                }
                val idBytes = identifier.toByteArray(Charsets.UTF_8)
                val buffer = ByteBuffer.allocate(1 + 4 + idBytes.size + 4 + 1)
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                buffer.put(hookType)
                buffer.putInt(idBytes.size)
                buffer.put(idBytes)
                buffer.putInt(uid)
                buffer.put(hookMode)
                buffer.array()
            }
            "remove" -> {
                if (parts.size != 3) return null
                val typeStr = parts[1].uppercase()
                val identifier = parts[2]
                val hookType: Byte = when (typeStr) {
                    "PID" -> HOOK_TYPE_PID
                    "PACKAGE" -> HOOK_TYPE_PACKAGE
                    else -> return null
                }
                val idBytes = identifier.toByteArray(Charsets.UTF_8)
                val buffer = ByteBuffer.allocate(1 + 4 + idBytes.size)
                buffer.order(ByteOrder.LITTLE_ENDIAN)
                buffer.put(hookType)
                buffer.putInt(idBytes.size)
                buffer.put(idBytes)
                buffer.array()
            }
            else -> null
        }
    }

    fun hookBinaryToCommand(data: ByteArray): String? {
        return try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val hookType = buffer.get()
            val idLen = buffer.getInt()
            if (idLen < 0 || idLen > buffer.remaining()) return null
            val idBytes = ByteArray(idLen)
            buffer.get(idBytes)
            val identifier = String(idBytes, Charsets.UTF_8)
            val typeStr = when (hookType) {
                HOOK_TYPE_PID -> "PID"
                HOOK_TYPE_PACKAGE -> "PACKAGE"
                else -> return null
            }
            if (buffer.remaining() >= 5) {
                val uid = buffer.getInt()
                val hookMode = buffer.get()
                val modeStr = hookModeToString(hookMode.toInt())
                "add:$typeStr:$identifier:$uid:$modeStr"
            } else {
                "remove:$typeStr:$identifier"
            }
        } catch (e: Exception) {
            null
        }
    }

    fun parseHookListEntry(line: String): HookEntry? {
        val parts = line.split(":", limit = 5)
        if (parts.size != 5) return null
        val type = parts[0].uppercase()
        if (type != "PID" && type != "PACKAGE") return null
        val identifier = parts[1]
        val uid = parts[2].toIntOrNull() ?: return null
        val mode = parts[3].uppercase()
        if (mode !in listOf("MONITOR_ONLY", "INTERCEPT_READ", "INTERCEPT_WRITE", "INTERCEPT_ALL")) return null
        val enabled = parts[4].trim() == "1"
        return HookEntry(type, identifier, uid, mode, enabled)
    }

    // ==================== 事件翻译 ====================

    fun eventToString(data: ByteArray): String? {
        return try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buffer.getInt()
            if (magic != EVENT_MAGIC) return null
            val eventType = buffer.getInt()
            val pid = buffer.getInt()
            val uid = buffer.getInt()
            val pathLen = buffer.getInt()
            if (pathLen < 0 || pathLen > buffer.remaining() - EVENT_TRAILER_SIZE) return null
            val pathBytes = ByteArray(pathLen)
            buffer.get(pathBytes)
            val path = String(pathBytes, Charsets.UTF_8)
            val timestamp = buffer.getLong()
            val result = buffer.getInt()
            val eventTypeStr = eventTypeName(eventType)
            val resultStr = if (result == 0) "ALLOW" else "DENY"
            "$eventTypeStr pid=$pid uid=$uid path=$path result=$resultStr ts=$timestamp"
        } catch (e: Exception) {
            null
        }
    }

    fun eventToMap(data: ByteArray): Map<String, Any>? {
        return try {
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
            val magic = buffer.getInt()
            if (magic != EVENT_MAGIC) return null
            val eventType = buffer.getInt()
            val pid = buffer.getInt()
            val uid = buffer.getInt()
            val pathLen = buffer.getInt()
            if (pathLen < 0 || pathLen > buffer.remaining() - EVENT_TRAILER_SIZE) return null
            val pathBytes = ByteArray(pathLen)
            buffer.get(pathBytes)
            val path = String(pathBytes, Charsets.UTF_8)
            val timestamp = buffer.getLong()
            val result = buffer.getInt()
            mapOf(
                "magic" to magic,
                "event_type" to eventType,
                "event_type_name" to eventTypeName(eventType),
                "pid" to pid,
                "uid" to uid,
                "path" to path,
                "timestamp" to timestamp,
                "result" to result,
                "result_name" to if (result == 0) "ALLOW" else "DENY"
            )
        } catch (e: Exception) {
            null
        }
    }

    // ==================== 工具方法 ====================

    fun hexDump(data: ByteArray, maxBytes: Int = 64): String {
        val len = minOf(data.size, maxBytes)
        if (len == 0) return "<empty>"
        val sb = StringBuilder()
        var offset = 0
        while (offset < len) {
            sb.append(String.format("%04X: ", offset))
            val rowEnd = minOf(offset + 16, len)
            for (i in offset until rowEnd) {
                if (i == offset + 8) sb.append(' ')
                sb.append(String.format("%02X ", data[i]))
            }
            val padding = (16 - (rowEnd - offset)) * 3 + if (rowEnd - offset <= 8) 1 else 0
            for (i in 0 until padding) sb.append(' ')
            sb.append(" |")
            for (i in offset until rowEnd) {
                val b = data[i].toInt() and 0xFF
                sb.append(if (b in 32..126) b.toChar() else '.')
            }
            sb.append('|')
            sb.append('\n')
            offset = rowEnd
        }
        if (data.size > maxBytes) sb.append("... (${data.size - maxBytes} more bytes)\n")
        return sb.toString().trimEnd('\n')
    }

    fun policyToMap(data: ByteArray): Map<String, Any> {
        if (data.size < 4) return emptyMap()
        return mapOf(
            "enabled" to (data[0] != 0.toByte()),
            "logLevel" to (data[1].toInt() and 0xFF),
            "defaultAction" to if (data[2] == 0x00.toByte()) "allow" else "deny",
            "reserved" to (data[3].toInt() and 0xFF)
        )
    }

    fun validateRuleString(rule: String): ValidationResult {
        val parts = rule.split(":", limit = 3)
        if (parts.size != 3) return ValidationResult(false, "格式无效，期望 'action:path:mode'")
        val actionStr = parts[0].lowercase()
        if (actionStr != "allow" && actionStr != "deny") return ValidationResult(false, "无效动作 '$actionStr'")
        if (parts[1].isEmpty()) return ValidationResult(false, "路径不能为空")
        val modeStr = parts[2].lowercase()
        if (parseModeMask(modeStr) < 0) return ValidationResult(false, "无效模式 '$modeStr'")
        return ValidationResult(true, parsedFields = mapOf("action" to actionStr, "path" to parts[1], "mode" to modeStr))
    }

    fun validateHookCommand(command: String): ValidationResult {
        val parts = command.split(":", limit = 5)
        if (parts.isEmpty() || parts[0].isEmpty()) return ValidationResult(false, "空命令")
        val operation = parts[0].lowercase()
        if (operation != "add" && operation != "remove") return ValidationResult(false, "无效操作 '$operation'")
        if (parts.size < 3) return ValidationResult(false, "字段不足")
        val typeStr = parts[1].uppercase()
        if (typeStr != "PID" && typeStr != "PACKAGE") return ValidationResult(false, "无效 Hook 类型 '$typeStr'")
        if (parts[2].isEmpty()) return ValidationResult(false, "标识符不能为空")
        val fields = mutableMapOf("operation" to operation, "type" to typeStr, "identifier" to parts[2])
        if (operation == "add") {
            if (parts.size != 5) return ValidationResult(false, "add 命令需要 5 个字段")
            fields["uid"] = parts[3]
            fields["mode"] = parts[4]
        }
        return ValidationResult(true, parsedFields = fields)
    }

    // ==================== 内部辅助方法 ====================

    private fun parseModeMask(modeStr: String): Int {
        return when (modeStr.lowercase()) {
            "r" -> MODE_READ.toInt()
            "w" -> MODE_WRITE.toInt()
            "rw", "wr" -> (MODE_READ.toInt() or MODE_WRITE.toInt())
            else -> -1
        }
    }

    private fun modeMaskToString(modeMask: Int): String {
        val hasRead = (modeMask and MODE_READ.toInt()) != 0
        val hasWrite = (modeMask and MODE_WRITE.toInt()) != 0
        return when {
            hasRead && hasWrite -> "rw"
            hasRead -> "r"
            hasWrite -> "w"
            else -> ""
        }
    }

    private fun hookModeToString(hookMode: Int): String {
        return when (hookMode) {
            HOOK_MODE_MONITOR_ONLY.toInt() -> "MONITOR_ONLY"
            HOOK_MODE_INTERCEPT_READ.toInt() -> "INTERCEPT_READ"
            HOOK_MODE_INTERCEPT_WRITE.toInt() -> "INTERCEPT_WRITE"
            HOOK_MODE_INTERCEPT_ALL.toInt() -> "INTERCEPT_ALL"
            else -> "UNKNOWN($hookMode)"
        }
    }

    private fun eventTypeName(eventType: Int): String {
        return when (eventType) {
            0 -> "OPEN"
            1 -> "READ"
            2 -> "WRITE"
            3 -> "CLOSE"
            4 -> "CREATE"
            5 -> "DELETE"
            6 -> "RENAME"
            7 -> "ATTR"
            else -> "EVENT($eventType)"
        }
    }
}
