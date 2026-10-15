# 交付说明

本文说明「N2 桌面」的目录结构、构建与验证命令、安装与使用步骤，以及已知限制。命令均在仓库根目录执行（Windows PowerShell）。

## 目录结构

```text
OemLauncher/
├─ shared/                            两端共用的纯 Kotlin 契约（不依赖 Android）
│  └─ src/main/kotlin/com/n2/oemlauncher/shared/
│     ├─ model/                       数据模型：应用键、目录、配置、布局设置、手表状态
│     ├─ config/                      默认值与清洗：LauncherDefaults、ConfigSanitizer、ConfigEditor
│     ├─ ordering/                    排序规则：AppOrdering（常用/隐藏/排序模式 → 最终顺序）
│     ├─ protocol/                    同步协议：路径与键、gzip 编解码、IconId、修订号策略、状态解析
│     └─ layout/                      蜂窝几何：螺旋、鱼眼透镜、投影、命中测试、边界、圆屏裁剪
├─ wear/                              Wear OS 启动器（主产品）
│  └─ src/main/kotlin/com/n2/oemlauncher/wear/
│     ├─ catalog/                     LauncherApps 目录查询与监听、图标解码缓存、应用启动
│     ├─ data/                        DataStore 配置与统计、目录快照、图标文件、同步状态存储
│     ├─ domain/                      目录 + 配置 → 可直接渲染的条目
│     ├─ input/                       表冠旋转适配与触觉反馈
│     ├─ sync/                        Data Layer：目录发布、desired 处理、state 回执、Tile 刷新通知
│     ├─ tile/                        常用应用 Tile 的模型、布局与服务
│     ├─ ui/                          Compose 界面：三种布局、长按菜单、设置与主题
│     └─ config/                      本模块常量（WearConstants）
├─ mobile/                            手机配套 App
│  └─ src/main/kotlin/com/n2/oemlauncher/mobile/
│     ├─ data/                        仓库与持久化：快照、图标内容寻址存储、演示目录工厂
│     ├─ editor/                      草稿历史（撤销/重做）、ViewModel 状态与导入会话
│     ├─ iconpack/                    ZIP 安全解压、图片裁剪与归一化、appfilter 解析、导入仓库
│     ├─ sync/                        WearGateway 抽象与实现、desired 构建、catalog/state 接收
│     ├─ ui/                          Compose 界面
│     │  ├─ screens/                  各页面：首页、编辑器、预览、外观、图标、裁剪、导入结果、帮助、同步诊断
│     │  ├─ components/               复用组件：预览渲染器、应用行、状态色调、相对时间、错误文案
│     │  ├─ navigation/               类型安全路由与 Scaffold/顶栏
│     │  └─ theme/                    配色、动态色与系统栏
│     └─ config/                      本模块常量（MobileConstants）
├─ docs/                              架构说明、同步协议与本文档
└─ gradle/                            版本目录（libs.versions.toml）与 wrapper
```

两端测试源码分别位于 `wear/src/test/` 与 `mobile/src/test/`，包结构与主源码一一对应。模块根目录下分别是应用入口与手写依赖容器：`wear` 的 `App.kt`、`AppContainer.kt`、`MainActivity.kt`，以及 `mobile` 的 `CompanionApplication.kt`、`AppContainer.kt`、`MainActivity.kt`。

## 构建与验证

### 单元测试与调试包

```powershell
.\gradlew.bat :shared:test :wear:testDebugUnitTest :mobile:testDebugUnitTest assembleDebug
```

产物：

- `wear\build\outputs\apk\debug\wear-debug.apk`
- `mobile\build\outputs\apk\debug\mobile-debug.apk`

### Lint

```powershell
.\gradlew.bat :wear:lintDebug :mobile:lintDebug
```

### Release（开启 R8 与资源压缩）

```powershell
.\gradlew.bat assembleRelease
```

产物：`wear\build\outputs\apk\release\wear-release.apk`、`mobile\build\outputs\apk\release\mobile-release.apk`。

### 签名与 keystore.properties

仓库根目录可选放一个 `keystore.properties`，格式如下（`storeFile` 相对仓库根目录）：

```properties
storeFile=keystore.jks
storePassword=<store 密码>
keyAlias=<key 别名>
keyPassword=<key 密码>
```

没有该文件时，release 构建自动回退到 debug 签名。这是刻意行为：Data Layer 要求两端包名与签名一致，本地开发用同一把 debug 签名即可让两个 release 包互通。

## 安装与使用

### 手表端

```powershell
adb -s <手表序列号> install -r wear\build\outputs\apk\debug\wear-debug.apk
```

设为默认主屏（是否允许替换取决于系统与厂商）：

```powershell
adb -s <手表序列号> shell cmd package set-home-activity com.n2.oemlauncher/com.n2.oemlauncher.wear.MainActivity
```

添加 Tile：在手表上从表盘向左滑到 Tile 轮播，用“编辑/添加”选择“N2 常用应用”（英文界面为 “N2 Favorites”）。开发机（模拟器）上也可以用系统调试广播直接添加：

```powershell
adb -s <手表序列号> shell am broadcast -a com.google.android.wearable.app.DEBUG_SURFACE --es operation add-tile --ecn component com.n2.oemlauncher/com.n2.oemlauncher.wear.tile.FavoritesTileService
adb -s <手表序列号> shell am broadcast -a com.google.android.wearable.app.DEBUG_SYSUI --es operation show-tile --ei index 0
```

操作方式：

- 长按图标（三种布局均可）打开菜单：切换常用、隐藏应用、打开系统应用信息。
- 旋转表冠：蜂窝布局逐格居中，网格与列表滚动（真机上带触觉反馈；模拟器的 `input rotaryencoder` 注入无效，见“已知限制”）。
- 设置页：调整布局模式、图标大小与形状、排序模式、标签、吸附、触觉与减弱动画；常用应用顺序也可在设置页调整。

### 手机端

```powershell
adb -s <手机序列号> install -r mobile\build\outputs\apk\debug\mobile-debug.apk
```

- 没有手表时：首页点“Try the demo”进入演示模式（40 个示例应用），或直接点“Refresh watch”等待连接。
- 编辑：首页“布局编辑”进入编辑器；搜索、排序模式、星标常用、拖拽或行尾菜单排序、隐藏应用；所有改动自动保存并可撤销/重做。
- 预览：点首页缩略图进入全屏预览，可拖动探索；预览按手表真实分辨率与密度绘制，与手表使用同一套算法。
- 外观：首页“外观”调整布局模式、图标大小与形状、鱼眼强度、网格列数、标签、吸附、触觉与减弱动画。
- 图标：首页“图标”为单个应用选择照片并裁剪（192 px PNG），或导入 ZIP 图标包（包名图片或 `appfilter.xml`）；导入先预览匹配结果，确认后应用，可随时恢复默认。
- 同步与诊断：首页同步卡片显示阶段与冲突处理（“载入手表配置”或“用我的修改覆盖”）；“同步诊断”页显示节点能力、收发时间、发送修订与结果、缺失图标与最近 API 错误，并提供“重新请求目录”与“重新发送”。

### 配对与同步的前提

- 两端 `applicationId` 必须一致（都是 `com.n2.oemlauncher`），签名也必须一致，否则 Data Layer 无法建立通道。
- 手表通过其 Wear OS 配套应用与手机配对，两端都需安装并启用 Google Play 服务。
- 配好后打开一次手表端应用，然后在手机首页点“Refresh watch”即可拉取目录。

## 已知限制

- **Data Layer 未在配对真机上端到端验证**：开发用的两台模拟器未配对、无 Wear OS 配套应用，catalog/state/desired 的真实往返、Asset 文件描述符读取与拒绝回执关联修订号都只经过假实现单测与静态检查，真机配对后需做一次完整回归。
- **模拟器性能数据只作参考**：模拟器使用软件渲染（swiftshader），帧率与滚动数据不能代表真机；功耗与流畅度需在真机确认。
- **能否替换系统主屏取决于手表厂商**：部分手表不允许第三方应用成为默认主屏；此时使用 Tile 或应用图标入口，功能不受影响。
- **表冠旋转需真机复验**：旋钮输入已通过 Compose `onRotaryScrollEvent` 接入三种布局，但模拟器的 `adb shell input rotaryencoder` 没有产生可观察事件，实际手感与滚动步进需在带旋钮的真机上确认。
- **列表布局使用 `LazyColumn`**：未改用 `TransformingLazyColumn`；当前滚动、焦点与触觉路径均已可用。
- **蜂窝布局的无障碍语义是“当前焦点”级别**：不是每个图标一个独立语义节点；完整的逐图标 TalkBack 导航尚未提供。
- **切换主题会清空进行中的 ZIP 导入预览**：导入是会话态，切换深浅色重建页面后需重新选择 ZIP；已应用的图标不受影响。
- **旧版手机（不写 `SyncKeys.REVISION`）的 schema/解码拒绝无法关联修订号**：此时手表保持 `lastDesiredRevision` 不变（协议允许的降级）；新版手机写入该键后拒绝回执可正确关联。
- **手表目录不会自动重发**：应用安装/卸载/更新与使用次数变化尚未接入目录发布触发点（`requestCatalogPublish()` 目前没有主代码调用者）；目录在手机请求（首页“Refresh watch”、首次配对）或上次发布失败重试时才更新。手表装完新应用后，请在手机上刷新一次目录。
- **手表端同步状态展示尚未接线**：同步仓库已提供 `SyncStatus` 状态流，但设置页的“最近同步”仍显示初始值 0；手表的本地编辑也仍走 `publishState`（带陈旧写入保护）而非 `localEdit` 统一事务。
- **DataStore 读取失败时手表以空状态继续启动**：为保证冷启动不被阻塞，此时收到 desired 会按空状态处理。
- **Tile 只显示前 4 个常用**：启动器本身仍是 7 个常用，Tile 上限是设计常量（`WearConstants.TILE_FAVORITES_MAX`）。
- **目录发布仍会重建全部默认图标**：每次发布（受 10 秒节流限制）都会刷新目录快照与图标，后续可改为复用磁盘缓存以进一步降低开销。
- **构建缓存注意事项**：开发过程中曾出现构建缓存产物导致安装后启动缺类（`NoClassDefFoundError`），最终安装前的构建建议加 `--no-build-cache`；如遇同类症状，先 `clean` 后重建再判定。
- **无障碍自定义操作需真机确认**：手机编辑器的“上移/下移”通过 `customActions` 提供，`uiautomator dump` 不呈现自定义操作，端到端体验需在 TalkBack 或真机无障碍服务下确认。

## 测试与验证结果

由编排者在最终验证后补充。

## 截图

由编排者在最终验证后补充。
