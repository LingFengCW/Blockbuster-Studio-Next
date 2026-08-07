# Blockbuster Studio Next（泠瀑）

一个基于 Fabric 的 Minecraft 模组，用于在 Minecraft 中制作动画和电影。  
A Minecraft mod for Fabric that allows creating animations and cinematics within Minecraft.

继承自 [McHorse 的 BBS mod](https://github.com/mchorse/bbs)，已迁移至 Minecraft 26.2（Fabric），由 [LingFengCW](https://github.com/LingFengCW) 维护（曾用名 lemonungood）。

## 功能 / Features

- 🎬 **角色动画** - 关键帧动画系统，支持路径、旋转、缩放、轨迹等
- 🎭 **形态系统** - 变身成任意实体、模型方块、粒子效果等
- 🎥 **摄像机控制** - 推拉、摇移、轨道、变焦等专业电影级运镜
- 📜 **动作录制** - 录制并回放玩家动作
- 🔊 **音频字幕** - 支持音频剪辑和字幕显示
- 🖼️ **模型方块** - 导入展示自定义模型
- 🔫 **自定义物品** - 枪械等交互物品
- 🌐 **局域网联机** - 支持多人协作制作
- 🖥️ **HTML 编辑器** - 基于 MCEF（Chromium / CEF）的现代化时间轴 / 属性编辑器，支持鼠标悬停高亮与现代化原生新建对话框

## 安装 / Installation

1. 安装 [Minecraft](https://www.minecraft.net/) 26.2
2. 安装 [Fabric Loader](https://fabricmc.net/use/)（最新版，对应 MC 26.2）
3. 安装 [Fabric API](https://modrinth.com/mod/fabric-api) 对应版本
4. 将本模组的 JAR 放入 `.minecraft/versions/<你的版本>/mods/`（或对应版本的 mods 目录）
5. （可选）安装 Sodium / Iris 以获得更好性能

> 提示：若使用独占全屏（exclusive fullscreen）模式，部分原生对话框可能无法置顶显示，建议使用窗口化 / 无边框模式。

## 构建 / Build

环境要求 / Requirements:

- JDK 21+
- 本项目使用 Gradle Wrapper，无需手动安装 Gradle

```bash
# 生成可运行的 mod jar
# （MC 26.2 为非混淆环境，使用 productionNamespace = 'official'，无需混淆映射）
./gradlew jar
```

构建产物位于 `build/libs/bbs-next-<version>-26.2.jar`，将其放入 mods 目录即可。

> 注意：不要使用 `./gradlew build` 或 `remapJar`。本项目未声明混淆映射，这些任务不存在或会失败，请使用 `./gradlew jar`。

## 许可证 / License

MIT - 详见 [LICENSE.md](LICENSE.md)

## 链接 / Links

- [GitHub](https://github.com/LingFengCW/Blockbuster-Studio-Next)
- [上游原版 Upstream](https://github.com/mchorse/bbs)
