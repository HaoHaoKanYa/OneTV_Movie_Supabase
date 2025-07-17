package top.cywin.onetv.movie.adapter;

import android.content.Context;

import androidx.lifecycle.LifecycleOwner;

/**
 * 集成管理器
 * 基于FongMi_TV架构设计，统一管理UI集成适配
 */
public class IntegrationManager {
    private static final String TAG = "IntegrationManager";
    
    private static IntegrationManager instance;
    
    private Context context;
    private UIAdapter uiAdapter;
    private ViewModelAdapter viewModelAdapter;
    private RepositoryAdapter repositoryAdapter;
    
    private boolean isInitialized = false;
    
    private IntegrationManager() {
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized IntegrationManager getInstance() {
        if (instance == null) {
            instance = new IntegrationManager();
        }
        return instance;
    }
    
    /**
     * 初始化集成管理器
     */
    public void initialize(Context context, LifecycleOwner lifecycleOwner) {
        if (isInitialized) {
            return;
        }
        
        this.context = context.getApplicationContext();
        
        // 初始化适配器
        initializeAdapters(lifecycleOwner);
        
        // 执行集成适配
        performIntegration();
        
        isInitialized = true;
    }
    
    /**
     * 初始化适配器
     */
    private void initializeAdapters(LifecycleOwner lifecycleOwner) {
        // 初始化UI适配器
        uiAdapter = new UIAdapter(context);
        
        // 初始化ViewModel适配器
        viewModelAdapter = new ViewModelAdapter(lifecycleOwner);
        
        // 初始化Repository适配器
        repositoryAdapter = new RepositoryAdapter();
    }
    
    /**
     * 执行集成适配
     */
    private void performIntegration() {
        try {
            // 1. 重新连接Repository
            repositoryAdapter.reconnectRepositories();
            
            // 2. 重新连接ViewModel
            viewModelAdapter.reconnectViewModels();
            
            // 3. 适配现有UI
            uiAdapter.adaptExistingUI();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 获取UI适配器
     */
    public UIAdapter getUIAdapter() {
        return uiAdapter;
    }
    
    /**
     * 获取ViewModel适配器
     */
    public ViewModelAdapter getViewModelAdapter() {
        return viewModelAdapter;
    }
    
    /**
     * 获取Repository适配器
     */
    public RepositoryAdapter getRepositoryAdapter() {
        return repositoryAdapter;
    }
    
    /**
     * 检查是否已初始化
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * 重新初始化
     */
    public void reinitialize(Context context, LifecycleOwner lifecycleOwner) {
        cleanup();
        isInitialized = false;
        initialize(context, lifecycleOwner);
    }
    
    /**
     * 检查系统状态
     */
    public boolean isSystemReady() {
        return isInitialized && 
               repositoryAdapter != null && 
               repositoryAdapter.isVodRepositoryReady() &&
               viewModelAdapter != null &&
               uiAdapter != null;
    }
    
    /**
     * 获取系统状态信息
     */
    public String getSystemStatus() {
        if (!isInitialized) {
            return "系统未初始化";
        }
        
        StringBuilder status = new StringBuilder();
        status.append("系统状态:\n");
        
        if (repositoryAdapter != null) {
            status.append("VOD Repository: ")
                  .append(repositoryAdapter.isVodRepositoryReady() ? "就绪" : "未就绪")
                  .append("\n");
            status.append("Live Repository: ")
                  .append(repositoryAdapter.isLiveRepositoryReady() ? "就绪" : "未就绪")
                  .append("\n");
        }
        
        if (viewModelAdapter != null) {
            status.append("ViewModel: 已连接\n");
        }
        
        if (uiAdapter != null) {
            status.append("UI适配器: 已初始化\n");
        }
        
        return status.toString();
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (uiAdapter != null) {
            uiAdapter.cleanup();
            uiAdapter = null;
        }
        
        if (viewModelAdapter != null) {
            viewModelAdapter.cleanup();
            viewModelAdapter = null;
        }
        
        if (repositoryAdapter != null) {
            repositoryAdapter.cleanup();
            repositoryAdapter = null;
        }
        
        context = null;
        isInitialized = false;
    }
}
