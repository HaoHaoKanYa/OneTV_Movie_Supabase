@echo off
REM OneTV Film Native Code Verification Script
REM 
REM Verifies the completeness of C++ bridge files
REM
REM @author OneTV Team
REM @since 2025-07-13

setlocal enabledelayedexpansion

echo === OneTV Film Native Code Verification ===
echo Verification time: %date% %time%
echo.

set "CPP_DIR=%~dp0"
set "ALL_PASSED=true"

echo Checking required files...

REM Check CMakeLists.txt
if exist "%CPP_DIR%CMakeLists.txt" (
    echo [32m✓ CMakeLists.txt found[0m
) else (
    echo [31m✗ CMakeLists.txt missing[0m
    set "ALL_PASSED=false"
)

REM Check quickjs-android.cpp
if exist "%CPP_DIR%quickjs-android.cpp" (
    echo [32m✓ quickjs-android.cpp found[0m
) else (
    echo [31m✗ quickjs-android.cpp missing[0m
    set "ALL_PASSED=false"
)

REM Check jsoup-bridge.cpp
if exist "%CPP_DIR%jsoup-bridge.cpp" (
    echo [32m✓ jsoup-bridge.cpp found[0m
) else (
    echo [31m✗ jsoup-bridge.cpp missing[0m
    set "ALL_PASSED=false"
)

REM Check http-bridge.cpp
if exist "%CPP_DIR%http-bridge.cpp" (
    echo [32m✓ http-bridge.cpp found[0m
) else (
    echo [31m✗ http-bridge.cpp missing[0m
    set "ALL_PASSED=false"
)

REM Check spider-bridge.cpp
if exist "%CPP_DIR%spider-bridge.cpp" (
    echo [32m✓ spider-bridge.cpp found[0m
) else (
    echo [31m✗ spider-bridge.cpp missing[0m
    set "ALL_PASSED=false"
)

REM Check setup-deps.sh
if exist "%CPP_DIR%setup-deps.sh" (
    echo [32m✓ setup-deps.sh found[0m
) else (
    echo [31m✗ setup-deps.sh missing[0m
    set "ALL_PASSED=false"
)

REM Check build-test.sh
if exist "%CPP_DIR%build-test.sh" (
    echo [32m✓ build-test.sh found[0m
) else (
    echo [31m✗ build-test.sh missing[0m
    set "ALL_PASSED=false"
)

echo.
echo Checking file contents...

REM Check CMakeLists.txt content
findstr /C:"add_library(film-native" "%CPP_DIR%CMakeLists.txt" >nul 2>&1
if !errorlevel! equ 0 (
    echo [32m✓ CMakeLists.txt contains library definition[0m
) else (
    echo [31m✗ CMakeLists.txt missing library definition[0m
    set "ALL_PASSED=false"
)

findstr /C:"quickjs-android.cpp" "%CPP_DIR%CMakeLists.txt" >nul 2>&1
if !errorlevel! equ 0 (
    echo [32m✓ CMakeLists.txt includes QuickJS source[0m
) else (
    echo [31m✗ CMakeLists.txt missing QuickJS source[0m
    set "ALL_PASSED=false"
)

findstr /C:"jsoup-bridge.cpp" "%CPP_DIR%CMakeLists.txt" >nul 2>&1
if !errorlevel! equ 0 (
    echo [32m✓ CMakeLists.txt includes Jsoup bridge[0m
) else (
    echo [31m✗ CMakeLists.txt missing Jsoup bridge[0m
    set "ALL_PASSED=false"
)

findstr /C:"http-bridge.cpp" "%CPP_DIR%CMakeLists.txt" >nul 2>&1
if !errorlevel! equ 0 (
    echo [32m✓ CMakeLists.txt includes HTTP bridge[0m
) else (
    echo [31m✗ CMakeLists.txt missing HTTP bridge[0m
    set "ALL_PASSED=false"
)

findstr /C:"spider-bridge.cpp" "%CPP_DIR%CMakeLists.txt" >nul 2>&1
if !errorlevel! equ 0 (
    echo [32m✓ CMakeLists.txt includes Spider bridge[0m
) else (
    echo [31m✗ CMakeLists.txt missing Spider bridge[0m
    set "ALL_PASSED=false"
)

REM Check C++ files for basic structure
findstr /C:"#include <jni.h>" "%CPP_DIR%quickjs-android.cpp" >nul 2>&1
if !errorlevel! equ 0 (
    echo [32m✓ QuickJS bridge includes JNI header[0m
) else (
    echo [31m✗ QuickJS bridge missing JNI header[0m
    set "ALL_PASSED=false"
)

findstr /C:"extern \"C\"" "%CPP_DIR%jsoup-bridge.cpp" >nul 2>&1
if !errorlevel! equ 0 (
    echo [32m✓ Jsoup bridge includes C exports[0m
) else (
    echo [31m✗ Jsoup bridge missing C exports[0m
    set "ALL_PASSED=false"
)

findstr /C:"JNIEXPORT" "%CPP_DIR%http-bridge.cpp" >nul 2>&1
if !errorlevel! equ 0 (
    echo [32m✓ HTTP bridge includes JNI exports[0m
) else (
    echo [31m✗ HTTP bridge missing JNI exports[0m
    set "ALL_PASSED=false"
)

findstr /C:"Java_" "%CPP_DIR%spider-bridge.cpp" >nul 2>&1
if !errorlevel! equ 0 (
    echo [32m✓ Spider bridge includes native methods[0m
) else (
    echo [31m✗ Spider bridge missing native methods[0m
    set "ALL_PASSED=false"
)

echo.
echo Checking dependencies...

if exist "%CPP_DIR%quickjs" (
    echo [32m✓ QuickJS source directory exists[0m
) else (
    echo [33m⚠ QuickJS source directory missing, run setup-deps.sh[0m
)

if exist "%CPP_DIR%curl" (
    echo [32m✓ libcurl directory exists[0m
) else (
    echo [33m⚠ libcurl directory missing, HTTP features will be disabled[0m
)

echo.
echo Generating report...

set "REPORT_FILE=%CPP_DIR%verification-report.txt"
echo OneTV Film Native Code Verification Report > "%REPORT_FILE%"
echo Generated: %date% %time% >> "%REPORT_FILE%"
echo. >> "%REPORT_FILE%"
echo === File Status === >> "%REPORT_FILE%"

for %%f in (CMakeLists.txt quickjs-android.cpp jsoup-bridge.cpp http-bridge.cpp spider-bridge.cpp setup-deps.sh build-test.sh) do (
    if exist "%CPP_DIR%%%f" (
        echo ✓ %%f found >> "%REPORT_FILE%"
    ) else (
        echo ✗ %%f missing >> "%REPORT_FILE%"
    )
)

echo. >> "%REPORT_FILE%"
echo === Dependencies === >> "%REPORT_FILE%"

if exist "%CPP_DIR%quickjs" (
    echo QuickJS: Installed >> "%REPORT_FILE%"
) else (
    echo QuickJS: Not installed >> "%REPORT_FILE%"
)

if exist "%CPP_DIR%curl" (
    echo libcurl: Installed >> "%REPORT_FILE%"
) else (
    echo libcurl: Not installed >> "%REPORT_FILE%"
)

echo. >> "%REPORT_FILE%"
echo === Verification Result === >> "%REPORT_FILE%"

if "%ALL_PASSED%"=="true" (
    echo Status: PASSED >> "%REPORT_FILE%"
) else (
    echo Status: FAILED >> "%REPORT_FILE%"
)

echo [36mReport generated: %REPORT_FILE%[0m

echo.

if "%ALL_PASSED%"=="true" (
    echo [32m🎉 Verification PASSED![0m
    echo [32mNative code files are complete and ready for Android build[0m
    exit /b 0
) else (
    echo [31m❌ Verification FAILED[0m
    echo [31mPlease fix the issues above[0m
    exit /b 1
)

echo.
echo Tips:
echo - Run setup-deps.sh in Git Bash or WSL to install dependencies
echo - Build the project in Android Studio to test compilation
echo - Check verification-report.txt for detailed information
