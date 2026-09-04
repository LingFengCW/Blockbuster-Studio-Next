#!/usr/bin/env bash
set -e

# 解析当前 Windows 用户名（不硬编码本机路径，避免暴露真实身份）
WINUSER="${USERNAME:-$(whoami)}"

echo "== offline build start =="

env -i \
  PATH="/d/Program Files/Java/jdk-26.0.1/bin:/usr/bin:/bin" \
  HOME="/c/Users/$WINUSER" \
  GRADLE_USER_HOME="/c/Users/$WINUSER/.gradle" \
  USERPROFILE="/c/Users/$WINUSER" \
  TEMP="/c/Users/$WINUSER/AppData/Local/Temp" \
  TMP="/c/Users/$WINUSER/AppData/Local/Temp" \
  java -jar /d/DEV/gradle-9.5.1/lib/gradle-gradle-cli-main-9.5.1.jar build -x test --offline

echo "== offline build done =="
echo "build/libs:"
ls -1 build/libs
