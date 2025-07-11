package top.cywin.onetv.movie.data.network

import android.annotation.SuppressLint
import android.util.Log
import okhttp3.OkHttpClient
// import okhttp3.dnsoverhttps.DnsOverHttps // 暂时注释，避免依赖问题
import java.net.InetAddress
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.Dns
import okhttp3.HttpUrl

/**
 * TVBOX网络配置 - 参考OneMoVie项目
 * 
 * 解决网络连接问题：
 * 1. DNS over HTTPS (DoH) 支持
 * 2. SSL证书信任配置
 * 3. 主机名验证跳过
 * 4. 增强的超时配置
 */
class TvboxNetworkConfig {

    companion object {
        private const val TIMEOUT = 15000L // 15秒超时，用户偏好设置
        
        /**
         * 创建TVBOX标准的HTTP客户端
         */
        fun createTvboxHttpClient(): OkHttpClient {
            Log.d("ONETV_MOVIE", "🌐 创建TVBOX标准HTTP客户端")
            
            return OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true) // 启用重试
                .dns(createTvboxDns()) // 使用TVBOX DNS配置
                .hostnameVerifier { _, _ -> true } // 跳过主机名验证
                .sslSocketFactory(getSSLContext().socketFactory, trustAllCertificates()) // 信任所有证书
                .build()
        }
        
        /**
         * 创建TVBOX DNS配置 - 暂时使用系统DNS
         */
        private fun createTvboxDns(): Dns {
            Log.d("ONETV_MOVIE", "🔧 配置TVBOX DNS (使用系统DNS)")

            // 暂时使用系统DNS，避免DoH依赖问题
            // TODO: 后续添加DoH支持
            return Dns.SYSTEM
        }
        
        /**
         * 获取SSL上下文 - 信任所有证书
         */
        private fun getSSLContext(): SSLContext {
            return try {
                val context = SSLContext.getInstance("TLS")
                context.init(null, arrayOf<TrustManager>(trustAllCertificates()), SecureRandom())
                context
            } catch (e: Exception) {
                Log.e("ONETV_MOVIE", "💥 SSL上下文创建失败", e)
                SSLContext.getDefault()
            }
        }
        
        /**
         * 信任所有证书的TrustManager
         */
        @SuppressLint("TrustAllX509TrustManager", "CustomX509TrustManager")
        private fun trustAllCertificates(): X509TrustManager {
            return object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {
                    // 信任所有客户端证书
                }
                
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {
                    // 信任所有服务器证书
                }
                
                override fun getAcceptedIssuers(): Array<X509Certificate> {
                    return arrayOf()
                }
            }
        }
        
        /**
         * 创建带有自定义超时的客户端
         */
        fun createTvboxHttpClient(timeoutSeconds: Long): OkHttpClient {
            val timeoutMs = TimeUnit.SECONDS.toMillis(timeoutSeconds)
            
            return createTvboxHttpClient().newBuilder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build()
        }
        
        /**
         * 创建不跟随重定向的客户端
         */
        fun createNoRedirectClient(timeoutSeconds: Long): OkHttpClient {
            return createTvboxHttpClient(timeoutSeconds).newBuilder()
                .followRedirects(false)
                .followSslRedirects(false)
                .build()
        }
    }
}
