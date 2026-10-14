# 手机 ⇄ 手表同步协议（v1）

所有常量都定义在 `shared/protocol` 中：

- 路径：`SyncPaths`
- DataMap 键：`SyncKeys`
- 能力名：`Capabilities`
- 版本：`ProtocolVersion`
- 限制：`ProtocolLimits`

两端代码只能引用这些常量，不得自己写字符串。

## 1. 参与方与能力

- 手表 App 声明能力 `oemlauncher_watch_app`，手机 App 声明能力 `oemlauncher_phone_app`，都写在各自的 `res/values/wear.xml` 中。
- 手机用 `CapabilityClient` 查找具有手表能力、并且已连接的节点；手表查找手机节点的方式相同。

## 2. 数据项与消息

| 路径 | 拥有方 | DataMap 内容 | 用途 |
| --- | --- | --- | --- |
| `/oemlauncher/v1/catalog` | 手表 | `schema`: Int；`payload`: `PayloadCodec.encodeCatalog`；`icon_<iconId>`: Asset × N | 应用目录，以及每个应用的默认图标（128px PNG） |
| `/oemlauncher/v1/config/desired` | 手机 | `schema`；`payload`: `encodeConfig`；`revision`: Long（与 payload 内的 `revision` 相同）；`icon_<iconId>`: Asset × N | 手机希望手表采用的配置。Asset 只包含 `iconOverrides` 引用的自定义图标（192px） |
| `/oemlauncher/v1/config/state` | 手表 | `schema`；`payload`: `encodeState` | 手表当前的配置和最近一次处理结果，同时充当回执 |

消息 `/oemlauncher/v1/message/request-catalog` 由手机发给手表，负载为空，意思是“请立即重新发布目录”。

### 通用规则

- `schema` 一律写 `ProtocolVersion.CURRENT`。
- desired 写入时调用 `setUrgent()`。catalog 和 state 只在内容变化时发布：比较 `IconId.of(payload)` 得到的摘要，摘要没变就不重发。
- 用 `SyncKeys.iconAssetKey(iconId)` 生成 Asset 的键，用 `Asset.createFromBytes` 创建 Asset。读取 Asset 时，在 IO 线程调用 `DataClient.getFdForAsset`；超过 `ProtocolLimits.MAX_ICON_BYTES` 的直接丢弃；读完后必须重新计算 `IconId.of(bytes)`，与键里的 id 不一致的也丢弃。
- 所有 payload 都通过 `PayloadCodec` 解码。遇到 `DecodeResult.Failure` 不得崩溃，只记录原因。
- Data Layer 调用都可能抛出 `ApiException`，例如没有 Google Play 服务（`API_UNAVAILABLE`）或手表未配对。必须捕获这些异常，并把它们转换成“未连接”状态。

## 3. 修订号与冲突处理（LWW）

`LauncherConfig.revision` 单调递增。任何一端提交编辑时：

- `revision = RevisionPolicy.next(所有已知修订号)`
- `origin` 标明来源（WATCH 或 PHONE）
- `editedAtMillis` 设为当前时间
- 这三项统一用 `ConfigEditor.stamp` 写入

### 手表处理 desired 的步骤

1. `schema` 不受支持：`lastResult = REJECTED_SCHEMA`。
2. 解码失败：`lastResult = REJECTED_INVALID`。
3. `RevisionPolicy.isRedelivery(...)` 为真：直接忽略，不更新 state。
4. `RevisionPolicy.shouldApply(...)` 为假，说明手表已有相等或更新的修订：`lastResult = REJECTED_STALE`。
5. 其他情况：
   - 读取全部图标 Asset，校验后按 `IconId` 存盘；
   - 被引用但没拿到的图标，写入 `missingIconIds`；
   - 应用配置并持久化；
   - `lastResult` 设为 `APPLIED`，如果有缺失图标则设为 `APPLIED_MISSING_ICONS`。

除第 3 种情况外，处理完都要把 `lastDesiredRevision` 设为本次 desired 的修订号，并发布 state。

第 1、2 种情况下 payload 可能无法解码，修订号取 DataMap 里的 `SyncKeys.REVISION`。这个键是可选的（旧版手机不写），缺失时 `lastDesiredRevision` 保持不变，仍然发布带拒绝结果的 state。缺少 `payload` 的 desired 按第 2 种情况处理。

### 手表本地编辑

长按菜单和手表设置页里的改动，按下面的方式提交，然后发布 state：

- `revision = RevisionPolicy.next(current.revision, lastDesiredRevision)`
- `origin = WATCH`

### 手机端

- 草稿要记录 `baseRevision`，即开始编辑时手表 `state.current.revision` 的值。
- 点击“同步到手表”时，按下面的方式取修订号，然后发布 desired：
  `revision = RevisionPolicy.next(baseRevision, state.current.revision, state.lastDesiredRevision, 上次发送的修订号)`
- 同步状态用 `SyncStatusResolver.resolve(上次发送的修订号, 最新 state)` 计算。
- `SyncStatusResolver.watchHasNewerEdits(baseRevision, state)` 为真时，提示“手表上有更新的修改”，并给出两个选项：“载入手表配置”，或“用我的修改覆盖”。
- 首次连上手表、手机还没有草稿时，以手表 `state.current` 作为草稿。

## 4. 手表发布目录的时机

- 应用安装、卸载或更新后，图标变化后，以及使用统计明显变化后（例如每启动 5 次），都要发布目录。这些发布需要节流，最多每 10 秒一次。
- 收到 `request-catalog` 消息时立即发布，不受节流限制。
- 目录中的图标统一是 128px 的 PNG，`iconId` 为 `IconId.of(png字节)`。

## 5. 兼容性

- 新增字段必须带默认值。读取端已经开启 `ignoreUnknownKeys` 和 `coerceInputValues`。
- 只有破坏性变更才提升 `ProtocolVersion.CURRENT`，并且要保留读取旧版本的能力。

## 6. 离线与降级

- 手机没有连上手表时，照常编辑并保存草稿；连上后提示用户同步。
- 手表没有手机时，所有功能都在本地可用。同步只是锦上添花。
