package com.ztros.ztrosu.ui.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

// UI-Only Mode: All persistence is in-memory only. No file I/O or root operations.

private const val TAG = "VFSPersistenceManager"

/**
 * VFS Configuration data class (UI-Only Mode)
 */
data class VFSConfig(
    val version: Int = 1,
    val enabled: Boolean = false,
    val logLevel: Int = 2,
    val defaultAction: String = "allow",
    val activeTemplateId: String = "",
    val lastModified: Long = System.currentTimeMillis()
) {
    fun toJSON(): JSONObject {
        return JSONObject().apply {
            put("version", version)
            put("enabled", enabled)
            put("logLevel", logLevel)
            put("defaultAction", defaultAction)
            put("activeTemplateId", activeTemplateId)
            put("lastModified", lastModified)
        }
    }

    companion object {
        fun fromJSON(json: JSONObject): VFSConfig {
            return VFSConfig(
                version = json.optInt("version", 1),
                enabled = json.optBoolean("enabled", false),
                logLevel = json.optInt("logLevel", 2),
                defaultAction = json.optString("defaultAction", "allow"),
                activeTemplateId = json.optString("activeTemplateId", ""),
                lastModified = json.optLong("lastModified", System.currentTimeMillis())
            )
        }
    }
}

object VFSPersistenceManager {

    private const val VFS_BASE_PATH = "/data/adb/ztrosu"
    private const val VFS_CONFIG_FILE = "$VFS_BASE_PATH/vfs_config.json"
    private const val VFS_HOOKS_FILE = "$VFS_BASE_PATH/vfs_hooks.json"
    private const val VFS_RULES_FILE = "$VFS_BASE_PATH/vfs_rules.json"
    private const val VFS_TEMPLATES_FILE = "$VFS_BASE_PATH/vfs_templates.json"
    private const val VFS_STATS_HISTORY_FILE = "$VFS_BASE_PATH/vfs_stats_history.json"
    private const val CONFIG_VERSION = 1

    private val _autoSaveEnabled = MutableStateFlow(true)
    val autoSaveEnabled: StateFlow<Boolean> = _autoSaveEnabled

    private var hasPendingChanges = false
    private var lastSaveTime = 0L
    private const val MIN_SAVE_INTERVAL_MS = 1000L

    // ==================== In-memory storage ====================

    private var memoryConfig: String? = null
    private var memoryHooks: String? = null
    private var memoryRules: String? = null
    private var memoryTemplates: String? = null
    private var memoryStatsHistory: String? = null

    suspend fun initialize(): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "[UI-Only] Persistence manager initialized (in-memory)")
        memoryConfig = createDefaultConfig()
        memoryHooks = JSONArray().toString()
        memoryRules = JSONArray().toString()
        memoryTemplates = JSONArray().toString()
        memoryStatsHistory = JSONArray().toString()
        true
    }

    private fun createDefaultConfig(): String {
        return JSONObject().apply {
            put("version", CONFIG_VERSION)
            put("enabled", false)
            put("logLevel", 2)
            put("defaultAction", "allow")
            put("activeTemplateId", "")
            put("lastModified", System.currentTimeMillis())
        }.toString()
    }

    // ==================== Main Configuration ====================

    suspend fun loadConfig(): VFSConfig? = withContext(Dispatchers.IO) {
        try {
            val jsonStr = memoryConfig ?: createDefaultConfig()
            VFSConfig.fromJSON(JSONObject(jsonStr))
        } catch (e: Exception) {
            Log.e(TAG, "[UI-Only] Failed to load config", e)
            null
        }
    }

    suspend fun saveConfig(config: VFSConfig): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = config.toJSON()
            json.put("lastModified", System.currentTimeMillis())
            memoryConfig = json.toString()
            lastSaveTime = System.currentTimeMillis()
            Log.i(TAG, "[UI-Only] Saved main configuration to memory")
            true
        } catch (e: Exception) {
            Log.e(TAG, "[UI-Only] Failed to save config", e)
            false
        }
    }

    // ==================== Hook Targets ====================

    suspend fun loadHookTargets(): List<VFSHookTarget> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = memoryHooks ?: return@withContext emptyList()
            val json = JSONArray(jsonStr)
            (0 until json.length()).map { VFSHookTarget.fromJSON(json.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveHookTargets(targets: List<VFSHookTarget>): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONArray(targets.map { it.toJSON() })
            memoryHooks = json.toString()
            lastSaveTime = System.currentTimeMillis()
            Log.i(TAG, "[UI-Only] Saved ${targets.size} hook targets to memory")
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveHookTargetsRaw(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        memoryHooks = jsonString
        lastSaveTime = System.currentTimeMillis()
        true
    }

    // ==================== Rules ====================

    suspend fun loadRules(): List<VFSRule> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = memoryRules ?: return@withContext emptyList()
            val json = JSONArray(jsonStr)
            (0 until json.length()).map { VFSRule.fromJson(json.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveRules(rules: List<VFSRule>): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONArray(rules.map { it.toJson() })
            memoryRules = json.toString()
            lastSaveTime = System.currentTimeMillis()
            Log.i(TAG, "[UI-Only] Saved ${rules.size} rules to memory")
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveRulesRaw(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        memoryRules = jsonString
        lastSaveTime = System.currentTimeMillis()
        true
    }

    // ==================== Templates ====================

    suspend fun loadTemplates(): List<VFSTemplate> = withContext(Dispatchers.IO) {
        try {
            val jsonStr = memoryTemplates ?: return@withContext emptyList()
            val json = JSONArray(jsonStr)
            (0 until json.length()).map { VFSTemplate.fromJSON(json.getJSONObject(it)) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveTemplates(templates: List<VFSTemplate>): Boolean = withContext(Dispatchers.IO) {
        try {
            val json = JSONArray(templates.map { it.toJSON() })
            memoryTemplates = json.toString()
            lastSaveTime = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun saveTemplatesRaw(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        memoryTemplates = jsonString
        lastSaveTime = System.currentTimeMillis()
        true
    }

    // ==================== Stats History ====================

    suspend fun loadStatsHistory(): JSONArray = withContext(Dispatchers.IO) {
        try {
            JSONArray(memoryStatsHistory ?: "[]")
        } catch (e: Exception) {
            JSONArray()
        }
    }

    suspend fun saveStatsHistory(history: JSONArray): Boolean = withContext(Dispatchers.IO) {
        memoryStatsHistory = history.toString()
        lastSaveTime = System.currentTimeMillis()
        true
    }

    suspend fun addStatsSnapshot(snapshot: JSONObject): Boolean = withContext(Dispatchers.IO) {
        try {
            val history = loadStatsHistory()
            history.put(snapshot)
            memoryStatsHistory = history.toString()
            true
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Backup/Restore (in-memory) ====================

    suspend fun backupToFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "[UI-Only] backupToFile: no-op (in-memory only)")
        true
    }

    suspend fun restoreFromFile(filePath: String): Boolean = withContext(Dispatchers.IO) {
        Log.i(TAG, "[UI-Only] restoreFromFile: no-op (in-memory only)")
        true
    }

    // ==================== Utility ====================

    private fun checkSaveInterval() {
        val now = System.currentTimeMillis()
        if (now - lastSaveTime < MIN_SAVE_INTERVAL_MS) {
            // In UI-Only mode, don't actually delay
        }
    }

    private fun triggerAutoSave(source: String) {
        hasPendingChanges = true
        Log.d(TAG, "[UI-Only] Auto-save triggered from: $source")
    }

    fun hasPendingChanges(): Boolean = hasPendingChanges

    fun getLastSaveTime(): Long = lastSaveTime

    fun clearPendingChanges() {
        hasPendingChanges = false
    }

    fun getStorageInfo(): String {
        return buildString {
            appendLine("VFSPersistenceManager (UI-Only Mode):")
            appendLine("  Storage: In-memory only")
            appendLine("  Config: ${if (memoryConfig != null) "loaded" else "empty"}")
            appendLine("  Hooks: ${if (memoryHooks != null) "loaded" else "empty"}")
            appendLine("  Rules: ${if (memoryRules != null) "loaded" else "empty"}")
            appendLine("  Templates: ${if (memoryTemplates != null) "loaded" else "empty"}")
            appendLine("  Last Save: $lastSaveTime")
        }
    }
}
