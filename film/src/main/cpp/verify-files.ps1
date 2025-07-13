# OneTV Film 原生代码文件验证脚本
# 
# 用于验证 C++ 桥接文件的完整性
#
# @author OneTV Team
# @since 2025-07-13

param(
    [switch]$Detailed = $false
)

# 颜色定义
$Red = "Red"
$Green = "Green"
$Yellow = "Yellow"
$Blue = "Cyan"

# 函数：打印彩色消息
function Write-ColorMessage {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    Write-Host $Message -ForegroundColor $Color
}

# 函数：检查文件
function Test-FileExists {
    param(
        [string]$FilePath,
        [string]$Description
    )
    
    if (Test-Path $FilePath) {
        $fileInfo = Get-Item $FilePath
        $size = [math]::Round($fileInfo.Length / 1KB, 2)
        Write-ColorMessage "✅ $Description ($size KB)" $Green
        return $true
    } else {
        Write-ColorMessage "❌ 缺少文件: $Description" $Red
        return $false
    }
}

# 函数：检查文件内容
function Test-FileContent {
    param(
        [string]$FilePath,
        [string]$Pattern,
        [string]$Description
    )
    
    if (Test-Path $FilePath) {
        $content = Get-Content $FilePath -Raw
        if ($content -match $Pattern) {
            Write-ColorMessage "✅ $Description" $Green
            return $true
        } else {
            Write-ColorMessage "❌ $Description" $Red
            return $false
        }
    } else {
        Write-ColorMessage "❌ 文件不存在: $FilePath" $Red
        return $false
    }
}

# 主验证函数
function Start-Verification {
    Write-ColorMessage "=== OneTV Film 原生代码文件验证 ===" $Blue
    Write-ColorMessage "验证时间: $(Get-Date)" $Blue
    Write-ColorMessage ""
    
    $cppDir = $PSScriptRoot
    $allPassed = $true
    
    # 检查必要文件
    Write-ColorMessage "检查必要文件..." $Blue
    
    $requiredFiles = @(
        @{ Path = "$cppDir\CMakeLists.txt"; Desc = "CMake 配置文件" },
        @{ Path = "$cppDir\quickjs-android.cpp"; Desc = "QuickJS Android 桥接" },
        @{ Path = "$cppDir\jsoup-bridge.cpp"; Desc = "Jsoup HTML 解析桥接" },
        @{ Path = "$cppDir\http-bridge.cpp"; Desc = "HTTP 请求桥接" },
        @{ Path = "$cppDir\spider-bridge.cpp"; Desc = "Spider 爬虫桥接" },
        @{ Path = "$cppDir\setup-deps.sh"; Desc = "依赖安装脚本" },
        @{ Path = "$cppDir\build-test.sh"; Desc = "构建测试脚本" }
    )
    
    foreach ($file in $requiredFiles) {
        if (-not (Test-FileExists $file.Path $file.Desc)) {
            $allPassed = $false
        }
    }
    
    Write-ColorMessage ""
    
    # 检查文件内容
    Write-ColorMessage "检查文件内容..." $Blue
    
    # 检查 CMakeLists.txt
    if (-not (Test-FileContent "$cppDir\CMakeLists.txt" "add_library\(film-native" "CMakeLists.txt 包含库定义")) {
        $allPassed = $false
    }
    
    if (-not (Test-FileContent "$cppDir\CMakeLists.txt" "quickjs-android\.cpp" "CMakeLists.txt 包含 QuickJS 源文件")) {
        $allPassed = $false
    }
    
    if (-not (Test-FileContent "$cppDir\CMakeLists.txt" "jsoup-bridge\.cpp" "CMakeLists.txt 包含 Jsoup 桥接")) {
        $allPassed = $false
    }
    
    if (-not (Test-FileContent "$cppDir\CMakeLists.txt" "http-bridge\.cpp" "CMakeLists.txt 包含 HTTP 桥接")) {
        $allPassed = $false
    }
    
    if (-not (Test-FileContent "$cppDir\CMakeLists.txt" "spider-bridge\.cpp" "CMakeLists.txt 包含 Spider 桥接")) {
        $allPassed = $false
    }
    
    # 检查 C++ 文件的基本结构
    if (-not (Test-FileContent "$cppDir\quickjs-android.cpp" "#include <jni\.h>" "QuickJS 桥接包含 JNI 头文件")) {
        $allPassed = $false
    }
    
    if (-not (Test-FileContent "$cppDir\jsoup-bridge.cpp" "extern \"C\"" "Jsoup 桥接包含 C 导出")) {
        $allPassed = $false
    }
    
    if (-not (Test-FileContent "$cppDir\http-bridge.cpp" "JNIEXPORT.*JNICALL" "HTTP 桥接包含 JNI 导出")) {
        $allPassed = $false
    }
    
    if (-not (Test-FileContent "$cppDir\spider-bridge.cpp" "Java_.*_native" "Spider 桥接包含原生方法")) {
        $allPassed = $false
    }
    
    Write-ColorMessage ""
    
    # 详细信息
    if ($Detailed) {
        Write-ColorMessage "详细信息..." $Blue
        
        foreach ($file in $requiredFiles) {
            if (Test-Path $file.Path) {
                $fileInfo = Get-Item $file.Path
                $lines = (Get-Content $file.Path).Count
                Write-ColorMessage "📄 $($file.Desc):" $Yellow
                Write-ColorMessage "   路径: $($file.Path)" $Yellow
                Write-ColorMessage "   大小: $([math]::Round($fileInfo.Length / 1KB, 2)) KB" $Yellow
                Write-ColorMessage "   行数: $lines" $Yellow
                Write-ColorMessage "   修改时间: $($fileInfo.LastWriteTime)" $Yellow
                Write-ColorMessage ""
            }
        }
    }
    
    # 检查依赖目录
    Write-ColorMessage "检查依赖目录..." $Blue
    
    if (Test-Path "$cppDir\quickjs") {
        Write-ColorMessage "✅ QuickJS 源码目录存在" $Green
    } else {
        Write-ColorMessage "⚠️ QuickJS 源码目录不存在，运行 setup-deps.sh 安装" $Yellow
    }
    
    if (Test-Path "$cppDir\curl") {
        Write-ColorMessage "✅ libcurl 目录存在" $Green
    } else {
        Write-ColorMessage "⚠️ libcurl 目录不存在，HTTP 功能将被禁用" $Yellow
    }
    
    Write-ColorMessage ""
    
    # 生成报告
    $reportPath = "$cppDir\verification-report.txt"
    $report = @"
OneTV Film 原生代码验证报告
生成时间: $(Get-Date)
验证脚本: $($MyInvocation.MyCommand.Path)

=== 文件状态 ===
"@
    
    foreach ($file in $requiredFiles) {
        if (Test-Path $file.Path) {
            $fileInfo = Get-Item $file.Path
            $report += "`n✅ $($file.Desc) ($([math]::Round($fileInfo.Length / 1KB, 2)) KB)"
        } else {
            $report += "`n❌ $($file.Desc) (缺失)"
        }
    }
    
    $report += @"

=== 依赖状态 ===
QuickJS: $(if (Test-Path "$cppDir\quickjs") { "已安装" } else { "未安装" })
libcurl: $(if (Test-Path "$cppDir\curl") { "已安装" } else { "未安装" })

=== 验证结果 ===
总体状态: $(if ($allPassed) { "通过" } else { "失败" })
"@
    
    $report | Out-File -FilePath $reportPath -Encoding UTF8
    Write-ColorMessage "📊 验证报告已生成: $reportPath" $Blue
    
    Write-ColorMessage ""
    
    # 最终结果
    if ($allPassed) {
        Write-ColorMessage "🎉 验证通过！" $Green
        Write-ColorMessage "原生代码文件完整，可以进行 Android 构建" $Green
    } else {
        Write-ColorMessage "❌ 验证失败" $Red
        Write-ColorMessage "请检查上述问题并修复" $Red
    }
    
    Write-ColorMessage ""
    Write-ColorMessage "提示:" $Blue
    Write-ColorMessage "- 在 Git Bash 或 WSL 中运行 ./setup-deps.sh 安装依赖" $Blue
    Write-ColorMessage "- 在 Android Studio 中构建项目测试编译" $Blue
    Write-ColorMessage "- 使用 -Detailed 参数查看详细信息" $Blue
    
    return $allPassed
}

# 运行验证
$result = Start-Verification
if (-not $result) {
    exit 1
}
