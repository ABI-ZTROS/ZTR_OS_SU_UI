package com.ztros.ztrosu.ui.util

import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

// UI-Only Mode: No actual netlink socket. Mock events generated periodically.

private const val TAG = "VFSNetlinkListener"

object VFSNetlinkListener {

    const val EVENT_VFS_OPEN = 1
    const val EVENT_VFS_READ = 2
    const val EVENT_VFS_WRITE = 3
    const val EVENT_VFS_CLOSE = 4
    const val EVENT_VFS_DENY = 5
    const val EVENT_HOOK_ADDED = 10
    const val EVENT_HOOK_REMOVED = 11
    const val EVENT_RULE_CHANGED = 12

    const val NETLINK_GROUP = 31
    private const val EVENT_HEADER_SIZE = 28
    private const val EVENT_MAGIC: Int = 0xAF5F

    data class VFSEvent(
        val eventType: Int,
        val pid: Int,
        val uid: Int,
        val path: String,
        val timestamp: Long,
        val result: Int
    ) {
        fun getEventTypeName(): String = when (eventType) {
            EVENT_VFS_OPEN -> "OPEN"; EVENT_VFS_READ -> "READ"; EVENT_VFS_WRITE -> "WRITE"
            EVENT_VFS_CLOSE -> "CLOSE"; EVENT_VFS_DENY -> "DENY"; EVENT_HOOK_ADDED -> "HOOK_ADDED"
            EVENT_HOOK_REMOVED -> "HOOK_REMOVED"; EVENT_RULE_CHANGED -> "RULE_CHANGED"
            else -> "UNKNOWN($eventType)"
        }
        fun getResultName(): String = if (result == 0) "ALLOW" else "DENY"
        override fun toString(): String = "VFSEvent(type=${getEventTypeName()}, pid=$pid, uid=$uid, path=$path, ts=$timestamp, result=${getResultName()})"
    }

    private val isListening = AtomicBoolean(false)
    private val totalEventsReceived = AtomicLong(0)
    private var eventCallback: ((VFSEvent) -> Unit)? = null
    private var mockEventThread: Thread? = null

    fun startListening(callback: (VFSEvent) -> Unit) {
        if (isListening.getAndSet(true)) {
            Log.w(TAG, "[UI-Only] Already listening")
            return
        }
        eventCallback = callback
        Log.i(TAG, "[UI-Only] Starting VFS Netlink event listener (mock)")
        startMockEventGenerator()
    }

    fun stopListening() {
        if (!isListening.getAndSet(false)) return
        mockEventThread?.interrupt()
        mockEventThread = null
        eventCallback = null
        Log.i(TAG, "[UI-Only] VFS Netlink event listener stopped. Total events: $totalEventsReceived")
    }

    fun isListeningNow(): Boolean = isListening.get()

    fun getTotalEventsReceived(): Long = totalEventsReceived.get()

    fun resetEventCount() {
        totalEventsReceived.set(0)
    }

    private fun startMockEventGenerator() {
        mockEventThread = Thread({
            val mockPaths = listOf(
                "/system/bin/su", "/data/data/com.app/databases/db.sqlite",
                "/sdcard/Download/file.apk", "/proc/self/status",
                "/system/framework/services.jar", "/data/local/tmp/script.sh"
            )
            val eventTypes = intArrayOf(EVENT_VFS_OPEN, EVENT_VFS_READ, EVENT_VFS_WRITE, EVENT_VFS_CLOSE)
            var index = 0
            while (isListening.get() && !Thread.currentThread().isInterrupted) {
                try {
                    Thread.sleep(2000 + (Math.random() * 3000).toLong())
                    val cb = eventCallback ?: continue
                    val eventType = eventTypes[index % eventTypes.size]
                    val path = mockPaths[index % mockPaths.size]
                    val pid = 1000 + (Math.random() * 9000).toInt()
                    val uid = 10000 + (Math.random() * 5000).toInt()
                    val result = if (Math.random() > 0.1) 0 else 1
                    val event = VFSEvent(
                        eventType = eventType,
                        pid = pid,
                        uid = uid,
                        path = path,
                        timestamp = System.currentTimeMillis(),
                        result = result
                    )
                    cb(event)
                    totalEventsReceived.incrementAndGet()
                    index++
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "[UI-Only] Error generating mock event", e)
                }
            }
        }, "VFSNetlink-Mock").apply { isDaemon = true }
        mockEventThread?.start()
    }

    // ==================== JNI stubs (no-op) ====================

    // UI-Only Mode: no actual JNI calls
    private fun nativeCreateNetlinkSocket(group: Int): Int = -1
    private fun nativeCloseNetlinkSocket(fd: Int) {}

    // ==================== Shell fallback stubs (no-op) ====================

    fun isNetlinkAvailable(): Boolean {
        // UI-Only: always report available
        return true
    }

    fun getNetlinkStatus(): String {
        return buildString {
            appendLine("VFSNetlinkListener Debug Info (UI-Only Mode):")
            appendLine("  Netlink Group: $NETLINK_GROUP")
            appendLine("  Netlink Available: true (mock)")
            appendLine("  Listening: ${isListening.get()}")
            appendLine("  Total Events: ${totalEventsReceived.get()}")
        }
    }

    // Buffered events for UI-Only mode
    private val bufferedEvents = mutableListOf<VFSEvent>()

    fun getBufferedEvents(): List<VFSEvent> = synchronized(bufferedEvents) {
        bufferedEvents.toList()
    }

    fun clearBuffer() {
        synchronized(bufferedEvents) {
            bufferedEvents.clear()
        }
    }

    fun addBufferedEvent(event: VFSEvent) {
        synchronized(bufferedEvents) {
            bufferedEvents.add(event)
            if (bufferedEvents.size > 500) {
                bufferedEvents.removeAt(0)
            }
        }
    }
}
