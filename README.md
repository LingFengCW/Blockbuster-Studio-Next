# Blockbuster Studio Next（泠瀑 / BBS‑NEXT）

一个基于 **Fabric** 的 Minecraft 26.2 模组，用于在游戏内制作动画与电影（MC 视频 / 动画制作工具）。
A Fabric Minecraft 26.2 mod for creating animations and cinematics inside the game.

本项目派生自 [McHorse 的 BBS mod](https://github.com/mchorse/bbs)（MIT），已移植到 Minecraft 26.2（Fabric /  Vulkan 渲染后端），由 [LingFengCW（泠沨）](https://github.com/LingFengCW) 维护。

---

## 核心特性 / Features

- 🎬 **时间轴编辑器** — 类 NLE / Premiere 风格的时间轴，支持轨道、关键帧、剪辑拖拽与实时预览
- 🎭 **形态 / 模型系统** — 变身任意实体、模型方块、粒子，以及自定义模型（custom models）
- 🎥 **专业运镜** — 推拉、摇移、轨道、变焦等电影级摄像机控制
- 🎞️ **动作序列（预览动作）** — 将攻击 / 指令 / 脚本 / 形态等离散动作片段拼接成序列并预览播放
- 📜 **动作录制与回放** — 录制玩家动作并作为 Replay 复用
- 🖼️ **HTML 编辑器** — 基于 MCEF（Chromium / CEF）的现代化编辑器叠层，覆盖原生 Dashboard，支持图标化控件、原生风格对话框、悬停高亮
- 🌐 **实时 3D 预览** — 编辑器在原位打开场景绑定的真实存档世界进行预览，编辑器本身不退出
- 🔧 **离线优先** — 完全离线构建与运行，不依赖在线资源

---

## 安装 / Installation

1. 安装 [Minecraft](https://www.minecraft.net/) 26.2
2. 安装 [Fabric Loader](https://fabricmc.net/use/)（最新版，对应 MC 26.2）
3. 安装 [Fabric API](https://modrinth.com/mod/fabric-api) 对应版本
4. 将本模组的 JAR 放入 `.minecraft/versions/<你的版本>/mods/`
5. （可选）安装 Sodium / Iris 以获得更好性能（本项目兼容 Vulkan + Sodium 渲染路径）

> 提示：独占全屏模式下部分原生对话框可能无法置顶，建议使用窗口化 / 无边框模式。

---

## 构建 / Build（⚠️ 必须离线）

本项目使用 **Gradle 9.5.1 + Loom SNAPSHOT**，且**必须离线构建**。
在线构建会拉取新的 Loom / MC 26.2 映射，导致大量旧 API 编译错误，整项目损坏。

环境要求 / Requirements:

- **JDK 26**（项目已适配，非 JDK 21）
- 离线 Gradle 9.5.1（已随仓库提供，见 `gradle-9.5.1`）

构建命令（离线，严禁联网）：

```bash
env -i PATH="/d/Program Files/Java/jdk-26.0.1/bin:/usr/bin:/bin" \
  HOME=/c/Users/<用户名> GRADLE_USER_HOME=/c/Users/<用户名>/.gradle \
  USERPROFILE=/c/Users/<用户名> TEMP=/c/Users/<用户名>/AppData/Local/Temp \
  TMP=/c/Users/<用户名>/AppData/Local/Temp \
  java -jar "/d/DEV/gradle-9.5.1/lib/gradle-gradle-cli-main-9.5.1.jar" \
  -p /d/DEV/bbs_clean build -x test --offline
```

也可以使用仓库内的封装脚本 `tools/offline_build.sh`。构建产物位于 `build/libs/bbs-next-<version>-26.2.jar`。

> 不要使用 `./gradlew build`（会触发在线解析 / 重映射并失败）。本项目使用离线 Gradle 直接调用。

### 部署 / Deploy

1. 删除 `versions/<版本>/mods/bbs-next-*.jar`
2. 拷贝 `build/libs/` 中的新 JAR（以实际文件名为准）
3. 删除 CEF 缓存 `versions/<版本>/bbs_editor/`，让编辑器重新解包

---

## 官网与教程 / Website & Docs

静态官网与中文教程位于 `docs/`（GitHub Pages 源目录，无需构建）：

- 首页：`docs/index.html`
- 教程：`docs/tutorial.html`

本地预览：`python -m http.server 8080` 后访问 `http://localhost:8080/docs/`。

---

## 许可证 / License

本项目采用**双许可证**结构（混合 MIT 原作 + PNC 二次分发），详见 [`NOTICE.md`](NOTICE.md)：

- **原作（McHorse 的 BBS mod）未修改或轻度修改（≤50% 有效源码行）部分**：MIT — 见 [`LICENSE-MIT`](LICENSE-MIT)
- **新增代码与全部修改（包 `lingfeng.bbsnext.*` 及改动 >50% 的文件）**：PNC（Public‑Not‑Commercial，公开‑非商业）— 见 [`LICENSE`](LICENSE)（英文）/ [`LICENSE.zh-CN`](LICENSE.zh-CN)

PNC 关键条款摘要：
- 闭源分发须在 README / 关于页 / 更新日志显著标注原作者（McHorse）；
- 整体聚合比例 > 30% 时不得用于任何商业行为；
- 修改后的衍生二进制未经原作者书面许可不得公开渠道分发（本地编译 / 自用除外）。

比率计算器（履行 PNC §5‑A）位于 [`tools/pnc_ratio.py`](tools/pnc_ratio.py)。

---

## 链接 / Links

- [GitHub](https://github.com/LingFengCW/Blockbuster-Studio-Next)
- [上游原版 Upstream (MIT)](https://github.com/mchorse/bbs)
