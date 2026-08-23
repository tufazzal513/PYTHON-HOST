package com.python.localhost.util

import android.os.Debug
import java.io.File

/**
 * Best-effort resource sampling. Because Chaquopy runs Python in the same process as
 * the app, these numbers are APP-LEVEL, not per-Python-script. They are accurate for
 * memory/uptime and approximate for CPU.
 */
object ResourceMonitor {
    fun memoryUsedMb(): Long {
        val rt = Runtime.getRuntime()
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024)
    }

    fun nativeHeapMb(): Long = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)

    /** Samples CPU over ~400ms and returns a percentage of one core, or null if /proc is unavailable. */
    fun cpuPercentSample(): Float? {
        val t0 = readCpuTicks() ?: return null
        Thread.sleep(400)
        val t1 = readCpuTicks() ?: return null
        val clk = 100f // typical Linux CONFIG_HZ
        val cpuSec = (t1 - t0) / clk
        val elapsedSec = 0.4f
        return ((cpuSec / elapsedSec) * 100f).coerceIn(0f, 100f)
    }

    private fun readCpuTicks(): Long? {
        val parts = readStat()?.split(" ") ?: return null
        if (parts.size <= 17) return null
        val utime = parts[13].toLongOrNull() ?: return null
        val stime = parts[14].toLongOrNull() ?: return null
        return utime + stime
    }

    private fun readStat(): String? = try {
        File("/proc/self/stat").readText()
    } catch (e: Exception) {
        null
    }
}
