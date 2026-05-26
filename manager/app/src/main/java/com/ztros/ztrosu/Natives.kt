package com.ztros.ztrosu

import android.os.Parcelable
import androidx.annotation.Keep
import androidx.compose.runtime.Immutable
import kotlinx.parcelize.Parcelize

/**
 * ZTR_OS SU - UI Only Mode (Mocked for testing)
 * All kernel communication methods return simulated values
 */
object Natives {
    // minimal supported kernel version
    const val MINIMAL_SUPPORTED_KERNEL = 33075

    const val KERNEL_SU_DOMAIN = "u:r:su:s0"

    const val ROOT_UID = 0
    const val ROOT_GID = 0

    // UI-Only Mode: No native library loading
    // init { System.loadLibrary("kernelsu") }

    // Simulated kernel state for UI testing
    private var mockVersion = 33075
    private var mockIsManager = true
    private var mockSuperKeyActive = false
    private var mockSuEnabled = true
    private var mockKernelUmountEnabled = true
    private var mockAvcSpoofEnabled = false
    private var mockZygiskEnabled = true
    private var mockHookMode: String? = "Manual"
    private var mockVersionTag: String? = "ZTR_OS-v1.0.0"

    val version: Int
        get() = mockVersion

    val allowList: IntArray
        get() = intArrayOf(1000, 2000) // Mock UIDs

    val isSafeMode: Boolean
        get() = false

    val isManager: Boolean
        get() = mockIsManager

    fun uidShouldUmount(uid: Int): Boolean = false

    fun getManagerAppid(): Int = 1000 // Mock manager UID

    fun getHookMode(): String? = mockHookMode

    fun getVersionTag(): String? = mockVersionTag

    fun isZygiskEnabled(): Boolean = mockZygiskEnabled

    fun getAppProfile(key: String?, uid: Int): Profile {
        return Profile(
            name = key ?: "",
            currentUid = uid,
            allowSu = true,
            rootUseDefault = true,
            rootTemplate = null,
            uid = ROOT_UID,
            gid = ROOT_GID,
            groups = listOf(),
            capabilities = listOf(),
            context = KERNEL_SU_DOMAIN,
            namespace = Profile.Namespace.INHERITED.ordinal,
            nonRootUseDefault = true,
            umountModules = true,
            rules = ""
        )
    }

    fun setAppProfile(profile: Profile?): Boolean = true

    fun isSuEnabled(): Boolean = mockSuEnabled

    fun setSuEnabled(enabled: Boolean): Boolean {
        mockSuEnabled = enabled
        return true
    }

    fun isKernelUmountEnabled(): Boolean = mockKernelUmountEnabled

    fun setKernelUmountEnabled(enabled: Boolean): Boolean {
        mockKernelUmountEnabled = enabled
        return true
    }

    fun getUserName(uid: Int): String? = when(uid) {
        0 -> "root"
        1000 -> "system"
        2000 -> "shell"
        else -> "user_$uid"
    }

    fun isAvcSpoofEnabled(): Boolean = mockAvcSpoofEnabled

    fun setAvcSpoofEnabled(enabled: Boolean): Boolean {
        mockAvcSpoofEnabled = enabled
        return true
    }

    fun getSuperuserCount(): Int = 5 // Mock count

    private const val NON_ROOT_DEFAULT_PROFILE_KEY = "$"
    private const val NOBODY_UID = 9999

    fun setDefaultUmountModules(umountModules: Boolean): Boolean {
        Profile(
            NON_ROOT_DEFAULT_PROFILE_KEY,
            NOBODY_UID,
            false,
            umountModules = umountModules
        ).let {
            return setAppProfile(it)
        }
    }

    fun isDefaultUmountModules(): Boolean {
        return true
    }

    fun requireNewKernel(): Boolean = false

    // ZTR_OS SU: SuperKey support (Mocked)
    fun setSuperKey(key: String): Boolean {
        mockSuperKeyActive = key.isNotEmpty()
        return true
    }

    fun verifySuperKey(key: String): Boolean = true

    fun isSuperKeyActive(): Boolean = mockSuperKeyActive

    val KSU_WORK_DIR = "/data/adb/ksu/"

    @Immutable
    @Parcelize
    @Keep
    data class Profile(
        val name: String,
        val currentUid: Int = 0,
        val allowSu: Boolean = false,
        val rootUseDefault: Boolean = true,
        val rootTemplate: String? = null,
        val uid: Int = ROOT_UID,
        val gid: Int = ROOT_GID,
        val groups: List<Int> = mutableListOf(),
        val capabilities: List<Int> = mutableListOf(),
        val context: String = KERNEL_SU_DOMAIN,
        val namespace: Int = Namespace.INHERITED.ordinal,
        val nonRootUseDefault: Boolean = true,
        val umountModules: Boolean = true,
        var rules: String = "",
    ) : Parcelable {
        enum class Namespace {
            INHERITED,
            GLOBAL,
            INDIVIDUAL,
        }

        constructor() : this("")
    }
}
