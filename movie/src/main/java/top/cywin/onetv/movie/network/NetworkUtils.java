package top.cywin.onetv.movie.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 网络工具类
 * 基于FongMi_TV架构设计
 */
public class NetworkUtils {
    private static final String TAG = "NetworkUtils";

    /**
     * 检查网络是否可用
     */
    public static boolean isNetworkAvailable(Context context) {
        if (context == null) {
            return false;
        }

        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }

            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否为WiFi网络
     */
    public static boolean isWifiConnected(Context context) {
        if (context == null) {
            return false;
        }

        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }

            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 检查是否为移动网络
     */
    public static boolean isMobileConnected(Context context) {
        if (context == null) {
            return false;
        }

        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return false;
            }

            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_MOBILE && networkInfo.isConnected();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取网络类型名称
     */
    public static String getNetworkTypeName(Context context) {
        if (context == null) {
            return "Unknown";
        }

        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) {
                return "Unknown";
            }

            NetworkInfo networkInfo = cm.getActiveNetworkInfo();
            if (networkInfo == null) {
                return "No Connection";
            }

            return networkInfo.getTypeName();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    /**
     * 检查域名是否可达
     */
    public static boolean isHostReachable(String host) {
        return isHostReachable(host, 5000);
    }

    /**
     * 检查域名是否可达（指定超时时间）
     */
    public static boolean isHostReachable(String host, int timeout) {
        if (host == null || host.trim().isEmpty()) {
            return false;
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isReachable(timeout);
        } catch (UnknownHostException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 解析域名获取IP地址
     */
    public static String resolveHostToIp(String host) {
        if (host == null || host.trim().isEmpty()) {
            return null;
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            return address.getHostAddress();
        } catch (UnknownHostException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查IP地址格式是否有效
     */
    public static boolean isValidIpAddress(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }

        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * 检查端口号是否有效
     */
    public static boolean isValidPort(int port) {
        return port > 0 && port <= 65535;
    }

    /**
     * 检查URL格式是否有效
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        return url.startsWith("http://") || url.startsWith("https://");
    }
}
