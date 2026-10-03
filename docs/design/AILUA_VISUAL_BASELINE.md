# AILUA P5.V 首轮视觉基线

本文件记录 UI-0→UI-2 的施工依据和视觉评审方法。当前参数与资产是首轮样张；正式验收结果由真机测试记录补充。

## 冻结基线与实机样本

- 分支：`local-pass3c-ai-runtime`。
- 冻结代码基线：`9246aa046ab11f1e8deed2bae3ecdd41ccbc41d8`。
- 首轮设备：小米 `M2007J22C`，Android 10 / API 29；原生截图 **1080×2340 px**。
- 安装前 Workspace 持久化样本：**3 pages / 18 items / 1 folder**。这些是数据库项数，不把 Launcher 的附加 Life Bento 页面算成数据库页。
- 安装前显示偏好：`showLabels=false`、`iconScale=1.1`、`showPageIndicator=false`。
- 安装前主题：`milk`、Palette override `cream`、`darkTheme=false`。

原始记录：[before-workspace.json](../../dist/qa/P5.V-UI-Theme/before-workspace.json)、[before-preferences.json](../../dist/qa/P5.V-UI-Theme/before-preferences.json)。主题样张不得重排已有桌面或临时改偏好来掩盖问题。

## 已查看的施工前截图

| 图片 | 实际可见问题 | 对应入口 |
| --- | --- | --- |
| [baseline-current.png](../../dist/qa/P5.V-UI-Theme/baseline-current.png) | 全屏乳白渐变；两块大 Widget 与白色 Dock 连成主要画面，壁纸没有可辨主体；图标有颜色但缺少丰富身份 | `ThemeWallpaper`、`ThemeWidgetFrame`、`HomeHotseat`、`ThemeResolver` |
| [baseline-glass.png](../../dist/qa/P5.V-UI-Theme/baseline-glass.png) | 灰蓝底上是大块深蓝实心卡片；所谓玻璃未透出壁纸轮廓；图标重复透明圆与明亮白边，实体图标对比不足 | 相同材料 Renderer 及旧 Glass `IconVisualSpec` |

两图均来自上述小米设备，已经实际打开检查。基线图只证明旧画面的现状，不证明新方案通过。其截图日期、状态栏时间、虚拟世界时间与当前人物沿用设备已有状态，不为主题展示修改业务 Runtime。

另有 `baseline-diary.png`、`baseline-mono.png`、`baseline-theme-center.png` 作为旧主题和入口回归素材。

## 首轮视觉原则

主视觉顺序固定为 **Wallpaper → Character → Apps → Glass**。壁纸提供颜色、大轮廓和光线，Widget 与 Dock 以局部材质承载内容。第一眼约 70% 真实手机、20% 角色生活、10% 主题个性。

- 文字只组织成 Primary、Secondary、Caption 三层。时间可以使用独立 Display，但不增加一串小数点字号。
- 玻璃只作用于背景材质。App 图标、消息徽标和前景文字保持完整不透明；拖拽来源的 ghost 透明态例外。
- 不用全屏乳白 overlay、满屏同构卡片、密集白边或一套透明圆底替代所有 App 图标。
- 局部玻璃仍须显露壁纸颜色、曲线或静物轮廓；只剩灰色或白色的材质不通过。
- 旧主题、手动壁纸、外部图标和用户显示偏好保持兼容。所有选中、放置、resize 状态继续清晰。
- 卡片的使用由内容决定：独立信息与可操作 Widget 可以有背景，普通列表以留白分层。

## 三个首轮样张

| Theme ID | 名称 | 画面方向 | 当前壁纸来源 |
| --- | --- | --- | --- |
| `default` | AILUA Default | 蓝、米白、珊瑚大幅抽象折纸，轻玻璃 | 自制数学曲线与光照 |
| `soft_home` | Soft Home | 暖光桌面、书、两杯茶、木纹和植物，浅玻璃 | 自绘临时静物插画，**不是照片** |
| `midnight_glass` | Midnight Glass | 深海蓝、清晰大轮廓、微光环带，暗玻璃 | 自制数学曲线与光照 |

生成脚本：[render_theme_wallpapers.py](../../scripts/render_theme_wallpapers.py)。内置资源为 `drawable-nodpi/wallpaper_default.webp`、`wallpaper_soft_home.webp`、`wallpaper_midnight_glass.webp`；全部是 RGB **1080×2340** 无损 WebP，没有联网下载素材、人物脸或绘入 UI 文案。

这三张是可重复生成的首轮设计样张。Soft Home 还未使用正式生活摄影，所有人物资产沿用已有 fallback，不宣称正式 ART 完成。

## 首轮停点与判断

连续推进材质 Renderer 与 Home 表现，使用同一台设备、同一套 Workspace 和显示偏好取得三个主题的真实截图。至少查看全图以及 Widget / Dock / 图标局部，并记录不满意处，再修画面。

评审需确认：壁纸是否承担画面、人物是否成为内容焦点、图标是否清楚、Dock 是否透出底图、三套主题是否确实不同、长文本是否可读、页面是否仍像同一台私人手机。

**UI-0→UI-2 的停点是三主题真实截图 → 人类视觉方向确认。** 在此之前不批量传播到其它 App，不开始正式人物 ART 或 P5.6。截图和构建结果由主施工记录补充，本文件不将待测项目标为 PASS。

