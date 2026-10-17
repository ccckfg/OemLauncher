# 项目进度

最后更新：2026-10-17 · 编排者

## 一句话现状

v1.0.0 已在 `master` 交付，但在用户的 Galaxy Watch 上实测几乎没法用（表盘消失、底部设置带挡住主屏）。现在按 `docs/decisions.md` 的 D1–D6 做第二版：保留系统表盘，并改用“摩卡玻璃”设计语言重做两端界面。

## 分支

| 分支 | 位置 | 说明 |
| --- | --- | --- |
| `master` | `18aab8d` | v1.0.0，`dist/` 里的安装包就是它 |
| `feat/wear-redesign` | 集成分支 | 重设计结构（中心齿轮、页面导航、设置页）+ 第二版的所有工作，里程碑验证后快进到 `master` |
| `archive/misty-phone-preview` | `5b687af` | 暂停时没提交的雾蓝手机预览，只作参考，不合并 |

## 里程碑

| 编号 | 内容 | 状态 |
| --- | --- | --- |
| M0 | 整理仓库：进度和决策文档、设计参考图入库、忽略 `kcys/`、归档未提交代码 | 完成 |
| M1 | 手表能用：Home 键转交保留表盘、无障碍开启引导、修复“没有可启动的应用”、拒绝“读取应用列表”权限时给出引导、国行声明；装到真机由用户试用 | 进行中（droid） |
| M2 | 设计规范：`docs/design/` 规范 + `shared` 设计参数 + 一页视觉样张，用户确认 | 规范和样张完成，等用户确认 |
| M3 | 手表界面按规范重做，精致 / 轻量两档 | 等 M2 |
| M4 | 手机界面按规范重做，预览跟上新手表界面 | 等 M2 |
| M5 | 真机和模拟器验证、文档截图、合并 `master`、刷新 `dist/` | 等 M3、M4 |

M1 和 M2 并行。

## 进行中

- M1：droid（DeepSeek V4.1 Flash max）在 worktree `orch-20261017-131204-57ed-wear` 里实现，分支 `orch/20261017-131204-57ed-wear`。
- M2：规范 `docs/design/README.md`；`shared/design/` 已有配色（Mocha / Latte）、玻璃规格、饼干轮廓和对比度测试；`LayoutSettings` 新增 `visualTier`、`iconStyle`（老版本两端兼容）。视觉样张：https://claude.ai/artifact/K1AfAPhxSm4QmuJommPdok ，等用户确认后再派 M3、M4。

## 待办与已知问题

- [ ] 停工时手表模拟器主屏只有中心齿轮，提示“没有可启动的应用”，原因未查（M1）。
- [ ] 三星国行会弹“读取应用列表”权限（`com.samsung.android.permission.GET_APP_LIST`，系统自动加的，我们的清单里没有），用户拒绝时现在会显示空列表、没有提示（M1）。
- [ ] 清单缺少国行版声明 `com.google.android.wearable.local_edition_compatible`（M1）。
- [ ] 国行手表的 Data Layer 经过 `com.google.android.wearable.app.cn`，手机和手表的同步从没在真机上跑过；需要一部能开 adb 的配对手机才能实测（M5，等用户确认手机）。
- [ ] 文档和截图还是 v1.0 的旧界面（M5）。

## 设备

| 设备 | 用途 | 谁来操作 |
| --- | --- | --- |
| Galaxy Watch5 Pro（SM-R920，国行，Wear OS 6，450×450） | 真机验证 | 只由编排者操作 |
| `emulator-5554`（AVD `oem_wear_36`，Wear OS 6，454×454） | 手表开发 | droid |
| `emulator-5556`（AVD `oem_phone_34`，Android 14） | 手机开发 | droid |

## 更新记录

- 2026-10-17：v1.0.0 装到用户手表上实测，确认两个阻塞问题；对齐 D1–D6；开始 M0。
- 2026-10-16：重设计做到一半时按用户要求全部暂停。
- 2026-10-15：v1.0.0 交付（中文界面、拼音搜索、交付文档和截图）。
