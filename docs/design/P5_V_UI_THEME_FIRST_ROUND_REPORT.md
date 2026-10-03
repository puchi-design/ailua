# P5.V UI / Theme 首轮实施与真机返修

日期：2026-10-03。施工基线：`local-pass3c-ai-runtime` / `9246aa046ab11f1e8deed2bae3ecdd41ccbc41d8`。

范围是任务书的 UIR-0→UIR-2 首次视觉停点，含用户明确要求的 Glass 图标白圈修复。三主题为视觉样张，尚未完成六套生产主题、正式角色资产或整个 UI Rescue。

## 用户指出的真机问题与修复

1. 原 Glass 图标使用相同透明圆底和白色描边。现改为不透明的 App 身份色实体图标，移除整圈白边；Folder 缩略图也不再套白边。
2. 首版材质截图出现与内容 padding 对齐的厚灰框、矩形孔。Glass 背板与前景拆为 sibling；Glass 关闭 native elevation；tint 与高光在整块背板绘制。失败图保留于 `dist/qa/P5.V-UI-Theme/rejected-gray-frame/`，修复前后评审板来自真实截图。
3. 角色 Widget 的头像太小、全地址和长动态连续省略。保留 4×2 span，头像扩大为 82 dp；姓名、现有活动、现有动态第一短句分层显示。4×1 使用紧凑排列。没有改角色卡、性别、人设或事件内容。

## 实现

- 原 ThemeStore / ThemeSelection / ThemeResolver / AiluaThemeRuntime 链保留。仅为 WidgetVisualSpec、DockVisualSpec 加带默认值的 blurRadiusDp、highlightAlpha、fallbackAlpha。
- 单一后端 Haze 1.3.1，后端类型仅在 Renderer 内部；Wallpaper 是采样源，文字与图标保持不透明。
- 删除 Glass 的 0.78 alpha 下限。API 29 使用低透明 tint 和局部高光，Glass 无 native shadow；API 31+ 代码路径使用 Haze backdrop blur。
- Default、Soft Home、Midnight Glass 使用内置图片；外部壁纸优先级与用户配色、图标覆盖能力保留。
- LifeBento 页面使用壁纸、统一前景色、三入口和纯动态列表；Home 的 Workspace 与 Pager 模型未动。
- ThemePreview / ImportedThemePreview 复用材质与壁纸绘制规则，修复无外部壁纸时误清当前 Wallpaper Source 的预览问题。
- 三壁纸均为项目脚本自绘。Soft Home 是临时静物插画，非实拍。来源、尺寸和 SHA256 见 `THEME_ASSET_MANIFEST.json`。

## 验证结果

| 项目 | 结果与证据 |
| --- | --- |
| 设备 | 小米 M2007J22C，Android 10 / API 29，1080×2340，ADB 正常 |
| 安装 | Debug App / 测试 APK 覆盖安装成功，保留数据 |
| 离线回归 | 587 tests；failures / errors / skipped 均为 0 |
| 构建 | assembleDebug、assembleDebugAndroidTest 成功 |
| 真机 UI | P5VThemeRescueSmokeTest：1 test PASS；生产主题选择、三主题、旧 Glass、暗色控制、LifeBento |
| Folder | 已有 Folder 从 Dock 打开，内容可见，关闭不改变名称和顺序 |
| Widget | 通过生产 resize 控件执行 4×2→4×1→2×2→4×2，全部恢复；三尺寸均可用 |
| 重启数据 | 覆盖安装后、强停后，Workspace 与偏好逐项对比一致：3 pages / 18 items / 1 folder；Dock rank、Widget span 保留 |
| 聊天与记忆 | chat_session 6、chat_turn 4、chat_variant 5、memory_entry 0；已有行逐项一致 |
| 用户选择 | 恢复原角色 mira、Milk + cream、dark=false；iconScale=1.1、showLabels=false、showPageIndicator=false 保留 |
| 日志 | 本轮 App UID logcat 无 FATAL EXCEPTION、OOM、SecurityException、资源缺失、ZIP/XML parser 异常 |
| 冻结范围 | Character / Romance / Proactive / World / Chat / Memory / Call Runtime、DB schema、Workspace / Folder / Drag / Router 无源码改动 |

截图与构建证据位于 `dist/qa/P5.V-UI-Theme/`。`build-visual-final.log` 是生产 APK 与 587 回归的最后构建；后续只重建截图测试 APK，不改变 App APK。

## 真机视觉结论与限制

已逐张查看真实截图并对首版失败结果返修。最新三主题没有厚灰框、矩形孔或玻璃图标白圈；壁纸轮廓连续，浅深前景清晰，角色状态可直接阅读。这是缺陷修复与母版样张，不能代替用户的审美确认。

- 小米 API 29 截图是 fallback，不是真 backdrop blur。Android 31+ 真 blur、性能与新系统设备仍未实测。
- 人物继续使用原头像 fallback；三主题图标仍是已有身份色和 glyph 投影，尚未做正式图标全套与主题独立画风。
- 用户现有第一页、默认页、图标分布、3 页和 Folder 都保留；没有强行换成任务书示例的四页默认布局。
- 已复拍 Milk / Glass；Diary / Mono 的 Resolver 离线回归通过，但本次最新 APK 未逐项复拍所有旧主题页面。
- Theme Center 四入口、Theme Lab、六套主题、正式 ART 和 UI-3→UI-10 留在用户确认视觉方向之后。本轮不开始 P5.6。

## 交付与复现

- APK：`dist/AILUA-P5.V-UI-Theme-first-round-debug.apk`
- SHA256：`8f97690e7c0a51f24fdcc27975d5472adfffe50e61db5aec30ebd370a306e168`
- 原图：`dist/qa/P5.V-UI-Theme/UIR-2/M2007J22C/`
- 三主题评审板：`ui-theme-review.jpg`
- 灰框返修对照：`material-before-after.jpg`
- 原始 PNG 保留；拼图仅等比缩放，不修饰 UI。

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest --offline --console=plain
# 使用绝对 adb 路径，对已完成 onboarding 的授权设备分别 install -r App 和 androidTest APK。
python scripts/run_theme_rescue_device_qa.py --adb D:/mandapi-chat-v1/android-sdk/platform-tools/adb.exe --serial pnhykzpzzxq8lbs8 --output dist/qa/P5.V-UI-Theme/UIR-2/M2007J22C
python scripts/render_theme_review_board.py dist/qa/P5.V-UI-Theme/UIR-2/M2007J22C --before-dir dist/qa/P5.V-UI-Theme/rejected-gray-frame
```

首轮 checkpoint 提交后停在任务书规定的真机视觉评审点。截图、APK、设备数据库副本和日志作为本机交付证据，不把用户数据库或聊天内容提交到 Git。
