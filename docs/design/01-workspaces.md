# 工作区预设（Workspaces）

## 基准：专业软件怎么做

- **Premiere Pro**：界面由「面板」组成，提供 16 个默认工作区（Essentials、Vertical、Learning、Assembly、Captions and Graphics、Review、Production 等）。用户可拖拽面板、重排后「Window → Workspaces → Save as New Workspace」保存；混乱时「Reset to Saved Layout」一键恢复。
- **DaVinci Resolve**：用 7 个「Pages」（Media/Cut/Edit/Fusion/Color/Fairlight/Deliver）按后期流程组织；`Workspace → Reset UI Layout` 恢复；面板可显隐、弹出。
- **Blender**：内置多套 Workspaces（Layout/Modeling/Animation/Sculpting…），N 面板放属性。

**共同规律**：① 按任务切预设；② 预设可保存/重置；③ 面板显隐是高频便捷操作。

## 我们的设计

编辑器已有固定三区（素材箱 / 视口 / 时间轴）。我们用 **`body[data-ws]` + CSS 变量** 切换预设，仅改变素材箱宽度与时间轴高度，不重排 DOM（避免 MCEF 全屏叠层下的布局抖动）。

| 预设 | 素材箱宽 | 时间轴高 | 适用 |
|---|---|---|---|
| 动画 (animation) | 300px | 200px | 调动作/关键帧，需要宽素材箱 |
| 剪辑 (editing) | 238px | 330px | 精剪时间轴，需要高轨道 |
| 预览 (preview) | 186px | 168px | 看 3D 画面，最大化视口 |

- 顶部工具栏放 `动画 / 剪辑 / 预览` 分段控件 + `重置布局` 按钮。
- 选择持久化到 `localStorage.bbs_workspace`，刷新后恢复。
- 「重置布局」清除 `data-ws`，回到 CSS 默认。

## 已落地

- HTML：`#wsSeg`（三个按钮）+ `#wsReset`。
- JS：`applyWorkspace(name)` / `syncWsSeg()`；初始化时从 `localStorage` 恢复。
- CSS：`body[data-ws] .asset-panel{width:var(--asset-w)}` 等。

## 规划（不在本次范围）

- 拖拽改变素材箱/时间轴尺寸并写入预设。
- 自定义工作区保存（多个命名预设）。
- 单面板最大化（Premiere 重音键 / AE 波浪号键）。
