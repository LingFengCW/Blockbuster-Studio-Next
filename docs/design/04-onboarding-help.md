# 引导 / 帮助 / 工具提示（Onboarding & Help）

## 基准：专业软件怎么做

- **Premiere**：启动 Home 屏内嵌教程与文档入口；界面分面板，初次使用有导向。
- **DaVinci Resolve**：各 Page 内嵌说明，官方手册详尽。
- **After Effects**：长命令带 `title` 工具提示，可视化快捷键编辑器内 hover 显示完整命令名。
- **通用**：首次使用引导（coach-mark / 漫游）已成为专业软件的标配，而非「读说明书」。

## 我们的设计

编辑器是 MCEF 全屏叠层，用户首次进入容易「不知道点哪」。我们用三层次引导：

1. **首次启动 coach-mark 漫游**（已实现）
   - 5 步 spotlight：素材库 → 实时预览视口 → 时间轴 → 录制 → 播放。
   - `localStorage.bbs_tour_seen` 保证只跑一次。
   - 支持上一步/下一步/跳过。

2. **帮助菜单**（已实现，4 项）
   - 新手引导（重看漫游）
   - 快捷键一览（模态列表）
   - 教程文档（模态，镜像 `docs/tutorial.html` 六节）
   - 命令面板（Ctrl+K）

3. **自定义 tooltip**（已实现）
   - 监听所有带 `title` 的元素，悬浮显示 `#tipBubble` 气泡，自动避让视口边缘。
   - 比原生 `title` 更及时、可定位，专业感更强。

4. **试用版警告**（已实现）
   - 首次就绪弹一次：说明这是试用 / 源码构建 / 非官方 / 较旧分支，避免误解。

## 已落地

- HTML：`#coachMask/#coachCard/#coachRing`、`#tutModal`、`#tipBubble`、`#mHelpTour/#mHelpShortcuts/#mHelpDoc/#mHelpCmd`。
- JS：`startTour/__showCoach/endTour`、`openShortcuts/openTutorial`、`initTooltips()`。
- 接线：帮助菜单三项 + 命令面板菜单项 + 教程关闭 + coach 上/下/跳过 + resize 重定位。

## 规划

- 上下文帮助（hover 某面板显示该面板说明）。
- 交互式教程（边做边学，而非纯文档）。
- 多语言 i18n 补全（当前 tooltip/引导以中文为主）。
