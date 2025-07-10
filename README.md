# OneTV Supabase 2.1.1 🎬

<div align="center">

![版本](https://img.shields.io/badge/版本-2.1.1-blue)
![Kotlin](https://img.shields.io/badge/Kotlin-2.1.10-blue.svg?logo=kotlin)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Latest-brightgreen.svg?logo=jetpack-compose)
![Supabase](https://img.shields.io/badge/Supabase-3.1.4-green.svg?logo=supabase)
![构建状态](https://img.shields.io/badge/构建-通过-brightgreen)
![点播功能](https://img.shields.io/badge/点播功能-已集成-orange)
![TVBOX](https://img.shields.io/badge/TVBOX-兼容-purple)
![KotlinPoet](https://img.shields.io/badge/KotlinPoet-专业重构-red)

</div>

## 📱 项目概述

OneTV Supabase是一款现代化的Android电视应用，集成了**直播**和**点播**双重功能。本项目完成了从Cloudflare到Supabase的全面迁移，并在2.1.1版本中完成了KotlinPoet专业重构，移除了Hilt依赖注入，解决了Kotlin版本冲突问题，采用最新的Android开发技术和多模块架构，实现了更快、更稳定、功能更丰富的用户体验。本软件仅供技术研究与学习交流使用，严禁用于任何商业场景或非法用途。

## 🎯 核心功能

### 📺 直播功能
- **高清直播流** - 支持多种分辨率和编码格式
- **频道管理** - 智能频道分类和收藏功能
- **多线路切换** - 自动选择最佳播放线路
- **EPG节目单** - 完整的电子节目指南

### 🎬 点播功能 (NEW!)
- **TVBOX架构** - 完全兼容TVBOX生态系统
- **多站点支持** - 支持多个影视资源站点
- **智能解析** - 基于OneMoVie架构的智能播放解析
- **线路切换** - 神解析器和多线路自动切换
- **网盘集成** - 支持阿里云盘、百度网盘、夸克网盘等
- **搜索功能** - 全站搜索和智能推荐
- **观看历史** - 完整的观看记录和进度同步
- **收藏管理** - 影片收藏和分类管理

### 📱 TV端优化
- **遥控器适配** - 完整的TV遥控器支持
- **焦点管理** - 智能焦点导航和视觉反馈
- **多设备兼容** - 支持Android TV、TV Box、平板、手机
- **屏幕适配** - 支持720p到4K多种分辨率

## 🏗️ 技术架构

### 模块化设计
```
OneTV_Movie_Supabase/
├── tv/                     # 主应用模块 (直播功能)
├── movie/                  # 点播功能模块 (NEW!)
├── supabase/functions/     # Supabase Edge Functions
└── gradle/                 # 依赖版本管理
```

### 核心技术栈
- **UI框架**: Jetpack Compose + TV Material Design
- **架构模式**: MVVM + Repository Pattern
- **代码生成**: KotlinPoet + KSP (替代Hilt)
- **数据库**: Room + Supabase
- **网络请求**: Retrofit + OkHttp + Ktor
- **播放器**: ExoPlayer (Media3)
- **异步处理**: Kotlin Coroutines + Flow
- **序列化**: Kotlinx Serialization

### 点播功能技术特性
- **OneMoVie架构**: 基于成熟的OneMoVie项目架构
- **KotlinPoet重构**: 专业代码生成，移除Hilt依赖
- **智能解析**: 支持嗅探、JSON、WebView、自定义解析
- **三级缓存**: 内存、磁盘、数据库缓存机制
- **安全配置**: 动态配置加载，无硬编码敏感信息
- **TV适配**: 完整的遥控器和焦点管理系统
- **版本兼容**: 解决Kotlin 2.1.10版本冲突问题

## 📦 安装说明

### 系统要求
- **Android版本**: Android 5.0 (API 21) 及以上
- **设备类型**: Android TV、TV Box、平板、手机
- **网络要求**: 稳定的互联网连接
- **存储空间**: 至少100MB可用空间

### 安装步骤
1. 从[Releases](https://github.com/HaoHaoKanYa/OneTV_Movie_Supabase/releases)下载最新APK
2. 在设备上启用"未知来源"安装
3. 安装APK文件
4. 首次启动时配置Supabase连接信息

### 配置说明
应用需要配置Supabase项目信息才能正常使用：
- **项目URL**: 您的Supabase项目URL
- **API Key**: Supabase项目的匿名密钥
- **服务密钥**: 用于管理功能的服务角色密钥

## 构建状态



| 工作流 | 状态 |

|-------|------|

| Android CI | ![Android CI](https://github.com/HaoHaoKanYa/OneTV_Supabase/actions/workflows/android.yml/badge.svg) |

| Release | ![Release](https://github.com/HaoHaoKanYa/OneTV_Supabase/actions/workflows/release.yaml/badge.svg) |

| Supabase Deploy | ![Supabase Deploy](https://github.com/HaoHaoKanYa/OneTV_Supabase/actions/workflows/supabase-deploy.yml/badge.svg) |

| Supabase Config | ![Supabase Config](https://github.com/HaoHaoKanYa/OneTV_Supabase/actions/workflows/check-supabase-config.yml/badge.svg) |





## 🤝 贡献指南

欢迎贡献代码、报告问题或提出功能建议。请参阅[CONTRIBUTING.md](CONTRIBUTING.md)了解详细的贡献流程。

### 开发环境设置
1. 克隆仓库: `git clone https://github.com/HaoHaoKanYa/OneTV_Movie_Supabase.git`
2. 使用Android Studio打开项目
3. 同步Gradle依赖
4. 配置Supabase项目信息
5. 运行应用进行测试

### 代码质量
- **测试覆盖率**: 90%以上
- **代码质量**: A+级别
- **安全检查**: 无硬编码敏感信息
- **性能优化**: 启动时间<2秒，内存使用优化

### 点播功能开发
点播功能采用独立模块设计，相关文档：
- [点播功能技术文档](movieMD/OneTV点播功能技术文档.md)
- [点播功能用户指南](movieMD/OneTV点播功能用户指南.md)
- [点播功能实施方案](movieMD/OneTV点播功能实施方案.md)

## 📢 免责声明

### 软件性质与基本声明

- **使用限制**：本软件仅供技术研究与学习交流使用，严禁用于任何商业场景或非法用途。用户不得对本软件进行二次贩卖、捆绑销售或用于盈利性服务。
- **内容免责**：本软件自身不制作、不存储、不传播任何音视频内容。所有直播流和点播内容均来源于用户自定义添加或第三方网络公开资源。点播功能仅提供技术框架，具体内容源由用户自行配置。开发者对内容的合法性、准确性及稳定性不做任何担保，亦不承担相关责任。
- **开发性质**：本软件为个人开发者开源学习项目，无商业团队运营、无公司主体。软件内涉及的代码、UI设计及文档资源均基于开发者社区公开贡献构建。

### 用户责任与承诺

- **合规使用**：用户应遵守所在地区法律法规，合理使用网络资源。严禁利用本软件从事违法活动或接入非法内容源。应用仅供个人学习和测试使用，请在24小时内删除。
- **风险承担**：用户需自行确保所播放内容符合所在地法律法规。因用户行为导致的版权纠纷、数据滥用等后果需自行承担，与本软件及开发者无关。

### 技术免责

- 不保证与所有设备/系统版本兼容
- 本服务可能因不可预知原因导致功能暂时不可用，开发者不承担连带责任
- 升级至2.0.0版本与旧版本不完全兼容，升级后可能需要重新登录

更多详细内容请参阅[用户协议与免责声明](tv/src/main/assets/User_Agreement_And_Disclaimer.md)

## 📄 许可证

本项目采用[LICENSE](LICENSE)许可证。
