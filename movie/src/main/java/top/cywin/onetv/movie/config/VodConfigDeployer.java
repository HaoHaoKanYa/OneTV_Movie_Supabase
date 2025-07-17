package top.cywin.onetv.movie.config;

import android.content.Context;
import android.util.Log;

import top.cywin.onetv.movie.api.config.VodConfig;
import top.cywin.onetv.movie.bean.Config;
import top.cywin.onetv.movie.impl.Callback;

/**
 * VOD配置部署器
 * 专门用于部署TVBOX兼容的影视接口
 */
public class VodConfigDeployer {
    private static final String TAG = "VodConfigDeployer";
    
    // OneTV官方影视接口URL
    private static final String ONETV_API_URL = "https://raw.githubusercontent.com/HaoHaoKanYa/OneTV-API/refs/heads/main/vod/output/onetv-api-movie.json";
    
    /**
     * 部署OneTV官方影视接口
     * @param context 上下文
     * @param callback 回调
     */
    public static void deployOnetvApiConfig(Context context, Callback callback) {
        Log.d(TAG, "开始部署OneTV官方影视接口");
        Log.d(TAG, "接口URL: " + ONETV_API_URL);
        
        try {
            // 创建VOD配置
            Config config = Config.find(ONETV_API_URL, "OneTV官方影视接口", 0);
            
            // 设置配置信息
            config.setUrl(ONETV_API_URL);
            config.setName("OneTV官方影视接口");
            config.setType(0); // 0表示VOD点播配置
            
            // 保存配置到数据库
            config.update();
            
            Log.d(TAG, "配置创建成功，开始加载接口数据");
            
            // 加载配置
            VodConfig.load(config, new Callback() {
                @Override
                public void success() {
                    Log.d(TAG, "OneTV官方影视接口部署成功！");
                    Log.d(TAG, "站点数量: " + VodConfig.get().getSites().size());
                    Log.d(TAG, "解析器数量: " + VodConfig.get().getParses().size());
                    
                    if (callback != null) {
                        callback.success();
                    }
                }
                
                @Override
                public void error(String msg) {
                    Log.e(TAG, "OneTV官方影视接口部署失败: " + msg);
                    
                    if (callback != null) {
                        callback.error(msg);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "部署配置时发生异常", e);
            if (callback != null) {
                callback.error("部署配置异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 部署自定义影视接口
     * @param context 上下文
     * @param configUrl 配置URL
     * @param configName 配置名称
     * @param callback 回调
     */
    public static void deployCustomConfig(Context context, String configUrl, String configName, Callback callback) {
        Log.d(TAG, "开始部署自定义影视接口");
        Log.d(TAG, "接口URL: " + configUrl);
        Log.d(TAG, "接口名称: " + configName);
        
        try {
            // 创建VOD配置
            Config config = Config.find(configUrl, configName, 0);
            
            // 设置配置信息
            config.setUrl(configUrl);
            config.setName(configName);
            config.setType(0); // 0表示VOD点播配置
            
            // 保存配置到数据库
            config.update();
            
            Log.d(TAG, "配置创建成功，开始加载接口数据");
            
            // 加载配置
            VodConfig.load(config, new Callback() {
                @Override
                public void success() {
                    Log.d(TAG, "自定义影视接口部署成功！");
                    Log.d(TAG, "站点数量: " + VodConfig.get().getSites().size());
                    Log.d(TAG, "解析器数量: " + VodConfig.get().getParses().size());
                    
                    if (callback != null) {
                        callback.success();
                    }
                }
                
                @Override
                public void error(String msg) {
                    Log.e(TAG, "自定义影视接口部署失败: " + msg);
                    
                    if (callback != null) {
                        callback.error(msg);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "部署自定义配置时发生异常", e);
            if (callback != null) {
                callback.error("部署配置异常: " + e.getMessage());
            }
        }
    }
    
    /**
     * 获取当前配置状态
     * @return 配置状态信息
     */
    public static String getConfigStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== VOD配置状态 ===\n");
        
        try {
            Config currentConfig = VodConfig.get().getConfig();
            if (currentConfig != null) {
                status.append("当前配置: ").append(currentConfig.getName()).append("\n");
                status.append("配置URL: ").append(currentConfig.getUrl()).append("\n");
                status.append("配置类型: ").append(currentConfig.getType() == 0 ? "VOD点播" : "其他").append("\n");
            } else {
                status.append("当前配置: 未设置\n");
            }
            
            status.append("站点数量: ").append(VodConfig.get().getSites().size()).append("\n");
            status.append("解析器数量: ").append(VodConfig.get().getParses().size()).append("\n");
            
            if (VodConfig.get().getHome() != null) {
                status.append("默认站点: ").append(VodConfig.get().getHome().getName()).append("\n");
            } else {
                status.append("默认站点: 未设置\n");
            }
            
        } catch (Exception e) {
            status.append("获取配置状态失败: ").append(e.getMessage()).append("\n");
        }
        
        return status.toString();
    }
    
    /**
     * 检查配置是否已加载
     * @return 是否已加载
     */
    public static boolean isConfigLoaded() {
        try {
            return VodConfig.get().getConfig() != null && 
                   !VodConfig.get().getSites().isEmpty();
        } catch (Exception e) {
            Log.e(TAG, "检查配置状态失败", e);
            return false;
        }
    }
    
    /**
     * 重新加载当前配置
     * @param callback 回调
     */
    public static void reloadCurrentConfig(Callback callback) {
        try {
            Config currentConfig = VodConfig.get().getConfig();
            if (currentConfig != null) {
                Log.d(TAG, "重新加载当前配置: " + currentConfig.getName());
                VodConfig.load(currentConfig, callback);
            } else {
                Log.w(TAG, "没有当前配置可以重新加载");
                if (callback != null) {
                    callback.error("没有当前配置");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "重新加载配置失败", e);
            if (callback != null) {
                callback.error("重新加载失败: " + e.getMessage());
            }
        }
    }
}
