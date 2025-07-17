package top.cywin.onetv.movie.cloudrive.manager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import top.cywin.onetv.movie.cloudrive.auth.AuthManager;
import top.cywin.onetv.movie.cloudrive.bean.CloudFile;
import top.cywin.onetv.movie.cloudrive.provider.CloudProvider;
import top.cywin.onetv.movie.cloudrive.provider.AlistProvider;
import top.cywin.onetv.movie.cloudrive.provider.BaiduProvider;
import top.cywin.onetv.movie.cloudrive.provider.AliProvider;
import top.cywin.onetv.movie.cloudrive.provider.OneDriveProvider;
import top.cywin.onetv.movie.cloudrive.provider.GoogleDriveProvider;
import top.cywin.onetv.movie.cloudrive.provider.DropboxProvider;
import top.cywin.onetv.movie.cloudrive.provider.WebDavProvider;
import top.cywin.onetv.movie.cloudrive.provider.FtpProvider;

/**
 * 网盘解析管理器
 * 基于FongMi_TV架构设计，支持多种网盘服务
 */
public class CloudDriveManager {
    private final Map<String, CloudProvider> providers;
    private final AuthManager authManager;

    public CloudDriveManager() {
        providers = new HashMap<>();
        authManager = new AuthManager();
        initProviders();
    }

    private void initProviders() {
        // 百度网盘
        providers.put("baidu", new BaiduProvider());
        // 阿里云盘
        providers.put("ali", new AliProvider());
        // OneDrive
        providers.put("onedrive", new OneDriveProvider());
        // Google Drive
        providers.put("googledrive", new GoogleDriveProvider());
        // Dropbox
        providers.put("dropbox", new DropboxProvider());
        // Alist
        providers.put("alist", new AlistProvider());
        // WebDAV
        providers.put("webdav", new WebDavProvider());
        // FTP
        providers.put("ftp", new FtpProvider());
    }

    /**
     * 列出指定路径下的文件
     */
    public List<CloudFile> listFiles(String providerType, String path, String token) throws Exception {
        CloudProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported provider: " + providerType);
        }
        return provider.listFiles(path, token);
    }

    /**
     * 获取文件下载链接
     */
    public String getDownloadUrl(String providerType, String fileId, String token) throws Exception {
        CloudProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported provider: " + providerType);
        }
        return provider.getDownloadUrl(fileId, token);
    }

    /**
     * 搜索文件
     */
    public List<CloudFile> searchFiles(String providerType, String keyword, String token) throws Exception {
        CloudProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported provider: " + providerType);
        }
        return provider.searchFiles(keyword, token);
    }

    /**
     * 获取文件信息
     */
    public CloudFile getFileInfo(String providerType, String fileId, String token) throws Exception {
        CloudProvider provider = providers.get(providerType);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported provider: " + providerType);
        }
        return provider.getFileInfo(fileId, token);
    }

    /**
     * 检查提供商是否支持
     */
    public boolean isProviderSupported(String providerType) {
        return providers.containsKey(providerType);
    }

    /**
     * 获取所有支持的提供商
     */
    public String[] getSupportedProviders() {
        return providers.keySet().toArray(new String[0]);
    }

    /**
     * 获取认证管理器
     */
    public AuthManager getAuthManager() {
        return authManager;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        for (CloudProvider provider : providers.values()) {
            try {
                provider.cleanup();
            } catch (Exception e) {
                // 忽略清理错误
            }
        }
        providers.clear();
    }
}
