package top.cywin.onetv.movie.utils

import android.Manifest
import android.content.Context
import androidx.annotation.RequiresPermission
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 网络工具类
 */
object NetworkUtils {
    
    /**
     * 检查网络连接状态
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
    
    /**
     * 处理网络异常
     */
    fun handleNetworkError(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> "网络连接失败，请检查网络设置"
            is SocketTimeoutException -> "网络请求超时，请稍后重试"
            is IOException -> "网络连接异常：${throwable.message}"
            is HttpException -> {
                when (throwable.code()) {
                    400 -> "请求参数错误"
                    401 -> "未授权访问"
                    403 -> "访问被禁止"
                    404 -> "请求的资源不存在"
                    500 -> "服务器内部错误"
                    502 -> "网关错误"
                    503 -> "服务暂时不可用"
                    else -> "网络请求失败：${throwable.code()}"
                }
            }
            else -> throwable.message ?: "未知网络错误"
        }
    }
    
    /**
     * 安全的网络请求执行
     */
    suspend fun <T> safeApiCall(
        apiCall: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        try {
            Result.success(apiCall())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 带重试的网络请求
     */
    suspend fun <T> retryApiCall(
        maxRetries: Int = 3,
        delayMs: Long = 1000,
        apiCall: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null
        
        repeat(maxRetries) { attempt ->
            try {
                return@withContext Result.success(apiCall())
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    kotlinx.coroutines.delay(delayMs * (attempt + 1))
                }
            }
        }
        
        Result.failure(lastException ?: Exception("重试失败"))
    }
}

/**
 * 网络状态枚举
 */
enum class NetworkState {
    AVAILABLE,
    UNAVAILABLE,
    LOSING,
    LOST
}

/**
 * API响应状态
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: Throwable) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
}

/**
 * 扩展函数：将Result转换为ApiResult
 */
fun <T> Result<T>.toApiResult(): ApiResult<T> {
    return if (isSuccess) {
        ApiResult.Success(getOrThrow())
    } else {
        ApiResult.Error(exceptionOrNull() ?: Exception("未知错误"))
    }
}

/**
 * 扩展函数：处理ApiResult
 */
inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) {
        action(data)
    }
    return this
}

inline fun <T> ApiResult<T>.onError(action: (Throwable) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) {
        action(exception)
    }
    return this
}

inline fun <T> ApiResult<T>.onLoading(action: () -> Unit): ApiResult<T> {
    if (this is ApiResult.Loading) {
        action()
    }
    return this
}
