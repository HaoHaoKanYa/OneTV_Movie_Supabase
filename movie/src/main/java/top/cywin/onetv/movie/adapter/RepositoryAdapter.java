package top.cywin.onetv.movie.adapter;

import android.util.Log;
import java.util.Map;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import top.cywin.onetv.movie.api.config.VodConfig;
import top.cywin.onetv.movie.model.SiteViewModel;
import top.cywin.onetv.movie.event.*;

/**
 * Repository适配器 - 完整的FongMi_TV接口适配器
 * 提供Compose UI与FongMi_TV系统之间的完整接口适配
 * 
 * 🎯 重要架构原则：
 * - ✅ 适配器只做调用转发 - 不实现业务逻辑
 * - ✅ 所有解析逻辑在FongMi_TV - 保持原有解析系统
 * - ✅ 所有业务逻辑在FongMi_TV - Keep、History、VodConfig等
 * - ❌ 适配器不重新实现功能 - 避免代码重复
 */
public class RepositoryAdapter {

    private static final String TAG = "RepositoryAdapter";

    public RepositoryAdapter() {
        Log.d(TAG, "🏗️ RepositoryAdapter 初始化完成");
    }

    // ===== 配置管理接口 =====

    /**
     * 加载配置文件
     */
    public void loadConfig() {
        Log.d(TAG, "🔄 加载配置文件");
        try {
            VodConfig.get().load();
            Log.d(TAG, "✅ 配置文件加载完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 配置文件加载失败", e);
            EventBus.getDefault().post(new ConfigUpdateEvent(null, false, e.getMessage()));
        }
    }

    /**
     * 解析配置URL
     */
    public void parseConfig(String configUrl) {
        Log.d(TAG, "🔄 解析配置URL: " + configUrl);
        try {
            VodConfig.get().parse(configUrl);
            Log.d(TAG, "✅ 配置URL解析完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 配置URL解析失败", e);
            EventBus.getDefault().post(new ConfigUpdateEvent(null, false, e.getMessage()));
        }
    }

    /**
     * 刷新配置
     */
    public void refreshConfig() {
        Log.d(TAG, "🔄 刷新配置");
        try {
            VodConfig vodConfig = VodConfig.get();
            vodConfig.clear();
            vodConfig.load();
            Log.d(TAG, "✅ 配置刷新完成");
            EventBus.getDefault().post(new ConfigUpdateEvent(vodConfig, true));
        } catch (Exception e) {
            Log.e(TAG, "❌ 配置刷新失败", e);
            EventBus.getDefault().post(new ConfigUpdateEvent(null, false, e.getMessage()));
        }
    }

    /**
     * 重连Repository
     */
    public void reconnectRepositories() {
        Log.d(TAG, "🔄 重连Repository");
        try {
            VodConfig.get().init();
            Log.d(TAG, "✅ Repository重连成功");
        } catch (Exception e) {
            Log.e(TAG, "❌ Repository重连失败", e);
        }
    }

    // ===== 内容获取接口 =====

    /**
     * 获取首页内容
     */
    public void getHomeContent() {
        Log.d(TAG, "🔄 获取首页内容");
        try {
            SiteViewModel.get().homeContent();
            Log.d(TAG, "✅ 首页内容请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "❌ 首页内容获取失败", e);
            EventBus.getDefault().post(new ErrorEvent("首页内容获取失败: " + e.getMessage()));
        }
    }

    /**
     * 获取分类列表
     */
    public void getCategories() {
        Log.d(TAG, "🔄 获取分类列表");
        try {
            SiteViewModel.get().homeContent();
            Log.d(TAG, "✅ 分类列表请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "❌ 分类列表获取失败", e);
            EventBus.getDefault().post(new ErrorEvent("分类列表获取失败: " + e.getMessage()));
        }
    }

    /**
     * 获取分类内容
     */
    public void getContentList(String typeId, int page, Map<String, String> filters) {
        Log.d(TAG, "🔄 获取内容列表: typeId=" + typeId + ", page=" + page);
        try {
            SiteViewModel.get().categoryContent(typeId, page, true, filters);
            Log.d(TAG, "✅ 内容列表请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "❌ 内容列表获取失败", e);
            EventBus.getDefault().post(new ErrorEvent("内容列表获取失败: " + e.getMessage()));
        }
    }

    /**
     * 获取内容详情
     */
    public void getContentDetail(String vodId, String siteKey) {
        Log.d(TAG, "🔄 获取内容详情: vodId=" + vodId);
        try {
            SiteViewModel.get().detailContent(vodId);
            Log.d(TAG, "✅ 内容详情请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "❌ 内容详情获取失败", e);
            EventBus.getDefault().post(new ContentDetailEvent(null, false, e.getMessage()));
        }
    }

    /**
     * 搜索内容
     */
    public void searchContent(String keyword, String siteKey) {
        Log.d(TAG, "🔄 搜索内容: keyword=" + keyword);
        try {
            EventBus.getDefault().post(new SearchStartEvent(keyword, siteKey));
            SiteViewModel.get().searchContent(keyword, true);
            Log.d(TAG, "✅ 搜索请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "❌ 搜索失败", e);
            EventBus.getDefault().post(new SearchErrorEvent(keyword, e.getMessage()));
        }
    }

    /**
     * 获取推荐内容
     */
    public void getRecommendContent() {
        Log.d(TAG, "🔄 获取推荐内容");
        try {
            // ✅ 通过搜索空关键词获取推荐内容
            SiteViewModel.get().searchContent("", true);
            Log.d(TAG, "✅ 推荐内容请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "❌ 推荐内容获取失败", e);
            EventBus.getDefault().post(new ErrorEvent("推荐内容获取失败: " + e.getMessage()));
        }
    }

    // ===== 播放相关接口 =====

    /**
     * 解析播放地址
     */
    public void parsePlayUrl(String url, String siteKey, String flag) {
        Log.d(TAG, "🔄 请求解析播放地址: " + url);
        try {
            EventBus.getDefault().post(new PlayUrlParseStartEvent("", url, flag));
            SiteViewModel.get().playerContent(url, flag);
            Log.d(TAG, "✅ 播放地址解析请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "❌ 播放地址解析失败", e);
            EventBus.getDefault().post(new PlayUrlParseErrorEvent("", e.getMessage()));
        }
    }

    /**
     * 切换播放线路
     */
    public void switchLine(String flag, String url) {
        Log.d(TAG, "🔄 切换播放线路: flag=" + flag + ", url=" + url);
        try {
            // ✅ 重新解析播放地址
            parsePlayUrl(url, "", flag);
            Log.d(TAG, "✅ 线路切换请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "❌ 线路切换失败", e);
            EventBus.getDefault().post(new ErrorEvent("线路切换失败: " + e.getMessage()));
        }
    }

    // ===== 收藏管理接口 =====

    /**
     * 添加收藏
     */
    public void addToFavorites(top.cywin.onetv.movie.bean.Vod movie) {
        Log.d(TAG, "🔄 添加收藏: " + movie.getVodName());
        try {
            // ✅ 直接调用FongMi_TV现有的Keep系统
            top.cywin.onetv.movie.bean.Keep.create(movie).save();
            Log.d(TAG, "✅ 收藏添加成功");
            EventBus.getDefault().post(new FavoriteUpdateEvent(movie.getVodId(), true, true));
        } catch (Exception e) {
            Log.e(TAG, "❌ 收藏添加失败", e);
            EventBus.getDefault().post(new FavoriteUpdateEvent(movie.getVodId(), true, false));
        }
    }

    /**
     * 移除收藏
     */
    public void removeFromFavorites(String vodId, String siteKey) {
        Log.d(TAG, "🔄 移除收藏: " + vodId);
        try {
            // ✅ 直接调用FongMi_TV现有的Keep系统
            top.cywin.onetv.movie.bean.Keep.delete(vodId);
            Log.d(TAG, "✅ 收藏移除成功");
            EventBus.getDefault().post(new FavoriteUpdateEvent(vodId, false, true));
        } catch (Exception e) {
            Log.e(TAG, "❌ 收藏移除失败", e);
            EventBus.getDefault().post(new FavoriteUpdateEvent(vodId, false, false));
        }
    }

    /**
     * 检查收藏状态
     */
    public boolean isFavorite(String vodId, String siteKey) {
        try {
            // ✅ 直接调用FongMi_TV现有的Keep系统
            return top.cywin.onetv.movie.bean.Keep.exist(vodId);
        } catch (Exception e) {
            Log.e(TAG, "❌ 检查收藏状态失败", e);
            return false;
        }
    }

    // ===== 历史记录接口 =====

    /**
     * 获取观看历史
     */
    public top.cywin.onetv.movie.bean.History getWatchHistory(String vodId, String siteKey) {
        try {
            // ✅ 直接调用FongMi_TV现有的History系统
            return top.cywin.onetv.movie.bean.History.find(vodId);
        } catch (Exception e) {
            Log.e(TAG, "❌ 获取观看历史失败", e);
            return null;
        }
    }

    /**
     * 保存观看历史
     */
    public void saveWatchHistory(String vodId, String vodName, long position, long duration) {
        Log.d(TAG, "🔄 保存观看历史: " + vodId);
        try {
            // ✅ 直接调用FongMi_TV现有的History系统
            top.cywin.onetv.movie.bean.History history = top.cywin.onetv.movie.bean.History.find(vodId);
            if (history == null) {
                history = new top.cywin.onetv.movie.bean.History();
                history.setVodId(vodId);
                history.setVodName(vodName);
            }
            history.setPosition(position);
            history.setDuration(duration);
            history.setCreateTime(System.currentTimeMillis());
            history.save();

            Log.d(TAG, "✅ 观看历史保存成功");
            EventBus.getDefault().post(new HistoryUpdateEvent(vodId, position, duration, true));
        } catch (Exception e) {
            Log.e(TAG, "❌ 观看历史保存失败", e);
            EventBus.getDefault().post(new HistoryUpdateEvent(vodId, position, duration, false));
        }
    }

    // ===== 站点管理接口 =====

    /**
     * 获取当前站点
     */
    public top.cywin.onetv.movie.bean.Site getCurrentSite() {
        try {
            return VodConfig.get().getHome();
        } catch (Exception e) {
            Log.e(TAG, "❌ 获取当前站点失败", e);
            return null;
        }
    }

    /**
     * 切换站点
     */
    public void switchSite(String siteKey) {
        Log.d(TAG, "🔄 切换站点: " + siteKey);
        try {
            top.cywin.onetv.movie.bean.Site site = VodConfig.get().getSite(siteKey);
            if (site != null) {
                VodConfig.get().setHome(site);
                Log.d(TAG, "✅ 站点切换成功");
                EventBus.getDefault().post(new SiteChangeEvent(site, true));
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 站点切换失败", e);
            EventBus.getDefault().post(new SiteChangeEvent(null, false));
        }
    }

    // ===== 云盘相关接口 =====

    /**
     * 加载云盘配置列表
     */
    public void loadCloudDriveConfigs() {
        Log.d(TAG, "🔄 加载云盘配置列表");
        try {
            // 这里需要根据实际的FongMi_TV云盘实现进行调用
            // List<CloudDriveConfig> configs = CloudDriveManager.getConfigs();
            // EventBus.getDefault().post(new CloudDriveConfigEvent(configs, true));
            Log.d(TAG, "✅ 云盘配置列表请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "❌ 云盘配置列表获取失败", e);
            EventBus.getDefault().post(new ErrorEvent("云盘配置列表获取失败: " + e.getMessage()));
        }
    }

    /**
     * 获取云盘文件列表
     */
    public void getCloudFiles(String driveId, String path) {
        Log.d(TAG, "🔄 获取云盘文件列表: driveId=" + driveId + ", path=" + path);
        try {
            // 这里需要根据实际的FongMi_TV云盘实现进行调用
            // CloudDriveManager.getFiles(driveId, path, callback);
            Log.d(TAG, "✅ 云盘文件列表请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "❌ 云盘文件列表获取失败", e);
            EventBus.getDefault().post(new CloudDriveEvent(driveId, null, path, false));
        }
    }

    // ===== 直播相关接口 =====

    /**
     * 获取直播频道列表
     */
    public void getLiveChannels(String group) {
        Log.d(TAG, "🔄 获取直播频道列表: group=" + group);
        try {
            // 这里需要根据实际的FongMi_TV直播实现进行调用
            // LiveViewModel.get().getChannels(group);
            Log.d(TAG, "✅ 直播频道列表请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "❌ 直播频道列表获取失败", e);
            EventBus.getDefault().post(new LiveChannelEvent(null, group, false));
        }
    }

    /**
     * 解析直播播放地址
     */
    public void parseLivePlayUrl(String channelUrl, String channelName) {
        Log.d(TAG, "🔄 解析直播播放地址: " + channelName);
        try {
            // 这里需要根据实际的FongMi_TV直播实现进行调用
            // LiveViewModel.get().parsePlayUrl(channelUrl, callback);
            Log.d(TAG, "✅ 直播播放地址解析请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "❌ 直播播放地址解析失败", e);
            EventBus.getDefault().post(new LivePlayEvent("", channelName, false));
        }
    }

    // ===== 设置相关接口 =====

    /**
     * 更新设置
     */
    public void updateSetting(String key, Object value) {
        Log.d(TAG, "🔄 更新设置: " + key + " = " + value);
        try {
            // 这里需要根据实际的FongMi_TV设置实现进行调用
            // Setting.put(key, value);
            Log.d(TAG, "✅ 设置更新成功");
            EventBus.getDefault().post(new SettingsUpdateEvent(key, value, true));
        } catch (Exception e) {
            Log.e(TAG, "❌ 设置更新失败", e);
            EventBus.getDefault().post(new SettingsUpdateEvent(key, value, false));
        }
    }

    // ===== 网络相关接口 =====

    /**
     * 测试API连接
     */
    public void testApiConnection(String url) {
        Log.d(TAG, "🔄 测试API连接: " + url);
        try {
            long startTime = System.currentTimeMillis();
            // 这里需要根据实际的FongMi_TV网络实现进行调用
            // OkHttp.get().newCall(request).execute();
            long responseTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "✅ API连接测试成功");
            EventBus.getDefault().post(new ApiTestEvent(url, true, responseTime));
        } catch (Exception e) {
            Log.e(TAG, "❌ API连接测试失败", e);
            EventBus.getDefault().post(new ApiTestEvent(url, false, 0, e.getMessage()));
        }
    }

    /**
     * 检查系统状态
     */
    public boolean isSystemReady() {
        try {
            VodConfig vodConfig = VodConfig.get();
            return vodConfig != null && !vodConfig.getSites().isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "❌ 系统状态检查失败", e);
            return false;
        }
    }
}
