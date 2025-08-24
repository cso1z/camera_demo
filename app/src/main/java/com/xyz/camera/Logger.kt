package com.xyz.camera

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class Logger(private val context: Context) {
    private var logFile: File? = null
    private var currentLogHour: Int = -1
    private val logDateFormat = SimpleDateFormat("yyyy-MM-dd_HH", Locale.getDefault())
    private val logTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    init {
        initLogFile()
    }
    
    private fun initLogFile() {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        
        if (currentHour != currentLogHour) {
            currentLogHour = currentHour
            val logDir = File(context.externalCacheDir, "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val fileName = "usb_camera_${logDateFormat.format(now.time)}.log"
            logFile = File(logDir, fileName)
        }
    }
    
    private fun writeLog(level: String, tag: String, message: String) {
        try {
            val timestamp = logTimeFormat.format(Date())
            val logEntry = "[$timestamp] [$level] [$tag] $message\n"
            
            // 检查是否需要创建新的日志文件
            initLogFile()
            
            logFile?.let { file ->
                FileOutputStream(file, true).use { fos ->
                    fos.write(logEntry.toByteArray())
                    fos.flush()
                }
            }
            
            // 同时输出到 Logcat
            when (level) {
                "D" -> Log.d(tag, message)
                "I" -> Log.i(tag, message)
                "W" -> Log.w(tag, message)
                "E" -> Log.e(tag, message)
                else -> Log.v(tag, message)
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to write log: ${e.message}")
        }
    }
    
    fun d(tag: String, message: String) = writeLog("D", tag, message)
    fun i(tag: String, message: String) = writeLog("I", tag, message)
    fun w(tag: String, message: String) = writeLog("W", tag, message)
    fun e(tag: String, message: String) = writeLog("E", tag, message)
    fun v(tag: String, message: String) = writeLog("V", tag, message)
    
    // 便捷方法，使用默认TAG
    fun d(message: String) = writeLog("D", "USBCam", message)
    fun i(message: String) = writeLog("I", "USBCam", message)
    fun w(message: String) = writeLog("W", "USBCam", message)
    fun e(message: String) = writeLog("E", "USBCam", message)
    fun v(message: String) = writeLog("V", "USBCam", message)
}
