package top.cywin.onetv.movie.ui.focus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * TV遥控器焦点管理器
 * 专为TV端遥控器操作优化的焦点管理系统
 */
object TVFocusManager {
    
    /**
     * 焦点状态
     */
    enum class FocusState {
        FOCUSED,
        UNFOCUSED,
        PRESSED
    }
    
    /**
     * 遥控器按键处理
     */
    fun handleTVKeyEvent(
        keyEvent: KeyEvent,
        onSelect: () -> Unit = {},
        onBack: () -> Unit = {},
        onMenu: () -> Unit = {},
        onUp: () -> Unit = {},
        onDown: () -> Unit = {},
        onLeft: () -> Unit = {},
        onRight: () -> Unit = {}
    ): Boolean {
        if (keyEvent.type != KeyEventType.KeyDown) return false
        
        return when (keyEvent.key) {
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                onSelect()
                true
            }
            Key.Back, Key.Escape -> {
                onBack()
                true
            }
            Key.Menu -> {
                onMenu()
                true
            }
            Key.DirectionUp -> {
                onUp()
                true
            }
            Key.DirectionDown -> {
                onDown()
                true
            }
            Key.DirectionLeft -> {
                onLeft()
                true
            }
            Key.DirectionRight -> {
                onRight()
                true
            }
            else -> false
        }
    }
}

/**
 * TV焦点修饰符
 * 为组件添加TV端焦点效果
 */
@Composable
fun Modifier.tvFocusable(
    enabled: Boolean = true,
    focusedScale: Float = 1.1f,
    focusedBorderWidth: Int = 3,
    focusedBorderColor: Color = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor: Color = Color.Transparent,
    onFocusChanged: (Boolean) -> Unit = {},
    onClick: () -> Unit = {}
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    
    return this
        .focusable(enabled)
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            onFocusChanged(focusState.isFocused)
        }
        .onKeyEvent { keyEvent ->
            TVFocusManager.handleTVKeyEvent(
                keyEvent = keyEvent,
                onSelect = {
                    isPressed = true
                    onClick()
                }
            )
        }
        .scale(if (isFocused) focusedScale else 1f)
        .border(
            width = if (isFocused) focusedBorderWidth.dp else 1.dp,
            color = if (isFocused) focusedBorderColor else unfocusedBorderColor,
            shape = RoundedCornerShape(8.dp)
        )
        .background(
            color = when {
                isPressed -> focusedBorderColor.copy(alpha = 0.3f)
                isFocused -> focusedBorderColor.copy(alpha = 0.1f)
                else -> Color.Transparent
            },
            shape = RoundedCornerShape(8.dp)
        )
}

/**
 * TV网格焦点修饰符
 * 专为网格布局优化的焦点导航
 */
@Composable
fun Modifier.tvGridFocusable(
    enabled: Boolean = true,
    gridColumns: Int,
    currentIndex: Int,
    totalItems: Int,
    onFocusChanged: (Boolean) -> Unit = {},
    onNavigate: (direction: FocusDirection) -> Unit = {},
    onClick: () -> Unit = {}
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    
    return this
        .focusable(enabled)
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            onFocusChanged(focusState.isFocused)
        }
        .onKeyEvent { keyEvent ->
            if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
            
            when (keyEvent.key) {
                Key.DirectionCenter, Key.Enter -> {
                    onClick()
                    true
                }
                Key.DirectionUp -> {
                    if (currentIndex >= gridColumns) {
                        focusManager.moveFocus(FocusDirection.Up)
                        onNavigate(FocusDirection.Up)
                    }
                    true
                }
                Key.DirectionDown -> {
                    if (currentIndex < totalItems - gridColumns) {
                        focusManager.moveFocus(FocusDirection.Down)
                        onNavigate(FocusDirection.Down)
                    }
                    true
                }
                Key.DirectionLeft -> {
                    if (currentIndex % gridColumns != 0) {
                        focusManager.moveFocus(FocusDirection.Left)
                        onNavigate(FocusDirection.Left)
                    }
                    true
                }
                Key.DirectionRight -> {
                    if ((currentIndex + 1) % gridColumns != 0 && currentIndex < totalItems - 1) {
                        focusManager.moveFocus(FocusDirection.Right)
                        onNavigate(FocusDirection.Right)
                    }
                    true
                }
                else -> false
            }
        }
        .scale(if (isFocused) 1.05f else 1f)
        .border(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        )
}

/**
 * TV列表焦点修饰符
 * 专为列表布局优化的焦点导航
 */
@Composable
fun Modifier.tvListFocusable(
    enabled: Boolean = true,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    onClick: () -> Unit = {}
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    
    return this
        .focusable(enabled)
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            onFocusChanged(focusState.isFocused)
        }
        .onKeyEvent { keyEvent ->
            if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
            
            when (keyEvent.key) {
                Key.DirectionCenter, Key.Enter -> {
                    onClick()
                    true
                }
                Key.DirectionUp -> {
                    if (!isFirst) {
                        focusManager.moveFocus(FocusDirection.Up)
                    }
                    true
                }
                Key.DirectionDown -> {
                    if (!isLast) {
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                    true
                }
                else -> false
            }
        }
        .background(
            color = if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f) else Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        )
        .border(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        )
}

/**
 * TV播放器控制焦点修饰符
 * 专为播放器控制栏优化的焦点管理
 */
@Composable
fun Modifier.tvPlayerControlFocusable(
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {},
    onSelect: () -> Unit = {},
    onLongPress: () -> Unit = {}
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
    
    return this
        .focusable(enabled)
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            onFocusChanged(focusState.isFocused)
        }
        .onKeyEvent { keyEvent ->
            when {
                keyEvent.type == KeyEventType.KeyDown && 
                (keyEvent.key == Key.DirectionCenter || keyEvent.key == Key.Enter) -> {
                    isPressed = true
                    onSelect()
                    true
                }
                keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Menu -> {
                    onLongPress()
                    true
                }
                else -> false
            }
        }
        .scale(if (isFocused) 1.1f else 1f)
        .background(
            color = when {
                isPressed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                else -> Color.Transparent
            },
            shape = RoundedCornerShape(8.dp)
        )
        .border(
            width = if (isFocused) 2.dp else 0.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else Color.Transparent,
            shape = RoundedCornerShape(8.dp)
        )
}

/**
 * TV搜索焦点修饰符
 * 专为搜索界面优化的焦点管理
 */
@Composable
fun Modifier.tvSearchFocusable(
    enabled: Boolean = true,
    onFocusChanged: (Boolean) -> Unit = {},
    onTextInput: (String) -> Unit = {},
    onSearch: () -> Unit = {},
    onClear: () -> Unit = {}
): Modifier {
    var isFocused by remember { mutableStateOf(false) }
    
    return this
        .focusable(enabled)
        .onFocusChanged { focusState ->
            isFocused = focusState.isFocused
            onFocusChanged(focusState.isFocused)
        }
        .onKeyEvent { keyEvent ->
            if (keyEvent.type != KeyEventType.KeyDown) return@onKeyEvent false
            
            when (keyEvent.key) {
                Key.DirectionCenter, Key.Enter -> {
                    onSearch()
                    true
                }
                Key.Back, Key.Delete -> {
                    onClear()
                    true
                }
                else -> {
                    // 处理文本输入
                    val char = keyEvent.utf16CodePoint.toChar()
                    if (char.isLetterOrDigit() || char.isWhitespace()) {
                        onTextInput(char.toString())
                        true
                    } else false
                }
            }
        }
        .background(
            color = if (isFocused) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(8.dp)
        )
        .border(
            width = if (isFocused) 2.dp else 1.dp,
            color = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            shape = RoundedCornerShape(8.dp)
        )
}

/**
 * 焦点请求器组合函数
 */
@Composable
fun rememberTVFocusRequester(): FocusRequester {
    return remember { FocusRequester() }
}

/**
 * 自动焦点效果
 */
@Composable
fun LaunchedEffect.requestInitialFocus(
    focusRequester: FocusRequester,
    delay: Long = 100L
) {
    LaunchedEffect(Unit) {
        delay(delay)
        focusRequester.requestFocus()
    }
}
