# OemLauncher 项目规则（所有 agent 必读）

## 项目是什么

- `wear/`：Wear OS 手表启动器（主产品）。
- `mobile/`：安卓手机配套应用，用来排序、改图标、改外观，并同步到手表。
- `shared/`：两端共用的纯 Kotlin 模块，包括模型、同步协议、配置编辑、排序规则和蜂窝布局算法。
- 两个 App 的 `applicationId` 都是 `com.n2.oemlauncher`。Data Layer 要求两端包名和签名一致，不要改。
- 动手前先读：`docs/architecture.md`（架构与决策）和 `docs/sync-protocol.md`（同步协议）。

## 模块归属

- `shared/` 是两端之间的契约，由编排者维护，其他人**不要修改**。确实需要改动时，写进结果卡的“遗留问题”，说明要改什么、为什么。
- `wear/` 和 `mobile/` 各有一位负责人，只改自己的模块目录。
- 根目录构建文件（`settings.gradle.kts`、`build.gradle.kts`、`gradle.properties`、`gradle/libs.versions.toml`）尽量不改。确需新依赖时，只在 `libs.versions.toml` 末尾追加条目，并在结果卡里列出。

## 编码规范（硬性要求）

1. **单文件不超过 300 行**，按物理行数计算，包括空行和注释。检查命令：`(Get-Content <文件>).Count`。
2. **职责分离**：UI 组件、状态与业务逻辑（ViewModel / UseCase）、数据访问（Repository / DataStore）、平台接口（PackageManager、LauncherApps、Data Layer）、工具函数各放各的包。Composable 不直接调用 PackageManager、DataStore 或 Data Layer。
3. **严禁硬编码**：
   - 用户可见的文字放在 `res/values/strings.xml`（英文，默认）和 `res/values-zh-rCN/strings.xml`（简体中文）。
   - 尺寸、时长、阈值、数量上限等常量，集中放在本模块 `config/` 包下的常量文件里。
   - 协议常量和蜂窝参数只用 `shared` 里的定义。
4. **性能**：
   - 滚动和动画路径上不分配对象，不解码图片。
   - 图片按显示尺寸解码并缓存。
   - 主线程不做 IO。
   - 协程使用结构化并发，跟随生命周期取消。
   - 页面不可见时，停止动画和监听。
5. **无障碍**：可点击元素要有内容描述，触控目标不小于 48dp。蜂窝布局用最近中心命中，保证点击区域不重叠。
6. **技术栈**：Kotlin + Jetpack Compose，不用 XML 布局，不用 kapt。依赖注入用手写的 `AppContainer`，不引入 Hilt。
7. **注释**：只在代码本身说明不了“为什么”的地方写，比如隐藏约束、平台坑、反直觉的行为。不要逐行解说代码。

## 构建与验证（Windows PowerShell，在仓库根目录执行）

- 单元测试和调试包：`.\gradlew.bat :shared:test :wear:testDebugUnitTest :mobile:testDebugUnitTest assembleDebug`
- Lint：`.\gradlew.bat :wear:lintDebug :mobile:lintDebug`
- Release（开启 R8）：`.\gradlew.bat assembleRelease`。没有 `keystore.properties` 时会自动使用 debug 签名。
- 如果提示找不到 SDK：在工作目录新建 `local.properties`，内容为 `sdk.dir=D\:\\Android_SDK`。这个文件已被 git 忽略。

## 模拟器

- adb：使用 PATH 里的 `adb`（`D:\MyApps\Adb\platform-tools\adb.exe`）。SDK 自带的 platform-tools 是空的，所以**不要用 `installDebug`**，请用 `adb -s <serial> install -r <apk>`。
- 手表：`emulator-5554`，Wear OS 6（API 36），454×454 圆屏。
- 手机：`emulator-5556`，Android 14（API 34，google_apis 镜像）。
- 截图：`adb -s <serial> exec-out screencap -p > <文件>.png`，截图保存到自己 worktree 下的 `.screenshots/` 目录（已被 git 忽略）。
- 只用 `-s <serial>` 操作分配给自己的那台设备。**不要**运行 `adb kill-server`，**不要**关闭、重启或擦除模拟器。
- 两台模拟器没有配对，也没有 Wear OS 配套应用，所以 Data Layer 无法端到端联调。同步逻辑要用单元测试和假实现验证；UI 要在没有连接时也能完整演示。

## Git

- 只在自己的分支上提交。提交信息用英文，例如 `feat(wear): add honeycomb layout`。
- 不要 push，不要 rebase，也不要改写别人的提交。
