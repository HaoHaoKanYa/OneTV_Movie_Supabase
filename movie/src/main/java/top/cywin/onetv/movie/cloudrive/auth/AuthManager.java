package top.cywin.onetv.movie.cloudrive.auth;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证管理器
 * 基于FongMi_TV架构设计
 */
public class AuthManager {
    private static final String TAG = "AuthManager";
    
    private final Map<String, String> tokens;
    private final Map<String, String> refreshTokens;
    
    public AuthManager() {
        tokens = new HashMap<>();
        refreshTokens = new HashMap<>();
    }
    
    /**
     * 保存访问令牌
     */
    public void saveToken(String providerType, String token) {
        tokens.put(providerType, token);
    }
    
    /**
     * 获取访问令牌
     */
    public String getToken(String providerType) {
        return tokens.get(providerType);
    }
    
    /**
     * 保存刷新令牌
     */
    public void saveRefreshToken(String providerType, String refreshToken) {
        refreshTokens.put(providerType, refreshToken);
    }
    
    /**
     * 获取刷新令牌
     */
    public String getRefreshToken(String providerType) {
        return refreshTokens.get(providerType);
    }
    
    /**
     * 移除令牌
     */
    public void removeToken(String providerType) {
        tokens.remove(providerType);
        refreshTokens.remove(providerType);
    }
    
    /**
     * 检查是否有有效令牌
     */
    public boolean hasValidToken(String providerType) {
        return tokens.containsKey(providerType) && tokens.get(providerType) != null;
    }
    
    /**
     * 清理所有令牌
     */
    public void clearAll() {
        tokens.clear();
        refreshTokens.clear();
    }
    
    /**
     * 获取所有已认证的提供商
     */
    public String[] getAuthenticatedProviders() {
        return tokens.keySet().toArray(new String[0]);
    }
}
