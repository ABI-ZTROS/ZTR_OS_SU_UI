package com.ztros.ztrosu.ui.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.ztros.ztrosu.R
import kotlinx.coroutines.*
import java.io.File

// UI-Only Mode: No actual polling. No sysfs reads, no Runtime.exec, no /proc scanning.

private const val TAG = "AntiBrickUser"
private const val CHANNEL_ID = "anti_brick_user"
private const val NOTIFICATION_ID = 1002

class AntiBrickUserService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var monitorJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    companion object {
        const val ACTION_START = "com.ztros.ztrosu.ANTIBRICK_USER_START"
        const val ACTION_STOP = "com.ztros.ztrosu.ANTIBRICK_USER_STOP"

        fun start(context: Context) {
            val intent = Intent(context, AntiBrickUserService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AntiBrickUserService::class.java))
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
            val channel = NotificationChannel(CHANNEL_ID, "防格机保护(用户层)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "用户层冗余防格机保护"
                setSound(null, null)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun startMonitoring() {
        Log.i(TAG, "[UI-Only] User-layer anti-brick started (mock mode)")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("防格机保护 [UI模式]")
            .setContentText("UI-Only: 用户层防格机保护模拟运行中")
            .setSmallIcon(R.drawable.ztros_shield)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AuroraSU:AntiBrickUser").apply {
            acquire(10 * 60 * 1000L)
        }

        // UI-Only: no actual polling, just keep service alive
        monitorJob = scope.launch {
            while (isActive) {
                delay(5000)
            }
        }
    }

    private fun isKernelModuleActive(): Boolean {
        // UI-Only: always return false (user layer is independent in UI mode)
        return false
    }

    private fun showBlockedNotification(riskType: String, cmdline: String, pid: Int) {
        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val view = LayoutInflater.from(this).inflate(R.layout.dialog_anti_brick, null)

            view.findViewById<TextView>(R.id.tv_risk_title).text = getRiskTitle(riskType)
            view.findViewById<TextView>(R.id.tv_risk_reason).text = "用户层拦截：${getRiskReason(riskType)}"
            view.findViewById<TextView>(R.id.tv_cmdline).text = cmdline
            view.findViewById<TextView>(R.id.tv_pid_info).text = "PID: $pid | 已自动终止"

            view.findViewById<Button>(R.id.btn_allow).apply {
                text = "知道了"
                setOnClickListener { try { wm.removeView(view) } catch (_: Exception) {} }
            }
            view.findViewById<Button>(R.id.btn_deny).apply {
                text = "查看日志"
                setOnClickListener { try { wm.removeView(view) } catch (_: Exception) {} }
            }

            val params = android.view.WindowManager.LayoutParams(
                android.view.WindowManager.LayoutParams.MATCH_PARENT, android.view.WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                android.graphics.PixelFormat.TRANSLUCENT
            ).apply { gravity = android.view.Gravity.CENTER; dimAmount = 0.7f }

            wm.addView(view, params)
        } catch (e: Exception) {
            Log.e(TAG, "[UI-Only] Failed to show notification", e)
        }
    }

    private fun getRiskTitle(riskType: String): String = when (riskType) {
        "RM_RF_ROOT" -> "⚠ 已拦截：删除整个文件系统"
        "DD_BLOCK" -> "⚠ 已拦截：写入块设备"
        "DD_ZERO" -> "⚠ 已拦截：擦除块设备"
        "MKFS" -> "⚠ 已拦截：格式化分区"
        "FDISK", "PARTED" -> "⚠ 已拦截：修改分区表"
        "FLASH" -> "⚠ 已拦截：刷写系统分区"
        "RECOVERY" -> "⚠ 已拦截：恢复出厂设置"
        else -> "⚠ 已拦截：高危操作"
    }

    private fun getRiskReason(riskType: String): String = when (riskType) {
        "RM_RF_ROOT" -> "rm -rf / 将删除设备上所有文件"
        "DD_BLOCK" -> "dd 命令正在向块设备写入数据"
        "DD_ZERO" -> "dd 命令正在用零填充块设备"
        "MKFS" -> "格式化命令将删除分区上的所有数据"
        "FDISK", "PARTED" -> "分区操作可能破坏分区表"
        "FLASH" -> "刷写命令可能覆盖系统分区"
        "RECOVERY" -> "恢复出厂设置将清除所有用户数据"
        else -> "检测到可能损坏设备的高危操作"
    }

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        scope.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        Log.i(TAG, "[UI-Only] User-layer anti-brick stopped")
    }
}
