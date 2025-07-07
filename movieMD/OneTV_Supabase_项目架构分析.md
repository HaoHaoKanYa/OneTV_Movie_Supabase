# OneTV Supabase 项目架构分析

## 项目概述

OneTV Supabase 是一款现代化的Android电视应用，专为提供流畅的直播观看体验而设计。该项目完成了从Cloudflare到Supabase的全面迁移，采用最新的Android开发技术和多模块架构。

### 基本信息
- **项目名称**: OneTV Supabase (壹来电视)
- **版本**: 2.0.0
- **开发语言**: Kotlin 2.1.10
- **UI框架**: Jetpack Compose
- **后端服务**: Supabase
- **目标平台**: Android TV + Mobile
- **最低SDK**: API 21 (TV) / API 24 (Mobile)
- **目标SDK**: API 34

## 技术架构

### 1. 多模块架构

项目采用现代化的多模块架构设计：

```
OneTV_Supabase/
├── core/                          # 核心模块
│   ├── data/                      # 数据层模块
│   ├── designsystem/              # 设计系统模块
│   └── util/                      # 工具类模块
├── tv/                            # Android TV应用模块
├── mobile/                        # 手机应用模块
├── supabase/                      # Supabase后端配置
│   ├── functions/                 # Edge Functions
│   └── sql/                       # 数据库脚本
└── supabase-kt-3.1.4/            # Supabase Kotlin SDK
```

### 2. 技术栈

#### 前端技术
- **Kotlin**: 2.1.10 - 主要开发语言
- **Jetpack Compose**: 最新版本 - 现代化UI框架
- **Compose Compiler**: 1.5.11
- **Android Gradle Plugin**: 8.5.2
- **Kotlin Serialization**: 1.7.0 - JSON序列化
- **Navigation Compose**: 2.7.7 - 导航框架

#### 媒体播放
- **Media3 ExoPlayer**: 1.4.0 - 视频播放引擎
- **Media3 HLS**: 1.4.0 - HLS流媒体支持
- **Media3 UI**: 1.4.0 - 播放器UI组件
- **FFmpeg Decoder**: 1.3.1+2 - 自定义解码器

#### 网络与数据
- **Supabase BOM**: 3.1.4 - Supabase服务集成
- **Ktor**: 3.1.1 - HTTP客户端
- **OkHttp**: 4.12.0 - 网络请求
- **Coil Compose**: 2.7.0 - 图片加载

#### UI组件
- **TV Material**: 1.0.0-rc01 - Android TV专用Material组件
- **Material Icons Extended**: 扩展图标库
- **Compose Foundation**: 1.7.0-beta07

### 3. Supabase后端架构

#### 数据库设计
项目使用PostgreSQL数据库，包含以下核心表：

**用户相关表**:
- `profiles` - 用户配置文件
- `user_settings` - 用户设置
- `user_roles` - 用户角色管理
- `user_sessions` - 用户会话管理
- `user_login_logs` - 登录日志

**内容相关表**:
- `channel_favorites` - 频道收藏
- `watch_history` - 观看历史
- `activation_codes` - 激活码管理
- `vip_transactions` - VIP交易记录

**系统相关表**:
- `app_configs` - 应用配置
- `service_messages` - 服务消息
- `online_users_stats` - 在线用户统计
- `support_conversations` - 客服对话
- `support_messages` - 客服消息
- `user_feedback` - 用户反馈

#### Edge Functions
项目实现了多个Edge Functions提供API服务：

- `app-configs` - 应用配置管理
- `channel-favorites` - 频道收藏管理
- `iptv-channels` - IPTV频道管理
- `online-users` - 在线用户统计
- `service-info` - 服务信息
- `support-management` - 客服管理
- `user-login-log` - 用户登录日志
- `user-profile` - 用户配置文件
- `user-role-management` - 用户角色管理
- `user-sessions` - 用户会话管理
- `user-settings` - 用户设置
- `vip-management` - VIP管理
- `watch_history` - 观看历史
- `watch_history_upsert` - 观看历史更新

### 4. 应用架构模式

#### MVVM架构
项目采用MVVM (Model-View-ViewModel) 架构模式：

- **Model**: 数据层，包含Repository、Entity、API客户端
- **View**: UI层，使用Jetpack Compose构建
- **ViewModel**: 业务逻辑层，处理UI状态和数据交互

#### 依赖注入
使用Kotlin的依赖注入模式，通过单例对象管理核心服务：
- `SupabaseClient` - Supabase客户端管理
- `AppData` - 应用数据管理
- `SupabaseRepository` - 数据仓库

#### 缓存策略
实现了多层缓存机制：
- `SupabaseCacheManager` - Supabase数据缓存
- `FileCacheRepository` - 文件缓存
- `SupabaseCacheStrategy` - 缓存策略管理

## 核心功能模块

### 1. 用户认证系统
- 用户注册、登录、密码重置
- JWT令牌认证
- 多角色权限系统 (user, vip, admin, super_admin)
- 会话管理和自动续期

### 2. 视频播放系统
- ExoPlayer集成，支持多种视频格式
- HLS流媒体播放
- 播放历史记录和同步
- 观看进度追踪
- 自定义播放器控制界面

### 3. 频道管理系统
- IPTV频道源管理
- 频道收藏功能
- 频道分类和搜索
- EPG电子节目指南支持

### 4. 用户数据同步
- 跨设备数据同步
- 观看历史云端存储
- 用户设置同步
- 收藏频道同步

### 5. VIP会员系统
- 激活码兑换
- VIP权限管理
- 会员到期提醒
- 交易记录管理

### 6. 客服支持系统
- 在线客服对话
- 用户反馈收集
- 问题分类和优先级
- 客服工单管理

## 安全特性

### 1. 数据安全
- Row Level Security (RLS) 策略
- JWT令牌验证
- API访问控制
- 敏感数据加密

### 2. 网络安全
- HTTPS通信
- 自定义TrustManager (开发环境)
- API密钥管理
- CORS跨域配置

### 3. 用户隐私
- 数据最小化原则
- 用户数据删除机制
- 隐私设置管理
- 匿名使用选项

## 部署与运维

### 1. 构建系统
- Gradle 8.7构建系统
- 多变体构建配置
- 代码混淆和资源压缩
- 自动签名配置

### 2. CI/CD流程
- GitHub Actions自动化
- 多环境部署支持
- 自动化测试集成
- 发布版本管理

### 3. 监控与日志
- 全局异常处理
- 错误日志收集
- 性能监控
- 用户行为分析

## 开发工具与配置

### 1. 开发环境
- Android Studio 2023.3.1+
- JDK 17+
- Gradle 8.6+
- Kotlin 2.1.10

### 2. 配置文件
- `local.properties` - 本地开发配置
- `key.properties` - 签名配置
- `supabase_config.properties` - Supabase配置
- `google-services.json` - Firebase配置

### 3. 构建配置
- 多模块依赖管理
- 版本目录统一管理
- ProGuard代码混淆
- 多架构支持 (ARM64, ARMv7)

## 项目特色

### 1. 现代化架构
- Kotlin Multiplatform准备
- 响应式编程模式
- 声明式UI设计
- 模块化架构

### 2. 用户体验
- 流畅的动画效果
- 直观的操作界面
- 快速的启动时间
- 稳定的播放性能

### 3. 可扩展性
- 插件化架构设计
- 多平台支持基础
- 灵活的配置系统
- 可扩展的功能模块

## 未来发展规划

### 2.1.0版本计划
- iOS平台支持
- 用户自定义主题
- 高级频道过滤
- 网络自适应播放

### 2.2.0版本规划
- 桌面端支持
- 云端配置同步
- AI推荐系统
- 多语言国际化

## 代码结构详细分析

### 1. 核心数据层 (core/data)

#### 主要组件
- **AppData.kt**: 应用数据管理中心，负责全局数据初始化
- **SupabaseClient.kt**: Supabase客户端单例，管理所有Supabase服务
- **SupabaseRepository.kt**: 数据仓库模式实现，封装数据访问逻辑
- **SupabaseUserRepository.kt**: 用户相关数据操作
- **StorageRepository.kt**: 文件存储管理

#### 缓存系统
```kotlin
// 缓存管理器架构
SupabaseCacheManager
├── SupabaseCacheConfig - 缓存配置
├── SupabaseCacheEntry - 缓存条目
├── SupabaseCacheKey - 缓存键管理
├── SupabaseCacheStrategy - 缓存策略
└── SupabaseFieldTracker - 字段变更追踪
```

#### 实体类设计
- **Channel**: 频道信息实体
- **EPG**: 电子节目指南实体
- **IPTVSource**: IPTV源配置实体
- **Git**: Git仓库配置实体

### 2. TV应用模块 (tv/)

#### 核心Activity
- **MainActivity.kt**: 主界面Activity，集成ExoPlayer播放器
- **LoginActivity.kt**: 用户登录界面
- **RegisterActivity.kt**: 用户注册界面
- **ForgotPasswordActivity.kt**: 密码重置界面

#### 应用生命周期
- **MyTVApplication.kt**: 应用程序类，负责全局初始化
  - Supabase客户端初始化
  - 全局异常处理器设置
  - 错误日志收集和邮件发送

#### 安全配置
- **UnsafeTrustManager.kt**: 开发环境SSL证书信任管理
- **BootReceiver.kt**: 系统启动广播接收器

### 3. Supabase集成详解

#### 客户端配置
```kotlin
object SupabaseClient {
    // 引导配置
    private val bootstrapUrl: String
    private val bootstrapKey: String

    // 服务模块
    val auth: Auth
    val postgrest: Postgrest
    val storage: Storage
    val functions: Functions
    val realtime: Realtime
}
```

#### 数据库连接管理
- 动态配置加载机制
- 多环境配置支持
- 连接池管理
- 自动重连机制

#### 实时数据同步
- Realtime订阅管理
- 数据变更监听
- 冲突解决策略
- 离线数据同步

### 4. 用户权限系统详解

#### 角色定义
```sql
CREATE TYPE user_role_type AS ENUM (
    'user',        -- 普通用户
    'vip',         -- VIP用户
    'admin',       -- 管理员
    'super_admin'  -- 超级管理员
);
```

#### 权限矩阵
| 功能 | user | vip | admin | super_admin |
|------|------|-----|-------|-------------|
| 基础播放 | ✓ | ✓ | ✓ | ✓ |
| 高清播放 | ✗ | ✓ | ✓ | ✓ |
| 频道管理 | ✗ | ✗ | ✓ | ✓ |
| 用户管理 | ✗ | ✗ | ✓ | ✓ |
| 系统配置 | ✗ | ✗ | ✗ | ✓ |

#### RLS策略示例
```sql
-- 用户只能访问自己的数据
CREATE POLICY "Users can only access their own data"
ON profiles FOR ALL
USING (auth.uid() = userid);

-- VIP用户可以访问高级功能
CREATE POLICY "VIP users can access premium features"
ON premium_content FOR SELECT
USING (
    EXISTS (
        SELECT 1 FROM profiles
        WHERE userid = auth.uid()
        AND (is_vip = true OR primary_role IN ('admin', 'super_admin'))
    )
);
```

### 5. 观看历史系统

#### 数据结构
```sql
CREATE TABLE watch_history (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id uuid NOT NULL REFERENCES auth.users(id),
    channel_name text NOT NULL,
    channel_url text NOT NULL,
    watch_start timestamp with time zone DEFAULT now(),
    watch_end timestamp with time zone,
    duration integer, -- 观看时长(秒)
    created_at timestamp with time zone DEFAULT now()
);
```

#### 同步机制
- **批量同步**: 定期批量上传观看记录
- **实时同步**: 重要事件实时同步
- **冲突解决**: 基于时间戳的冲突解决
- **数据去重**: 防止重复记录生成

#### 性能优化
- 本地缓存机制
- 分页加载历史记录
- 索引优化查询
- 定期数据清理

### 6. 网络架构

#### HTTP客户端配置
```kotlin
// OkHttp配置
val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .addInterceptor(AuthInterceptor())
    .addInterceptor(LoggingInterceptor())
    .build()
```

#### API设计模式
- RESTful API设计
- 统一错误处理
- 请求重试机制
- 响应缓存策略

#### 网络状态管理
- 网络连接监听
- 自动重连机制
- 离线模式支持
- 网络质量自适应

### 7. 媒体播放架构

#### ExoPlayer集成
```kotlin
class VideoPlayerManager {
    private val exoPlayer: ExoPlayer
    private val watchHistoryTracker: WatchHistoryTracker

    fun playMedia(mediaItem: MediaItem) {
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()

        // 开始追踪观看历史
        watchHistoryTracker.startTracking(mediaItem)
    }
}
```

#### 播放器功能
- 多格式支持 (HLS, DASH, MP4等)
- 自适应码率播放
- 字幕支持
- 音频轨道切换
- 播放速度控制
- 画中画模式

#### 观看历史追踪
- 播放开始/结束事件
- 观看时长统计
- 播放进度保存
- 异常中断处理

### 8. UI/UX设计系统

#### Compose主题系统
```kotlin
@Composable
fun OneTVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
```

#### 响应式设计
- 多屏幕尺寸适配
- 横竖屏切换支持
- 动态字体大小
- 无障碍功能支持

#### 动画系统
- 页面转场动画
- 列表项动画
- 加载状态动画
- 手势交互动画

### 9. 错误处理与日志系统

#### 全局异常处理
```kotlin
Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
    val errorMessage = "Unhandled exception in thread ${thread.name}:\n${throwable.stackTraceToString()}"
    Log.e("GlobalException", errorMessage)

    // 写入错误日志
    writeErrorLogToFile(errorMessage)

    // 发送错误报告
    sendErrorReportViaEmail()
}
```

#### 日志分级
- **DEBUG**: 开发调试信息
- **INFO**: 一般信息记录
- **WARN**: 警告信息
- **ERROR**: 错误信息
- **FATAL**: 致命错误

#### 错误上报机制
- 自动错误收集
- 邮件错误报告
- 崩溃日志分析
- 用户反馈集成

### 10. 性能优化策略

#### 内存管理
- 图片缓存优化
- 对象池复用
- 内存泄漏检测
- GC优化策略

#### 启动优化
- 延迟初始化
- 预加载关键数据
- 启动页面优化
- 冷启动时间监控

#### 网络优化
- 请求合并
- 数据压缩
- CDN加速
- 缓存策略优化

## 开发最佳实践

### 1. 代码规范
- Kotlin编码规范
- 函数命名约定
- 注释文档标准
- 代码审查流程

### 2. 测试策略
- 单元测试覆盖
- 集成测试设计
- UI自动化测试
- 性能测试方案

### 3. 版本管理
- Git工作流程
- 分支管理策略
- 版本发布流程
- 回滚应急预案

## 总结

OneTV Supabase项目展现了现代Android应用开发的最佳实践，通过采用最新的技术栈和架构模式，实现了高性能、可维护、可扩展的流媒体应用。项目的多模块架构、完善的后端服务集成、以及现代化的UI设计，为用户提供了优质的观看体验，同时为开发者提供了良好的代码结构和开发体验。

该项目的技术架构具有以下特点：
1. **现代化**: 采用最新的Kotlin、Compose、Supabase技术栈
2. **模块化**: 清晰的模块划分，便于维护和扩展
3. **安全性**: 完善的权限控制和数据安全机制
4. **性能**: 多层缓存和优化策略确保流畅体验
5. **可扩展**: 为多平台支持和功能扩展奠定基础

这个项目为Android TV应用开发提供了一个优秀的参考模板，展示了如何构建一个功能完整、架构清晰、性能优异的现代化应用。
