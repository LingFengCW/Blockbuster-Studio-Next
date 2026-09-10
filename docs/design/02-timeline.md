# 时间轴专业特性（Timeline）

## 基准：专业软件怎么做

联网调研要点（Premiere / OpenShot / DaVinci）：

- **吸附（Snapping）**：Premiere 时间轴左上「Snap」按钮，靠近 clip/标记/端点时自动对齐，可开关。
- **缩放**：Premiere `+`/`-` 与 `\` 适配；OpenShot `Ctrl+滚轮` 缩放、底部导航条。
- **标记（Markers）**：OpenShot 定义为「可快速跳转的时间点」；Premiere 标记可上色、加注释，用于 beat/场景切换/待审。
- **波纹编辑（Ripple, B）**：修剪后下游自动补齐空隙。
- **剃刀（Razor, C / Ctrl+K）**：在播放头切断 clip。
- **J-K-L 梭动**：J 倒放、K 停、L 快进。
- **轨道头**：眼睛（显隐）、喇叭（静音）、锁（防误改）。
- **时间码**：HH:MM:SS:FF，状态栏常驻。

## 我们的设计

编辑器时间轴已是多轨（场景/相机/音频 + 角色剪辑段），我们用前端便捷层补齐专业手感：

| 特性 | 交互 | 状态 |
|---|---|---|
| 缩放 | 按钮 `放大/缩小`、`适应`、`=`/`-`、`Ctrl+滚轮`(规划) | ✅ 已落地（`pxPerTickScale`） |
| 缩放指示 | 工具栏 `100%` 标签，随缩放实时更新 | ✅ 已落地 |
| 吸附 | 工具栏 `吸附` 按钮 + `S` 键，状态高亮 | ✅ 前端开关（后端拖拽吸附规划中） |
| 标记 | `M` 键 / `★标记` 按钮在播放头打点；点击标记跳帧 | ✅ 已落地（会话级） |
| 时间码 | 状态栏 `时间码` 字段 HH:MM:SS:FF（默认 30fps，读 `s.fps`） | ✅ 已落地 |
| 播放头逐帧 | `←`/`→`、`Home`/`End` | ✅ 已落地 |
| 波纹/剃刀 | — | ⬜ 规划（需后端 timeline 模型支持） |
| 轨道头显隐/静音/锁定 | — | ⬜ 规划 |
| J-K-L 梭动 | — | ⬜ 规划（前端可视反馈） |

## 已落地（关键实现）

- `renderMarkers(s)`：在 `#tlMarkers` 覆盖层按 `pxPerTickOf` 定位标记；`addMarker()` 推入 `bbsMarkers` 会话数组；点击标记 `send('setCursor',{tick})` 跳帧。
- `updateZoomLabel()`：同步 `#tlZoomLbl` 百分比。
- `pxPerTickOf(dur,w)` 已乘 `pxPerTickScale`，缩放即改该系数后 `renderTimeline`。

## 备注

- 标记当前为**会话级**（刷新重置）。持久化需后端 `Film` 数据结构新增 `markers` 字段并由 `EditorBridge` 读写——列为 roadmap。
- 吸附开关目前只驱动 UI 状态；真正让拖拽 clip 吸附到端点/标记需后端时间轴命中逻辑，属较大改动，单独立项。
