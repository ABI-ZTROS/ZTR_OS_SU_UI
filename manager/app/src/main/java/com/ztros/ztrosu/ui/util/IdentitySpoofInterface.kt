package com.ztros.ztrosu.ui.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// UI-Only Mode: All sysfs reads return mock data. No kernel communication.

private const val TAG = "IdentitySpoof"

enum class SpoofIdType(val code: Int, val displayName: String) {
    MAC_WIFI(0, "WiFi MAC"),
    MAC_BT(1, "蓝牙 MAC"),
    ANDROID_ID(2, "Android ID"),
    BUILD_SERIAL(3, "序列号"),
    IMEI(4, "IMEI"),
    IMSI(5, "IMSI"),
    AD_ID(6, "广告 ID"),
    GSF_ID(7, "GSF ID"),
    WIDEWINE(8, "Widevine"),
    FINGERPRINT(9, "指纹")
}

enum class SpoofStrategy(val code: Int, val displayName: String) {
    FIXED(0, "固定值"),
    RANDOM(1, "随机"),
    RANDOM_PER_APP(2, "每应用随机"),
    ROTATE(3, "定时轮换")
}

data class SpoofRule(
    val id: Int,
    val packageName: String,
    val idType: SpoofIdType,
    val strategy: SpoofStrategy,
    val currentValue: String,
    val enabled: Boolean = true
)

object IdentitySpoofInterface {

    private val spoofBase = "/sys/kernel/ztrosu/spoof"

    // In-memory mock rules
    private val mockRules = mutableListOf(
        SpoofRule(1, "com.example.app1", SpoofIdType.ANDROID_ID, SpoofStrategy.RANDOM, "a1b2c3d4e5f6", true),
        SpoofRule(2, "com.example.app2", SpoofIdType.MAC_WIFI, SpoofStrategy.FIXED, "02:00:00:00:00:01", true),
        SpoofRule(3, "com.example.app3", SpoofIdType.IMEI, SpoofStrategy.RANDOM_PER_APP, "350000000000001", false)
    )
    private var mockEnabled = true
    private var nextId = 4

    suspend fun getRules(): List<SpoofRule> = withContext(Dispatchers.IO) {
        // UI-Only: return mock rules
        mockRules.toList()
    }

    suspend fun addRule(
        packageName: String,
        idType: SpoofIdType,
        strategy: SpoofStrategy,
        fakeValue: String = "",
        rotateIntervalSec: Int = 0
    ): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] addRule: $packageName ${idType.name} ${strategy.name}")
        val value = if (fakeValue.isBlank()) "MOCK_${System.currentTimeMillis()}" else fakeValue
        mockRules.add(SpoofRule(nextId++, packageName, idType, strategy, value, true))
        true
    }

    suspend fun removeRule(ruleId: Int): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] removeRule: $ruleId")
        mockRules.removeAll { it.id == ruleId }
        true
    }

    suspend fun rotateAll(): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "[UI-Only] rotateAll (no-op)")
        true
    }

    suspend fun isEnabled(): Boolean = withContext(Dispatchers.IO) {
        mockEnabled
    }

    suspend fun setEnabled(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        mockEnabled = enabled
        Log.d(TAG, "[UI-Only] setEnabled: $enabled")
        true
    }

    private fun parseRuleLine(line: String): SpoofRule? {
        val parts = line.split(":")
        if (parts.size < 6) return null
        return try {
            val id = parts[0].toIntOrNull() ?: return null
            val packageName = parts[1]
            val idType = SpoofIdType.values().find { it.name == parts[2] } ?: return null
            val strategy = SpoofStrategy.values().find { it.code == parts[3].toIntOrNull() } ?: return null
            val value = parts[4]
            val enabled = parts[5] == "1"
            SpoofRule(id, packageName, idType, strategy, value, enabled)
        } catch (e: Exception) { null }
    }
}
