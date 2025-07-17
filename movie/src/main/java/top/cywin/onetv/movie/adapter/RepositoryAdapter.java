package top.cywin.onetv.movie.adapter;

import top.cywin.onetv.movie.api.config.VodConfig;
import top.cywin.onetv.movie.api.config.LiveConfig;
import top.cywin.onetv.movie.bean.Site;
import top.cywin.onetv.movie.bean.Live;
import top.cywin.onetv.movie.impl.Callback;

/**
 * Repository适配器
 * 基于FongMi_TV架构设计，重新连接Repository
 */
public class RepositoryAdapter {
    private static final String TAG = "RepositoryAdapter";
    
    /**
     * 重新连接Repository
     */
    public void reconnectRepositories() {
        // 重新连接VOD Repository
        reconnectVodRepository();
        
        // 重新连接Live Repository
        reconnectLiveRepository();
        
        // 初始化数据源
        initializeDataSources();
    }
    
    /**
     * 重新连接VOD Repository
     */
    private void reconnectVodRepository() {
        try {
            // 初始化VOD配置
            VodConfig.get().init();
            
            // 加载默认配置
            loadDefaultVodConfig();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 重新连接Live Repository
     */
    private void reconnectLiveRepository() {
        try {
            // 初始化Live配置
            LiveConfig.get().init();
            
            // 加载默认配置
            loadDefaultLiveConfig();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 初始化数据源
     */
    private void initializeDataSources() {
        // 初始化VOD数据源
        initVodDataSources();
        
        // 初始化Live数据源
        initLiveDataSources();
    }
    
    /**
     * 初始化VOD数据源
     */
    private void initVodDataSources() {
        // 检查是否有可用的VOD站点
        if (VodConfig.get().getSites() != null && !VodConfig.get().getSites().isEmpty()) {
            // 设置默认站点
            Site defaultSite = VodConfig.get().getSites().get(0);
            VodConfig.get().setHome(defaultSite);
        }
    }
    
    /**
     * 初始化Live数据源
     */
    private void initLiveDataSources() {
        // 检查是否有可用的Live源
        if (LiveConfig.get().getLives() != null && !LiveConfig.get().getLives().isEmpty()) {
            // 设置默认Live源
            Live defaultLive = LiveConfig.get().getLives().get(0);
            LiveConfig.get().setHome(defaultLive);
        }
    }
    
    /**
     * 加载默认VOD配置
     */
    private void loadDefaultVodConfig() {
        // 可以从本地文件或网络加载默认配置
        // 这里提供基础框架
        VodConfig.get().load(new Callback() {
            @Override
            public void success() {
                onVodConfigLoaded();
            }
            
            @Override
            public void error(String msg) {
                onVodConfigError(msg);
            }
        });
    }
    
    /**
     * 加载默认Live配置
     */
    private void loadDefaultLiveConfig() {
        // 可以从本地文件或网络加载默认配置
        // 这里提供基础框架
        LiveConfig.get().load(new Callback() {
            @Override
            public void success() {
                onLiveConfigLoaded();
            }
            
            @Override
            public void error(String msg) {
                onLiveConfigError(msg);
            }
        });
    }
    
    /**
     * VOD配置加载成功回调
     */
    private void onVodConfigLoaded() {
        // VOD配置加载成功
        // 可以通知UI更新
    }
    
    /**
     * VOD配置加载错误回调
     */
    private void onVodConfigError(String msg) {
        // VOD配置加载失败
        // 可以显示错误信息或使用备用配置
    }
    
    /**
     * Live配置加载成功回调
     */
    private void onLiveConfigLoaded() {
        // Live配置加载成功
        // 可以通知UI更新
    }
    
    /**
     * Live配置加载错误回调
     */
    private void onLiveConfigError(String msg) {
        // Live配置加载失败
        // 可以显示错误信息或使用备用配置
    }
    
    /**
     * 获取VOD Repository状态
     */
    public boolean isVodRepositoryReady() {
        return VodConfig.get().getSites() != null && !VodConfig.get().getSites().isEmpty();
    }
    
    /**
     * 获取Live Repository状态
     */
    public boolean isLiveRepositoryReady() {
        return LiveConfig.get().getLives() != null && !LiveConfig.get().getLives().isEmpty();
    }
    
    /**
     * 重新加载配置
     */
    public void reloadConfigs() {
        loadDefaultVodConfig();
        loadDefaultLiveConfig();
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        // 清理VOD配置
        if (VodConfig.get() != null) {
            VodConfig.get().clear();
        }
        
        // 清理Live配置
        if (LiveConfig.get() != null) {
            LiveConfig.get().clear();
        }
    }
}
