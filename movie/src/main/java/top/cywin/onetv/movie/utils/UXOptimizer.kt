package top.cywin.onetv.movie.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
// KotlinPoet专业重构 - 移除Hilt相关import
// import javax.inject.Inject
// import javax.inject.Singleton

/**
 * 用户体验优化工具类
 * 提供动画、反馈、引导、个性化等UX优化功能
 * KotlinPoet专业重构 - 移除Hilt依赖，使用标准构造函数
 */
// @Singleton
class UXOptimizer(
    private val context: Context
) {
    
    private val preferences: SharedPreferences = 
        context.getSharedPreferences("movie_ux_prefs", Context.MODE_PRIVATE)
    
    // UX状态管理
    private val _isFirstLaunch = MutableStateFlow(preferences.getBoolean("is_first_launch", true))
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()
    
    private val _showGuide = MutableStateFlow(false)
    val showGuide: StateFlow<Boolean> = _showGuide.asStateFlow()
    
    // 用户偏好设置
    private val _animationEnabled = MutableStateFlow(preferences.getBoolean("animation_enabled", true))
    val animationEnabled: StateFlow<Boolean> = _animationEnabled.asStateFlow()
    
    private val _hapticEnabled = MutableStateFlow(preferences.getBoolean("haptic_enabled", true))
    val hapticEnabled: StateFlow<Boolean> = _hapticEnabled.asStateFlow()

    /**
     * 标记首次启动完成
     */
    fun markFirstLaunchCompleted() {
        _isFirstLaunch.value = false
        preferences.edit().putBoolean("is_first_launch", false).apply()
    }

    /**
     * 显示引导
     */
    fun showGuide() {
        _showGuide.value = true
    }

    /**
     * 隐藏引导
     */
    fun hideGuide() {
        _showGuide.value = false
    }

    /**
     * 设置动画开关
     */
    fun setAnimationEnabled(enabled: Boolean) {
        _animationEnabled.value = enabled
        preferences.edit().putBoolean("animation_enabled", enabled).apply()
    }

    /**
     * 设置触觉反馈开关
     */
    fun setHapticEnabled(enabled: Boolean) {
        _hapticEnabled.value = enabled
        preferences.edit().putBoolean("haptic_enabled", enabled).apply()
    }

    /**
     * 优化的淡入动画
     */
    @Composable
    fun OptimizedFadeIn(
        visible: Boolean,
        duration: Int = 300,
        content: @Composable () -> Unit
    ) {
        val animationEnabled by animationEnabled.collectAsState()
        
        if (animationEnabled) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = duration,
                        easing = FastOutSlowInEasing
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = duration / 2,
                        easing = FastOutLinearInEasing
                    )
                )
            ) {
                content()
            }
        } else {
            if (visible) {
                content()
            }
        }
    }

    /**
     * 优化的滑入动画
     */
    @Composable
    fun OptimizedSlideIn(
        visible: Boolean,
        direction: SlideDirection = SlideDirection.UP,
        duration: Int = 400,
        content: @Composable () -> Unit
    ) {
        val animationEnabled by animationEnabled.collectAsState()
        
        if (animationEnabled) {
            val enterTransition = when (direction) {
                SlideDirection.UP -> slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(duration, easing = FastOutSlowInEasing)
                )
                SlideDirection.DOWN -> slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(duration, easing = FastOutSlowInEasing)
                )
                SlideDirection.LEFT -> slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(duration, easing = FastOutSlowInEasing)
                )
                SlideDirection.RIGHT -> slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(duration, easing = FastOutSlowInEasing)
                )
            }
            
            val exitTransition = when (direction) {
                SlideDirection.UP -> slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(duration / 2, easing = FastOutLinearInEasing)
                )
                SlideDirection.DOWN -> slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(duration / 2, easing = FastOutLinearInEasing)
                )
                SlideDirection.LEFT -> slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(duration / 2, easing = FastOutLinearInEasing)
                )
                SlideDirection.RIGHT -> slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(duration / 2, easing = FastOutLinearInEasing)
                )
            }
            
            AnimatedVisibility(
                visible = visible,
                enter = enterTransition + fadeIn(tween(duration)),
                exit = exitTransition + fadeOut(tween(duration / 2))
            ) {
                content()
            }
        } else {
            if (visible) {
                content()
            }
        }
    }

    /**
     * 优化的缩放动画
     */
    @Composable
    fun OptimizedScaleIn(
        visible: Boolean,
        duration: Int = 300,
        content: @Composable () -> Unit
    ) {
        val animationEnabled by animationEnabled.collectAsState()
        
        if (animationEnabled) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(
                        durationMillis = duration,
                        easing = FastOutSlowInEasing
                    )
                ) + fadeIn(tween(duration)),
                exit = scaleOut(
                    targetScale = 0.8f,
                    animationSpec = tween(
                        durationMillis = duration / 2,
                        easing = FastOutLinearInEasing
                    )
                ) + fadeOut(tween(duration / 2))
            ) {
                content()
            }
        } else {
            if (visible) {
                content()
            }
        }
    }

    /**
     * 带触觉反馈的点击修饰符
     */
    @Composable
    fun Modifier.optimizedClickable(
        onClick: () -> Unit,
        hapticType: HapticFeedbackType = HapticFeedbackType.LongPress
    ): Modifier {
        val hapticFeedback = LocalHapticFeedback.current
        val hapticEnabled by hapticEnabled.collectAsState()
        
        return this.pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    if (hapticEnabled) {
                        hapticFeedback.performHapticFeedback(hapticType)
                    }
                    onClick()
                }
            )
        }
    }

    /**
     * 智能加载状态
     */
    @Composable
    fun SmartLoadingIndicator(
        isLoading: Boolean,
        message: String = "加载中...",
        showProgress: Boolean = true
    ) {
        OptimizedFadeIn(visible = isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (showProgress) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    /**
     * 智能错误提示
     */
    @Composable
    fun SmartErrorMessage(
        error: String?,
        onRetry: (() -> Unit)? = null,
        onDismiss: () -> Unit = {}
    ) {
        OptimizedSlideIn(visible = error != null, direction = SlideDirection.DOWN) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    
                    if (onRetry != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("忽略")
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            Button(onClick = onRetry) {
                                Text("重试")
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 功能引导提示
     */
    @Composable
    fun FeatureGuide(
        title: String,
        description: String,
        targetBounds: androidx.compose.ui.geometry.Rect? = null,
        onNext: () -> Unit = {},
        onSkip: () -> Unit = {}
    ) {
        val showGuide by showGuide.collectAsState()
        
        OptimizedFadeIn(visible = showGuide) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        // 阻止背景点击
                    }
            ) {
                // 半透明遮罩
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures { /* 阻止点击 */ }
                        }
                ) {
                    // 引导卡片
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onSkip) {
                                    Text("跳过")
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                Button(onClick = onNext) {
                                    Text("下一步")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 自动消失的提示消息
     */
    @Composable
    fun AutoDismissMessage(
        message: String,
        visible: Boolean,
        duration: Long = 3000L,
        onDismiss: () -> Unit
    ) {
        LaunchedEffect(visible) {
            if (visible) {
                delay(duration)
                onDismiss()
            }
        }
        
        OptimizedSlideIn(visible = visible, direction = SlideDirection.UP) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.inverseOnSurface
                )
            }
        }
    }

    /**
     * 记录用户行为
     */
    fun recordUserAction(action: String, details: Map<String, Any> = emptyMap()) {
        val actionCount = preferences.getInt("action_${action}_count", 0) + 1
        preferences.edit()
            .putInt("action_${action}_count", actionCount)
            .putLong("action_${action}_last_time", System.currentTimeMillis())
            .apply()
    }

    /**
     * 获取用户偏好建议
     */
    fun getUserPreferenceSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        
        // 基于使用频率的建议
        val searchCount = preferences.getInt("action_search_count", 0)
        val favoriteCount = preferences.getInt("action_favorite_count", 0)
        val historyCount = preferences.getInt("action_history_count", 0)
        
        if (searchCount > 10) {
            suggestions.add("您经常使用搜索功能，建议将搜索历史保存更长时间")
        }
        
        if (favoriteCount > 20) {
            suggestions.add("您收藏了很多内容，建议定期整理收藏夹")
        }
        
        if (historyCount > 50) {
            suggestions.add("您的观看历史较多，建议开启自动清理功能")
        }
        
        return suggestions
    }

    /**
     * 滑动方向枚举
     */
    enum class SlideDirection {
        UP, DOWN, LEFT, RIGHT
    }
}
