package top.cywin.onetv.movie.network;

import android.text.TextUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import top.cywin.onetv.movie.network.config.ProxyConfig;
import top.cywin.onetv.movie.network.config.HostsConfig;
import top.cywin.onetv.movie.network.interceptor.HeaderInterceptor;
import top.cywin.onetv.movie.network.interceptor.ProxyInterceptor;
import top.cywin.onetv.movie.network.interceptor.TrustAllManager;

/**
 * 网络管理器
 * 基于FongMi_TV架构设计，支持代理和Hosts配置
 */
public class NetworkManager {
    private static final String TAG = "NetworkManager";
    private OkHttpClient client;
    private ProxyConfig proxyConfig;
    private HostsConfig hostsConfig;

    public NetworkManager() {
        initClient();
    }

    private void initClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true);

        // SSL配置
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
            builder.sslSocketFactory(sslContext.getSocketFactory(), new TrustAllManager());
            builder.hostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 添加拦截器
        builder.addInterceptor(new HeaderInterceptor());
        builder.addInterceptor(new ProxyInterceptor());
        builder.addInterceptor(new HostsInterceptor());

        client = builder.build();
    }

    /**
     * 配置代理
     */
    public void configureProxy(ProxyConfig config) {
        this.proxyConfig = config;
        if (config != null && config.isEnabled()) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                new InetSocketAddress(config.getHost(), config.getPort()));

            OkHttpClient.Builder builder = client.newBuilder().proxy(proxy);

            if (!TextUtils.isEmpty(config.getUsername())) {
                Authenticator authenticator = (route, response) -> {
                    String credential = Credentials.basic(config.getUsername(), config.getPassword());
                    return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
                };
                builder.proxyAuthenticator(authenticator);
            }

            client = builder.build();
        }
    }

    /**
     * 配置Hosts重定向
     */
    public void configureHosts(HostsConfig config) {
        this.hostsConfig = config;
        // Hosts重定向逻辑在HostsInterceptor中实现
    }

    /**
     * 获取OkHttpClient实例
     */
    public OkHttpClient getClient() {
        return client;
    }

    /**
     * 获取代理配置
     */
    public ProxyConfig getProxyConfig() {
        return proxyConfig;
    }

    /**
     * 获取Hosts配置
     */
    public HostsConfig getHostsConfig() {
        return hostsConfig;
    }

    /**
     * Hosts拦截器
     */
    private class HostsInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();

            if (hostsConfig != null && hostsConfig.isEnabled()) {
                String host = request.url().host();
                String redirectHost = hostsConfig.getRedirectHost(host);

                if (!TextUtils.isEmpty(redirectHost) && !redirectHost.equals(host)) {
                    HttpUrl newUrl = request.url().newBuilder()
                        .host(redirectHost)
                        .build();
                    request = request.newBuilder()
                        .url(newUrl)
                        .addHeader("Host", host)
                        .build();
                }
            }

            return chain.proceed(request);
        }
    }

    /**
     * 重置网络配置
     */
    public void reset() {
        this.proxyConfig = null;
        this.hostsConfig = null;
        initClient();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
}
