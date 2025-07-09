package top.cywin.onetv.movie.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.util.regex.Pattern

/**
 * 安全检查工具
 * 用于检查代码中是否存在硬编码的敏感信息
 */
object SecurityChecker {
    
    private const val TAG = "SecurityChecker"
    
    // 敏感信息模式
    private val SENSITIVE_PATTERNS = listOf(
        // Supabase URL模式
        Pattern.compile("https://[a-zA-Z0-9-]+\\.supabase\\.co", Pattern.CASE_INSENSITIVE),
        
        // JWT Token模式
        Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"),
        
        // API Key模式
        Pattern.compile("sbp_[a-zA-Z0-9]{40,}"),
        
        // 其他可能的敏感信息
        Pattern.compile("password\\s*=\\s*[\"'][^\"']+[\"']", Pattern.CASE_INSENSITIVE),
        Pattern.compile("secret\\s*=\\s*[\"'][^\"']+[\"']", Pattern.CASE_INSENSITIVE),
        Pattern.compile("token\\s*=\\s*[\"'][^\"']+[\"']", Pattern.CASE_INSENSITIVE)
    )
    
    // 允许的例外（模板、示例等）
    private val ALLOWED_EXCEPTIONS = listOf(
        "your-project.supabase.co",
        "your-anon-key-here",
        "demo_token",
        "example.com",
        "template",
        "placeholder"
    )
    
    /**
     * 检查单个文件是否包含硬编码敏感信息
     */
    fun checkFile(file: File): List<SecurityIssue> {
        val issues = mutableListOf<SecurityIssue>()
        
        if (!file.exists() || !file.isFile || !file.canRead()) {
            return issues
        }
        
        try {
            val content = file.readText()
            val lines = content.lines()
            
            lines.forEachIndexed { lineIndex, line ->
                SENSITIVE_PATTERNS.forEach { pattern ->
                    val matcher = pattern.matcher(line)
                    while (matcher.find()) {
                        val match = matcher.group()
                        
                        // 检查是否是允许的例外
                        if (!isAllowedException(match)) {
                            issues.add(
                                SecurityIssue(
                                    file = file,
                                    line = lineIndex + 1,
                                    content = line.trim(),
                                    match = match,
                                    type = detectIssueType(match),
                                    severity = SecuritySeverity.HIGH
                                )
                            )
                        }
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "检查文件失败: ${file.absolutePath}", e)
        }
        
        return issues
    }
    
    /**
     * 检查目录下的所有Kotlin文件
     */
    fun checkDirectory(directory: File): List<SecurityIssue> {
        val issues = mutableListOf<SecurityIssue>()
        
        if (!directory.exists() || !directory.isDirectory) {
            return issues
        }
        
        directory.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .forEach { file ->
                issues.addAll(checkFile(file))
            }
        
        return issues
    }
    
    /**
     * 检查项目的movie模块
     */
    fun checkMovieModule(context: Context): SecurityCheckResult {
        val issues = mutableListOf<SecurityIssue>()
        
        try {
            // 获取源代码目录（这在实际应用中可能无法访问）
            // 这里主要用于开发时的安全检查
            val sourceDir = File(context.applicationInfo.sourceDir)
            val parentDir = sourceDir.parentFile?.parentFile
            
            if (parentDir != null) {
                val movieDir = File(parentDir, "movie/src/main/java")
                if (movieDir.exists()) {
                    issues.addAll(checkDirectory(movieDir))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "检查movie模块失败", e)
        }
        
        return SecurityCheckResult(
            totalFiles = 0, // 实际应用中无法准确统计
            issuesFound = issues.size,
            issues = issues,
            isSecure = issues.isEmpty()
        )
    }
    
    /**
     * 检查是否是允许的例外
     */
    private fun isAllowedException(match: String): Boolean {
        return ALLOWED_EXCEPTIONS.any { exception ->
            match.contains(exception, ignoreCase = true)
        }
    }
    
    /**
     * 检测问题类型
     */
    private fun detectIssueType(match: String): SecurityIssueType {
        return when {
            match.contains("supabase.co") -> SecurityIssueType.SUPABASE_URL
            match.startsWith("eyJ") -> SecurityIssueType.JWT_TOKEN
            match.startsWith("sbp_") -> SecurityIssueType.SERVICE_ROLE_KEY
            match.contains("password", ignoreCase = true) -> SecurityIssueType.PASSWORD
            match.contains("secret", ignoreCase = true) -> SecurityIssueType.SECRET
            match.contains("token", ignoreCase = true) -> SecurityIssueType.TOKEN
            else -> SecurityIssueType.UNKNOWN
        }
    }
    
    /**
     * 生成安全检查报告
     */
    fun generateReport(result: SecurityCheckResult): String {
        val report = StringBuilder()
        
        report.appendLine("=== OneTV 安全检查报告 ===")
        report.appendLine("检查时间: ${java.util.Date()}")
        report.appendLine("检查结果: ${if (result.isSecure) "✅ 安全" else "❌ 发现安全问题"}")
        report.appendLine("发现问题: ${result.issuesFound} 个")
        report.appendLine()
        
        if (result.issues.isNotEmpty()) {
            report.appendLine("详细问题列表:")
            result.issues.groupBy { it.type }.forEach { (type, issues) ->
                report.appendLine("${type.displayName}:")
                issues.forEach { issue ->
                    report.appendLine("  - 文件: ${issue.file.name}")
                    report.appendLine("    行号: ${issue.line}")
                    report.appendLine("    内容: ${issue.content}")
                    report.appendLine("    匹配: ${issue.match}")
                    report.appendLine()
                }
            }
        }
        
        report.appendLine("安全建议:")
        report.appendLine("1. 所有敏感配置应从app_configs表动态读取")
        report.appendLine("2. 使用环境变量或安全的密钥管理服务")
        report.appendLine("3. 定期轮换API密钥")
        report.appendLine("4. 确保敏感文件不被提交到版本控制")
        
        return report.toString()
    }
}

/**
 * 安全问题数据类
 */
data class SecurityIssue(
    val file: File,
    val line: Int,
    val content: String,
    val match: String,
    val type: SecurityIssueType,
    val severity: SecuritySeverity
)

/**
 * 安全检查结果
 */
data class SecurityCheckResult(
    val totalFiles: Int,
    val issuesFound: Int,
    val issues: List<SecurityIssue>,
    val isSecure: Boolean
)

/**
 * 安全问题类型
 */
enum class SecurityIssueType(val displayName: String) {
    SUPABASE_URL("Supabase URL"),
    JWT_TOKEN("JWT Token"),
    SERVICE_ROLE_KEY("Service Role Key"),
    PASSWORD("密码"),
    SECRET("密钥"),
    TOKEN("令牌"),
    UNKNOWN("未知")
}

/**
 * 安全问题严重程度
 */
enum class SecuritySeverity(val displayName: String) {
    LOW("低"),
    MEDIUM("中"),
    HIGH("高"),
    CRITICAL("严重")
}
