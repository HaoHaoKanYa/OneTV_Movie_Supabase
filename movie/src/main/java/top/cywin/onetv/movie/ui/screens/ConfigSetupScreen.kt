package top.cywin.onetv.movie.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import top.cywin.onetv.movie.ui.focus.tvFocusable
import top.cywin.onetv.movie.viewmodel.ConfigSetupViewModel

/**
 * 配置设置界面
 * 用于首次设置或更新服务器配置信息
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigSetupScreen(
    onConfigComplete: () -> Unit,
    onSkip: (() -> Unit)? = null,
    viewModel: ConfigSetupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    var projectUrl by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var showApiKey by remember { mutableStateOf(false) }
    
    val urlFocusRequester = remember { FocusRequester() }
    val keyFocusRequester = remember { FocusRequester() }
    
    LaunchedEffect(Unit) {
        // 检查是否已有配置
        if (viewModel.hasExistingConfig()) {
            val config = viewModel.getExistingConfig()
            projectUrl = config.first
            apiKey = config.second
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 标题和说明
        Icon(
            imageVector = Icons.Default.Settings,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "服务器配置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "请输入您的Supabase项目信息",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 配置表单
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 项目URL输入
                OutlinedTextField(
                    value = projectUrl,
                    onValueChange = { projectUrl = it },
                    label = { Text("项目URL") },
                    placeholder = { Text("https://your-project.supabase.co") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(urlFocusRequester)
                        .tvFocusable(),
                    singleLine = true
                )
                
                // API Key输入
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("API Key") },
                    placeholder = { Text("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { showApiKey = !showApiKey },
                            modifier = Modifier.tvFocusable(
                                onClick = { showApiKey = !showApiKey }
                            )
                        ) {
                            Icon(
                                imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showApiKey) "隐藏" else "显示"
                            )
                        }
                    },
                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(keyFocusRequester)
                        .tvFocusable(),
                    singleLine = true
                )
                
                // 帮助信息
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Column {
                            Text(
                                text = "如何获取配置信息？",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            
                            Text(
                                text = "1. 登录您的Supabase控制台\n2. 在项目设置中找到API配置\n3. 复制项目URL和anon public key",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 错误信息
        if (uiState.error != null) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = uiState.error ?: "未知错误",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 跳过按钮（如果允许）
            if (onSkip != null) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusable(onClick = onSkip)
                ) {
                    Text("跳过")
                }
            }
            
            // 保存按钮
            Button(
                onClick = {
                    viewModel.saveConfig(
                        projectUrl = projectUrl.trim(),
                        apiKey = apiKey.trim(),
                        onSuccess = onConfigComplete
                    )
                },
                enabled = projectUrl.isNotBlank() && apiKey.isNotBlank() && !uiState.isLoading,
                modifier = Modifier
                    .weight(if (onSkip != null) 1f else 2f)
                    .tvFocusable(
                        onClick = {
                            viewModel.saveConfig(
                                projectUrl = projectUrl.trim(),
                                apiKey = apiKey.trim(),
                                onSuccess = onConfigComplete
                            )
                        }
                    )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("保存配置")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 测试连接按钮
        TextButton(
            onClick = {
                viewModel.testConnection(
                    projectUrl = projectUrl.trim(),
                    apiKey = apiKey.trim()
                )
            },
            enabled = projectUrl.isNotBlank() && apiKey.isNotBlank() && !uiState.isLoading,
            modifier = Modifier.tvFocusable(
                onClick = {
                    viewModel.testConnection(
                        projectUrl = projectUrl.trim(),
                        apiKey = apiKey.trim()
                    )
                }
            )
        ) {
            Icon(
                imageVector = Icons.Default.NetworkCheck,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("测试连接")
        }
    }
    
    // 请求初始焦点
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        urlFocusRequester.requestFocus()
    }
}
