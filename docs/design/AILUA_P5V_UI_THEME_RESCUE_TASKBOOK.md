# AILUA P5.V — UI & Theme Rescue 总任务书

> 冻结产品架构和业务 Runtime，只重做“看起来是什么”与“主题如何生产”。

本文件可直接交给 Codex / Claude 施工。先连续完成 **Renderer → Home → AILUA Default / Soft Home / Midnight Glass 三主题真机对照**，在第一次视觉评审点停下。确认母版方向后再继续 UI-3→UI-10，按阶段提交并在整体验证后推送。首轮三主题是验证材料与布局的样张，六套正式主题在 UI-5 完成。所有视觉选择以真机截图和实际壁纸为准。

## 0. 基线、边界和交付定义

**唯一基线**：`local-pass3c-ai-runtime` / `9246aa046ab11f1e8deed2bae3ecdd41ccbc41d8`。这是 Character Runtime 与当前 Launcher / Theme 架构的冻结基线。开工先核对：

```powershell
git status --short
git branch --show-current
git rev-parse HEAD
git ls-remote origin refs/heads/local-pass3c-ai-runtime
```

若分支已前进，从最新 HEAD 继续并先核对相关文件的变化；不得 reset 到旧提交，不得回滚 R1→R4。记录工作树原有未跟踪文件，提交时只暂存本轮文件。

冻结：`CharacterRuntimeResolver`、`CharacterRuntimeProfile`、Romance / Initiative / World / Chat / Memory / Call Runtime、数据库 schema、Workspace 数据结构、Pager、Folder、拖拽/resize 语义、Widget Host 模型、`AppRouter`、Character Card V2 与 `extensions.ailua`。本轮只读这些模块提供的数据；不得为视觉效果改变事件、消息、关系、页面位置的语义。不得另建 ThemeEngineV2、LauncherV2、WorkspaceV2 或第二套主题状态。

当前主题主链必须保持：

```text
ThemeStore → ThemeSelection → ThemeResolver → AiluaThemeRuntime
→ ThemeWallpaper / Widget / Dock / Icon / Screen
```

外部兼容主链保持：`UnifiedThemeImporter` → `.ailuatheme` / MTZ / ColorOS / Android Icon Pack → `ExternalThemeRepository`。导入器服务于内部生产和高级导入；普通消费者看到的是经过整理、具备商用权利的 AILUA 官方主题。第三方未授权素材只能用于兼容测试和内部参考，不能打入正式 APK 或官方主题包。

**最终观感**：约 70% 真实手机与 Launcher、20% 人物生活感、10% 主题人格。壁纸提供画面，人物是焦点，应用图标有身份，Widget 与 Dock 是覆盖物。完成标准由真实 1080×2340 截图评估，单测只能验证行为不倒退。

## 1. 施工纪律

1. 串行执行 UI-0→UI-10；除下述三主题视觉评审点外，阶段间不召开验收会。每阶段留下截图、简短记录和 checkpoint commit。评审点之前不能批量迁移 App、制作正式人物 ART 或开始 P5.6。
2. 每次准备修改代码，先读 HEAD 中的实际实现和现有测试。改 Renderer 与设计资源；不因某个视觉目标重写业务或 Workspace。
3. 原机已有 **3 页、18 项、1 文件夹**，是不可丢失的持久化样本。安装使用 `adb install -r`；不清数据。任何新的默认页排布仅适用于首次安装，绝不迁移、重排、重置已有用户桌面。
4. 主题切换不得改变桌面项目位置、Folder、Widget span、当前角色、对话或关系记录；任意 Skin + Palette + 壁纸 + 图标覆盖仍能自由组合。切换和应用前后都保留原有回退路径。
5. 主题设计不得靠所有层一起发白、重度磨砂或大面积空卡片来掩盖底图。所有视觉容差以 Light / Regular / Dark 玻璃样张、实际壁纸、文字对比度和 Android 10 截图决定。
6. 不制作正式男主立绘，不更改人设，不启动 P5.6，不新增 App。早期样张可用有授权的占位图、自有临时图或抽象图；正式人物资产待独立 ART 阶段确定。

## 2. UI-0：视觉基线与材料规格

创建以下三份可执行的设计记录；本文是施工总控，三个文件写具体尺寸、对比、样张和决策：

```text
docs/design/AILUA_VISUAL_BASELINE.md
docs/design/AILUA_THEME_MATERIAL.md
docs/design/AILUA_HOME_SPEC.md
```

用当前 APK 在小米 Android 10 截 Milk / Glass / Diary / Mono 的首页、主题中心、应用网格、Dock、Widget 与两张高频应用页面。保存“施工前”图片和设备/主题/页码清单，作为回归基线。无需先画大而全概念图。

写入并执行以下视觉约束：

- 首页视觉顺序：**Wallpaper → Character → Apps → Glass**。Widget 不能伪装成整屏背景；Dock 不能是大块实心白板。
- 文本只有 Primary / Secondary / Caption 三层；不再为每个单页发明小数点字号与透明度。应用主图标和彩色徽标保持清晰不透明。
- 不使用全屏乳白 overlay、满屏同构卡片、所有图标同一种半透明圆底、密集白色 1dp 边框；背景本身已模糊时不再套整屏模糊。
- Card 只用于真正独立的信息块；普通列表靠留白和分割线。动态/相册/聊天等已经迁入全局 `LocalAiluaTheme`，本轮只修视觉不重做页面结构或逻辑。
- 每个主题样张覆盖亮色、暗色、浅/深壁纸、长标签、中文/英文混排；文字与图标必须有实际可读性。

**UI-0 完成证据**：三份规格、施工前图与现状问题列表。列表应指明截图位置和代码入口，避免“感觉太 Demo”式描述。

## 3. UI-1：Theme Material 和 Renderer

### 3.1 先修确切的 alpha 错误

当前 `ThemeResolver.kt` 给 Glass Widget 的 `surfaceAlpha = 0.32f`、Dock 的 `surfaceAlpha = 0.28f`，但 `ThemeWidgetFrame.kt` 用 `coerceIn(0.78f, 1f)`，`HomeHotseat.kt` 用 `coerceIn(0.78f, 0.96f)`，令主题参数失效。两个 Renderer 应按 `backgroundStyle` 区分材料：GLASS 使用经校验的低透明度主题值，CARD / PAPER / FLAT 保留各自的不透明规则，TRANSPARENT 真正透明。不得把所有风格一律改成低 alpha。

`ThemeWidgetFrame` 还应使用 `WidgetVisualSpec.backgroundColor`、`cornerRadiusDp`、`border`、`shadow` 等已有参数，而不是统一套 `runtime.surfaces.raised` 与 `runtime.shapes.medium`。Dock 同样读取现有 `DockVisualSpec` 的颜色、圆角、边界与 padding。编辑选中态须继续明显可见。

`ThemePreview.kt`、`ImportedThemePreview.kt` 的缩略图和真实 Home 必须共用相同的材料换算规则，至少以一个小的 renderer helper 避免预览“透明”而桌面“白塑料”。请不要把相同 alpha clamp 又复制到第三处。

### 3.2 只增加必要参数

现有 `WidgetVisualSpec` 和 `DockVisualSpec` 可追加带默认值的 `blurRadiusDp`、`highlightAlpha`、`fallbackAlpha`。先用这三个字段及已有 `surfaceAlpha`、`backgroundColor`、`border`、`shadow`。`saturation` 仅在实机截图证明必要时增加。不要引入折射、色差、噪声引擎、镜头变形等新运行时概念。

初始校准范围，实际值由截图与可读性确定：

| 材料 | Tint alpha | Blur | Highlight alpha |
| --- | ---: | ---: | ---: |
| Light Glass | 0.10–0.16 | 18–24 dp | 0.18–0.24 |
| Regular Glass | 0.14–0.22 | 22–28 dp | 0.15–0.22 |
| Dark Glass | 0.20–0.30 | 24–32 dp | 0.08–0.16 |

Dock 目标高度 82–92 dp、圆角 28–34 dp、模糊 24–30 dp、Tint alpha 0.14–0.22。动态对比取决于壁纸和字色，不把这些范围硬编码为全主题常量。玻璃下面仍能辨认壁纸色彩、大轮廓与明暗；如果只剩灰色，视为不通过。

### 3.3 真正的背景采样与 Android 10 退化

只在 Wallpaper / Workspace 的最小局部做背景采样 → blur → tint → 细高光/边界 → 内容。库状态封装在 `ui/themeengine` 或 `ui/home` 的材质 Renderer 后；业务页面、Widget Host、Theme Runtime 不接触 `HazeState`、`Sky` 等具体后端类型。视觉预设只声明材质参数。

优先试 Haze 的小范围接入。**先固定一个与当前 Kotlin `2.2.10`、Compose BOM `2024.09.00`、compileSdk `36.1` 能编译的版本，并验证真实 APK**；不要直接追最新版本并为它升级整套构建链。若兼容性或渲染确实阻塞，再验证 Cloudy；最终只留下一个背景模糊后端，不同时依赖两个。

Android API 31+ 检查真正的 backdrop blur；Android 10 / API 29 默认使用低透明 tint + 局部 gradient highlight + 轻阴影的性能友好退化。其 `fallbackAlpha` 按壁纸内容和文字可读性校准，绝不能回到 0.78 白色遮罩。不要在 API 29 默认启用慢速 CPU/RenderScript 动态模糊。壁纸切换、拖拽、翻页与主题预览时不得出现冻结残影、黑块、闪烁或卡住。

技术判断依据：[Haze Android 平台说明](https://chrisbanes.github.io/haze/dev/blur/platforms/)记载 Android 30 及以下默认采用 scrim，实验性旧系统模糊需额外启用；[Haze 2 发布记录](https://github.com/chrisbanes/haze/blob/main/CHANGELOG.md)显示新版对 compile SDK 的要求已前进；[Cloudy 官方 README](https://github.com/skydoves/Cloudy/blob/main/README.md)同样将旧系统 CPU 模糊默认关闭。Android 的 `RenderEffect` 从 API 31 起提供，且对自身 RenderNode 模糊不能替代背景采样，见[官方 API 文档](https://developer.android.com/reference/android/graphics/RenderEffect)。

**UI-1 完成证据**：同一壁纸下 Glass Widget / Dock 的预览与实景对照、API 29 与 API 31+ 截图、依赖版本与性能选择记录、非 Glass 主题回归。

## 4. UI-2：首页 Launcher 外观

只改 `VirtualHomeScreen.kt`、`WorkspacePage.kt`、`LifeBentoPage.kt`、`ThemeWidgetFrame.kt`、`HomeHotseat.kt`、`AppIconItem.kt` 等表现层。`DropResolver`、`FolderDropResolver`、Workspace Repository、Pager state、resize 与 Hotseat 持久化保持原样。

首页从上到下：轻量 Status Bar；时间/环境 Widget；角色/生活 Widget；6–8 个有视觉身份的核心 App；页指示器；Dock。壁纸需要真实可见。第一屏的角色信息只保留姓名、地点/当前状态、一句动态，图片资产未获正式授权前继续用现有占位，不临时生成人物脸。

不要把“Living / Social / World / Create”写成四个不可拖动的功能页。它们是**首次安装默认排布的内容建议**：

| 分组 | 建议入口 |
| --- | --- |
| Living | 时间、角色状态，消息、生活、动态、相册、联系人、信箱、记忆、应用库中挑 6–8 个 |
| Social | 消息、联系人、动态、信箱、通话、相册 |
| World | 生活、地图、日记、记忆、关系、他的手机、世界书 |
| Create | 角色、剧场、主题、设置、Provider 与高级创作工具 |

当前 `WorkspaceSeed.fromLegacyOrder` 只造一个 Workspace 页，`VirtualHomeScreen` 还追加 `LIFE_BENTO_PAGE_ID`。执行者必须先按当前 Pager 展示顺序和首次安装流程验证四分组方案。如果调整新用户默认布局，只通过现有 `DesktopPage` / `DesktopItem` seed 表达，保留拖动与删除；已安装用户保持原有 `isHome`、页数和项目位置。**本轮验收不以强行变成恰好四页为目标**；优先达到第一眼 Living Home 的观感，避免为了页数改 Pager 或迁移数据库。

检查锁屏解锁和强停重启后仍回到可用首页；首次引导条只在真实 onboarding 状态显示，不能变成永久大白卡。虚拟 Status Bar 与 Home Bar 继续由现有 SystemUI 承担。

**UI-2 完成证据**：Milk / Glass / Diary / Mono 的首屏和第二页回归截图，加上同一 Workspace 下 AILUA Default / Soft Home / Midnight Glass 三主题样张；同一台设备调整前后 Workspace page/item/folder 行及 Widget span 精确一致；拖动、跨页、Folder 与 resize 冒烟无回归。

## 5. UI-3：Dock

Dock 是主题最明显的一层材料，使用 `HomeHotseat.kt` 投影现有五个 Hotseat slot，保持空 slot 可落图标、Folder 可展开、拖动 hover 与编辑边框可读。默认 4–5 个核心入口属于首次安装 seed；不能为改外观替换现有用户的 Hotseat 项。

Icon 与 Badge 以 100% 不透明度绘制，低透明度只作用于 Dock 背板。图标绘制不从容器继承 alpha。检查浅/深壁纸、外部 Icon Pack、文件夹预览、空槽和编辑状态。Dock 底部与虚拟 Home Bar 的距离在不同屏高和字体比例下保持一致。

**UI-3 完成证据**：四主题 Dock 放大截图、浅/深壁纸各一、拖动到空槽和 Folder 的录屏或截图；五个 slot 的原有点击和放置测试通过。

## 6. UI-4：图标系统

现有 `AppIconItem.kt` 使用 `IconVisualSpec` 控制容器，`IconResolver.kt` 统一外部来源，Home / Hotseat / Folder / App Library 走同一入口。只升级视觉资产、规格与回退，不另建 IconResolverV2。

- 最终来源优先级固定：**用户手动指定 → 当前 AILUA 主题自带图标 → 用户选择的 Android Icon Pack → AILUA 默认 Identity Icon → generic fallback**。当前 `IconResolver.resolveBitmap()` 先要求 `iconSourceOverrideId` 非空，再读取 manual map；施工须调整这一解析顺序，同时保持旧手动映射的读取兼容。主题图标与用户 Android Pack 如何组合须由 UI 明示覆盖关系，不能静默吞掉已选择来源。
- 官方主题可选 Default、Illustrated、Soft、Pixel、Minimal、Dark Neon 等不同图标语言。同一个 `chat` 在不同主题下允许完全不同的视觉，但仍沿用同一 `iconKey`、点击路径和 App Identity。
- 首批至少重绘：消息、生活、动态、相册、联系人、信箱、记忆、关系、日记、他的手机、剧场、应用库、设置、角色。至少 12 个核心图标遮住文字后仍能明显区分；不能只换统一圆形底色加 Material glyph。
- Built-in 图标与正式主题图标允许采用自有矢量或合法商用位图。统一安全区、视重、圆角和长标签基线；图标在 56 dp、缩小后的 Dock 与 Folder 缩略图仍可识别。
- 外部 Android Icon Pack / MTZ / ColorOS 图标保持现有 source override、手动映射和缺失 fallback。普通用户选官方主题时，缺失图标退回对应官方视觉身份，不出现透明空白或混乱的大小。矢量或 WebP 均可，优先保证识别度和 100% 不透明 Icon 本体。
- 风格变化只经 `ThemeResolver` / `IconVisualSpec` 与既有资产源传递。主题切换不得改 `sourceId`、点击目标或桌面布局。编辑抖动/拖动放大与图标材质分离。
- 新图标包至少覆盖核心入口和设置/主题入口；列出完整覆盖率表。未覆盖的低频工具逐项人工验收，不使用无意义同色圆圈占位。

**UI-4 完成证据**：至少 14 个首批图标的无文字识别板；每主题同一组核心 App 的 56 dp / Dock / Folder / App Library 对照；五级来源优先级逐项测试，外部图标包选择与缺失回退；应用点击、长按、拖动、Badge 不回归。

## 7. UI-5：六套官方 Theme Pack

当前 `ThemeWallpaper.kt` 先读外部 `wallpaperSourceId`，否则使用 `ThemeResolver` 的渐变；官方主题目前主要靠渐变，无法承担“壁纸为主视觉”的要求。在原有 ThemeSelection 与 ThemeResolver 链中增加**官方内置壁纸资产解析**，不添加第三套主题状态。解码继续使用有界尺寸和后台线程；裁切、浅/深色文本对比、横竖屏和低内存回退要实测。

第一视觉评审点先以同一 Workspace 制作 **AILUA Default / Soft Home / Midnight Glass** 三主题对照，截真机图后停下确认。它们决定母版、浅玻璃与深玻璃是否成立。评审通过后完成下面全部六套成品；只做名字、换 palette 或换底图而图标/Widget/Dock 仍相同，不算一套独立主题。

| 编号 | 主题 | 壁纸与气质 | 材质与图标 |
| --- | --- | --- | --- |
| 01 | AILUA Default | 现代、干净，最像真实手机；最终产品默认 | 轻玻璃、真实可辨的 App 图标 |
| 02 | Soft Home | 暖色书桌，可辨认书、茶杯、木纹、植物；不是动态房间 | Soft Glass、插画图标 |
| 03 | Rainy Study | 深色雨夜书房/人物氛围、暖灯 | Dark Glass、低饱和实体图标 |
| 04 | Sakura Diary | 粉白纸张、日记感 | Paper 为主，少 Blur 或无 Blur、插画图标 |
| 05 | Y2K Love PC | 粉紫、像素窗口、小窗口标题 | Paper / Solid Surface、Pixel icon，`blur = 0` |
| 06 | Midnight Glass | 深蓝/黑人物或抽象壁纸 | Dark Glass、实体渐变 Icon；展示真 backdrop blur |

六套主题共用 Launcher、Workspace、业务 Runtime。壁纸、palette、Widget、Dock、Icon、字体、Lock Screen 和 SystemUI accent 都从原 `AiluaThemeRuntime` 逐渐表达。图标配置包括来源、大小、标签颜色与标签显隐；Widget / Dock 配置包括样式、tint、alpha、blur、radius、border、highlight 和必要的 shadow。第一版 Theme JSON 不必把所有字段公开成编辑器参数。`ThemeResolver` 仍是最终投影，不因 JSON 扩展产生平行解析器。

官方包资源建议与已有外部包保持概念一致；第一轮无需强制把全部内置资源外置：

```text
theme/<theme-id>/
  manifest.json          # id、展示名、版本、来源/授权、缺失回退
  theme.json             # wallpaper/palette/widget/dock/icon/typography/systemui 选择
  wallpaper/home.webp
  wallpaper/lock.webp
  icons/chat.webp
  icons/living.webp
  icons/moments.webp
  icons/gallery.webp
  ...
  preview/home.webp
```

建立 Theme Pack 素材清单，每项记录：官方 ID、展示名、壁纸来源、图标集、Widget / Dock 材料档位、light/dark 文本方案、作者/授权依据、允许商用范围、文件 hash、导出尺寸、缺失回退。内部 MTZ/ColorOS 只提供结构参考和兼容样本，未授权资源不能拼贴成官方包。临时人物占位图不能冒充正式男主 ART；图未就绪时用授权场景/抽象图。

**UI-5 完成证据**：六套主题均有授权清单、完整图标覆盖表、可重复打包/预览/应用流程、首页与锁屏真机图；切换每套时壁纸、图标、Widget、Dock 协同变化，用户单独覆盖壁纸/图标仍可混搭。

## 8. UI-6：Theme Center 消费入口

当前 `ThemeCenterSheet.kt` 有“内置 / 我的主题 / 图标包 / 导入 / 配色 / 壁纸 / 图标样式”七个 Tab。普通用户只看到 **主题 / 壁纸 / 图标 / 我的** 四个入口。顶部直接展示可辨的真实桌面大预览；主题列表显示 AILUA Default、Soft Home 等官方完整主题，用一行自然语言说明其质感。主流程为“预览 → 应用 → 返回桌面”，不要求用户认识格式名。

“我的”放已下载主题、已收藏、自定义搭配；这些状态必须真实持久化后再显示，不能放死按钮。开发者模式开启后，“我的”额外显示导入主题文件、Android 图标包、Theme Lab。入口按 **设置 → 开发者 → Theme Lab → 导入外部主题** 可导航到达，内部 QA 不依赖神秘手势。原 `ExternalThemeCenter.kt` 和已导入主题数据保留；已导入主题继续可预览、应用、删除，使用中的来源删除时安全 fallback。

预览 `ThemePreview.kt` 与真实 Home 的壁纸、图标、Widget、Dock、palette、暗色状态保持一致。`ThemeStore.selection` 是唯一当前选择；预览不能提前污染持久选择。主题卡片应展示足够大的真实桌面局部，不以小色块替代主题视觉。

**UI-6 完成证据**：普通用户四入口截图与三步应用录屏；“我的”三种真实数据状态；开发者入口可达；预览与应用后的并排截图；混搭和删除 fallback 不丢 Workspace。

## 9. UI-7：Theme Import Pipeline 与 Theme Lab

此阶段修复现有归一化链的明显缺口，目标是抽取可用于 AILUA 的视觉资产，不执行 OEM 系统主题。沿用 `ExternalThemePackage`、`ThemeAssetStore`、`UnifiedThemeImporter` 和各格式 importer；保持压缩包安全边界、SAF 权限与已安装记录兼容。

### 9.1 明确可应用的 shell 与壁纸角色

`ExternalThemePackage.basePresetId` 已被读入/保存，但 `ExternalThemeRepository.apply()` 在 `preserveShell = false` 时固定设为 `milk`。当来源 **明确**声明有效 `basePresetId` 时，允许选择该 AILUA shell；未声明时保留当前 shell 或用明示的安全默认。官方生产导出的包必须明确自己的 Base。不可把 MTZ importer 的占位 `milk` 当作第三方作者指定的设计意图。

当前壁纸按列表首张选择；在归一化元信息中区分 `home`、`lock`、`other`，至少 Home 和 Lock Wallpaper 分别可预览/应用。旧包没有角色标签时沿用原排序与 fallback，不破坏现有已安装主题。`ThemeWallpaper.kt` 和锁屏壁纸消费同一角色解析，不各自猜文件名。

### 9.2 Icon Pack 与 OEM adapter 的范围

`AndroidIconPackParser.kt` 已读取 `appfilter.xml`、drawable list、calendar prefix；`AndroidIconPackResolver.kt` 目前把 calendar prefix 固定接 `1`。正式支持静态映射、package/AILUA alias、manual override、缺失 fallback，并按实际日期解析 Calendar 对应图标；测试月末、跨月与缺失某日资源时的静态 fallback。动态 Clock 延后。保持现有 installed-pack 扫描与损坏资源容错。

MTZ / HyperOS 只抽 metadata、preview、Home/Lock wallpaper、静态图标、颜色线索；ColorOS 只抽 themeInfo、preview、Home/Lock wallpaper、launcher 图标、颜色线索。SystemUI 脚本、framework overlay、可执行锁屏、字体引擎、声音和 OEM Widget Runtime 不执行。可将原文件保存在 raw 供内部分析，但正式包只包含授权可用的归一化视觉资产。

### 9.3 Theme Lab 生产工具

只在开发者模式提供 Theme Lab，在原主题链上组合 Imported source、Home Wallpaper、Lock Wallpaper、Icon Source、Base Skin（Default/Glass/Paper/Y2K）、Palette（自动/手动）、Widget/Dock blur、alpha、radius，并即时预览；最后可 **Export as AILUA Theme**。导出物须通过现有 `.ailuatheme` importer 再导入、预览、应用和重启，形成可重复 round trip。不要建独立 ThemeLabRuntime。

`ImportedThemePreview.kt` 目前手工拼假桌面。自动生成 `preview-home.webp` 时，采用实际 `ThemeResolver` + 临时 Preview Workspace Fixture，复用 Home 的 `WorkspacePage`、`ThemeWidgetFrame`、`HomeHotseat`、`AppIconItem` 等视觉 Composable 离屏或测试容器截图。预览 Fixture 不写持久 Workspace，不启用真实点击/拖拽；同一 theme 在预览与真实桌面的壁纸裁切、图标、材质必须一致。

**UI-7 完成证据**：显式 Base 应用、旧包 fallback、Home/Lock 分离、Calendar 日期/缺图回退、synthetic MTZ/ColorOS/AILUA 与 Android Pack 回归；Theme Lab 导出再导入与真实 Launcher 预览对照。不得下载商业主题充测试样本。

## 10. UI-8：App Library

`AppLibraryScreen.kt` 已有搜索、分类和 App 网格，继续沿用现有目录与 `AppRouter`，只改视觉。顶部搜索 App；下方按“社交 / 生活 / 世界 / 创作”显示较大的图标组，每组少文字、少外层卡片描边。分组只是展示方式，不替换用户 Workspace，也不改变搜索和启动结果。低频 Provider、创作工具可降低视觉权重，但仍能找到。

**UI-8 完成证据**：主目录、搜索结果、空结果、长标题、四主题截图；从目录启动每类 App 仍走原有路由。

## 11. UI-9：Lock Screen、Notification、Control Center

桌面通过三主题视觉评审后再处理。沿用 P5.5 SystemUI 的交互、通知数据与现有 `LocalAiluaTheme`；本轮只改变壁纸选择、材质、颜色、圆角、字体和版面。Lock Screen 以壁纸、时间、日期、Character Live Activity/Notification 为主，不像设置页。Notification 保持 Avatar、App、时间、内容的真实手机层次。Control Center 可以用本轮最明显的 Glass，但首页不能因此变成控制中心。

全 App 已由 `MainActivity` 的 `AiluaThemeProvider` 提供主题，不再造 provider。主题切换时 Chat、Living、Gallery 等页面仍用同一 palette；人物和聊天 Runtime 只读不改。需要保护图片上的文字时用局部 scrim，不用整屏乳白遮罩。

**UI-9 完成证据**：锁屏/通知/控制中心各有 Default、Soft Home、Midnight Glass 截图；亮暗文字可读、通知点击及控制中心手势不回归。

## 12. UI-10：真机视觉、性能与数据验收

每个 checkpoint 至少从在线设备保存实机截图，不用预览图冒充。最终至少覆盖 realme Android 16 与小米 `M2007J22C` Android 10 / API 29；若某台暂未在线，先完成可用设备的验证，报告中标明未覆盖项，设备可用后补测。API 31+ 必须确认真背景模糊，API 29 确认降级材质；不得把伪玻璃截图称作真 blur。

每次截图评审板使用统一命名并记录设备、API、主题选择、页码、壁纸来源和昼夜模式：

```text
dist/qa/P5.V-UI-Theme/<checkpoint>/<device>/
  01-default-home.png
  02-soft-home.png
  03-rainy-study.png
  04-y2k.png
  05-theme-center.png
  06-app-library.png
  07-lockscreen.png
  08-control-center.png
  ui-theme-review.jpg
```

前期未完成的主题按实际文件名标明 `pending`，不能拿别的主题冒名占位。评审板是一眼看全的拼图，原始全分辨率 PNG 必须同时保留。第一次停点至少交 Default / Soft Home / Midnight Glass 在**同一 Workspace**的原图和拼图。

| 验收维度 | 判定 |
| --- | --- |
| 默认桌面 | 遮掉 AILUA Logo 后仍像成熟手机桌面，而非 AI Dashboard |
| Soft Home | 书、茶杯、木纹、植物在玻璃下仍可辨；不变成白雾 |
| Dark Glass | 人物/主体不被灰色 Widget 吞掉，浅深文字均可读 |
| Y2K | `blur = 0` 仍是一套完整主题，证明主题不限于玻璃参数 |
| 图标 | 至少 12 个核心 App 遮住文字后仍可明显区分 |
| 四套旧主题 | Milk / Glass / Diary / Mono 可继续预览、应用、混搭 |
| 六套新主题 | 壁纸、图标、Widget、Dock、锁屏与系统 accent 协同，预览接近实景 |
| 兼容管线 | synthetic `.ailuatheme` / MTZ / ColorOS 与合法已安装 Android Pack 可导入/回退，旧数据不丢 |
| Android 10 / 16 | 旧系统 fallback 可用；新系统局部真 blur，无黑块/残影/崩溃 |

性能动作：页面横滑、拖拽 App、打开 Folder、下拉控制中心/通知、主题切换。记录同设备施工前后帧表现与肉眼持续卡顿情况；同屏最多保留 2–3 块明显 Glass Surface。若多层动态采样成本高，先减少 Glass 数量，不模糊整屏、不牺牲所有图标和文字清晰度。

数据安全：覆盖安装前后与强停重启后，对比 Workspace 页数、item 数、Folder、Hotseat rank、Widget span、当前角色、聊天数据、主题选择。真机自动化使用生产导航，不清应用数据；日志排查 crash、图片加载、权限与文件持久化。最终跑 `testDebugUnitTest`、`assembleDebug` 与必要的 Android instrumentation，报告准确测试数量和设备缺口。测试通过只证明不回归，视觉结论必须基于截图。

## 13. Checkpoint 与交付

各 checkpoint 可单独回滚，不把 200 个文件放进最后一个提交：

| Checkpoint | 建议 commit 标题 | 范围 |
| --- | --- | --- |
| UIR-0 | `Freeze UI rescue baseline` | 三份设计规格与原始截图 |
| UIR-1 | `Fix theme material rendering` | alpha、真实 blur、API 29 fallback |
| UIR-2 | `Redesign launcher home presentation` | Wallpaper / Living Home / 已有页的表现层 |
| UIR-3 | `Redesign dock and icon presentation` | Dock、图标视觉与解析优先级 |
| UIR-4 | `Add six production theme packs` | 版权清单与六主题资产、Runtime 映射 |
| UIR-5 | `Redesign theme center` | 四入口与真实预览 |
| UIR-6 | `Move external imports into Theme Lab` | 开发者入口与导出工具 |
| UIR-7 | `Improve theme normalization pipeline` | Base、Home/Lock、Calendar 与 adapter |
| UIR-8 | `Converge App Library` | App Library 表现层 |
| UIR-9 | `Converge Lock Notification Control Center` | SystemUI 表现层 |
| UIR-10 | `Accept UI rescue on real devices` | 两设备、报告、APK、截图板 |

第一次视觉停点发生在 UIR-2 之后、正式重绘图标和六主题之前：交 `01-default-home.png`、`02-soft-home.png`、`09-midnight-glass.png` 三张同一 Workspace 原图与 review board，等待视觉方向确认。当前仓库中的原始 `9246aa0` 必须留在历史中，不回退 Character Runtime。若一台手机暂时不可用，不用等待设备来完成文档、资源整理和可在线设备的施工。

最终交付：三份设计文档、主题资源与商用权利清单、Theme Lab 导出说明、Debug APK、简短实施报告、两设备截图板。验证后 `push origin local-pass3c-ai-runtime` 并核对远端 HEAD。停止在 UI/Theme Rescue，不开始正式人物 ART、在线 Character QA 或 P5.6。

## 14. 可直接发送的第一条施工指令

> 从 `local-pass3c-ai-runtime` 的 `9246aa046ab11f1e8deed2bae3ecdd41ccbc41d8` 开始执行 `AILUA P5.V UI & Theme Rescue`。Character Runtime、Workspace、Pager、Drag/Drop、Widget Host、Database、AppRouter 全部冻结。先读取最新 `ThemeRuntime.kt`、`ThemeResolver.kt`、`ThemeStore.kt`、`ThemeWidgetFrame.kt`、`HomeHotseat.kt`、`VirtualHomeScreen.kt` 和真实设备基线，再做 UI-0→UI-2。保留现有主题链，修复 Glass alpha 被 Renderer 强制夹到 0.78 以上的问题，给既有 GLASS 样式接入局部 backdrop blur；Android 10 必须有可读、低透明度的 fallback。以同一 Workspace 制作 AILUA Default、Soft Home、Midnight Glass 三主题真机对照及拼图，提交 checkpoint 和 Debug APK 后停下等待视觉方向确认。在此之前不要批量迁 App、不要改业务 Runtime、不要开始 ART 或 P5.6。

视觉方向确认后从最新 HEAD 继续 UI-3→UI-10，完成六套官方主题、Theme Center、Theme Lab、导入归一化与两设备视觉验收。若某阶段有明确技术阻塞，先记录可复现证据并做局部修复；不得用新 Launcher/Theme 架构绕过问题。
