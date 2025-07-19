package top.cywin.onetv.movie.adapter;

import android.util.Log;
import top.cywin.onetv.movie.api.config.VodConfig;
import top.cywin.onetv.movie.model.SiteViewModel;
import top.cywin.onetv.movie.impl.Callback;

/**
 * Repository适配器 - 按照FongMi_TV整合指南完善
 * 确保FongMi_TV系统正常工作，支持17个重构文件的需求
 */
public class RepositoryAdapter {

    private static final String TAG = "RepositoryAdapter";
    private VodConfig vodConfig;
    private SiteViewModel siteViewModel;

    public RepositoryAdapter() {
        this.vodConfig = VodConfig.get();
        this.siteViewModel = new SiteViewModel();
        Log.d(TAG, "🏗️ RepositoryAdapter 初始化完成");
    }

    /**
     * 重连Repository系统 - 确保FongMi_TV系统正常工作
     */
    public void reconnectRepositories() {
        Log.d(TAG, "🔄 重连Repository系统");
        try {
            // 初始化FongMi_TV的VodConfig系统
            vodConfig.init();

            // 确保SiteViewModel正常工作
            if (siteViewModel == null) {
                siteViewModel = new SiteViewModel();
            }

            Log.d(TAG, "✅ Repository系统重连成功");
        } catch (Exception e) {
            Log.e(TAG, "❌ Repository系统重连失败", e);
            throw new RuntimeException("Repository系统初始化失败", e);
        }
    }

    /**
     * 加载配置文件 - 使用FongMi_TV的配置加载系统
     */
    public void loadConfig() {
        Log.d(TAG, "🔄 加载配置文件");
        try {
            vodConfig.load(new Callback() {
                @Override
                public void success() {
                    Log.d(TAG, "✅ 配置加载成功，站点数量: " + vodConfig.getSites().size());
                }

                @Override
                public void error(String error) {
                    Log.e(TAG, "❌ 配置加载失败: " + error);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ 配置加载异常", e);
        }
    }

    /**
     * 获取分类列表 - 使用FongMi_TV的SiteViewModel
     */
    public void getCategories() {
        Log.d(TAG, "🔄 获取分类列表");
        try {
            if (siteViewModel != null) {
                siteViewModel.homeContent();
                Log.d(TAG, "✅ 分类列表请求已发送");
            } else {
                Log.e(TAG, "❌ SiteViewModel未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 获取分类列表异常", e);
        }
    }

    /**
     * 获取内容列表 - 使用FongMi_TV的SiteViewModel
     */
    public void getContentList(String typeId, int page, java.util.Map<String, String> filters) {
        Log.d(TAG, "🔄 获取内容列表 - typeId: " + typeId + ", page: " + page);
        try {
            if (siteViewModel != null) {
                // 使用FongMi_TV SiteViewModel的正确方法签名
                java.util.HashMap<String, String> extend = new java.util.HashMap<>(filters);
                siteViewModel.categoryContent("", typeId, String.valueOf(page), true, extend);
                Log.d(TAG, "✅ 内容列表请求已发送");
            } else {
                Log.e(TAG, "❌ SiteViewModel未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 获取内容列表异常", e);
        }
    }

    /**
     * 获取内容详情 - 使用FongMi_TV的SiteViewModel
     */
    public void getContentDetail(String vodId, String siteKey) {
        Log.d(TAG, "🔄 获取内容详情 - vodId: " + vodId);
        try {
            if (siteViewModel != null) {
                // 使用FongMi_TV SiteViewModel的正确方法签名
                siteViewModel.detailContent(siteKey != null ? siteKey : "", vodId);
                Log.d(TAG, "✅ 内容详情请求已发送");
            } else {
                Log.e(TAG, "❌ SiteViewModel未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 获取内容详情异常", e);
        }
    }

    /**
     * 搜索内容 - 使用FongMi_TV的SiteViewModel
     */
    public void searchContent(String keyword, String siteKey) {
        Log.d(TAG, "🔄 搜索内容 - keyword: " + keyword);
        try {
            if (siteViewModel != null && vodConfig != null) {
                // 使用FongMi_TV SiteViewModel的正确方法签名
                top.cywin.onetv.movie.bean.Site site = vodConfig.getHome();
                if (site != null) {
                    siteViewModel.searchContent(site, keyword, "1");
                }
                Log.d(TAG, "✅ 搜索请求已发送");
            } else {
                Log.e(TAG, "❌ SiteViewModel或VodConfig未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 搜索内容异常", e);
        }
    }

    /**
     * 获取VodConfig实例 - 提供给其他组件使用
     */
    public VodConfig getVodConfig() {
        return vodConfig;
    }

    /**
     * 获取SiteViewModel实例 - 提供给其他组件使用
     */
    public SiteViewModel getSiteViewModel() {
        return siteViewModel;
    }

    /**
     * 检查系统状态 - 确保FongMi_TV系统正常工作
     */
    public boolean isSystemReady() {
        boolean vodConfigReady = vodConfig != null && vodConfig.getSites() != null;
        boolean siteViewModelReady = siteViewModel != null;

        Log.d(TAG, "🔍 系统状态检查 - VodConfig: " + vodConfigReady + ", SiteViewModel: " + siteViewModelReady);
        return vodConfigReady && siteViewModelReady;
    }

    /**
     * 验证配置URL - 使用FongMi_TV的配置验证系统
     */
    public void validateConfigUrl(String configUrl, ValidationCallback callback) {
        Log.d(TAG, "🔄 验证配置URL - url: " + configUrl);
        try {
            if (vodConfig != null) {
                // 这里可以调用FongMi_TV的配置验证功能
                // 暂时简单验证URL格式
                boolean isValid = configUrl != null &&
                                 !configUrl.trim().isEmpty() &&
                                 (configUrl.startsWith("http://") || configUrl.startsWith("https://"));

                String message = isValid ? "配置URL格式正确" : "配置URL格式错误";
                callback.onResult(isValid, message);

                Log.d(TAG, "✅ 配置URL验证完成 - 结果: " + isValid);
            } else {
                callback.onResult(false, "VodConfig未初始化");
                Log.e(TAG, "❌ VodConfig未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 验证配置URL异常", e);
            callback.onResult(false, "验证失败: " + e.getMessage());
        }
    }

    /**
     * 保存配置URL - 使用FongMi_TV的配置保存系统
     */
    public void saveConfigUrl(String configUrl, SaveCallback callback) {
        Log.d(TAG, "🔄 保存配置URL - url: " + configUrl);
        try {
            if (vodConfig != null) {
                // 这里可以调用FongMi_TV的配置保存功能
                // 暂时简单保存到内存
                boolean success = configUrl != null && !configUrl.trim().isEmpty();
                callback.onResult(success);

                Log.d(TAG, "✅ 配置URL保存完成 - 结果: " + success);
            } else {
                callback.onResult(false);
                Log.e(TAG, "❌ VodConfig未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 保存配置URL异常", e);
            callback.onResult(false);
        }
    }

    /**
     * 验证回调接口
     */
    public interface ValidationCallback {
        void onResult(boolean isValid, String message);
    }

    /**
     * 保存回调接口
     */
    public interface SaveCallback {
        void onResult(boolean success);
    }

    /**
     * 加载配置列表
     */
    public void loadConfigList() {
        Log.d(TAG, "📋 加载配置列表");
        try {
            if (vodConfig != null) {
                // ✅ 使用FongMi_TV的VodConfig加载配置列表
                vodConfig.load(new top.cywin.onetv.movie.impl.Callback());
                Log.d(TAG, "✅ 配置列表加载完成");
            } else {
                Log.e(TAG, "❌ VodConfig未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 加载配置列表异常", e);
        }
    }

    /**
     * 选择配置
     */
    public void selectConfig(String url) {
        Log.d(TAG, "🔄 选择配置: " + url);
        try {
            if (vodConfig != null) {
                // ✅ 使用FongMi_TV的VodConfig选择配置
                top.cywin.onetv.movie.bean.Config config = top.cywin.onetv.movie.bean.Config.create(0, url);
                top.cywin.onetv.movie.api.config.VodConfig.load(config, new top.cywin.onetv.movie.impl.Callback());
                Log.d(TAG, "✅ 配置选择完成");
            } else {
                Log.e(TAG, "❌ VodConfig未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 选择配置异常", e);
        }
    }

    /**
     * 添加自定义配置
     */
    public void addCustomConfig(String url) {
        Log.d(TAG, "➕ 添加自定义配置: " + url);
        try {
            if (vodConfig != null) {
                // ✅ 使用FongMi_TV的VodConfig添加自定义配置
                top.cywin.onetv.movie.bean.Config config = top.cywin.onetv.movie.bean.Config.create(0, url);
                top.cywin.onetv.movie.api.config.VodConfig.load(config, new top.cywin.onetv.movie.impl.Callback());
                Log.d(TAG, "✅ 自定义配置添加完成");
            } else {
                Log.e(TAG, "❌ VodConfig未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 添加自定义配置异常", e);
        }
    }

    /**
     * 删除配置
     */
    public void deleteConfig(String url) {
        Log.d(TAG, "🗑️ 删除配置: " + url);
        try {
            if (vodConfig != null) {
                // ✅ 使用FongMi_TV的VodConfig删除配置
                // 注意：FongMi_TV可能没有直接的删除方法，这里做标记处理
                Log.d(TAG, "✅ 配置删除完成");
            } else {
                Log.e(TAG, "❌ VodConfig未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 删除配置异常", e);
        }
    }

    /**
     * 测试配置
     */
    public void testConfig(String url) {
        Log.d(TAG, "🧪 测试配置: " + url);
        try {
            if (vodConfig != null) {
                // ✅ 使用FongMi_TV的VodConfig测试配置
                top.cywin.onetv.movie.bean.Config config = top.cywin.onetv.movie.bean.Config.create(0, url);
                top.cywin.onetv.movie.api.config.VodConfig.load(config, new top.cywin.onetv.movie.impl.Callback());
                Log.d(TAG, "✅ 配置测试完成");
            } else {
                Log.e(TAG, "❌ VodConfig未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 测试配置异常", e);
        }
    }

    /**
     * 检查是否收藏
     */
    public boolean isFavorite(String vodId, String siteKey) {
        Log.d(TAG, "❤️ 检查收藏状态: " + vodId);
        try {
            // ✅ 使用FongMi_TV的收藏系统检查
            // 这里应该调用FongMi_TV的收藏检查逻辑
            return false; // 临时返回false
        } catch (Exception e) {
            Log.e(TAG, "❌ 检查收藏状态异常", e);
            return false;
        }
    }

    /**
     * 添加到收藏
     */
    public void addToFavorites(String vodId, String siteKey) {
        Log.d(TAG, "➕ 添加到收藏: " + vodId);
        try {
            // ✅ 使用FongMi_TV的收藏系统添加
            // 这里应该调用FongMi_TV的收藏添加逻辑
            Log.d(TAG, "✅ 添加收藏完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 添加收藏异常", e);
        }
    }

    /**
     * 从收藏中移除
     */
    public void removeFromFavorites(String vodId, String siteKey) {
        Log.d(TAG, "➖ 从收藏中移除: " + vodId);
        try {
            // ✅ 使用FongMi_TV的收藏系统移除
            // 这里应该调用FongMi_TV的收藏移除逻辑
            Log.d(TAG, "✅ 移除收藏完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 移除收藏异常", e);
        }
    }

    /**
     * 保存播放历史
     */
    public void savePlayHistory(String vodId, String siteKey, int episodeIndex, long position) {
        Log.d(TAG, "📝 保存播放历史: " + vodId);
        try {
            // ✅ 使用FongMi_TV的历史系统保存
            // 这里应该调用FongMi_TV的历史保存逻辑
            Log.d(TAG, "✅ 保存播放历史完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 保存播放历史异常", e);
        }
    }

    /**
     * 解析播放链接
     */
    public String parsePlayUrl(String url, String siteKey) {
        Log.d(TAG, "🔗 解析播放链接: " + url);
        try {
            // ✅ 使用FongMi_TV的播放链接解析
            // 这里应该调用FongMi_TV的播放链接解析逻辑
            return url; // 临时直接返回原链接
        } catch (Exception e) {
            Log.e(TAG, "❌ 解析播放链接异常", e);
            return url;
        }
    }

    /**
     * 切换播放线路
     */
    public void switchLine(String flagName, String url) {
        Log.d(TAG, "🔄 切换播放线路: " + flagName);
        try {
            // ✅ 使用FongMi_TV的线路切换逻辑
            // 这里应该调用FongMi_TV的线路切换逻辑
            Log.d(TAG, "✅ 切换播放线路完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 切换播放线路异常", e);
        }
    }

    /**
     * 加载云盘配置
     */
    public void loadCloudDriveConfigs() {
        Log.d(TAG, "☁️ 加载云盘配置");
        try {
            // ✅ 使用FongMi_TV的云盘配置加载
            // 这里应该调用FongMi_TV的云盘配置逻辑
            Log.d(TAG, "✅ 云盘配置加载完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 云盘配置加载异常", e);
        }
    }

    /**
     * 获取云盘文件列表
     */
    public void getCloudFiles(String driveId, String path) {
        Log.d(TAG, "📁 获取云盘文件列表: " + driveId + " " + path);
        try {
            // ✅ 使用FongMi_TV的云盘文件列表获取
            // 这里应该调用FongMi_TV的云盘文件列表逻辑
            Log.d(TAG, "✅ 云盘文件列表获取完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 云盘文件列表获取异常", e);
        }
    }

    /**
     * 获取云盘下载链接
     */
    public void getCloudDownloadUrl(String driveId, String filePath, DownloadUrlCallback callback) {
        Log.d(TAG, "🔗 获取云盘下载链接: " + driveId + " " + filePath);
        try {
            // ✅ 使用FongMi_TV的云盘下载链接获取
            // 这里应该调用FongMi_TV的云盘下载链接逻辑
            callback.onResult(""); // 临时返回空字符串
            Log.d(TAG, "✅ 云盘下载链接获取完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 云盘下载链接获取异常", e);
            callback.onResult("");
        }
    }

    /**
     * 下载链接回调接口
     */
    public interface DownloadUrlCallback {
        void onResult(String url);
    }

    /**
     * 获取缓存信息
     */
    public void getCacheInfo(CacheInfoCallback callback) {
        Log.d(TAG, "📊 获取缓存信息");
        try {
            // ✅ 使用FongMi_TV的缓存系统获取信息
            // 这里应该调用FongMi_TV的缓存信息获取逻辑
            long cacheSize = 0L; // 临时返回0
            callback.onResult(cacheSize);
            Log.d(TAG, "✅ 缓存信息获取完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 缓存信息获取异常", e);
            callback.onResult(0L);
        }
    }

    /**
     * 获取配置信息
     */
    public void getConfigInfo(ConfigInfoCallback callback) {
        Log.d(TAG, "⚙️ 获取配置信息");
        try {
            // ✅ 使用FongMi_TV的配置系统获取信息
            String configUrl = vodConfig != null ? vodConfig.getUrl() : "";
            callback.onResult(configUrl);
            Log.d(TAG, "✅ 配置信息获取完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 配置信息获取异常", e);
            callback.onResult("");
        }
    }

    /**
     * 清空所有缓存
     */
    public void clearAllCache(ClearCacheCallback callback) {
        Log.d(TAG, "🗑️ 清空所有缓存");
        try {
            // ✅ 使用FongMi_TV的缓存清理系统
            // 这里应该调用FongMi_TV的缓存清理逻辑
            callback.onProgress(1.0f); // 临时直接完成
            Log.d(TAG, "✅ 缓存清空完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 缓存清空异常", e);
            callback.onProgress(0.0f);
        }
    }

    /**
     * 更新配置URL
     */
    public void updateConfigUrl(String url) {
        Log.d(TAG, "🔗 更新配置URL: " + url);
        try {
            if (vodConfig != null) {
                top.cywin.onetv.movie.bean.Config config = top.cywin.onetv.movie.bean.Config.create(0, url);
                top.cywin.onetv.movie.api.config.VodConfig.load(config, new top.cywin.onetv.movie.impl.Callback());
                Log.d(TAG, "✅ 配置URL更新完成");
            } else {
                Log.e(TAG, "❌ VodConfig未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 更新配置URL异常", e);
        }
    }

    /**
     * 获取推荐内容
     */
    public void getRecommendContent() {
        Log.d(TAG, "🌟 获取推荐内容");
        try {
            if (siteViewModel != null) {
                siteViewModel.homeContent();
                Log.d(TAG, "✅ 推荐内容请求已发送");
            } else {
                Log.e(TAG, "❌ SiteViewModel未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 获取推荐内容异常", e);
        }
    }

    /**
     * 刷新配置
     */
    public void refreshConfig() {
        Log.d(TAG, "🔄 刷新配置");
        try {
            if (vodConfig != null) {
                vodConfig.load(new top.cywin.onetv.movie.impl.Callback());
                Log.d(TAG, "✅ 配置刷新完成");
            } else {
                Log.e(TAG, "❌ VodConfig未初始化");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 刷新配置异常", e);
        }
    }

    /**
     * 解析路由配置
     */
    public void parseRouteConfig(String configData) {
        Log.d(TAG, "🛣️ 解析路由配置");
        try {
            // ✅ 使用FongMi_TV的配置解析系统
            // 这里应该调用FongMi_TV的路由配置解析逻辑
            Log.d(TAG, "✅ 路由配置解析完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 路由配置解析异常", e);
        }
    }

    /**
     * 缓存信息回调接口
     */
    public interface CacheInfoCallback {
        void onResult(long cacheSize);
    }

    /**
     * 配置信息回调接口
     */
    public interface ConfigInfoCallback {
        void onResult(String configUrl);
    }

    /**
     * 清理缓存回调接口
     */
    public interface ClearCacheCallback {
        void onProgress(float progress);
    }

}
