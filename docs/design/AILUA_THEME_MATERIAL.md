# AILUA P5.V 首轮主题材料规格

基线：`local-pass3c-ai-runtime` / `9246aa046ab11f1e8deed2bae3ecdd41ccbc41d8`。保留 `ThemeStore → ThemeSelection → ThemeResolver → AiluaThemeRuntime`；下面记录首轮 Renderer 的当前参数与待验证边界。

## 确切的旧问题

基线的 Glass Widget 声明 `surfaceAlpha=0.32`，Dock 声明 `surfaceAlpha=0.28`，Renderer 却分别强制到 `0.78..1.0` 和 `0.78..0.96`。实际材质因此更接近实心色板。[baseline-glass.png](../../dist/qa/P5.V-UI-Theme/baseline-glass.png) 能看到深蓝卡片遮住灰蓝背景，缺乏壁纸与覆盖物的关系。

修复要求是让 Renderer 尊重不同材质的 spec。Glass 的低 alpha 不能被全局提升到 0.78；CARD、PAPER、FLAT、TRANSPARENT 仍按自己的材料语义绘制。Widget 必须读自身颜色、圆角、border、shadow，不能固定用 `surfaces.raised` / `shapes.medium`。

## 当前实现边界

- 只有 `WidgetVisualSpec` 与 `DockVisualSpec` 增加 `blurRadiusDp`、`highlightAlpha`、`fallbackAlpha`，都有默认值。
- `AiluaMaterialRenderer.kt` 把现有 Widget / Dock spec 投影成共享材质参数，预览与 Home 使用同一 alpha 规则。
- Haze 状态与 API 判断封装在材料 Renderer 中，业务模块和 `AiluaThemeRuntime` 不持有 Haze 类型。
- 背景采样来源是壁纸层，前景图标与文字不进入采样源，不随容器 alpha 一起变淡。
- alpha 校验为有限值的 `0..1`，不设置 0.78 最小值；异常 alpha 使用合法 fallback。
- `TRANSPARENT` 不绘制 tint；Glass 使用局部高光，只有 spec 明确要求时绘制边框。编辑选中态仍可使用 accent 边框。

未加入折射、色差、噪声引擎、动态场景或第二套 Theme Runtime。

## 三主题当前代码参数

以下来自当前 `ThemeCatalog.kt` / `ThemeResolver.kt`，是首轮真实渲染输入，后续视觉校准应同步修改本表。

| 参数 | AILUA Default | Soft Home | Midnight Glass |
| --- | ---: | ---: | ---: |
| Theme ID | `default` | `soft_home` | `midnight_glass` |
| Palette | `mist` | `cream` | `mist`，暗色投影 |
| Wallpaper | `builtin/default` | `builtin/soft_home` | `builtin/midnight_glass` |
| Widget style | `light_glass` | `soft_glass` | `dark_glass` |
| Widget tint | `#FFFBF4` | `#FFFBF4` | `#152B42` |
| Widget foreground | `#283039` | `#283039` | `#F5F8FC` |
| Widget surface alpha | 0.14 | 0.14 | 0.24 |
| Widget fallback alpha | 0.20 | 0.20 | 0.28 |
| Widget blur radius | 22 dp | 22 dp | 28 dp |
| Widget highlight alpha | 0.20 | 0.20 | 0.10 |
| Widget corner | 24 dp | 24 dp | 24 dp |
| Widget shadow | 0 dp | 0 dp | 0 dp |
| Widget content padding | 18 dp | 18 dp | 18 dp |
| Dock tint | `#FFFBF4` | `#FFFBF4` | `#142A40` |
| Dock surface alpha | 0.16 | 0.16 | 0.22 |
| Dock fallback alpha | 0.22 | 0.22 | 0.28 |
| Dock blur radius | 28 dp | 28 dp | 28 dp |
| Dock highlight alpha | 0.20 | 0.20 | 0.10 |
| Dock corner | 30 dp | 30 dp | 30 dp |
| Dock shadow | 0 dp | 0 dp | 0 dp |
| Dock horizontal / vertical padding | 12 / 12 dp | 12 / 12 dp | 12 / 12 dp |
| Dock icon scale multiplier | 0.92 | 0.92 | 0.92 |
| Widget / Dock border | 0 dp | 0 dp | 0 dp |

三主题图标当前使用 `identity`：实体渐变、Squircle、白色 glyph、2 dp shadow、无描边。它们还是现有 App 身份的样张投影，未宣称完成 UI-4 的全套正式图标重绘。

Dock 容器当前最小高度 82 dp；最终尺寸以真实内容、保留的 `iconScale=1.1` 与小米截图测量为准，不将最小值写成已测得的固定最终高度。

## 后端与系统降级

当前单一后端固定为 **`dev.chrisbanes.haze:haze:1.3.1`**，不为追最新后端升级整套项目构建链。当前 compileSdk 36.1，Compose 与 Kotlin 沿用项目已有版本。

API 31+ 的代码路径为壁纸 backdrop source → Haze blur → theme tint → 局部 gradient highlight → optional border → foreground。代码中将 `noiseFactor=0`，保持壁纸的大轮廓清晰。

**小米 Android 10 / API 29 明确走 fallback**：低透明 theme tint + 局部渐变高光，使用本表 fallback alpha。Glass 的 native elevation 为 0，避免透明背板边缘出现灰环。该设备没有运行真 backdrop blur，报告与截图不能称其为真 blur。也不默认启用 CPU / RenderScript 动态模糊。

API 31+ 真 backdrop blur 的显示、拖动与性能仍需在线新系统设备实测。依赖已接入或 Debug build 成功都不等于此项目已经通过。

## 材料视觉检查

同一 Workspace 分别观察 Default / Soft Home / Midnight Glass：玻璃后的蓝色带、木纹或深蓝环带仍可辨认；前景文字稳定可读；Dock 中图标和彩色 badge 不变淡；时间 Widget、角色 Widget 与 Dock 的材料属于同一主题。

另检查 Milk / Glass / Diary / Mono 保留相应 CARD / GLASS / PAPER / TRANSPARENT 语义；拖动、跨页、resize 时没有残影、闪烁或不透明块。小米 fallback 和新系统 blur 分别留证，不以其中一种代替另一种验证。

## 首轮资产来源

三张原生 1080×2340 WebP 均由 [render_theme_wallpapers.py](../../scripts/render_theme_wallpapers.py) 生成。Default / Midnight 是自制抽象；Soft Home 是自绘临时静物插画。没有外部主题图片、商业素材或生成 API 调用。

真机返修：API 29 首版出现 padding 对齐的灰色厚框与矩形孔。GLASS 已移除 native elevation，背板与前景分为 sibling，fallback tint 和高光整块绘制；普通 CARD / PAPER 的阴影保留。首版失败截图在 rejected-gray-frame 中留证。
