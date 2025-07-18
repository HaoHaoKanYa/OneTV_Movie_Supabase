package top.cywin.onetv.movie.adapter;

import android.util.Log;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.ui.SiteViewModel;
import com.fongmi.android.tv.impl.Callback;

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
        this.siteViewModel = SiteViewModel.get();
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
                siteViewModel = SiteViewModel.get();
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
                siteViewModel.categoryContent(typeId, String.valueOf(page), true, filters);
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
                siteViewModel.detailContent(vodId);
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
            if (siteViewModel != null) {
                siteViewModel.searchContent(keyword);
                Log.d(TAG, "✅ 搜索请求已发送");
            } else {
                Log.e(TAG, "❌ SiteViewModel未初始化");
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
}
