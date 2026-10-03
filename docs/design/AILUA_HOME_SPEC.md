# AILUA P5.V 首轮 Home 规格

本文件定义 UI-0→UI-2 的 Launcher 视觉母版。基线为 `local-pass3c-ai-runtime` / `9246aa046ab11f1e8deed2bae3ecdd41ccbc41d8`；只改 presentation，Workspace、Pager、Folder、drag / resize、Widget Host、Runtime 和数据库 schema 继续使用当前实现。

## 保留的真机状态

小米 `M2007J22C`，Android 10 / API 29，截图 1080×2340。施工前数据库为 **3 pages / 18 items / 1 folder**；已有 `showLabels=false`、`iconScale=1.1`、`showPageIndicator=false`。首次安装的建议排布不适用于这份已有桌面，不增删或搬动项目以取得更整齐的样张。

原始证据：[before-workspace.json](../../dist/qa/P5.V-UI-Theme/before-workspace.json)、[before-preferences.json](../../dist/qa/P5.V-UI-Theme/before-preferences.json)。对照图：[baseline-current.png](../../dist/qa/P5.V-UI-Theme/baseline-current.png)、[baseline-glass.png](../../dist/qa/P5.V-UI-Theme/baseline-glass.png)。

## 首屏的视觉顺序

```text
真实壁纸（承担颜色、光线与大轮廓）
  Status Bar
  Time / Ambient Widget
  Character / Life Widget
  已有 App Grid
  Page Indicator（仅当用户偏好开启）
  Dock（五个既有 slot）
  Virtual Home Bar
```

Time / Ambient 传达时间、日期、天气或世界状态。角色 Widget 只强调角色名、当前地点或状态、一句动态；长文案按现有约束换行或省略，不堆满关系数字或多彩状态块。人物头像继续使用已有资源与 fallback，当前活跃人物和世界时间不为主题样张改写。

Widget 是壁纸上的局部覆盖物。两块 Widget 不再以连续实心白板或深蓝板压住整个上半屏；Widget 间和外围留出可见壁纸。对这台已有桌面，视觉优化必须在原有 item span 内完成，不能缩改数据库 span 来取巧。

## 尺寸与材料

- 页面横向间距读 `runtime.layout.screenHorizontalPadding`，当前为 24 dp； section / item gap 沿用当前 28 / 12 dp，局部网格间距继续由现有 Workspace 布局管理。
- Widget 的颜色、前景、24 dp 圆角、18 dp content padding、blur / alpha / highlight 读主题 spec；完整参数见 [材料规格](AILUA_THEME_MATERIAL.md)。
- Dock 读主题 spec：30 dp 圆角、12 dp 内边距、最小高度 82 dp、0.92 图标材质缩放；保留用户 `iconScale=1.1` 及已有 slot 内容。
- 图标、badge 与文字以完整前景绘制。只有 Dock / Widget 背板低透明，拖拽 ghost 继续按交互状态绘制。
- UI 的文字层级集中为 Primary、Secondary、Caption。用户已关闭图标标签，首轮实机图因此不显示标签；不偷偷开启标签去证明图标识别。
- 用户已关闭页指示器，首轮图保持关闭。当前位置、首页标记与附加 Life Bento 页继续沿用已有 Pager 行为。

这些是当前输入和排版约束，最终实际像素位置及 Dock 高度需由新截图确认。

## 三套同布局样张

| 主题 | 壁纸可见主体 | 前景方向 | Android 10 材料 |
| --- | --- | --- | --- |
| AILUA Default | 蓝 / 米白 / 珊瑚的大幅折纸曲线 | 深色文字、实体 App 身份 | 浅 tint 与轻高光 |
| Soft Home | 暖色桌面、书、两杯茶、植物；上部干净 | 深色文字、温暖 palette | 浅 tint 与轻高光 |
| Midnight Glass | 深蓝环带与微光，大轮廓仍可辨认 | 浅色文字、实体 App 身份 | 暗 tint 与轻高光 |

所有背景来自自制脚本：[render_theme_wallpapers.py](../../scripts/render_theme_wallpapers.py)。Soft Home 明确是临时静物插画；不把它描述成真实摄影。三主题截图必须用真实 Launcher Renderer，不通过手工拼图冒充安装结果。

## Life Bento 与新用户建议

Life Bento 如涉及首轮 presentation，只保留“角色主视觉 / 状态 + 最多三个主要入口 + 今日动态列表”的结构，消费现有 runtime 数据。不新增世界事件、关系规则或假生活数据来填视觉。

未来新用户可参考 Living / Social / World / Create 的默认内容分组，普通用户仍可拖动。它们不是新增的硬编码功能页。首轮不以四页为目标，不改已有小米的三份数据库页，不重设首页标记。

## 真机检查与停止条件

用覆盖安装保留数据，在这台小米上连续切换三个主题，保存实际 Home 全图和 Widget / Dock 局部。检查壁纸裁切、深浅文字、长角色状态、五个 slot、Folder 预览、关闭标签的图标识别、页面切换、编辑边框、拖动与 resize。

主题切换和强停重启后比较 Workspace 内容与显示偏好：page / item / folder 行、项目坐标、Widget span、Hotseat slot 与原始状态一致。记录实际结果，不把“结构没改”当作数据已验证。

**UI-0→UI-2 结束后停在 Default / Soft Home / Midnight Glass 三张真机图与人类视觉方向确认。** 在黄金首页成立后才推进正式 UI-4 图标、六套 Theme Pack 和其它页面；不开始正式人物 ART 或 P5.6。API 31+ 真模糊在新设备上另行验证，本机 Android 10 的 fallback 不承担该验证。

