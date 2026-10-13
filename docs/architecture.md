# 架构说明

## 1. 产品目标

- **手表端（`wear/`）**：可以设为默认主屏的 Wear OS 启动器。
  - 提供蜂窝、网格、列表三种布局，常用应用固定在中心或最前面。
  - 支持表冠和触觉反馈。
  - 冷启动快，滚动流畅，空闲时不耗电。
  - 没有手机时也能完成全部基础设置。
- **手机端（`mobile/`）**：配套编辑器。
  - 可以调整排序、设置常用应用、隐藏应用、替换图标（从相册裁剪，或导入 ZIP 图标包），以及修改外观设置。
  - 提供与手表真实尺寸一致的圆屏实时预览。
  - 支持离线编辑。连上手表后同步，并显示同步结果。

## 2. 模块与依赖

```
mobile ──► shared ◄── wear
```

- `shared`：纯 Kotlin/JVM 模块，不依赖 Android，能直接在 JVM 上跑单元测试。
- `wear`、`mobile`：Android 应用，`applicationId` 都是 `com.n2.oemlauncher`。

## 3. shared 提供什么（冻结的契约）

| 包 | 内容 | 两端怎么用 |
| --- | --- | --- |
| `model` | `AppKey`、`CatalogEntry`、`WatchCatalog`、`WatchDisplay`、`LauncherConfig`、`LayoutSettings`、枚举、`WatchConfigState` | 统一的数据模型 |
| `config` | `LauncherDefaults`（默认值与取值范围）、`ConfigSanitizer` / `CatalogSanitizer`（清洗输入）、`ConfigEditor`（纯函数编辑） | 手机编辑器的所有改动，以及手表长按菜单里的改动，都通过 `ConfigEditor` 完成 |
| `ordering` | `AppOrdering.arrange` | 手表的渲染顺序和手机的预览顺序都必须由它计算 |
| `protocol` | `SyncPaths` / `SyncKeys` / `Capabilities`、`ProtocolVersion`、`ProtocolLimits`、`PayloadCodec`、`IconId`、`RevisionPolicy`、`SyncStatusResolver` | 两端的同步逻辑 |
| `layout` | `HexSpiral`、`FisheyeLens`、`HoneycombGeometry`、`HoneycombProjector` / `HoneycombFrame`、`HoneycombHitTest`、`PanBounds`、`HoneycombDefaults`、`DisplayMath`、`RoundScreen` | 手表用来渲染蜂窝；手机按手表的真实尺寸绘制预览 |

### 蜂窝算法怎么用

1. 建立几何：`HoneycombGeometry(count, pitchPx)`。其中 `pitchPx = HoneycombDefaults.pitchFor(图标直径px)`，图标直径来自 `HoneycombDefaults.iconDiameterDp(iconSize)`。
2. 建立透镜：`FisheyeLens(layout.fisheyeStrength)`。
3. 每一帧调用 `HoneycombProjector.project(panX, panY, 视口半径, frame)`，结果写入同一个复用的 `HoneycombFrame`。坐标以屏幕中心为原点；`alpha` 为 0 的图标跳过，不绘制也不参与命中。
4. 点击命中：`HoneycombHitTest.hit(frame, x - cx, y - cy, 图标半径px, 最小触控半径px)`。
5. 拖动边界：使用 `PanBounds.forGeometry(geometry, 视口半径)`。拖动过程中用 `rubberBand` 产生阻尼，松手后回到 `clamp` 的结果。
6. 吸附：用 `geometry.snapTarget(panX, panY, out)` 计算让最近图标居中的偏移量；`geometry.panToCenter(index, out)` 让指定图标居中，用于表冠逐个切换焦点。
7. 顺序：单元 0 在正中心，然后一圈一圈向外排列，每圈从左上开始顺时针。`AppOrdering.arrange(...).all` 的顺序就是单元的顺序，所以常用应用正好占据中心和第一圈（最多 7 个）。

## 4. 手表端结构（建议）

包名为 `com.n2.oemlauncher.wear`。

- `catalog/`：通过 `LauncherApps` 查询可启动的 Activity，监听安装、卸载和更新，按尺寸解码图标并放进 LRU 缓存，启动应用时带上从图标位置展开的动画。
- `data/`：
  - 用 DataStore 持久化 `LauncherConfig`（`PayloadCodec.configToJson`）和使用统计。
  - 保存目录快照，冷启动时先显示快照，再在后台刷新。
  - 自定义图标按 `IconId` 命名存盘。
- `domain/`：把目录、配置和使用统计交给 `AppOrdering`，再转换成 UI 模型。
- `ui/`：
  - 启动器根界面。
  - 蜂窝布局：用一个 Canvas 绘制全部图标，配合自定义手势。
  - 网格布局。
  - 列表布局：使用 `TransformingLazyColumn`。
  - 应用长按菜单、设置页和主题。
- `input/`：表冠旋转（rotary）和触觉反馈。
- `sync/`：`WearableListenerService`，负责发布目录、处理 desired config、发布 state。
- `tile/`：常用应用 Tile。
- `config/`：本模块的常量。
- `AppContainer`：手写的依赖容器，在 `Application` 中创建。

## 5. 手机端结构（建议）

包名为 `com.n2.oemlauncher.mobile`。

- `ui/`：首页（连接与同步状态）、布局编辑器（拖拽排序、常用、隐藏）、圆屏实时预览、外观设置、图标管理、关于与帮助、主题。
- `editor/`：草稿管理（记录基于手表的哪个修订号）、撤销与重做、提交。
- `iconpack/`：
  - 导入 ZIP 图标包，安全解压。
  - 裁剪相册图片。
  - 压缩成 192px 的 PNG 或 WebP。
  - 按包名匹配应用。
- `data/`：用 DataStore 保存草稿和设置；保存图标文件；缓存最近一次收到的手表目录。
- `sync/`：
  - 用 `CapabilityClient` 发现手表。
  - 用 `MessageClient` 请求目录。
  - 用 `DataClient` 发布 desired config。
  - 监听 catalog 和 state 的变化。
- `config/`：本模块的常量。
- `AppContainer`：手写的依赖容器。

## 6. 关键决策

1. **相同的 applicationId 和签名**：这是 Data Layer 的硬性要求。release 包默认使用 debug 签名，也可以通过 `keystore.properties` 改用正式签名。
2. **配置冲突按修订号最大者胜（LWW）**：
   - 任何一端提交编辑时，都用 `RevisionPolicy.next` 取一个比已知所有修订号都大的值。
   - 手表收到的修订号如果不比当前新，就拒绝，并回报 `REJECTED_STALE`。
   - 手机收到这个结果后提示“手表上有更新的修改”。
3. **图标按内容寻址**：`IconId` 是 SHA-256 摘要的前 24 位十六进制。同一个图标只存一份，也只传一次。
4. **负载压缩**：JSON 经 gzip 压缩后传输。单个 DataItem 上限 90KB；解压后上限 2MB，用来防止压缩炸弹。
5. **手表端可以独立使用**：没有手机时，布局、常用、隐藏和排序这些设置都能在手表上完成。
6. **主屏替换**：manifest 中声明 HOME 入口，但能否设为默认主屏取决于系统和厂商。因此另外提供 Tile 和应用图标两个入口，保证在任何手表上都能用。

## 7. 性能原则

- **冷启动**：先读取目录快照和配置（各一次磁盘读取）。首帧不等待 PackageManager。
- **蜂窝布局**：
  - 几何按（应用数，图标尺寸）缓存。
  - 每一帧只调用 `project`，结果写入复用的 `HoneycombFrame`。
  - 用一个 Canvas 绘制全部图标，图标都是预先解码好的 `ImageBitmap`。
- **图标**：在后台线程按目标像素解码，内存中用按字节计算容量的 LRU 缓存，同时写入磁盘缓存。
- **动画与生命周期**：
  - 停止交互后，动画随之停止。
  - 离开前台时，停止所有监听。
  - Release 构建开启 R8 和资源压缩，并接入 ProfileInstaller。
- **模拟器的局限**：模拟器使用软件渲染（swiftshader），帧率数据不能代表真机，性能结论要在真机上确认。
