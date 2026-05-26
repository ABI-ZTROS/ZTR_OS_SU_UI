package com.ztros.ztrosu.ui.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import android.util.Log
import com.ztros.ztrosu.Natives
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import java.io.File

/**
 * ZTR_OS SU UI-Only Mode - KsuCli
 * Simplified version without libsu dependencies
 */
private const val TAG = "KsuCli"

@Parcelize
data class FlashResult(val code: Int, val err: String, val showReboot: Boolean) : Parcelable

/**
 * Check if we have root access (UI-Only mode always returns true for testing)
 */
fun rootAvailable(): Boolean = true

/**
 * Get kernel version string
 */
fun getKernelVersion(): String {
    return try {
        System.getProperty("os.version") ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }
}

/**
 * Install module - UI-Only mode (simulated)
 */
suspend fun installModule(uri: Uri, onFinish: (Boolean) -> Unit) = withContext(Dispatchers.IO) {
    // In UI-Only mode, just simulate success
    Log.i(TAG, "UI-Only: Simulating module install for $uri")
    onFinish(true)
}

/**
 * Flash module - UI-Only mode (simulated)
 */
suspend fun flashModule(uri: Uri, onFinish: (FlashResult) -> Unit) = withContext(Dispatchers.IO) {
    Log.i(TAG, "UI-Only: Simulating module flash for $uri")
    onFinish(FlashResult(0, "", true))
}

/**
 * Flash AnyKernel zip - UI-Only mode (simulated)
 */
suspend fun flashAnyKernel(uri: Uri, onFinish: (FlashResult) -> Unit) = withContext(Dispatchers.IO) {
    Log.i(TAG, "UI-Only: Simulating AnyKernel flash for $uri")
    onFinish(FlashResult(0, "", true))
}

/**
 * Reboot device
 */
fun reboot(reason: String = "") {
    ShellUtils.fastCmdResult("reboot $reason")
}

/**
 * Reboot to recovery
 */
fun rebootRecovery() {
    reboot("recovery")
}

/**
 * Reboot to bootloader
 */
fun rebootBootloader() {
    reboot("bootloader")
}

/**
 * Get SELinux enforce status
 */
fun getSelinuxEnforce(): Boolean? {
    return try {
        val result = ShellUtils.fastCmd("getenforce").trim()
        when (result.lowercase()) {
            "enforcing" -> true
            "permissive" -> false
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Set SELinux enforce mode
 */
fun setSelinuxEnforce(enforce: Boolean): Boolean {
    return try {
        val value = if (enforce) "1" else "0"
        ShellUtils.fastCmdResult("setenforce $value")
    } catch (e: Exception) {
        false
    }
}

/**
 * Get feature status - UI-Only mode returns simulated values
 */
fun getFeatureStatus(feature: String): String {
    return when (feature) {
        "avc_spoof" -> "supported"
        "su_compat" -> "supported"
        "kernel_umount" -> "supported"
        else -> "unsupported"
    }
}

/**
 * Toggle module state - UI-Only mode (simulated)
 */
fun toggleModule(moduleId: String, enable: Boolean): Boolean {
    Log.i(TAG, "UI-Only: Toggling module $moduleId to $enable")
    return true
}

/**
 * Remove module - UI-Only mode (simulated)
 */
fun removeModule(moduleId: String): Boolean {
    Log.i(TAG, "UI-Only: Removing module $moduleId")
    return true
}

/**
 * Check if module is enabled - UI-Only mode (always true)
 */
fun isModuleEnabled(moduleId: String): Boolean {
    return true
}

/**
 * Get module version - UI-Only mode
 */
fun getModuleVersion(moduleId: String): String {
    return "1.0.0"
}

/**
 * Get module author - UI-Only mode
 */
fun getModuleAuthor(moduleId: String): String {
    return "ZTR_OS"
}

/**
 * Get module description - UI-Only mode
 */
fun getModuleDescription(moduleId: String): String {
    return "UI-Only test module"
}

/**
 * Get module update url - UI-Only mode
 */
fun getModuleUpdateUrl(moduleId: String): String? {
    return null
}

/**
 * Get module version code - UI-Only mode
 */
fun getModuleVersionCode(moduleId: String): Long {
    return 1000L
}

/**
 * Get module size - UI-Only mode
 */
fun getModuleSize(moduleId: String): Long {
    return 0L
}

/**
 * Get module last update - UI-Only mode
 */
fun getModuleLastUpdate(moduleId: String): Long {
    return System.currentTimeMillis()
}

/**
 * List all modules - UI-Only mode (returns mock list)
 */
fun listModules(): List<String> {
    return listOf("test_module_1", "test_module_2", "test_module_3")
}

/**
 * Get module count
 */
fun getModuleCount(): Int = 3

/**
 * Get enabled module count
 */
fun getEnabledModuleCount(): Int = 3

/**
 * Get disabled module count
 */
fun getDisabledModuleCount(): Int = 0

/**
 * Get module info - UI-Only mode
 */
fun getModuleInfo(moduleId: String): ModuleInfo {
    return ModuleInfo(
        id = moduleId,
        name = "Test Module",
        version = "1.0.0",
        versionCode = 1000,
        author = "ZTR_OS",
        description = "UI-Only test module",
        enabled = true,
        updateUrl = null,
        size = 0,
        lastUpdate = System.currentTimeMillis()
    )
}

@Parcelize
data class ModuleInfo(
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Long,
    val author: String,
    val description: String,
    val enabled: Boolean,
    val updateUrl: String?,
    val size: Long,
    val lastUpdate: Long
) : Parcelable

/**
 * Get superuser list - UI-Only mode (returns mock list)
 */
fun getSuperuserList(): List<SuperuserInfo> {
    return listOf(
        SuperuserInfo(1000, "android", true),
        SuperuserInfo(2000, "shell", true),
        SuperuserInfo(9999, "test_app", false)
    )
}

@Parcelize
data class SuperuserInfo(
    val uid: Int,
    val packageName: String,
    val allowed: Boolean
) : Parcelable

/**
 * Allow superuser - UI-Only mode
 */
fun allowSuperuser(uid: Int): Boolean {
    Log.i(TAG, "UI-Only: Allowing superuser for uid $uid")
    return true
}

/**
 * Deny superuser - UI-Only mode
 */
fun denySuperuser(uid: Int): Boolean {
    Log.i(TAG, "UI-Only: Denying superuser for uid $uid")
    return true
}

/**
 * Revoke superuser - UI-Only mode
 */
fun revokeSuperuser(uid: Int): Boolean {
    Log.i(TAG, "UI-Only: Revoking superuser for uid $uid")
    return true
}

/**
 * Get app profile - UI-Only mode
 */
fun getAppProfile(uid: Int): AppProfile? {
    return AppProfile(
        uid = uid,
        allowSu = true,
        rootUseDefault = true,
        rootTemplate = null,
        umountModules = false
    )
}

@Parcelize
data class AppProfile(
    val uid: Int,
    val allowSu: Boolean,
    val rootUseDefault: Boolean,
    val rootTemplate: String?,
    val umountModules: Boolean
) : Parcelable

/**
 * Set app profile - UI-Only mode
 */
fun setAppProfile(profile: AppProfile): Boolean {
    Log.i(TAG, "UI-Only: Setting app profile for uid ${profile.uid}")
    return true
}

/**
 * Get profile template - UI-Only mode
 */
fun getProfileTemplates(): List<String> {
    return listOf("default", "none", "inherited")
}

/**
 * Save profile template - UI-Only mode
 */
fun saveProfileTemplate(name: String, template: String): Boolean {
    Log.i(TAG, "UI-Only: Saving profile template $name")
    return true
}

/**
 * Delete profile template - UI-Only mode
 */
fun deleteProfileTemplate(name: String): Boolean {
    Log.i(TAG, "UI-Only: Deleting profile template $name")
    return true
}

/**
 * Get backup list - UI-Only mode
 */
fun getBackupList(): List<String> {
    return emptyList()
}

/**
 * Restore backup - UI-Only mode
 */
fun restoreBackup(name: String): Boolean {
    Log.i(TAG, "UI-Only: Restoring backup $name")
    return true
}

/**
 * Delete backup - UI-Only mode
 */
fun deleteBackup(name: String): Boolean {
    Log.i(TAG, "UI-Only: Deleting backup $name")
    return true
}

/**
 * Get log path - UI-Only mode
 */
fun getLogPath(): String {
    return "/data/local/tmp/ksu_log.txt"
}

/**
 * Clear logs - UI-Only mode
 */
fun clearLogs(): Boolean {
    Log.i(TAG, "UI-Only: Clearing logs")
    return true
}

/**
 * Get bug report - UI-Only mode
 */
fun getBugreportFile(activity: Activity): File {
    return File(activity.cacheDir, "bugreport.txt").apply {
        writeText("UI-Only Mode Bug Report\nGenerated: ${Date()}\n")
    }
}

/**
 * Restart activity
 */
fun restartActivity(activity: Activity) {
    val intent = activity.intent
    activity.finish()
    activity.startActivity(intent)
}

/**
 * Refresh activity
 */
fun refreshActivity(activity: Activity) {
    restartActivity(activity)
}

/**
 * Check if module is exist - UI-Only mode
 */
fun isModuleExist(moduleId: String): Boolean {
    return listModules().contains(moduleId)
}

/**
 * Check if module has webui - UI-Only mode
 */
fun hasWebUI(moduleId: String): Boolean {
    return false
}

/**
 * Get module webui path - UI-Only mode
 */
fun getModuleWebUIPath(moduleId: String): String? {
    return null
}

/**
 * Open module webui - UI-Only mode
 */
fun openModuleWebUI(activity: Activity, moduleId: String) {
    // No-op in UI-Only mode
}

/**
 * Install module from url - UI-Only mode
 */
suspend fun installModule(url: String, onFinish: (Boolean) -> Unit) {
    Log.i(TAG, "UI-Only: Installing module from $url")
    onFinish(true)
}

/**
 * Check for module update - UI-Only mode
 */
suspend fun checkModuleUpdate(moduleId: String): Boolean {
    return false
}

/**
 * Get module update info - UI-Only mode
 */
fun getModuleUpdateInfo(moduleId: String): ModuleUpdateInfo? {
    return null
}

@Parcelize
data class ModuleUpdateInfo(
    val version: String,
    val versionCode: Long,
    val zipUrl: String,
    val changelog: String
) : Parcelable

/**
 * Update module - UI-Only mode
 */
suspend fun updateModule(moduleId: String, onFinish: (Boolean) -> Unit) {
    Log.i(TAG, "UI-Only: Updating module $moduleId")
    onFinish(true)
}

/**
 * Get version name - UI-Only mode
 */
fun getVersionName(): String {
    return "1.0.0"
}

/**
 * Get version code - UI-Only mode
 */
fun getVersionCode(): Long {
    return 1000L
}

/**
 * Check for update - UI-Only mode
 */
suspend fun checkForUpdate(): Boolean {
    return false
}

/**
 * Get update info - UI-Only mode
 */
fun getUpdateInfo(): UpdateInfo? {
    return null
}

@Parcelize
data class UpdateInfo(
    val version: String,
    val versionCode: Long,
    val apkUrl: String,
    val changelog: String
) : Parcelable

/**
 * Download and install update - UI-Only mode
 */
suspend fun downloadAndInstallUpdate(onProgress: (Int) -> Unit, onFinish: (Boolean) -> Unit) {
    onFinish(false)
}

/**
 * Get default umount modules - UI-Only mode
 */
fun isDefaultUmountModules(): Boolean {
    return false
}

/**
 * Set default umount modules - UI-Only mode
 */
fun setDefaultUmountModules(umount: Boolean): Boolean {
    Log.i(TAG, "UI-Only: Setting default umount modules to $umount")
    return true
}

/**
 * Check if SELinux policy is valid - UI-Only mode
 */
fun isSepolicyValid(rules: String?): Boolean {
    return true
}

/**
 * List app profile templates - UI-Only mode
 */
fun listAppProfileTemplates(): List<String> {
    return listOf("default", "none")
}

/**
 * Set SELinux policy - UI-Only mode
 */
fun setSepolicy(rules: String): Boolean {
    Log.i(TAG, "UI-Only: Setting SELinux policy")
    return true
}

/**
 * Get SELinux policy - UI-Only mode
 */
fun getSepolicy(): String {
    return ""
}

/**
 * Get SELinux policy for package - UI-Only mode
 */
fun getSepolicy(packageName: String): String {
    return ""
}

/**
 * Launch app - UI-Only mode
 */
fun launchApp(packageName: String): Boolean {
    Log.i(TAG, "UI-Only: Launching app $packageName")
    return true
}

/**
 * Force stop app - UI-Only mode
 */
fun forceStopApp(packageName: String): Boolean {
    Log.i(TAG, "UI-Only: Force stopping app $packageName")
    return true
}

/**
 * Restart app - UI-Only mode
 */
fun restartApp(packageName: String): Boolean {
    Log.i(TAG, "UI-Only: Restarting app $packageName")
    return true
}

/**
 * Install module from URI with callback - UI-Only mode
 */
fun install(uri: Uri, onFinish: (Boolean) -> Unit) {
    Log.i(TAG, "UI-Only: Installing module from URI $uri")
    installModule(uri, onFinish)
}
