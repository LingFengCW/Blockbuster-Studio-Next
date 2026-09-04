#!/usr/bin/env bash
# 离线构建并部署到 luzhi 实例（每次打包后调用）
# 用法：bash tools/build_deploy_luzhi.sh
set -uo pipefail

MODS="/d/E/MC/.minecraft/versions/luzhi/mods"
CACHE="/d/E/MC/.minecraft/versions/luzhi/bbs_editor"
PROJECT="/d/DEV/bbs_clean"
GRADLE_JAR="/d/DEV/gradle-9.5.1/lib/gradle-gradle-cli-main-9.5.1.jar"

# 动态取用户主目录，避免硬编码本机路径
HOME_DIR=$(cd ~ && pwd)

cd "$PROJECT" || { echo "!! 无法进入 $PROJECT"; exit 1; }

echo "==> 离线构建（绝不可在线：Loom SNAPSHOT）"
env -i PATH="/d/Program Files/Java/jdk-26.0.1/bin:/usr/bin:/bin" \
  HOME="$HOME_DIR" GRADLE_USER_HOME="$HOME_DIR/.gradle" \
  USERPROFILE="$HOME_DIR" TEMP="$HOME_DIR/AppData/Local/Temp" \
  TMP="$HOME_DIR/AppData/Local/Temp" \
  java -jar "$GRADLE_JAR" -p "$PROJECT" build -x test --offline
BUILD_EXIT=$?
if [ "$BUILD_EXIT" -ne 0 ]; then echo "!! 构建失败 exit=$BUILD_EXIT"; exit "$BUILD_EXIT"; fi
echo "==> 构建成功"

# 取最新主 jar（排除 -sources.jar，sources 不能进 mods 否则 Fabric 当重复 mod 加载）
NEWJAR=$(ls -t build/libs/bbs-next-*-26.2.jar 2>/dev/null | grep -v sources | head -1 || true)
if [ -z "$NEWJAR" ]; then echo "!! 未找到构建产物"; exit 1; fi
echo "==> 新 jar: $(basename "$NEWJAR")"

# 安全删除 mods 内所有 bbs-next jar（用 find，避免引号包通配符不展开 / 参数过长）
echo "==> 清理 mods 内旧 bbs-next jar"
find "$MODS" -maxdepth 1 -name 'bbs-next-*.jar' -delete

echo "==> 部署主 jar（不含 sources）"
cp "$NEWJAR" "$MODS/"

echo "==> 清除 CEF 编辑器缓存（强制重新 extract）"
rm -rf "$CACHE"

echo "==> 部署完成"
ls -la "$MODS" | grep bbs-next
