package top.cywin.onetv.movie.adapter;

import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import top.cywin.onetv.movie.bean.Result;
import top.cywin.onetv.movie.bean.Vod;
import top.cywin.onetv.movie.model.SiteViewModel;
import top.cywin.onetv.movie.model.LiveViewModel;

/**
 * ViewModel适配器
 * 基于FongMi_TV架构设计，重新连接ViewModel
 */
public class ViewModelAdapter {
    private static final String TAG = "ViewModelAdapter";
    
    private SiteViewModel siteViewModel;
    private LiveViewModel liveViewModel;
    private LifecycleOwner lifecycleOwner;
    
    public ViewModelAdapter(LifecycleOwner lifecycleOwner) {
        this.lifecycleOwner = lifecycleOwner;
        this.siteViewModel = new SiteViewModel();
        this.liveViewModel = new LiveViewModel();
    }
    
    /**
     * 重新连接ViewModel
     */
    public void reconnectViewModels() {
        // 连接SiteViewModel
        reconnectSiteViewModel();
        
        // 连接LiveViewModel
        reconnectLiveViewModel();
        
        // 设置数据观察者
        setupDataObservers();
    }
    
    /**
     * 重新连接SiteViewModel
     */
    private void reconnectSiteViewModel() {
        if (siteViewModel != null) {
            // 重新初始化SiteViewModel
            siteViewModel.clear();
        }
    }
    
    /**
     * 重新连接LiveViewModel
     */
    private void reconnectLiveViewModel() {
        if (liveViewModel != null) {
            // 重新初始化LiveViewModel
            liveViewModel.clear();
        }
    }
    
    /**
     * 设置数据观察者
     */
    private void setupDataObservers() {
        setupSiteDataObservers();
        setupLiveDataObservers();
    }
    
    /**
     * 设置站点数据观察者
     */
    private void setupSiteDataObservers() {
        if (siteViewModel != null && lifecycleOwner != null) {
            // 观察搜索结果
            siteViewModel.result.observe(lifecycleOwner, new Observer<Result>() {
                @Override
                public void onChanged(Result result) {
                    onSearchResultChanged(result);
                }
            });
            
            // 观察内容详情
            siteViewModel.detail.observe(lifecycleOwner, new Observer<Vod>() {
                @Override
                public void onChanged(Vod vod) {
                    onContentDetailChanged(vod);
                }
            });
            
            // 观察播放地址
            siteViewModel.player.observe(lifecycleOwner, new Observer<Result>() {
                @Override
                public void onChanged(Result result) {
                    onPlayerResultChanged(result);
                }
            });
        }
    }
    
    /**
     * 设置直播数据观察者
     */
    private void setupLiveDataObservers() {
        if (liveViewModel != null && lifecycleOwner != null) {
            // 观察直播频道
            liveViewModel.live.observe(lifecycleOwner, live -> {
                onLiveChannelChanged(live);
            });
            
            // 观察EPG数据
            liveViewModel.epg.observe(lifecycleOwner, epg -> {
                onEpgDataChanged(epg);
            });
        }
    }
    
    /**
     * 搜索结果变化回调
     */
    private void onSearchResultChanged(Result result) {
        // 处理搜索结果变化
        if (result != null && result.getList() != null) {
            // 更新UI显示搜索结果
        }
    }
    
    /**
     * 内容详情变化回调
     */
    private void onContentDetailChanged(Vod vod) {
        // 处理内容详情变化
        if (vod != null) {
            // 更新UI显示内容详情
        }
    }
    
    /**
     * 播放器结果变化回调
     */
    private void onPlayerResultChanged(Result result) {
        // 处理播放器结果变化
        if (result != null) {
            // 启动播放器
        }
    }
    
    /**
     * 直播频道变化回调
     */
    private void onLiveChannelChanged(Object live) {
        // 处理直播频道变化
        if (live != null) {
            // 更新直播频道列表
        }
    }
    
    /**
     * EPG数据变化回调
     */
    private void onEpgDataChanged(Object epg) {
        // 处理EPG数据变化
        if (epg != null) {
            // 更新节目单显示
        }
    }
    
    /**
     * 获取SiteViewModel
     */
    public SiteViewModel getSiteViewModel() {
        return siteViewModel;
    }
    
    /**
     * 获取LiveViewModel
     */
    public LiveViewModel getLiveViewModel() {
        return liveViewModel;
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (siteViewModel != null) {
            siteViewModel.clear();
        }
        if (liveViewModel != null) {
            liveViewModel.clear();
        }
        lifecycleOwner = null;
    }
}
