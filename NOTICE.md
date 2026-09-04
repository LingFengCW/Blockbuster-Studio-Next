# NOTICE — 许可证与署名（Licensing & Attribution）

本项目 **BBS‑NEXT（泠瀑）** 是 [McHorse 的 BBS mod](https://github.com/mchorse/bbs-mod)（MIT）的派生（fork）作品，
由 [LingFengCW（泠沨）](https://github.com/LingFengCW) 维护并移植至 Minecraft 26.2（Fabric）。

## 许可证按作者归属（per‑author）

本仓库遵循 MIT 的「谁写的代码归谁」原则，按代码来源分别适用许可证：

| 代码来源 | 路径 / 范围 | 版权与许可证 |
|----------|-------------|--------------|
| 原作（McHorse） | `src/.../mchorse/bbs_mod/` 中源自上游的文件 | © McHorse，**[MIT](LICENSE-MIT)** |
| 本项目（LingFeng） | 包 `lingfeng.bbsnext.*` 的全部文件，以及本项目新增 / 重写的文件 | © LingFeng（泠沨），**[PNC（公开‑非商业）](LICENSE) / [中文](LICENSE.zh-CN)** |

- 原作仓库：<https://github.com/mchorse/bbs-mod>
- 原作许可证：MIT（见 `LICENSE-MIT`）
- 本项目（LingFeng）新增与修改部分在原作 MIT 之上追加版权声明，原作者的 MIT 权利不受减损。

## 原作署名

原作 BBS mod 版权归属 **McHorse**。分发时须在文档显著位置（软件关于页 / README / 更新日志）标注原作者与上游仓库。

## 商业化限制（PNC 第 4、5 条，仅约束本项目 LingFeng 专有部分）

- 未经原作者（McHorse）书面许可，不得以原作者或本项目的名义开展任何商业活动。
- 本项目 LingFeng 专有部分受 PNC 约束：未经原作者书面许可，不得用于、分发或运行任何商业行为。
- 自愿捐赠、官方项目推广、官方开发文档的编写与分发不受上述商业限制约束。

## 二进制再分发（PNC 二进制条款，仅约束本项目 LingFeng 专有部分）

- 原版未修改的二进制构建可经公开渠道自由分发。
- **修改后的衍生二进制构建**，未经原作者书面许可，不得通过公开下载渠道（镜像站、应用商店、公开文件托管平台）对外分发；
  本地编译、个人离线打包自用、团队内部分发不受此限制。

## PNC 比率计算逻辑（PNC 第 5‑A 条）

为履行 PNC 第 5‑A 条“保留比率计算逻辑源代码”的要求，本仓库在
[`tools/pnc_ratio.py`](tools/pnc_ratio.py) 中保留了第 5 节定义的基础计算逻辑（可在其之上扩展，但不得删改基础逻辑）。
发生许可争议时，可运行该工具输出聚合比例作为判定参考。
