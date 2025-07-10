package top.cywin.onetv.movie.codegen

import java.io.File

/**
 * 代码质量验证器
 * 验证生成代码的质量和规范性
 */
object CodeQualityValidator {
    
    fun validate(outputDir: File): CodeQualityReport {
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val suggestions = mutableListOf<String>()
        
        val kotlinFiles = outputDir.walkTopDown()
            .filter { it.extension == "kt" }
            .toList()
        
        kotlinFiles.forEach { file ->
            validateFile(file, warnings, errors, suggestions)
        }
        
        return CodeQualityReport(
            totalFiles = kotlinFiles.size,
            warnings = warnings,
            errors = errors,
            suggestions = suggestions
        )
    }
    
    private fun validateFile(
        file: File, 
        warnings: MutableList<String>, 
        errors: MutableList<String>, 
        suggestions: MutableList<String>
    ) {
        val content = file.readText()
        
        // 检查命名规范
        if (!file.nameWithoutExtension.matches(Regex("[A-Z][a-zA-Z0-9]*"))) {
            warnings.add("文件名不符合命名规范: ${file.name}")
        }
        
        // 检查必要的导入
        if (!content.contains("import android.util.Log")) {
            suggestions.add("建议添加Log导入: ${file.name}")
        }
        
        // 检查错误处理
        if (content.contains("try {") && !content.contains("catch")) {
            errors.add("缺少异常处理: ${file.name}")
        }
    }
}
