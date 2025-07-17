package top.cywin.onetv.movie.network.interceptor;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * HTTP头部拦截器
 * 基于FongMi_TV架构设计，自动添加必要的HTTP头部
 */
public class HeaderInterceptor implements Interceptor {
    private static final String TAG = "HeaderInterceptor";
    
    private static final String DEFAULT_USER_AGENT = 
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        Request.Builder requestBuilder = originalRequest.newBuilder();

        // 添加User-Agent（如果没有设置）
        if (originalRequest.header("User-Agent") == null) {
            requestBuilder.addHeader("User-Agent", DEFAULT_USER_AGENT);
        }

        // 添加Accept头部（如果没有设置）
        if (originalRequest.header("Accept") == null) {
            requestBuilder.addHeader("Accept", "*/*");
        }

        // 添加Accept-Language头部（如果没有设置）
        if (originalRequest.header("Accept-Language") == null) {
            requestBuilder.addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        }

        // 添加Accept-Encoding头部（如果没有设置）
        if (originalRequest.header("Accept-Encoding") == null) {
            requestBuilder.addHeader("Accept-Encoding", "gzip, deflate, br");
        }

        // 添加Connection头部（如果没有设置）
        if (originalRequest.header("Connection") == null) {
            requestBuilder.addHeader("Connection", "keep-alive");
        }

        // 添加Cache-Control头部（如果没有设置）
        if (originalRequest.header("Cache-Control") == null) {
            requestBuilder.addHeader("Cache-Control", "no-cache");
        }

        Request newRequest = requestBuilder.build();
        return chain.proceed(newRequest);
    }

    /**
     * 获取默认User-Agent
     */
    public static String getDefaultUserAgent() {
        return DEFAULT_USER_AGENT;
    }
}
