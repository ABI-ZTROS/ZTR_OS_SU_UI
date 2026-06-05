package com.ztros.ztrosu.ui.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// UI-Only Mode: Rules stored in memory only. No kernel writes.

private const val TAG = "VFSRuleEngine"

enum class VFSOp {
    OPEN, READ, WRITE, CLOSE;

    companion object {
        fun fromMode(mode: String): Set<VFSOp> {
            val ops = mutableSetOf<VFSOp>()
            if (mode.contains('r', ignoreCase = true)) { ops.add(READ); ops.add(OPEN) }
            if (mode.contains('w', ignoreCase = true)) { ops.add(WRITE); ops.add(OPEN) }
            return ops
        }
        fun fromString(str: String): VFSOp? = when (str.uppercase()) {
            "OPEN" -> OPEN; "READ", "R" -> READ; "WRITE", "W" -> WRITE; "CLOSE" -> CLOSE; else -> null
        }
    }
}

enum class RuleAction {
    ALLOW, DENY, LOG_ONLY;
    companion object {
        fun fromString(str: String): RuleAction? = when (str.lowercase()) {
            "allow", "a" -> ALLOW; "deny", "d" -> DENY; "log", "log_only", "l" -> LOG_ONLY; else -> null
        }
    }
}

enum class RuleType { PATH_RULE, UID_RULE, COMBO_RULE }

data class VFSRule(
    val id: String = UUID.randomUUID().toString(),
    val action: RuleAction = RuleAction.DENY,
    val pathPattern: String = "/*",
    val uidFilter: Int? = null,
    val opTypes: Set<VFSOp> = setOf(VFSOp.READ, VFSOp.WRITE),
    val priority: Int = 0,
    val enabled: Boolean = true,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val ruleType: RuleType
        get() = when {
            uidFilter != null -> RuleType.COMBO_RULE
            pathPattern.contains("*") || pathPattern.contains("?") -> RuleType.PATH_RULE
            else -> RuleType.PATH_RULE
        }

    fun toSimpleFormat(): String {
        val mode = buildString {
            if (VFSOp.READ in opTypes) append('r')
            if (VFSOp.WRITE in opTypes) append('w')
        }
        return "${action.name.lowercase()}:$pathPattern:$mode"
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id); put("action", action.name); put("pathPattern", pathPattern)
        put("uidFilter", uidFilter); put("opTypes", JSONArray(opTypes.map { it.name }))
        put("priority", priority); put("enabled", enabled); put("description", description)
        put("createdAt", createdAt)
    }

    fun toJSON(): JSONObject = toJson()

    companion object {
        fun fromJson(json: JSONObject): VFSRule {
            val opTypesArray = json.optJSONArray("opTypes")
            val opTypes = mutableSetOf<VFSOp>()
            if (opTypesArray != null) {
                for (i in 0 until opTypesArray.length()) VFSOp.fromString(opTypesArray.getString(i))?.let { opTypes.add(it) }
            } else { opTypes.add(VFSOp.READ); opTypes.add(VFSOp.WRITE) }
            return VFSRule(
                id = json.optString("id", UUID.randomUUID().toString()),
                action = RuleAction.fromString(json.optString("action", "DENY")) ?: RuleAction.DENY,
                pathPattern = json.optString("pathPattern", "/*"),
                uidFilter = if (json.has("uidFilter") && !json.isNull("uidFilter")) json.getInt("uidFilter") else null,
                opTypes = opTypes, priority = json.optInt("priority", 0),
                enabled = json.optBoolean("enabled", true),
                description = if (json.has("description") && !json.isNull("description")) json.getString("description") else null,
                createdAt = json.optLong("createdAt", System.currentTimeMillis())
            )
        }
        fun fromSimpleFormat(ruleStr: String): VFSRule? {
            val parts = ruleStr.split(":")
            if (parts.size < 3) return null
            val action = RuleAction.fromString(parts[0]) ?: return null
            val pathPattern = parts[1]
            val opTypes = VFSOp.fromMode(parts[2])
            val uidFilter = if (parts.size > 3) parts[3].toIntOrNull() else null
            return VFSRule(action = action, pathPattern = pathPattern, opTypes = opTypes, uidFilter = uidFilter)
        }
    }
}

object VFSRuleEngine {

    // In-memory rule storage
    private val rules = mutableListOf<VFSRule>()

    suspend fun addRule(rule: VFSRule): Boolean = withContext(Dispatchers.IO) {
        rules.add(rule)
        Log.d(TAG, "[UI-Only] Added rule: ${rule.toSimpleFormat()}")
        true
    }

    suspend fun removeRule(ruleId: String): Boolean = withContext(Dispatchers.IO) {
        val removed = rules.removeAll { it.id == ruleId }
        Log.d(TAG, "[UI-Only] Removed rule: $ruleId (found=$removed)")
        removed
    }

    suspend fun updateRule(rule: VFSRule): Boolean = withContext(Dispatchers.IO) {
        val index = rules.indexOfFirst { it.id == rule.id }
        if (index >= 0) { rules[index] = rule; true } else false
    }

    suspend fun getRules(): List<VFSRule> = withContext(Dispatchers.IO) {
        rules.filter { it.enabled }.sortedByDescending { it.priority }
    }

    suspend fun getAllRules(): List<VFSRule> = withContext(Dispatchers.IO) {
        rules.sortedByDescending { it.priority }
    }

    suspend fun getRule(ruleId: String): VFSRule? = withContext(Dispatchers.IO) {
        rules.find { it.id == ruleId }
    }

    suspend fun clearRules(): Boolean = withContext(Dispatchers.IO) {
        rules.clear()
        Log.d(TAG, "[UI-Only] Cleared all rules")
        true
    }

    suspend fun enableRule(ruleId: String): Boolean = withContext(Dispatchers.IO) {
        rules.find { it.id == ruleId }?.let { rules[rules.indexOf(it)] = it.copy(enabled = true); true } ?: false
    }

    suspend fun disableRule(ruleId: String): Boolean = withContext(Dispatchers.IO) {
        rules.find { it.id == ruleId }?.let { rules[rules.indexOf(it)] = it.copy(enabled = false); true } ?: false
    }

    suspend fun getRuleCount(): Int = withContext(Dispatchers.IO) { rules.size }

    suspend fun getActiveRuleCount(): Int = withContext(Dispatchers.IO) { rules.count { it.enabled } }

    fun evaluate(path: String, uid: Int, op: VFSOp): RuleAction {
        val matchingRules = rules.filter { it.enabled }.sortedByDescending { it.priority }
        for (rule in matchingRules) {
            if (matchesPath(rule.pathPattern, path) && matchesUid(rule.uidFilter, uid) && rule.opTypes.contains(op)) {
                return rule.action
            }
        }
        return RuleAction.ALLOW
    }

    private fun matchesPath(pattern: String, path: String): Boolean {
        // Simple glob matching for UI demo
        if (pattern == "/*" || pattern == "/**") return true
        if (pattern.endsWith("/**")) return path.startsWith(pattern.dropLast(3))
        if (pattern.endsWith("/*")) return path.startsWith(pattern.dropLast(2))
        return path == pattern
    }

    private fun matchesUid(filter: Int?, uid: Int): Boolean {
        return filter == null || filter == uid
    }

    suspend fun exportRules(): JSONArray = withContext(Dispatchers.IO) {
        JSONArray(rules.map { it.toJson() })
    }

    suspend fun importRules(jsonArray: JSONArray): Boolean = withContext(Dispatchers.IO) {
        try {
            rules.clear()
            for (i in 0 until jsonArray.length()) {
                rules.add(VFSRule.fromJson(jsonArray.getJSONObject(i)))
            }
            true
        } catch (e: Exception) { false }
    }

    suspend fun loadRulesFromPersistence(): Boolean = withContext(Dispatchers.IO) {
        try {
            val persisted = VFSPersistenceManager.loadRules()
            rules.clear()
            rules.addAll(persisted)
            true
        } catch (e: Exception) { false }
    }

    suspend fun saveRulesToPersistence(): Boolean = withContext(Dispatchers.IO) {
        VFSPersistenceManager.saveRules(rules)
    }

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] VFSRuleEngine initialized")
        true
    }

    fun validateRule(rule: VFSRule): Pair<Boolean, String> {
        if (rule.pathPattern.isBlank()) return Pair(false, "Path pattern cannot be blank")
        if (rule.pathPattern.startsWith("/") && rule.pathPattern.length < 2) return Pair(false, "Path pattern too short")
        return Pair(true, "Valid")
    }

    // Alias for compatibility with VFSDebugScreen
    suspend fun deleteRule(ruleId: String): Boolean = removeRule(ruleId)

    fun getDebugInfo(): String {
        return buildString {
            appendLine("VFSRuleEngine (UI-Only Mode):")
            appendLine("  Total Rules: ${rules.size}")
            appendLine("  Active Rules: ${rules.count { it.enabled }}")
            rules.forEach { appendLine("  - ${it.toSimpleFormat()} (priority=${it.priority})") }
        }
    }
}
