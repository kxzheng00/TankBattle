#!/usr/bin/env bash
# ============================================================
#  TankBattle 一键打包脚本（Git Bash / macOS / Linux）
#  用法：
#    ./build-apk.sh                 打 release 包（已签名），产物复制到 dist/
#    ./build-apk.sh assembleDebug   打 debug 包
#    ./build-apk.sh clean           清理
#  这个脚本替你兜住本机的三个坑：
#    1) 默认 JAVA_HOME 是 jdk-1.8，AGP 8.x 要求 JDK 17+，这里自动挑 JDK 21
#    2) 系统全局 CLASSPATH 指向 jdk1.8 的 tools.jar，会让高版本 JVM 启动失败，这里清掉
#    3) mavenCentral 域名在本机网络不可达，仓库已在 build.gradle 里换成镜像
# ============================================================
set -uo pipefail
cd "$(dirname "$0")" || exit 1

# 坑 2：清掉全局 CLASSPATH
unset CLASSPATH

# 坑 1：挑一个 JDK 17+
java_major() {
  local home="$1" bin version
  [ -n "$home" ] || return 1
  if [ -x "$home/bin/java" ]; then bin="$home/bin/java"
  elif [ -x "$home/bin/java.exe" ]; then bin="$home/bin/java.exe"
  else return 1; fi
  version=$("$bin" -version 2>&1 | head -1 | sed -E 's/.*version "([0-9]+)[^"]*".*/\1/')
  case "$version" in ''|*[!0-9]*) return 1 ;; esac
  [ "$version" -ge 17 ] 2>/dev/null
}

JDK=""
for c in "${JAVA_HOME_21:-}" "E:/Java/dragonwell-21.0.5.0.5" \
         "/c/Program Files/Android/Android Studio/jbr" \
         "/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
         "${JAVA_HOME:-}"; do
  if java_major "$c"; then JDK="$c"; break; fi
done

if [ -z "$JDK" ]; then
  echo "[错误] 没找到 JDK 17 及以上版本。"
  echo "       请安装 JDK 17+ 后设置 JAVA_HOME_21，或修改本脚本里的候选路径。"
  exit 1
fi

export JAVA_HOME="$JDK"
export PATH="$JAVA_HOME/bin:$PATH"

TASK="${1:-assembleRelease}"
echo "[信息] JDK  : $JAVA_HOME"
echo "[信息] 任务 : $TASK"
echo

./gradlew "$TASK" --console=plain
if [ $? -ne 0 ]; then
  echo
  echo "[失败] 构建中止"
  exit 1
fi

case "$TASK" in
  assembleRelease) SRC="app/build/outputs/apk/release/app-release.apk"; SUFFIX="release" ;;
  assembleDebug)   SRC="app/build/outputs/apk/debug/app-debug.apk";      SUFFIX="debug"   ;;
  *) exit 0 ;;
esac

VER=$(grep -m1 -oE 'versionName[[:space:]]+"[^"]+"' app/build.gradle | sed -E 's/.*"([^"]+)".*/\1/')
VER=${VER:-1.0}
DST="dist/TankBattle-v${VER}-${SUFFIX}.apk"

if [ ! -f "$SRC" ]; then
  echo "[警告] 没找到 $SRC"
  exit 1
fi
mkdir -p dist
cp -f "$SRC" "$DST"
echo
echo "[完成] $DST  ($(( $(wc -c < "$DST") / 1024 )) KB)"
