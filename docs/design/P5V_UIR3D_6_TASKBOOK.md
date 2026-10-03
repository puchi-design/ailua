你睡觉前直接把下面这份发给 Codex。**这不是继续乱加功能，而是把现在已经救回来的 UI 一口气收成“能卖”的版本。**

当前顺序我建议固定成：

**UIR-3 正式图标收口 → UIR-4 六套完整主题 → UIR-5 Theme Center 商品化 → UIR-6 真机视觉总验收 → 再开始正式角色 ART。**

人物 Runtime、聊天、世界、关系、主动消息这些全部先冻结。

---

# AILUA P5.V — UIR-3D → UIR-6 夜间总任务书

## 0. 开工原则

以 **UIR-3 当前任务完成后的最新远端 HEAD** 作为唯一施工基线。

不要提前写死 `161fa13`，因为 UIR-3 正式图标完成后必然会有新提交。

施工前：

```text
git checkout local-pass3c-ai-runtime
git pull
git rev-parse HEAD
```

将该 HEAD 记录到：

```text
docs/design/P5_V_UI_PRODUCTIZATION_REPORT.md
```

---

# 1. 本轮唯一目标

把 AILUA 从：

> “已经不难看了”

推进到：

> **“打开截图就像一个真正有商品价值的 AI 手机主题产品。”**

本轮不是继续堆业务。

不是继续修 Character Runtime。

不是继续造 Theme Engine。

只做：

```text
正式图标
正式主题
主题中心
真机视觉收口
```

---

# 2. 冻结边界

以下全部冻结，禁止重构：

```text
CharacterRuntime
RomanceRuntime
ProactiveRuntime
WorldRuntime
ChatRuntime
MemoryRuntime
CallRuntime

Workspace database
Workspace page model
DesktopItem
Folder model
Widget placement
Widget resize semantics
Pager
Drag / Drop
Hotseat data model
AppRouter

ThemeStore
ThemeSelection
ThemeResolver 主链
AiluaThemeRuntime 主结构
UnifiedThemeImporter 主结构
```

允许：

```text
ThemeCatalog 增主题配置
PaletteCatalog 增 palette
Wallpaper asset
Built-in icon asset
IconResolver 增官方主题图标解析
Renderer 参数调整
Theme Center UI
Theme Lab UI
Preview UI
```

---

# 3. 禁止再发生的事

不允许出现：

```text
ThemeEngineV2
LauncherV2
HomeV2
GlassEngineV2
ThemeMarketplaceBackend
IconEngineV2
CharacterThemeRuntime
```

不允许为了 Y2K：

> 重写 Dock。

不允许为了 Soft Home：

> 做动态房间。

不允许为了 Rainy Study：

> 做天气引擎。

不允许为了 Theme Center：

> 改 ThemeStore 数据模型。

**主题永远只负责“怎么画”。**

---

# 4. UIR-3D — 正式图标系统收口

当前 prototype 已证明：

```text
Default
Soft
Midnight
Y2K
```

四种视觉语言成立。

接下来不能再继续“试图标”，直接进入 Production。

---

# 5. 首批正式 Icon 范围

固定只做 18 个核心 App：

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

不要扩到 30、40 个。

其余继续使用现有 fallback。

---

# 6. 图标解析优先级

现有 IconResolver 继续使用。

最终顺序必须是：

```text
manualIconOverrides
        ↓
用户明确指定的 Android Icon Pack / Imported Theme
        ↓
当前官方 AILUA Theme Icon Pack
        ↓
AILUA Default 官方 Icon
        ↓
现有 AppVisualIdentity vector fallback
```

非常重要：

> **用户自己的图标选择优先。**

换主题不能偷偷把用户指定 Icon Pack 清掉。

---

# 7. 官方 Icon Pack 组织

不要把 108 张图片硬编码进 `when(iconKey)`。

建议资源概念：

```text
drawable-nodpi/
  icon_default_chat.webp
  icon_default_living.webp
  ...

  icon_soft_chat.webp
  icon_soft_living.webp
  ...

  icon_rain_chat.webp
  ...

  icon_sakura_chat.webp
  ...

  icon_y2k_chat.webp
  ...

  icon_midnight_chat.webp
```

然后建立非常薄的：

```text
BuiltInThemeIconRegistry
```

只负责：

```text
iconStyleId + iconKey
→ drawable resource
```

它不是新架构。

只是资源 lookup。

---

# 8. 六套 Icon Art Direction

## A. Default

关键词：

```text
现代
清晰
轻 3D
低装饰
真实手机
```

修正 prototype：

- 减少“AI 3D icon pack”味；
- living 不再只是普通房子；
- Settings 可以保留 gear；
- Gallery 保持明显；
- Chat 保持简单高识别度；
- 光源统一左上；
- 主体面积统一约 70%。

---

## B. Soft Home

关键词：

```text
水彩
纸感
温暖
小物件
插画
```

允许：

```text
茶杯
信纸
花朵
相册
书
小灯
```

禁止：

- 每个图标都画成复杂小场景；
- 过多花纹；
- 56dp 看不清。

原则：

> 一个主体 + 一个最多很小的辅助元素。

---

## C. Rainy Study

关键词：

```text
雨夜
蓝灰
墨水
暖灯
书房
```

不是 Midnight 的换色版。

颜色：

```text
muted navy
slate blue
warm amber
paper beige
```

材质：

```text
semi-flat
ink / enamel
```

禁止：

> 高饱和霓虹。

---

## D. Sakura Diary

关键词：

```text
手账
纸张
贴纸
樱花
邮票
印章
```

Icon 可以有：

```text
纸边
胶带
花瓣
手绘线
```

但必须保持点击区正常。

---

## E. Y2K Love PC

关键词：

```text
pixel
old PC
pink
lavender
blue
control panel
```

正式版不要所有 Icon 同一个紫色方块。

允许背景轻微变化：

```text
pink
lavender
baby blue
cream
```

但保持同一 palette。

Settings 不必须是普通 gear：

```text
Control Panel
switch panel
old PC settings
```

---

## F. Midnight Glass

关键词：

```text
night
deep blue
violet
subtle cyan
rim glow
```

修正 prototype：

- 降低 20–30% 游戏装备感；
- living 不要 RPG 房屋；
- 减少金属塑料；
- 保持实体底；
- 不使用玻璃透明白圈。

---

# 9. Icon 尺寸验收

所有正式图标至少检查：

```text
144px review
96px
56dp real launcher
48dp compact
Dock 实际尺寸
```

重点看：

```text
主体是否糊
小细节是否消失
亮边是否过厚
不同 App 是否还能识别
```

如果 144px 很漂亮但 56dp 糊成一坨：

**FAIL。**

---

# 10. UIR-4 — 六套 Production Theme

完成图标之后，正式做六套。

固定名单：

```text
default
soft_home
rainy_study
sakura_diary
y2k_love
midnight_glass
```

旧：

```text
milk
glass
diary
mono
```

保留兼容。

不删除。

---

# 11. Theme 01 — AILUA Default

当前视觉已经基本成立。

不要大改布局。

收口内容：

```text
正式 Default Icons
正式 preview
壁纸色彩微调
Dock icon scale
Widget / Dock contrast
```

目标：

> 像一台普通用户也愿意长期使用的手机。

Default 不能过度乙女。

它负责第一印象。

---

# 12. Theme 02 — Soft Home

当前书桌方向保留。

正式版：

```text
warm desk / room wallpaper
soft watercolor icons
warm glass
low contrast
```

禁止：

> 做动态书房。

就是一张壁纸。

重点是：

```text
壁纸 + 图标 + Glass
```

---

# 13. Theme 03 — Rainy Study

新增。

壁纸建议：

```text
夜窗
雨痕
书桌
暖灯
书页
角色存在感只作为极轻背景暗示
```

不用正式角色 ART。

可以：

```text
空椅子
外套
杯子
书
灯
```

营造：

> “他刚刚还在这里。”

Widget：

```text
dark semi-glass
alpha 0.20–0.26
blur 24–28
warm highlight
```

Dock 同类。

---

# 14. Theme 04 — Sakura Diary

旧 Diary 可以作为技术参考，但必须变成新的 Production Theme。

壁纸：

```text
cream paper
sakura
stamp
tape
journal margin
```

Widget：

```text
PAPER
blur = 0
alpha 0.94–0.98
```

Dock：

```text
paper strip
```

图标：

```text
sticker / paper / stamp
```

目的：

> 证明 AILUA Theme 不是只会毛玻璃。

---

# 15. Theme 05 — Y2K Love PC

这是差异最大的一套。

Wallpaper：

```text
pink / lavender
old desktop pattern
window fragments
grid / checker
```

但注意：

> 壁纸里不要画假按钮，让用户误以为能点击。

Widget：

```text
solid/paper
radius 2–6dp
1px border
shadow 很小或 0
```

Dock 视觉做成：

> taskbar。

但仍然是现有 Hotseat Renderer。

不要改数据结构。

Typography：

```text
monospace / bitmap-feeling fallback
```

不要引入版权不明字体。

---

# 16. Theme 06 — Midnight Glass

当前已经接近成品。

收口：

```text
正式 Icon
正式 Wallpaper
Android 31+ 真 blur
减少游戏感
```

它承担：

> 展示 AILUA 最漂亮的高级 Glass。

---

# 17. 六套 Theme 差异必须达到这个标准

遮住 Theme 名称，只看截图：

必须至少能明显辨认：

```text
Default
Soft Home
Y2K
Midnight
```

Rainy 与 Midnight 也不能只是：

> 一个偏灰、一个偏蓝。

Sakura 与 Soft 也不能只是：

> 一个粉一点、一个黄一点。

必须是：

**材质语言不同。**

---

# 18. 主题资产 Manifest

升级：

```text
docs/design/THEME_ASSET_MANIFEST.json
```

每张正式图标、壁纸记录：

```text
themeId
assetType
iconKey
path
sha256
width
height
source
license
status
```

正式资源状态：

```text
production
```

临时：

```text
prototype
temporary
```

必须分清。

---

# 19. UIR-4 Android 31+ Blur 验证

在 UIR-4 完成前必须补。

找 Android 12+ 设备。

至少实测：

```text
Soft Home
Midnight Glass
```

检查：

```text
Haze backdrop 实际生效
Widget
Dock
页面 swipe
App drag
Folder
Control Center
```

截：

```text
android31-soft-home.png
android31-midnight.png
android31-midnight-drag.png
```

如果真 blur 性能很差：

**优先减少 blur surface 数量。**

不要重写 Theme Engine。

---

# 20. UIR-5 — Theme Center 商品化

这一步必须在六套主题完成后开始。

当前 Theme Center 是开发者工具。

要改成消费者产品。

---

# 21. 普通用户只看四个入口

固定：

```text
主题
壁纸
图标
我的
```

不再直接展示：

```text
内置
图标包
导入
配色
MTZ
ColorOS
```

---

# 22. Theme Center 首页

不要再：

> 一行两个小卡。

改成视觉商品展示。

示例：

```text
AILUA Theme

让这台手机变成你的世界


┌─────────────────────┐
│                     │
│    大型 9:16 Preview │
│                     │
└─────────────────────┘

Soft Home
书与茶的午后
插画 · 暖色 · 轻玻璃

[应用]
```

下一张：

```text
Rainy Study
雨夜书房
深色 · 静谧 · 暖灯
```

---

# 23. Theme Detail

点 Theme 卡进入：

```text
大预览
主题名称
一句描述
材质标签

[应用完整主题]

只使用壁纸
只使用图标
```

利用现有 ThemeSelection Mix & Match。

不要再造一套配置。

---

# 24. 壁纸页

简单：

```text
官方壁纸
我的壁纸
已导入
```

卡片显示真实 thumbnail。

点击先 Preview。

然后：

```text
应用壁纸
```

不要自动连带换 Theme。

---

# 25. 图标页

展示：

```text
Default
Soft Illustration
Rainy
Sakura
Y2K Pixel
Midnight
```

然后下面：

```text
我的图标包
```

如果检测到 Android Icon Pack：

显示包名和预览。

普通用户不需要看到：

```text
appfilter.xml
ADW
drawable
```

---

# 26. 我的

首版：

```text
当前主题
最近使用
已导入主题
自定义搭配
```

暂时不做：

```text
账号收藏
云同步
主题购买
```

---

# 27. Theme Lab

原来这些：

```text
ImportThemeSection
IconPacksSection
UnifiedThemeImporter
MtzThemeImporter
ColorOsThemeImporter
```

全部保留。

迁到：

```text
设置
→ 开发者模式
→ Theme Lab
```

---

# 28. Theme Lab UI

内容：

```text
Theme Lab

[导入外部主题]

支持：
AILUA
MIUI / HyperOS
ColorOS

[已安装 Android 图标包]

[Theme Asset Inspector]

[Export AILUA Theme]
```

这才是我们内部生产工具。

---

# 29. MTZ / ColorOS 的重新定位

明确写进代码注释和文档：

> OEM importers are asset adapters, not full OEM theme runtimes.

也就是：

```text
MTZ
↓
wallpaper
icons
preview
metadata

ColorOS
↓
wallpaper
icons
preview
metadata
```

不要继续浪费时间模拟：

```text
SystemUI
OEM lockscreen runtime
font engine
sound theme
framework overlay
```

---

# 30. Theme Preview 必须改

当前 Preview 还是手工拼。

UIR-5 改成统一 fixture。

建立：

```text
ThemePreviewFixture
```

固定：

```text
9:41
角色 Widget
4 App
Dock
Wallpaper
```

但它必须复用真实：

```text
ThemeWidgetFrame
AppIconItem / official icon renderer
HomeHotseat material renderer
ThemeWallpaper
```

不要重新手写一个“假 Launcher”。

---

# 31. Preview 不接真实数据库

避免过度工程化。

`ThemePreviewFixture` 使用静态 preview data：

```text
角色：沈砚
状态：在书店整理新到的旧书
时间：21:47
天气：小雨
```

只复用真实视觉组件。

不读取 WorkspaceGraph。

---

# 32. UIR-5 视觉标准

Theme Center 应该给人的感觉是：

> “选皮肤。”

不是：

> “改系统配置。”

禁止出现大量：

```text
checkbox
switch
technical dropdown
developer terminology
```

---

# 33. UIR-6 — 全局视觉收口

Theme Center 完成后，不再开发功能。

开始总验收。

---

# 34. 必须截图的页面

六主题 Home：

```text
01-default
02-soft
03-rainy
04-sakura
05-y2k
06-midnight
```

然后：

```text
07-theme-center
08-theme-detail
09-wallpaper-center
10-icon-center
11-my-theme
12-theme-lab
```

再补：

```text
13-app-library
14-lockscreen
15-notification
16-control-center
17-chat
18-living
```

---

# 35. 评审板

生成：

```text
ui-product-review-board.jpg
```

最好：

```text
6 Theme Home
+
Theme Center
+
System UI
```

一次性肉眼看。

---

# 36. 真机设备矩阵

至少：

```text
Xiaomi Android 10
Android 12+
```

如果 realme Android 16 还可用：

再补：

```text
Android 16
```

---

# 37. 必跑回归

完整离线：

```text
testDebugUnitTest
assembleDebug
assembleDebugAndroidTest
```

目标：

> 当前全部已有测试继续 PASS。

不要为了让新测试过，删除旧测试。

---

# 38. 数据不回归

必须保存施工前后：

```text
Workspace snapshot
Theme prefs
Character selected
Chat row count
Chat row IDs
Folder membership
Widget placement
Hotseat rank
```

完成后逐项对比。

---

# 39. UI 回归重点

检查：

```text
Home swipe
Drag icon
Folder open
Folder close
Widget resize
Theme change
Theme restart persistence
App launch
Chat input
Control Center
Lockscreen
```

---

# 40. 性能

重点观察：

```text
Midnight Glass
Soft Home
```

因为 Blur 成本最高。

如果掉帧：

按顺序优化：

```text
减少 blur surface 数
降低 blur radius
静态 surface fallback
```

最后才考虑换库。

不要一看到掉帧就：

> 重写 renderer。

---

# 41. 本轮 Commit 计划

建议严格：

```text
UIR-3D
Finalize six-theme icon art directions

UIR-3E
Complete core production icons

UIR-4A
Add Rainy Study

UIR-4B
Add Sakura Diary

UIR-4C
Add Y2K Love PC

UIR-4D
Polish six production themes

UIR-4E
Verify modern Android glass

UIR-5A
Redesign Theme Center navigation

UIR-5B
Implement production theme cards

UIR-5C
Implement wallpaper/icon/my sections

UIR-5D
Move OEM import into Theme Lab

UIR-5E
Unify real renderer theme previews

UIR-6
Real-device product visual acceptance
```

---

# 42. 每个 Checkpoint 都必须满足

```text
build PASS
tests PASS
workspace unchanged
screenshots reviewed
commit
push
```

不要 6 个阶段最后才 commit。

---

# 43. Codex 自主停止规则

出现任何以下情况立即停：

```text
需要修改数据库 schema
需要重做 Launcher layout
需要改 Character Runtime
需要改聊天协议
需要改 World Runtime
Haze 要求升级整个 Compose 主版本
主题资源无法合法确定来源
真机出现 Workspace 数据变化
```

必须报告，不自行扩大施工范围。

---

# 44. 不要等我确认的小问题

下面这些 Codex 可以自主决定：

```text
2dp / 4dp 微调
icon optical alignment
preview spacing
小范围 palette 调整
wallpaper crop
thumbnail crop
文字 maxLines
```

不要每个小东西停下来问。

---

# 45. 必须等视觉确认的节点

只有两个：

### 停点 A

六套 Theme Home 真机评审板。

### 停点 B

Theme Center + Theme Lab 真机评审板。

这两个才值得停。

---

# 46. UIR-6 完成后才干什么

如果这一整轮通过：

**下一阶段不是 P5.6。**

先做：

# `P5.V-ART — 正式三男主视觉资产`

因为到那时候：

```text
Launcher 好看了
主题好看了
图标好看了
主题中心能卖了
```

缺的最大视觉短板就会变成：

> **人物还是当前 fallback 头像。**

那时再正式做：

```text
沈砚
周野
诺亚
```

角色：

```text
头像
半身
Living 图
来电图
聊天头像
锁屏 Live Activity
相册/生活图基础资产
```

这时投入 ART 才不会浪费。

---

# 47. ART 之后

才回到：

```text
Online Character QA
```

验证三个人的实际模型输出。

然后：

```text
P5.6
```

---

# 最后给 Codex 的开工指令

你可以直接复制下面这一段，然后睡：

> 完成当前 UIR-3 图标 prototype/production checkpoint 后，不开始新业务功能。从最新 `origin/local-pass3c-ai-runtime` HEAD 继续执行《AILUA P5.V UIR-3D → UIR-6 夜间总任务书》。当前 Launcher/Home/Workspace/Character Runtime/Theme Runtime 主架构全部冻结。
>
> 第一阶段完成 18 个核心 App 的正式主题图标，并保持用户 external/manual icon override 优先。随后完成六套 Production Theme：AILUA Default、Soft Home、Rainy Study、Sakura Diary、Y2K Love PC、Midnight Glass。各主题必须在壁纸、图标、Widget/Dock 材质上形成明显不同的完整视觉语言，禁止只换色。
>
> 六主题完成后再将 Theme Center 从现有七项配置面板改为普通用户可理解的四入口：主题 / 壁纸 / 图标 / 我的。MIUI MTZ、ColorOS `.theme`、Android Icon Pack 和本地 raw import 不删除底层能力，但从普通 Theme Center 隐藏，迁入开发者模式下的 Theme Lab。OEM importer 定位为 AILUA 官方主题生产/素材转换工具，不继续模拟 OEM SystemUI、锁屏脚本、字体或声音运行时。
>
> Theme Preview 必须逐步复用真实 Wallpaper、Widget、Dock、Icon renderer，不再维护一套与正式桌面不同的假视觉实现。禁止改数据库 schema、Workspace 数据模型、Pager、Drag/Drop、Folder、AppRouter、Character/Chat/World/Romance/Proactive Runtime。禁止创建 ThemeEngineV2、LauncherV2、IconEngineV2。
>
> 每个 checkpoint 跑离线测试、构建并 push。UIR-4 六主题完成后生成真机六主题视觉评审板并继续施工；UIR-5 完成后生成 Theme Center / Theme Lab 评审板；最终 UIR-6 跑完整真机回归、保存 Workspace/Theme/Chat 数据前后对照并输出 `P5_V_UI_PRODUCTIZATION_REPORT.md`。Android 31+ 设备可用时必须验证 Soft Home 与 Midnight Glass 真 backdrop blur；若设备不可用，明确标记未验证，不伪造结论。UIR-6 完成后停止，不开始 ART、P5.6 或新的 Runtime。

这份足够它继续干很久，而且**不会把工程重新带进“越修 UI 越造架构”的坑里**。

你现在最该做的是睡觉，下一次回来应该只看两样东西：**六主题评审板和 Theme Center 评审板。**
