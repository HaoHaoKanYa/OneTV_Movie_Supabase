package top.cywin.onetv.film.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.*

/**
 * 设备工具类
 * 
 * 基于 FongMi/TV 的设备信息获取工具
 * 提供设备信息、应用信息等功能
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
object DeviceUtils {
    
    private const val TAG = "ONETV_DEVICE_UTILS"
    
    /**
     * 设备信息数据类
     */
    data class DeviceInfo(
        val deviceId: String,
        val model: String,
        val brand: String,
        val manufacturer: String,
        val product: String,
        val device: String,
        val board: String,
        val hardware: String,
        val androidVersion: String,
        val apiLevel: Int,
        val buildId: String,
        val fingerprint: String,
        val isEmulator: Boolean,
        val isTablet: Boolean,
        val isTv: Boolean,
        val screenDensity: String,
        val screenResolution: String
    )
    
    /**
     * 应用信息数据类
     */
    data class AppInfo(
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val targetSdkVersion: Int,
        val minSdkVersion: Int,
        val installTime: Long,
        val updateTime: Long,
        val isSystemApp: Boolean,
        val isDebuggable: Boolean
    )
    
    /**
     * 🔧 获取设备信息
     */
    fun getDeviceInfo(context: Context): DeviceInfo {
        return try {
            val displayMetrics = context.resources.displayMetrics
            
            DeviceInfo(
                deviceId = getDeviceId(context),
                model = Build.MODEL,
                brand = Build.BRAND,
                manufacturer = Build.MANUFACTURER,
                product = Build.PRODUCT,
                device = Build.DEVICE,
                board = Build.BOARD,
                hardware = Build.HARDWARE,
                androidVersion = Build.VERSION.RELEASE,
                apiLevel = Build.VERSION.SDK_INT,
                buildId = Build.ID,
                fingerprint = Build.FINGERPRINT,
                isEmulator = isEmulator(),
                isTablet = isTablet(context),
                isTv = isTv(context),
                screenDensity = getDensityString(displayMetrics.densityDpi),
                screenResolution = "${displayMetrics.widthPixels}x${displayMetrics.heightPixels}"
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取设备信息失败", e)
            createDefaultDeviceInfo()
        }
    }
    
    /**
     * 🔧 获取应用信息
     */
    fun getAppInfo(context: Context): AppInfo {
        return try {
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageInfo(context.packageName, 0)
            val applicationInfo = packageInfo.applicationInfo
            
            AppInfo(
                packageName = packageInfo.packageName,
                versionName = packageInfo.versionName ?: "unknown",
                versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                },
                targetSdkVersion = applicationInfo.targetSdkVersion,
                minSdkVersion = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    applicationInfo.minSdkVersion
                } else {
                    1
                },
                installTime = packageInfo.firstInstallTime,
                updateTime = packageInfo.lastUpdateTime,
                isSystemApp = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0,
                isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取应用信息失败", e)
            createDefaultAppInfo(context)
        }
    }
    
    /**
     * 🔧 获取设备 ID
     */
    private fun getDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (e: Exception) {
            Log.e(TAG, "获取设备 ID 失败", e)
            "unknown"
        }
    }
    
    /**
     * 🔧 检查是否为模拟器
     */
    private fun isEmulator(): Boolean {
        return try {
            (Build.FINGERPRINT.startsWith("generic") ||
             Build.FINGERPRINT.startsWith("unknown") ||
             Build.MODEL.contains("google_sdk") ||
             Build.MODEL.contains("Emulator") ||
             Build.MODEL.contains("Android SDK built for x86") ||
             Build.MANUFACTURER.contains("Genymotion") ||
             Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic") ||
             "google_sdk" == Build.PRODUCT)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 🔧 检查是否为平板
     */
    private fun isTablet(context: Context): Boolean {
        return try {
            val configuration = context.resources.configuration
            val screenLayout = configuration.screenLayout and android.content.res.Configuration.SCREENLAYOUT_SIZE_MASK
            
            screenLayout == android.content.res.Configuration.SCREENLAYOUT_SIZE_LARGE ||
            screenLayout == android.content.res.Configuration.SCREENLAYOUT_SIZE_XLARGE
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 🔧 检查是否为 TV
     */
    private fun isTv(context: Context): Boolean {
        return try {
            val packageManager = context.packageManager
            packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 🔧 获取屏幕密度字符串
     */
    private fun getDensityString(densityDpi: Int): String {
        return when {
            densityDpi <= 120 -> "ldpi"
            densityDpi <= 160 -> "mdpi"
            densityDpi <= 240 -> "hdpi"
            densityDpi <= 320 -> "xhdpi"
            densityDpi <= 480 -> "xxhdpi"
            densityDpi <= 640 -> "xxxhdpi"
            else -> "unknown"
        }
    }
    
    /**
     * 🔧 获取 CPU 架构
     */
    fun getCpuArchitecture(): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"
            } else {
                @Suppress("DEPRECATION")
                Build.CPU_ABI
            }
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * 🔧 获取内存信息
     */
    fun getMemoryInfo(context: Context): Map<String, Long> {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            mapOf(
                "total_memory" to memoryInfo.totalMem,
                "available_memory" to memoryInfo.availMem,
                "used_memory" to (memoryInfo.totalMem - memoryInfo.availMem),
                "threshold" to memoryInfo.threshold,
                "low_memory" to if (memoryInfo.lowMemory) 1L else 0L
            )
        } catch (e: Exception) {
            Log.e(TAG, "获取内存信息失败", e)
            emptyMap()
        }
    }
    
    /**
     * 🔧 获取存储信息
     */
    fun getStorageInfo(context: Context): Map<String, Long> {
        return try {
            val internalDir = context.filesDir
            val externalDir = context.getExternalFilesDir(null)
            
            val internalTotal = internalDir.totalSpace
            val internalFree = internalDir.freeSpace
            val internalUsed = internalTotal - internalFree
            
            val result = mutableMapOf(
                "internal_total" to internalTotal,
                "internal_free" to internalFree,
                "internal_used" to internalUsed
            )
            
            if (externalDir != null) {
                val externalTotal = externalDir.totalSpace
                val externalFree = externalDir.freeSpace
                val externalUsed = externalTotal - externalFree
                
                result["external_total"] = externalTotal
                result["external_free"] = externalFree
                result["external_used"] = externalUsed
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "获取存储信息失败", e)
            emptyMap()
        }
    }
    
    /**
     * 🔧 获取系统属性
     */
    fun getSystemProperty(key: String, defaultValue: String = ""): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            val result = process.inputStream.bufferedReader().readText().trim()
            if (result.isNotEmpty()) result else defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }
    
    /**
     * 🔧 创建默认设备信息
     */
    private fun createDefaultDeviceInfo(): DeviceInfo {
        return DeviceInfo(
            deviceId = "unknown",
            model = "unknown",
            brand = "unknown",
            manufacturer = "unknown",
            product = "unknown",
            device = "unknown",
            board = "unknown",
            hardware = "unknown",
            androidVersion = "unknown",
            apiLevel = 0,
            buildId = "unknown",
            fingerprint = "unknown",
            isEmulator = false,
            isTablet = false,
            isTv = false,
            screenDensity = "unknown",
            screenResolution = "unknown"
        )
    }
    
    /**
     * 🔧 创建默认应用信息
     */
    private fun createDefaultAppInfo(context: Context): AppInfo {
        return AppInfo(
            packageName = context.packageName,
            versionName = "unknown",
            versionCode = 0L,
            targetSdkVersion = 0,
            minSdkVersion = 0,
            installTime = 0L,
            updateTime = 0L,
            isSystemApp = false,
            isDebuggable = false
        )
    }
}
