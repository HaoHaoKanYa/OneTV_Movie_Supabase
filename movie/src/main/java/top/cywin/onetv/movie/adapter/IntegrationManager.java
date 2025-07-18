package top.cywin.onetv.movie.adapter;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.LifecycleOwner;

/**
 * 集成管理器 - 按照FongMi_TV整合指南完善
 * 统一管理UI集成适配，支持17个重构文件的需求
 */
public class IntegrationManager {

    private static final String TAG = "IntegrationManager";
    private static IntegrationManager instance;
    private Context context;
    private RepositoryAdapter repositoryAdapter;
    private UIAdapter uiAdapter;
    private ViewModelAdapter viewModelAdapter;
    private boolean isInitialized = false;

    private IntegrationManager() {
        Log.d(TAG, "🏗️ IntegrationManager 单例创建");
    }

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
        Log.d(TAG, "🔄 初始化IntegrationManager");

        if (isInitialized) {
            Log.d(TAG, "✅ IntegrationManager已初始化，跳过");
            return;
        }

        try {
            this.context = context;

            // 初始化各个适配器
            initializeAdapters(lifecycleOwner);

            // 建立适配器之间的连接
            connectAdapters();

            // 验证系统完整性
            validateSystem();

            isInitialized = true;
            Log.d(TAG, "✅ IntegrationManager初始化完成");

        } catch (Exception e) {
            Log.e(TAG, "❌ IntegrationManager初始化失败", e);
            throw new RuntimeException("IntegrationManager初始化失败", e);
        }
    }

    /**
     * 初始化适配器
     */
    private void initializeAdapters(LifecycleOwner lifecycleOwner) {
        Log.d(TAG, "🔄 初始化适配器");

        try {
            // 初始化RepositoryAdapter
            repositoryAdapter = new RepositoryAdapter();
            repositoryAdapter.reconnectRepositories();

            // 初始化UIAdapter
            uiAdapter = new UIAdapter(context);
            uiAdapter.adaptExistingUI();

            // 初始化ViewModelAdapter
            viewModelAdapter = new ViewModelAdapter(lifecycleOwner);
            viewModelAdapter.reconnectViewModels();

            Log.d(TAG, "✅ 适配器初始化完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 适配器初始化失败", e);
            throw new RuntimeException("适配器初始化失败", e);
        }
    }

    /**
     * 连接适配器
     */
    private void connectAdapters() {
        Log.d(TAG, "🔄 连接适配器");

        try {
            // 这里可以建立适配器之间的连接关系
            // 目前适配器通过MovieApp单例访问，无需额外连接

            Log.d(TAG, "✅ 适配器连接完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 适配器连接失败", e);
            throw new RuntimeException("适配器连接失败", e);
        }
    }

    /**
     * 验证系统完整性
     */
    private void validateSystem() {
        Log.d(TAG, "🔄 验证系统完整性");

        try {
            // 验证RepositoryAdapter
            if (repositoryAdapter == null || !repositoryAdapter.isSystemReady()) {
                throw new RuntimeException("RepositoryAdapter未就绪");
            }

            // 验证UIAdapter
            if (uiAdapter == null || !uiAdapter.isUIReady()) {
                throw new RuntimeException("UIAdapter未就绪");
            }

            // 验证ViewModelAdapter
            if (viewModelAdapter == null || !viewModelAdapter.isViewModelReady()) {
                throw new RuntimeException("ViewModelAdapter未就绪");
            }

            Log.d(TAG, "✅ 系统完整性验证通过");
        } catch (Exception e) {
            Log.e(TAG, "❌ 系统完整性验证失败", e);
            throw new RuntimeException("系统完整性验证失败", e);
        }
    }

    /**
     * 获取RepositoryAdapter
     */
    public RepositoryAdapter getRepositoryAdapter() {
        return repositoryAdapter;
    }

    /**
     * 获取UIAdapter
     */
    public UIAdapter getUIAdapter() {
        return uiAdapter;
    }

    /**
     * 获取ViewModelAdapter
     */
    public ViewModelAdapter getViewModelAdapter() {
        return viewModelAdapter;
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
     * 检查系统状态 - 确保所有适配器正常工作
     */
    public boolean isSystemReady() {
        boolean initialized = isInitialized;
        boolean repositoryReady = repositoryAdapter != null && repositoryAdapter.isSystemReady();
        boolean uiReady = uiAdapter != null && uiAdapter.isUIReady();
        boolean viewModelReady = viewModelAdapter != null && viewModelAdapter.isViewModelReady();

        Log.d(TAG, "🔍 系统状态检查 - 初始化: " + initialized +
                   ", Repository: " + repositoryReady +
                   ", UI: " + uiReady +
                   ", ViewModel: " + viewModelReady);

        return initialized && repositoryReady && uiReady && viewModelReady;
    }

    /**
     * 获取系统状态信息 - 详细的状态报告
     */
    public String getSystemStatus() {
        if (!isInitialized) {
            return "❌ 系统未初始化";
        }

        StringBuilder status = new StringBuilder();
        status.append("🔍 IntegrationManager系统状态:\n");

        if (repositoryAdapter != null) {
            status.append("📦 RepositoryAdapter: ")
                  .append(repositoryAdapter.isSystemReady() ? "✅ 就绪" : "❌ 未就绪")
                  .append("\n");
        } else {
            status.append("📦 RepositoryAdapter: ❌ 未初始化\n");
        }

        if (uiAdapter != null) {
            status.append("🎨 UIAdapter: ")
                  .append(uiAdapter.isUIReady() ? "✅ 就绪" : "❌ 未就绪")
                  .append("\n");
        } else {
            status.append("🎨 UIAdapter: ❌ 未初始化\n");
        }

        if (viewModelAdapter != null) {
            status.append("🔄 ViewModelAdapter: ")
                  .append(viewModelAdapter.isViewModelReady() ? "✅ 就绪" : "❌ 未就绪")
                  .append("\n");
        } else {
            status.append("🔄 ViewModelAdapter: ❌ 未初始化\n");
        }

        return status.toString();
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        Log.d(TAG, "🔄 清理IntegrationManager资源");

        try {
            if (viewModelAdapter != null) {
                viewModelAdapter.cleanup();
            }

            repositoryAdapter = null;
            uiAdapter = null;
            viewModelAdapter = null;
            context = null;
            isInitialized = false;

            Log.d(TAG, "✅ IntegrationManager资源清理完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ IntegrationManager资源清理失败", e);
        }
    }
}
