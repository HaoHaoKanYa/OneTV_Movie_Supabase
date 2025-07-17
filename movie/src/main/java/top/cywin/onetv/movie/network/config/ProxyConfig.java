package top.cywin.onetv.movie.network.config;

/**
 * 代理配置类
 * 基于FongMi_TV架构设计
 */
public class ProxyConfig {
    private boolean enabled;
    private String host;
    private int port;
    private String username;
    private String password;
    private ProxyType type;

    public enum ProxyType {
        HTTP, HTTPS, SOCKS4, SOCKS5
    }

    public ProxyConfig() {
        this.enabled = false;
        this.type = ProxyType.HTTP;
        this.port = 8080;
    }

    public ProxyConfig(String host, int port) {
        this();
        this.host = host;
        this.port = port;
        this.enabled = true;
    }

    public ProxyConfig(String host, int port, String username, String password) {
        this(host, port);
        this.username = username;
        this.password = password;
    }

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ProxyType getType() {
        return type;
    }

    public void setType(ProxyType type) {
        this.type = type;
    }

    /**
     * 检查代理配置是否有效
     */
    public boolean isValid() {
        return enabled && host != null && !host.trim().isEmpty() && port > 0 && port <= 65535;
    }

    /**
     * 获取代理地址字符串
     */
    public String getProxyAddress() {
        if (!isValid()) {
            return null;
        }
        return host + ":" + port;
    }

    /**
     * 是否需要认证
     */
    public boolean needsAuthentication() {
        return username != null && !username.trim().isEmpty();
    }

    @Override
    public String toString() {
        return "ProxyConfig{" +
                "enabled=" + enabled +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", username='" + username + '\'' +
                ", type=" + type +
                '}';
    }

    /**
     * 从字符串解析代理配置
     * 格式：host:port 或 username:password@host:port
     */
    public static ProxyConfig fromString(String proxyString) {
        if (proxyString == null || proxyString.trim().isEmpty()) {
            return new ProxyConfig();
        }

        try {
            ProxyConfig config = new ProxyConfig();
            String[] parts;

            if (proxyString.contains("@")) {
                // 包含认证信息
                String[] authParts = proxyString.split("@");
                String[] userPass = authParts[0].split(":");
                config.setUsername(userPass[0]);
                if (userPass.length > 1) {
                    config.setPassword(userPass[1]);
                }
                parts = authParts[1].split(":");
            } else {
                // 不包含认证信息
                parts = proxyString.split(":");
            }

            if (parts.length >= 2) {
                config.setHost(parts[0]);
                config.setPort(Integer.parseInt(parts[1]));
                config.setEnabled(true);
            }

            return config;
        } catch (Exception e) {
            return new ProxyConfig();
        }
    }
}
