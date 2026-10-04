# AILUA P5.V UIR-3D → UIR-6 交付报告

日期：2026-10-04。分支：`local-pass3c-ai-runtime`。

状态：**COMPLETED_UIR6**。六套正式主题、主题中心和 Theme Lab 已实现，最终 Android 10 功能回归、两套冷启动和完整数据保护对照通过。性能仅记录 Debug 观察，API 31+ 真玻璃未验证。

## 1. 基线与提交

任务书：[P5V_UIR3D_6_TASKBOOK.md](H:/ailua/docs/design/P5V_UIR3D_6_TASKBOOK.md)。用户随后否决书桌、夜窗和海岸方向，要求增加油画壁纸；最终资源已按要求替换。

| 用途 | 提交 | 状态 |
|---|---|---|
| 源码施工基线 | `9ee6887eb77fd3b2f709c6e60eb7e16a72461e99` | 已同步远端 |
| 恢复施工文档记录 | `f6e0d2b1adf147eb1700776b56ae3ef05fa423bb` | 文档提交，源码基线仍为上行 |
| UIR-3D/E + UIR-4 | `19524d1de21e6ed1288c4035ed88c00bc8d6f906` | 已提交并 push |
| UIR-5 | `9dac45b6d8df5f6b9751f9c067b8017fe529f12c` | 已提交并 push |
| UIR-6 最终源码 / `finalCodeHead` | `3ca945d9f3d82651badd9bbbf968fb6132cbbcf1` | 已提交并 push，远端源码 HEAD 一致 |

本报告不自引用尚未生成的报告提交 SHA。最终交付 HEAD 由报告提交后的 [delivery-receipt.json](H:/ailua/dist/qa/P5.V-UIProduct/delivery-receipt.json) 记录，并在最终回复中给出。

## 2. 主题与资产

| 主题 | 最终视觉 |
|---|---|
| AILUA Default | 现代图标、原有几何壁纸、轻玻璃 |
| Soft Home | 玫瑰花丛油画、柔和图标、轻玻璃 |
| Rainy Study | 湖畔花田油画、湖蓝图标、清透玻璃 |
| Sakura Diary | 樱花、纸张与手绘贴纸 |
| Y2K Love PC | 粉紫像素图标、桌面面板材质 |
| Midnight Glass | 深蓝夜色与玻璃 |

壁纸页在“跟随主题”后展示六张油画：玫瑰花丛、湖畔花田、粉色花枝、林间小路、睡莲、花园午后。复用现有预览、应用及混搭链路；旧书桌、夜窗和海岸版本未进入最终正式资源。

资产清单：[THEME_ASSET_MANIFEST.json](H:/ailua/docs/design/THEME_ASSET_MANIFEST.json)。

- 正式资源 **120/120**：108 个图标、6 张主题壁纸、6 张图库油画；`pendingCount=0`、`visualReviewPendingCount=0`。Soft/Rain 的主题壁纸与图库对应项使用相同画作，120 是资源条目数。
- 图标统一 256×256 RGB lossless WebP；Y2K 先 nearest 到 32px，再 nearest 到 256px。壁纸统一 1080×2340 RGB lossless WebP，按比例裁切。
- 新油画原图实际为 941×1672，睡莲为 940×1672；未将请求尺寸写成服务实际输出尺寸。
- 本地保留 **141 个本轮成功生成的有效原始 PNG**，包含被否决的替换版本；该数不是交付图片数或 API 请求次数。默认几何壁纸保留项目自有脚本来源，其余生成素材记录指定服务、Prompt、原图及资源哈希。
- Prompt：[UI_PRODUCTIZATION_PROMPTS.jsonl](H:/ailua/docs/design/UI_PRODUCTIZATION_PROMPTS.jsonl)、[UI_OIL_WALLPAPER_PROMPTS.jsonl](H:/ailua/docs/design/UI_OIL_WALLPAPER_PROMPTS.jsonl)。两张被否决的初始书桌/夜窗原 Prompt 正文缺失已在清单注明，未补造记录。

资产权利状态为 `unverified-provider-output-terms`；`production` 表示视觉整合与检查完成，不表示商用授权已独立核实，用户已取消将该核查作为本轮施工前置条件。

## 3. UI 收口与局部修复

主题中心提供主题、壁纸、图标、我的主题四入口。详情在预览上方展示名称和材质，预览复用真实 Wallpaper / Widget / Dock / Icon renderer。外部格式技术入口集中到开发者 Theme Lab；导入、Inspector、导出沿用现有主题链路。

- Android 返回先退出详情或 Lab 子页，再关闭 sheet；AppRouter 不变。
- AILUA schema 1 保存合法 `basePreset`，导入后映射到既有 `basePresetId`；可选 `iconStyle` 和 `wallpaperStyle` 通过现有 metadata 传递。回导恢复 Widget / Dock / 字体及壁纸亮暗前景，同时使用导入图片源；未知样式不被接受，OEM 包保持当前 shell。
- legacy / 稀疏图标包缺少位图时仍可导出，Inspector 明确显示 vector fallback。用户 manual / 外部图标优先链保留，wallpaper-only 包不清除手动图标。
- Y2K nearest / pixel 标志由实际 AILUA 外部包 metadata 决定，换 skin 不会把其它 OEM、Android 或手动图片误当像素图。
- 油画缩略图和最终真机截图等待实际 bitmap 加载，最终壁纸截图不使用渐变占位图。

### Workspace 坐标修复与冻结边界

真机拖动暴露出编辑面板打开后 Pager 视口缩小、grid 的 `boundsInRoot()` 被裁切的问题，导致绘制行高与落格计算不一致。局部 UI 修复让 WorkspacePage 发布 `positionInRoot + size` 的完整绘制 bounds；Home 继续使用已有 Pager 可见 bounds 限制起拖及 hover，有效落点必须位于可见视口内。真实长按移动到空格、再拖回的完整布局恢复已通过。

该修复不改变列数、行数、分页模型、持久化或 DropResolver。Character / Romance / Proactive / World / Chat / Memory / Call Runtime、数据库 schema、DesktopItem、Folder、Widget Host、Hotseat 数据模型、AppRouter、ThemeStore / ThemeSelection / ThemeResolver 主链及 AiluaThemeRuntime 主结构保持冻结。未创建新 Launcher、Theme 或 Glass engine。

## 4. 测试与设备证据

设备：**小米 M2007J22C，Android 10 / API 29，1080×2340**。ADB 正常，覆盖安装、启动和真实界面操作已执行。

| 检查 | 实际结果 | 证据 |
|---|---|---|
| 离线单元测试 | **622/622 PASS；116 suites；0 failure / error / skip** | `app/build/test-results/testDebugUnitTest/TEST-*.xml` |
| 最终 Debug / androidTest 构建 | **PASS，20s** | `build-final-oil.log` |
| 六主题 / 图标 / Folder 真机测试 | **1 PASS，50.063s** | `device/themes-instrumentation.log` |
| 商品 UI 与 Workspace 手势最终回归 | **2 PASS，172.434s** | `device/product-instrumentation.log` |
| 已加载图片的主题预览补截 | **1 PASS，10.724s** | `device/captureLoadedThemePreview.log` |
| 六油画实际预览→应用 | **PASS**，shell 与 Workspace 保持 | `device/product-smoke-events.txt` 最后成功段 |
| 导出→导入 | **PASS**，含 legacy、Default+Mono、Midnight+油画、Soft+Midnight 的样式与亮暗前景恢复 | 同上 |
| Widget resize / 长按拖动→空格→拖回 | **PASS**，恢复原 span 与完整布局 | 同上 |
| Control Center / 锁定解锁 / 通知面板 | **PASS** | 同上 |
| Soft Home 强停→冷启动 | **PASS**，主题、外部/手动图标、Workspace 与角色选择保持 | `device/cold-start-soft_home-persistence.txt` |
| Midnight Glass 强停→冷启动 | **PASS**，主题、外部/手动图标、Workspace 与角色选择保持 | `device/cold-start-midnight_glass-persistence.txt` |
| 最终全量数据保护对照 | **PASS，15/15 exactChecks**，包含最后预览补截之后 | `after-final-review-preservation.json` |
| 交付 APK 与手机已安装 APK | **SHA-256 完全一致** | `artifact-verification.json` |
| 已采集 crash buffer | **AILUA 条目 0** | `device/crash-buffer.log` |

证据根目录：[P5.V-UIProduct](H:/ailua/dist/qa/P5.V-UIProduct/)。最终 20s 构建为 QA 参数增量构建，unit test 任务 `UP-TO-DATE`；622 项结果来自已完成的测试 XML。最终 app 与上述两组最终真机测试所使用 app 字节一致。最后预览补截仅更新 AndroidTest 等待逻辑，共用 helper 确认 bitmap 已加载后捕获 07/08，没有再变更 app 产品代码。crash buffer 的 0 条只表示本次采集范围内未发现 AILUA 条目。

`product-smoke-events.txt` 为追加日志，保留早期失败与复测过程；最终结论以两份最终 instrumentation 的 `OK` 和最后成功段为准。两套冷启动均执行 prepare、实际 `am force-stop`、重新启动和 verify；对应 `cold-prepare-*` / `cold-verify-*` 日志均 PASS。最终 15 项逐项对照完整 schema、Workspace / Folder / Widget、聊天与记忆行及保护 prefs，3 页 / 18 项 / 1 Folder 和全部受保护内容保持一致。报告和机器 QA 仅记录结果与证据路径，不含数据库行、聊天正文、prefs 值或凭证。

## 5. 真实截图与视觉检查

最终要求的 18 张截图已保存于 [device](H:/ailua/dist/qa/P5.V-UIProduct/device/)：

- `01-default`～`06-midnight`：六套主题首页。
- `07-theme-center`～`12-theme-lab`：主题中心、详情、壁纸、图标、我的主题、Lab。
- `13-app-library`～`18-living`：应用库、锁屏、通知、控制中心、Chat、Living。

另有六套 Folder、Inspector、冷启动与手势补充图。四张评审板已重制，对应 source SHA-256 清单全部复核通过：`six-theme-home-review`、`theme-center-review`、`system-app-review`、`ui-product-review-board`。全部 18 张独立原截图均为 1080×2340，哈希与 sidecar 一致；评审板仅按比例缩放，标题放在设备图外，未修饰截图。

正式图标和油画原图/规范化图已实际打开检查，manifest 逐项记录检查及资源哈希。补截后的 08 单张确认真实油画已加载；最终重制的 six-theme-home、theme-center、system-app 和包含全部 18 张截图的 ui-product-review-board 均已由主施工 Codex 实际打开复看，视觉检查通过。该结果是施工检查，尚未把用户最终视觉方向确认写成已收到。

## 6. 性能与 Android 范围

API 29 实测使用 **tint + highlight 玻璃 fallback**。没有 API 31+ 设备，Haze 真 backdrop blur **未验证**，不能宣称 Glass Production Verified。

性能只作为 **Android 10 Debug 观察结果**。Instrumentation 中 Soft Home 22 帧有 14 帧 jank（63.64%），Midnight Glass 21 帧有 11 帧 jank（52.38%）；样本小且包含测试框架开销。

另行唤醒手机、确认解锁且 AILUA 在前台后，使用独立 ADB 执行六次真实 350ms 页面手势，再读取 gfxinfo；不通过 Instrumentation 注入该性能采样，少于 30 帧的样本拒绝采用：

| live ADB 样本 | rendered / janky | jank | p50 / p90 / p95 / p99 | High input latency 计数 |
|---|---|---|---|---|
| Soft Home | 282 / 11 | 3.90% | 11 / 13 / 14 / 46ms | 276 |
| Midnight Glass | 279 / 13 | 4.66% | 11 / 13 / 15 / 40ms | 273 |

证据：`device/performance-soft_home-live-adb.txt`、`device/performance-midnight_glass-live-adb.txt` 及 `live-home-*.png`。首次 `performance-soft-live-adb.txt` 的 0 帧结果因未确认前台/解锁作废，0% 不表示通过。有效样本证明本次页面手势采样已取得；High input latency 计数也如实保留，未进一步归因。这不是 release benchmark，不据低 jank 宣称所有交互或整体性能均已通过。

## 7. 交付与停点

| 交付件 | 路径 | SHA-256 |
|---|---|---|
| Debug APK | [AILUA-P5.V-UIProduct-debug.apk](H:/ailua/dist/AILUA-P5.V-UIProduct-debug.apk) | `c47474d2132421566fffdb313501b703b970c0e4f507d550696f45f8e95c36fa` |
| 六张油画 PNG 包 | [AILUA-oil-wallpapers.zip](H:/ailua/dist/AILUA-oil-wallpapers.zip) | `7d1554346db032f07801526608377f2f3445c57cd79bc18f04b7467fc8590560` |
| 机器 QA | [UI_PRODUCTIZATION_QA.json](H:/ailua/docs/design/UI_PRODUCTIZATION_QA.json) | 随报告提交 |
| 最终交付 HEAD / hashes 回执 | [delivery-receipt.json](H:/ailua/dist/qa/P5.V-UIProduct/delivery-receipt.json) | 报告提交后生成 |

本轮停在 UIR-6。未生产正式男主脸或角色 ART，未开始 P5.6。
