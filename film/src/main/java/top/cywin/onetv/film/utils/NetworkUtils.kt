package top.cywin.onetv.film.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.telephony.TelephonyManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

/**
 * 网络工具类
 * 
 * 基于 FongMi/TV 的网络检测和管理工具
 * 提供网络状态检测、连通性测试等功能
 * 
 * @author OneTV Team
 * @since 2025-07-13
 */
object NetworkUtils {
    
    private const val TAG = "ONETV_NETWORK_UTILS"
    private const val CONNECTIVITY_TEST_TIMEOUT = 5000 // 5秒
    private const val PING_TIMEOUT = 3000 // 3秒
    
    /**
     * 网络信息数据类
     */
    data class NetworkInfo(
        val isConnected: Boolean,
        val type: String,
        val subtype: String,
        val operator: String,
        val isWifi: Boolean,
        val isMobile: Boolean,
        val isEthernet: Boolean,
        val signalStrength: Int
    )
    
    /**
     * 🌐 检查网络是否可用
     */
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.isConnected == true
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查网络可用性失败", e)
            false
        }
    }
    
    /**
     * 🌐 获取网络信息
     */
    fun getNetworkInfo(context: Context): NetworkInfo {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                
                if (capabilities != null) {
                    val isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    val isMobile = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                    val isEthernet = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                    
                    NetworkInfo(
                        isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                        type = when {
                            isWifi -> "WIFI"
                            isMobile -> "MOBILE"
                            isEthernet -> "ETHERNET"
                            else -> "UNKNOWN"
                        },
                        subtype = getNetworkSubtype(telephonyManager),
                        operator = telephonyManager.networkOperatorName ?: "",
                        isWifi = isWifi,
                        isMobile = isMobile,
                        isEthernet = isEthernet,
                        signalStrength = getSignalStrength(context)
                    )
                } else {
                    createDisconnectedNetworkInfo()
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                if (networkInfo != null) {
                    NetworkInfo(
                        isConnected = networkInfo.isConnected,
                        type = networkInfo.typeName ?: "UNKNOWN",
                        subtype = networkInfo.subtypeName ?: "",
                        operator = telephonyManager.networkOperatorName ?: "",
                        isWifi = networkInfo.type == ConnectivityManager.TYPE_WIFI,
                        isMobile = networkInfo.type == ConnectivityManager.TYPE_MOBILE,
                        isEthernet = networkInfo.type == ConnectivityManager.TYPE_ETHERNET,
                        signalStrength = getSignalStrength(context)
                    )
                } else {
                    createDisconnectedNetworkInfo()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取网络信息失败", e)
            createDisconnectedNetworkInfo()
        }
    }
    
    /**
     * 🌐 检查 URL 是否可直接访问
     */
    suspend fun isDirectlyAccessible(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = CONNECTIVITY_TEST_TIMEOUT
                connection.readTimeout = CONNECTIVITY_TEST_TIMEOUT
                connection.instanceFollowRedirects = false
                
                val responseCode = connection.responseCode
                connection.disconnect()
                
                responseCode in 200..399
            } catch (e: Exception) {
                Log.d(TAG, "URL 直接访问失败: $url", e)
                false
            }
        }
    }
    
    /**
     * 🌐 测试主机连通性
     */
    suspend fun isHostReachable(host: String, port: Int, timeout: Int = PING_TIMEOUT): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), timeout)
                socket.close()
                true
            } catch (e: Exception) {
                Log.d(TAG, "主机连通性测试失败: $host:$port", e)
                false
            }
        }
    }
    
    /**
     * 🌐 测试网络延迟
     */
    suspend fun measureNetworkLatency(host: String, port: Int = 80): Long {
        return withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val socket = Socket()
                socket.connect(InetSocketAddress(host, port), PING_TIMEOUT)
                val latency = System.currentTimeMillis() - startTime
                socket.close()
                latency
            } catch (e: Exception) {
                Log.d(TAG, "网络延迟测试失败: $host", e)
                -1L
            }
        }
    }
    
    /**
     * 🌐 检查是否为计费网络
     */
    fun isMeteredNetwork(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                connectivityManager.isActiveNetworkMetered
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.type == ConnectivityManager.TYPE_MOBILE
            }
        } catch (e: Exception) {
            Log.e(TAG, "检查计费网络失败", e)
            false
        }
    }
    
    /**
     * 🌐 获取网络类型
     */
    fun getNetworkType(context: Context): String {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)
                
                when {
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI"
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "MOBILE"
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true -> "ETHERNET"
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true -> "VPN"
                    else -> "UNKNOWN"
                }
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.typeName ?: "UNKNOWN"
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取网络类型失败", e)
            "UNKNOWN"
        }
    }
    
    /**
     * 🌐 获取网络子类型
     */
    private fun getNetworkSubtype(telephonyManager: TelephonyManager): String {
        return try {
            when (telephonyManager.networkType) {
                TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
                TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
                TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
                TelephonyManager.NETWORK_TYPE_EHRPD -> "eHRPD"
                TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
                TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
                TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
                TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
                TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
                TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
                TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPA+"
                TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
                TelephonyManager.NETWORK_TYPE_IDEN -> "iDen"
                TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
                TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
                TelephonyManager.NETWORK_TYPE_UNKNOWN -> "UNKNOWN"
                else -> "UNKNOWN"
            }
        } catch (e: Exception) {
            "UNKNOWN"
        }
    }
    
    /**
     * 🌐 获取信号强度
     */
    private fun getSignalStrength(context: Context): Int {
        return try {
            // 这里需要更复杂的实现来获取实际信号强度
            // 简化实现，返回默认值
            -1
        } catch (e: Exception) {
            -1
        }
    }
    
    /**
     * 🌐 创建断开连接的网络信息
     */
    private fun createDisconnectedNetworkInfo(): NetworkInfo {
        return NetworkInfo(
            isConnected = false,
            type = "NONE",
            subtype = "",
            operator = "",
            isWifi = false,
            isMobile = false,
            isEthernet = false,
            signalStrength = -1
        )
    }
    
    /**
     * 🌐 格式化网络速度
     */
    fun formatNetworkSpeed(bytesPerSecond: Long): String {
        return when {
            bytesPerSecond >= 1024 * 1024 -> String.format("%.1f MB/s", bytesPerSecond / (1024.0 * 1024.0))
            bytesPerSecond >= 1024 -> String.format("%.1f KB/s", bytesPerSecond / 1024.0)
            else -> "$bytesPerSecond B/s"
        }
    }
    
    /**
     * 🌐 格式化数据大小
     */
    fun formatDataSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
