package com.ztros.ztrosu.ui.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.ztros.ztrosu.R
import kotlinx.coroutines.*
import java.io.File

// UI-Only Mode: No actual service, just UI mock. No sysfs reads or writes.

private const val TAG = "AntiBrickService"
private const val CHANNEL_ID = "anti_brick"
private const val NOTIFICATION_ID = 1001

class AntiBrickService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_START = "com.ztros.ztrosu.ANTIBRICK_START"
        const val ACTION_STOP = "com.ztros.ztrosu.ANTIBRICK_STOP"

        fun start(context: Context) {
            val intent = Intent(context, AntiBrickService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AntiBrickService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "防格机保护", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "监控高危命令执行"
                setSound(null, null)
                enableVibration(true)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun startMonitoring() {
        Log.i(TAG, "[UI-Only] Kernel-layer anti-brick started (mock mode)")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("防格机保护 [UI模式]")
            .setContentText("UI-Only: 防格机保护模拟运行中")
            .setSmallIcon(R.drawable.ztros_shield)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AuroraSU:AntiBrick").apply {
            acquire(10 * 60 * 1000L)
        }

        // UI-Only: no actual polling, just keep service alive
        monitorJob = scope.launch {
            while (isActive) {
                delay(5000) // Check every 5s (no-op in UI mode)
            }
        }
    }

    private fun isUserLayerServiceActive(): Boolean = false

    private suspend fun checkPendingRequests() = withContext(Dispatchers.IO) {
        // UI-Only: no actual sysfs reads
    }

    private fun showConfirmDialog(id: Int, pid: Int, uid: Int, riskType: String, cmdline: String) {
        // UI-Only: dialog display logic preserved for UI testing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                denyRequest(id)
                return
            }
        }

        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_anti_brick, null)

        view.findViewById<TextView>(R.id.tv_risk_title).text = getRiskTitle(riskType)
        view.findViewById<TextView>(R.id.tv_risk_reason).text = getRiskReason(riskType)
        view.findViewById<TextView>(R.id.tv_cmdline).text = cmdline
        view.findViewById<TextView>(R.id.tv_pid_info).text = "PID: $pid | UID: $uid"

        view.findViewById<Button>(R.id.btn_allow).setOnClickListener {
            allowRequest(id)
            wm.removeView(view)
        }
        view.findViewById<Button>(R.id.btn_deny).setOnClickListener {
            denyRequest(id)
            wm.removeView(view)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or WindowManager.LayoutParams.FLAG_DIM_BEHIND or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.CENTER; dimAmount = 0.7f }

        try { wm.addView(view, params) } catch (e: Exception) { denyRequest(id) }
    }

    private fun allowRequest(id: Int) {
        Log.i(TAG, "[UI-Only] Allowed request #$id (no-op)")
    }

    private fun denyRequest(id: Int) {
        Log.i(TAG, "[UI-Only] Denied request #$id (no-op)")
    }

    private fun getRiskTitle(riskType: String): String = when (riskType) {
        "RM_RF_ROOT" -> "⚠ 高危：删除整个文件系统"
        "DD_BLOCK" -> "⚠ 高危：写入块设备"
        "DD_ZERO" -> "⚠ 高危：擦除块设备"
        "MKFS" -> "⚠ 高危：格式化分区"
        "FDISK", "PARTED" -> "⚠ 高危：修改分区表"
        "FLASH" -> "⚠ 高危：刷写系统分区"
        "RECOVERY" -> "⚠ 高危：恢复出厂设置"
        else -> "⚠ 高危操作 detected"
    }

    private fun getRiskReason(riskType: String): String = when (riskType) {
        "RM_RF_ROOT" -> "rm -rf / 将删除设备上所有文件，导致系统无法启动"
        "DD_BLOCK" -> "dd 命令正在向块设备写入数据，可能覆盖系统分区"
        "DD_ZERO" -> "dd 命令正在用零填充块设备，将永久擦除所有数据"
        "MKFS" -> "格式化命令将删除分区上的所有数据和文件系统"
        "FDISK", "PARTED" -> "分区操作可能破坏分区表，导致数据丢失"
        "FLASH" -> "刷写命令可能覆盖系统分区，导致设备变砖"
        "RECOVERY" -> "恢复出厂设置将清除所有用户数据和已安装应用"
        else -> "检测到可能损坏设备的高危操作"
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        Log.i(TAG, "[UI-Only] Anti-brick monitoring stopped")
    }
}
