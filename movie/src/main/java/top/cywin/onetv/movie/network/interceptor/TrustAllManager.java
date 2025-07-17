package top.cywin.onetv.movie.network.interceptor;

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * 信任所有证书的TrustManager
 * 基于FongMi_TV架构设计，用于处理SSL证书验证
 */
public class TrustAllManager implements X509TrustManager {
    private static final String TAG = "TrustAllManager";

    @Override
    public void checkClientTrusted(X509Certificate[] chain, String authType) {
        // 信任所有客户端证书
    }

    @Override
    public void checkServerTrusted(X509Certificate[] chain, String authType) {
        // 信任所有服务器证书
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
