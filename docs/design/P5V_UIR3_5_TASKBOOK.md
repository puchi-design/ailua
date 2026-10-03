就按这个顺序继续，而且**从 `161fa13` 开始冻结 Launcher 视觉骨架，不再重新排首页**。

我刚又对了最新仓库。现在 `ThemeCatalog` 已经同时保留旧的 Milk / Glass / Diary / Mono，以及这轮新样张 Default / Soft Home / Midnight Glass；`AppIconItem` 仍然主要靠 Material Icon glyph + 身份色生成，所以 **UIR-3 的核心确实应该从“正式图标资产”下手**。当前 Theme Center 还是 7 个栏目塞进一个 BottomSheet，也正好留给后面的 UIR-5 重做。\
[ThemeCatalog.kt](https://github.com/puchi-design/ailua/blob/161fa130f683d0c85368971e384583d410731aec/app/src/main/java/com/example/ui/themeengine/ThemeCatalog.kt) · [ThemeCenterSheet.kt](https://github.com/puchi-design/ailua/blob/161fa130f683d0c85368971e384583d410731aec/app/src/main/java/com/example/ui/themecenter/ThemeCenterSheet.kt)

# P5.V UIR-3 → UIR-5 总任务书

## 基线与冻结

施工基线：

```text
branch:
local-pass3c-ai-runtime

HEAD:
161fa130f683d0c85368971e384583d410731aec
```

把这一版视为：

```text
Launcher Layout Frozen
Theme Runtime Frozen
Workspace Frozen
Character Runtime Frozen
```

这意味着接下来允许改变：

```text
图标长什么样
壁纸是什么
主题配色
Widget / Dock 材质参数
Theme Center 怎么展示
```

但禁止重新改变：

```text
首页 Widget 位置
Workspace 网格
Pager
拖拽
Folder
Dock 数据结构
Character Widget 信息结构
ThemeStore → ThemeResolver → Runtime 链路
```

---

# UIR-3 — Production Icon System

目标不是“再换一批 Material 图标”。

目标是：

> **从现在开始，AILUA 首页上的图标本身就是商品视觉资产。**

当前 `AppIconItem` 已经有不错的 fallback：

```text
外部 bitmap
→ Theme container
→ Material glyph
```

保留。

只在现有 IconResolver 体系里增加 **Built-in Theme Icon Pack**。

不要建第二套 Icon 系统。

## 正确解析顺序

最终统一：

```text
用户 manual override
        ↓
用户指定 Android Icon Pack / Imported Theme
        ↓
当前 AILUA 官方主题自带图标
        ↓
AILUA Default 官方图标
        ↓
现有 vector identity fallback
```

注意：

**用户主动选择的外部 Icon Pack 永远优先于官方主题图标。**

所以主题切换不能把用户自己的图标设置覆盖掉。

---

## 官方图标首批范围

不要一次做所有边角功能。

先做首页和用户高频可见的 18 个：

```text
chat
living
moments
gallery

contacts
mailbox
call
memories

relations
diary
check_phone
theater

world
lore
creator
assistant

apps
settings
```

其他应用继续 fallback。

这样不会为了画 80 个图标拖死整个 UI Rescue。

---

# 图标不能再只是“换底色”

六套主题至少形成 5 种真正不同的视觉语言。

### AILUA Default

定位：

> 真手机感。

图标：

```text
干净 squircle
中高饱和色块
简单自绘 glyph
不描白圈
少阴影
```

类似现代手机图标，而不是 Material icon demo。

---

### Soft Home

定位：

> 可爱、温暖、收藏感。

图标应该像：

```text
小信封
茶杯
小花
照片
书本
电话
星星
```

采用统一插画笔触。

允许：

```text
米色底
粉绿蓝棕
少量手绘阴影
```

这是最应该明显区别 Default 的一套。

---

### Rainy Study

图标：

```text
低饱和蓝灰
暖黄点缀
墨水 / 书房感
```

可以仍然较现代，但不要和 Midnight 只差一个颜色。

---

### Sakura Diary

图标：

```text
纸张
贴纸
手账
粉色印章
细线插画
```

允许不规则的小装饰。

---

### Y2K Love PC

必须真正做成：

```text
16/32-bit pixel aesthetic
old PC / desktop icon
粉紫蓝
像素边缘
```

**禁止圆角 Material 卡片。**

这个主题甚至可以：

```text
IconShape = NONE / square
shadow = 0
```

---

### Midnight Glass

图标不做透明玻璃圈。

采用：

```text
深色实体底
蓝紫渐变
少量 cyan / pink glow
白色或浅蓝 glyph
```

视觉应该像夜间系统 App，而不是“玻璃按钮”。

---

# 图标资产建议

不要给 ThemeRuntime 增加一堆字段。

现有：

```kotlin
AiluaThemePreset.iconStyleId
```

直接继续承担：

> 当前主题使用哪个官方 Icon Pack。

例如：

```text
iconStyleId =
default_icons
soft_home_icons
rainy_study_icons
sakura_icons
y2k_icons
midnight_icons
```

Resolver 不需要知道图片文件名。

IconResolver 内部根据：

```text
ThemeStore.selection.themePresetId
→ ThemeCatalog
→ iconStyleId
```

寻找内置图标。

这仍然属于现有架构。

---

# UIR-3 验收

同一 Workspace：

```text
Default
Soft Home
Y2K
Midnight
```

分别截图。

遮掉壁纸和 Widget，只看图标时，也应该能判断：

> 这是四套不同主题。

如果只是：

> 同一个图标换了背景色，

直接 FAIL。

---

# UIR-4 — 六套首发主题

这一轮正式把现在的三套视觉 Sample 升成真正产品主题。

首发固定：

| Theme ID | 名称 | 定位 |
|---|---|---|
| `default` | AILUA Default | 清透真实手机 |
| `soft_home` | Soft Home | 暖色书桌 / 插画 |
| `rainy_study` | Rainy Study | 雨夜书房 / 乙女 |
| `sakura_diary` | Sakura Diary | 樱花手账 |
| `y2k_love` | Y2K Love PC | 少女复古电脑 |
| `midnight_glass` | Midnight Glass | 深夜高级玻璃 |

旧：

```text
milk
glass
diary
mono
```

**不要删除。**

因为旧用户选择可能已经存进：

```text
SharedPreferences
```

它们继续作为：

> Legacy / Classic Themes

存在。

只是普通 Theme Center 首页不再把它们作为首发产品重点展示。

---

# 01 AILUA Default

目前这张已经可以作为基础。

接下来只需要：

```text
正式图标
更干净的 Dock 图标比例
正式 preview
```

不建议大改现在壁纸。

它承担：

> “第一次打开 AILUA 不会吓到普通用户。”

---

# 02 Soft Home

当前这张书桌虽然只是临时程序绘制图，但方向已经验证。

正式版可以替换为：

> 更完整、更精致的书桌/卧室/咖啡桌壁纸。

但仍然只是一张图片。

不要做动态场景。

材质继续：

```text
soft glass
浅暖 tint
低 alpha
```

正式图标必须成为这套主题最大的识别点。

---

# 03 Rainy Study

这套是接下来新增里最值得做的一套。

壁纸：

```text
深夜窗户
雨
桌灯
书桌
人物可以只留剪影/局部
```

不要直接上正式男主 ART。

现在先验证 Theme。

Widget：

```text
dark translucent
比 Midnight 稍暖
```

Accent：

```text
warm amber
muted blue
```

整体感觉：

> 深夜两个人还醒着。

---

# 04 Sakura Diary

这里可以继承旧 `Diary` 的：

```text
Paper
Serif
低 blur
```

但不要简单改名字。

正式做成：

```text
樱花纸张壁纸
手账贴纸 Icon
Paper Widget
Paper Dock
```

这个主题反而应该证明：

> AILUA Theme ≠ 全部毛玻璃。

---

# 05 Y2K Love PC

这是六套里差异最大的一套，所以一定要做。

壁纸可以是：

```text
粉紫 old desktop
格纹
小窗口
像素装饰
```

Widget：

```text
矩形
2–6dp radius
solid / paper
1px border
```

Dock：

可以视觉上变成：

```text
taskbar
```

但注意：

> 只是 Renderer 换皮。

Dock 数据结构仍然是原 Dock。

不要重新实现 taskbar。

Typography：

```text
mono / pixel-like system fallback
```

不需要为了这个主题引入奇怪字体版权。

---

# 06 Midnight Glass

现在已经基本成立。

UIR-4 主要补：

```text
正式 Midnight 图标
正式壁纸
Android 12+ 真 blur 验证
```

这里必须成为：

> Haze 真效果展示主题。

Android 10 继续 fallback。

---

# 壁纸资产规则

正式六套之后，当前 `THEME_ASSET_MANIFEST.json` 要升级。

每个资源至少记录：

```text
themeId
path
sha256
width
height
source
license/provenance
status
```

用户正式上架资产不得出现来源不清。

外部 MTZ / ColorOS 提取的第三方壁纸：

> 只能内部参考/测试，不能直接成为官方发布资源，除非有明确授权。

---

# UIR-4 不做什么

六主题阶段不要顺手开：

```text
Theme Store 后端
云同步主题
付费系统
主题账户
主题创作者社区
在线素材市场
```

这轮只有：

> 本地内置六套真正能展示的产品主题。

---

# Android 12+ Blur

不用阻塞正式图标施工。

但是 UIR-4 **结束前**必须补一次 Android 31+：

```text
Soft Home
Midnight Glass
```

两套测试。

主要看：

```text
Haze 是否真的工作
Widget blur 是否正常
Dock blur 是否正常
横滑是否明显掉帧
拖动图标是否闪烁
```

如果没找到 Android 12+ 设备：

> UIR-4 可以提交，但不能标记 Glass Production Verified。

---

# UIR-5 — Theme Center 重做

等六套主题真的好看之后再做这里。

否则主题中心只是把难看的主题展示得更漂亮，没有意义。

当前 7 个栏目：

```text
内置
我的主题
图标包
导入
配色
壁纸
图标样式
```

正式产品入口改成四个：

```text
主题
壁纸
图标
我的
```

---

# Theme Center 不再像设置面板

现在是：

> 七个文字按钮 + 配置项。

新版本应该像：

> 手机主题商店 / 壁纸商店。

首页首先看到主题图。

## 「主题」

顶部：

```text
AILUA Theme
让这台手机变成你的世界
```

然后大卡展示：

```text
Soft Home
[大型手机预览]

书与茶的午后
暖色 · 插画 · Soft Glass

[应用]
```

建议一列大卡，不要现在一行两个小卡。

手机主题是视觉商品。

预览图必须足够大。

---

# 六套主题可以做分类

不要搞复杂推荐算法。

简单分：

```text
推荐
氛围
复古
极简
```

甚至首版只：

```text
全部主题
```

都可以。

六套主题不值得先做复杂分类。

---

# 「壁纸」

只展示：

```text
官方壁纸
已导入壁纸
```

点击立即 Preview。

用户确认后 Apply。

这里不需要用户知道 ThemePreset。

---

# 「图标」

展示：

```text
AILUA Default
Soft Illustration
Diary
Pixel Y2K
Midnight
```

以及：

```text
已安装 Android 图标包
```

Android 图标包也不要写：

> ADW / appfilter

普通用户看到：

> 我的图标包

即可。

---

# 「我的」

显示：

```text
当前主题
收藏
已导入
自定义搭配
```

开发者模式关闭：

**看不到 MTZ / ColorOS。**

---

# Theme Lab

现有这些东西：

```text
ImportThemeSection
IconPacksSection
UnifiedThemeImporter
MtzThemeImporter
ColorOsThemeImporter
```

全部保留。

但迁移到：

```text
设置
→ 开发者选项
→ Theme Lab
```

Theme Lab 页面：

```text
外部主题导入
Android 图标包
Theme Asset Inspector
```

里面才显示：

```text
AILUA
MIUI / HyperOS MTZ
ColorOS Theme
Android Icon Pack
```

这样就完全符合我们现在的产品逻辑：

> **普通用户消费主题。\
> 我们和高级用户生产主题。**

---

# Theme Center 一个很重要的修正

当前 `ThemePreview` / `ImportedThemePreview` 还是“手工拼的小桌面”。

UIR-5 应逐渐改成：

> **真实 Launcher Preview。**

不是重新写 Preview UI。

最好做一个：

```text
ThemePreviewFixture
```

给固定：

```text
时间
角色
5 App
Dock
Wallpaper
```

然后复用真实：

```text
Widget renderer
Icon renderer
Dock renderer
Wallpaper renderer
```

Preview 和正式桌面才能永远一致。

不需要让完整 Workspace 数据库跑进 Preview。

只是复用真实视觉组件。

---

# Theme Card 应该展示什么

不用搞商品详情页。

首版主题卡：

```text
[9:16 Preview]

Soft Home

书与茶的午后
插画 · 暖色 · 轻玻璃

✓ 当前主题
```

点进去才出现：

```text
全屏预览
应用主题
只用壁纸
只用图标
```

这也利用了现有：

```text
ThemeSelection
```

本来就支持 Mix & Match。

---

# Legacy Theme 怎么处理

Milk / Glass / Diary / Mono：

不要删除。

可以放在：

```text
我的
→ 经典主题
```

或者：

```text
主题
→ AILUA Classics
```

但不跟首发六套抢第一屏。

尤其现在：

```text
ThemeCatalog.DEFAULT_ID = milk
```

**这一轮先不要强改。**

等六套完整通过、首次安装迁移逻辑明确以后，再决定新安装是否默认切：

```text
default
```

否则容易无意义地制造老用户迁移问题。

---

# 提交顺序

这次我建议直接定死：

```text
UIR-3A
Built-in production icon resolver

UIR-3B
18 core Default icons

UIR-3C
Soft / Sakura / Y2K / Midnight icon packs

UIR-3D
Icon real-device comparison

UIR-4A
Rainy Study

UIR-4B
Sakura Diary

UIR-4C
Y2K Love PC

UIR-4D
Polish six production themes

UIR-4E
Android 31+ blur verification

UIR-5A
Four-tab Theme Center

UIR-5B
Large theme preview cards

UIR-5C
Wallpaper / Icon / My sections

UIR-5D
Move MTZ / ColorOS / raw import into Theme Lab

UIR-5E
Theme Center real-device acceptance
```

---

# 第一轮先做到哪里停

不要让 Codex 一晚上把 Theme Center 都重写掉。

**下一停点放在 UIR-3D。**

也就是：

> 先把正式图标跑通，然后拿当前已有三主题 + Y2K 图标概念做一次真机对照。

要求截图：

```text
Default
Soft Home
Midnight Glass
Y2K icon prototype
```

如果图标这一关过了，再让它继续做 Rainy / Sakura / Y2K 三套完整主题。

---

## 可以直接发给 Codex 的执行指令

> 从 `local-pass3c-ai-runtime` / `161fa130f683d0c85368971e384583d410731aec` 开始执行 P5.V UIR-3。`161fa13` 视为 Launcher/Home/Workspace/Theme Runtime 冻结视觉基线，不允许重构 ThemeStore、ThemeResolver、AiluaThemeRuntime、Workspace、Pager、Drag/Drop、Folder、Dock 数据结构或 Character Runtime。\
>
> 本轮只建立正式 Built-in Theme Icon Pack，并完成 18 个核心 AILUA App 的商品级图标。继续使用现有 `AiluaThemePreset.iconStyleId` 作为官方图标包选择键；只允许在现有 IconResolver 链内增加 bundled icon resolution，不创建第二套 Icon Engine。用户手工图标和外部 Android/Icon Theme override 必须继续优先。缺失官方图标必须回退到现有 `AppVisualIdentity`，不能导致空图标。\
>
> 第一阶段完成 Default、Soft Home、Midnight Glass 三套正式图标语言，并制作 Y2K Love PC 的一套可验证 prototype。禁止仅用同一 Material glyph 换背景颜色冒充不同主题。Soft Home 必须体现插画图标语言；Y2K 必须体现像素/旧电脑图标语言；Midnight 必须使用实体深色/渐变图标，不恢复玻璃白圈。\
>
> 保持当前真实 Workspace、Widget、Dock、角色、聊天和用户设置不变。在小米 M2007J22C Android 10 上覆盖安装，跑完整离线测试并输出四主题同 Workspace 真机对照板。完成后停在视觉评审点，不继续 Rainy Study、Sakura Diary、Theme Center，也不开始 P5.6。

这一轮如果图标做好，**你现在这套 UI 会再明显抬一个档次**。目前壁纸和材质已经过线，下一眼最暴露“工程味”的就是那些 Material glyph。
