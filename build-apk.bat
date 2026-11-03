@echo off
rem ============================================================
rem  TankBattle 一键打包脚本（Windows）
rem  用法：
rem    build-apk.bat                 -> 打 release 包（已签名），产物复制到 dist\
rem    build-apk.bat assembleDebug   -> 打 debug 包
rem    build-apk.bat clean           -> 清理
rem  这个脚本替你兜住本机的两个坑：
rem    1) 系统全局 CLASSPATH 指向 jdk1.8 的 tools.jar，会让构建 JVM 启动失败，这里清掉
rem    2) 默认 JAVA_HOME 是 jdk-1.8，AGP 8.x 要 JDK 17+；
rem       真正生效的 JDK 已在 gradle.properties 的 org.gradle.java.home 里钉到 JDK 21，
rem       所以这里只要 Gradle 启动器能跑就行（jdk1.8 启动器 + JDK21 daemon 已验证可用）。
rem       如果装了 JDK 21 并设置了 JAVA_HOME_21，本脚本会优先用它当启动器。
rem ============================================================
setlocal enabledelayedexpansion
cd /d "%~dp0"

set "CLASSPATH="

if defined JAVA_HOME_21 set "JAVA_HOME=%JAVA_HOME_21%"

set "TASK=%~1"
if "%TASK%"=="" set "TASK=assembleRelease"

echo [信息] 启动 JVM : %JAVA_HOME%
echo [信息] 构建任务 : %TASK%
echo.

call "%~dp0gradlew.bat" %TASK% --console=plain
if errorlevel 1 goto :failed

if /i "%TASK%"=="assembleRelease" goto :copy_release
if /i "%TASK%"=="assembleDebug" goto :copy_debug
exit /b 0

:copy_release
set "SRC=app\build\outputs\apk\release\app-release.apk"
set "SUFFIX=release"
goto :do_copy

:copy_debug
set "SRC=app\build\outputs\apk\debug\app-debug.apk"
set "SUFFIX=debug"
goto :do_copy

:do_copy
set "VER=1.0"
for /f "tokens=2" %%v in ('findstr versionName app\build.gradle') do set "VER=%%~v"
if not exist "dist" mkdir "dist"
set "DST=dist\TankBattle-v!VER!-!SUFFIX!.apk"
if not exist "!SRC!" (
  echo [警告] 没找到 !SRC!
  exit /b 1
)
copy /y "!SRC!" "!DST!" >nul
echo.
echo [完成] !DST!
exit /b 0

:failed
echo.
echo [失败] 构建中止
exit /b 1
