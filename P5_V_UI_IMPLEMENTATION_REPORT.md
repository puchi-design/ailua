# P5.V-UI0–UI6 实施与真机验证

## 交付

- 分支：`local-pass3c-ai-runtime`。
- 基线：`57e83641223cc72f1a29042497b33231176e2bed`，含已完成的 P5.5。沿当前工作继续，没有回退或覆盖 P5.5。
- UI0–UI4、UI5-A/B/C/D/E、UI6 全部完成。
- 最终实现代码 HEAD：`cbf54895d9f1ccee5488237e90edf000c399afbe`。本报告为后续文档提交；包含报告的最终分支 HEAD 见交付消息或 `git rev-parse HEAD`。
- APK：`dist/AILUA-P5.V-UI0-6-debug.apk`。
- SHA-256：`28296c4697d361906d48376fcd6c88e816f1d560899dd91bef1a7ca756c3a484`。

## 实施内容

| 阶段 | 完成内容 | Checkpoint |
| --- | --- | --- |
| UI0–UI4 | Runtime 语义规格、七个公共组件、Virtual Chrome、Home/LifeBento、Chat、Living、Profile | `3d6ae43` |
| UI5-A | 动态、三列相册与媒体详情、消息、联系人、复用聊天组件的群聊 | `bcd9130` |
| UI5-B | 日记列表与阅读、信箱与纸张阅读、日期分组记忆、沉浸剧场 | `0fb58d2` |
| UI5-C | 他的手机、关系、地点、世界书、应用库 | `b7375e2` |
| UI5-D | 来电、通话、通话记录、角色工坊、设置、隐私、现实连接 | `70d19ce` |
| UI5-E | P5.5 锁屏、通知卡、控制中心、Live Activity 的视觉归一 | `ad273ae` |
| UI6 | 公共弹层、Home 编辑/文件夹/主题预览、名称与字号清理、统一底部 Home、真机 smoke | `cbf5489` |

沿用 P5.5 已在 App Root 建立的唯一 AiluaThemeProvider。所有页面共享 LocalAiluaTheme；Material3 的颜色（含 surfaceContainer）、字体与形状映射到该 Runtime。

- Surface：screen / raised / inset / overlay / divider。
- Shape：10 / 16 / 22 / pill；Text：30 / 20 / 17 / 15 / 13 / 11，锁屏大时钟保留专用字阶。
- 七个公共组件：AiluaScreenScaffold、AiluaTopBar、AiluaSectionHeader、AiluaSurface、AiluaChip、AiluaMediaFrame、CharacterPortrait。
- 角色视觉统一通过 CharacterPortrait，内部使用原 AiluaAvatar fallback。Moments/Gallery/Diary/CheckPhone/Theater 使用 AiluaMediaFrame。
- 普通列表采用行、留白和分隔线；通知、控件、Hero、弹层保留独立容器。
- UI6 源码扫描：迁移 Screen 无旧四色 import、手写 fontSize 或直接 AiluaAvatar 调用。旧色仅保留在主题兼容和 procedural fallback art 等明确例外中。

Workspace 拖拽、Folder drop、Pager、Resize、数据库语义、Chat/Memory/World/Call 引擎未修改。P5.5 的锁屏状态、通知 Repository、Heads-up 策略、专注/静音、亮度、SystemUI Controller 未改。底部 Home 统一返回已有主屏，顶部返回仍逐页返回。

Chat 的生成/流式/分支/记忆/再生成/语音、群聊能力、日记喜欢、信件投递/回信、剧场选择/变量/历史/书签、世界书启用/模拟器、Creator JSON/SAF/校验/复制/保存均保留原状态和回调。开发时间入口仅在原 developer 设置启用时出现。应用库名称精简，搜索兼容新中文显示名与原名称/分类。

## 构建与测试

统一执行一次完整离线收口：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest --offline --no-configuration-cache --no-daemon --max-workers=1
```

- 单测 **451/451 PASS**：96 suites，0 failure/error/skip。
- Debug APK 构建、覆盖安装 **PASS**。
- 最终真机 instrumentation **2/2 PASS**。
- 证据位于 `dist/qa/P5.V-UI/`：`ui6-build.log`、`ui6-unit-results.json`、`ui6-instrumentation.log`。

首轮 smoke 错误地认为应用库“通话”直达记录页；既有路由实际直达通话页。已修正测试，改为真实挂断后验证记录页，未修改生产路由。随后只重编译测试 APK，再跑两项通过；未重复完整单测。首轮失败日志保留为 `ui6-instrumentation-first-run.log`。

## 真机 smoke

实际设备为 **小米 M2007J22C，Android 10 / API 29，1080×2340**，不是任务示例中的 realme。ADB：`D:\mandapi-chat-v1\android-sdk\platform-tools\adb.exe`。主 APK 和测试 APK 覆盖安装，未清数据。

MIUI 曾提示允许 AILUA 打开测试 APK；授权后，宿主脚本用与 ActivityScenario 相同的 MAIN/LAUNCHER Intent 协助启动。未关闭系统安全功能。

| 检查 | 结果 |
| --- | --- |
| Home/Chat/Living/Profile/Messages | PASS，真实入口导航、绘制、返回主屏 |
| Moments/Gallery/Diary/Mailbox/CheckPhone/Theater | PASS，包含日记和信件阅读页 |
| Milk → Home；Glass → Chat；Diary → Diary/Mailbox；Mono → Settings/Control Center | PASS，保留 Cream Palette override，验证组合 |
| 来电 → 接听 → 通话 → 挂断 → 通话记录 | PASS，操作专门创建的 QA 虚拟来电 |
| 状态栏下拉、通知中心、控制中心、锁屏/解锁 | PASS，通知为空时保留真实空态 |
| Chat / IME | PASS，键盘弹出后 Composer 和发送按钮可见，未发送消息 |
| Android 10 文件选择器 | PASS，相册导入打开 DocumentsUI，取消后返回相册，未导入个人照片 |
| 覆盖安装和强制重启后的布局 | PASS，完整行数据不变：3 页、18 项、1 文件夹，含位置、Widget span、Hotseat rank |
| 重启外观持久化 | PASS，主题与外观设置一致 |
| Logcat | 本次应用 UID 日志未检出 FATAL/ANR、资源缺失、权限、文件、ZIP/XML 或 SQLite 异常 |

证据：`ui6-verification-summary.json`、`ui6-device-logcat.txt`、安装前后 workspace JSON、重启前后 preferences JSON。数据库快照仅留本机 QA 目录。

## 截图

目录：`dist/qa/P5.V-UI/`。总览：`ui6-all-pages-review.jpg`。

| 文件 | 内容 |
| --- | --- |
| `01-home.png` | Milk Home/LifeBento；原默认页另存 `01-home-default.png`，Widget 页另存 `01-home-workspace.png` |
| `02-chat.png` | Glass 聊天 |
| `03-living.png` / `04-profile.png` | 生活 / 资料 |
| `05-messages.png` / `06-moments.png` | 消息 / 动态 |
| `07-gallery.png` | 三列相册 |
| `08-diary.png` / `08-diary-reader.png` | Diary 日记列表 / 阅读 |
| `09-mailbox.png` / `09-mailbox-reader.png` | Diary 信箱 / 阅读；列表保留类别切换后的真实滚动位置 |
| `10-checkphone.png` / `11-theater.png` | 他的手机 / 剧场 |
| `12-lockscreen.png` / `13-notifications.png` / `14-control-center.png` | Mono 锁屏 / 通知 / 控制中心 |
| `15-call.png` / `16-call-history.png` / `18-incoming-call.png` | 通话 / 记录 / 来电 |
| `17-settings.png` | Mono 设置 |

补充：`ui6-chat-ime.png`、`ui6-saf-picker.png`、`ui6-saf-return.png`。总览不含个人桌面或系统授权提示。

## 测试数据与视觉剩余

- UI0–UI4 时聊天为空，写入 4 条标记“［UI QA］”的本地消息和 1 个回复分支，仍保留供查看。UI6 未新增聊天或调用外部 AI。
- 两轮 smoke 各创建并结束一通标记 `P5.V-UI6 QA` 的虚拟来电，正常记录保留。未结束既有用户通话或清理历史。
- 最终为 Milk + Cream；图标缩放、标签设置、布局未重置，未删除原导入主题。
- Canvas 头像、GalleryVisualCanvas、Living 占位表现按要求保留，相册仍有明显占位图观感。统一入口已就绪，未制作正式角色资产。
- 故事、日记、信件、事件正文的人设、称谓和世界观保留。Living 使用“最近动态”，因为原 UI 投影没有事件日期。
- 本轮是 UI 与对应交互 smoke；没有重跑真实 AI Provider、音频生成、完整外部主题导入或全部业务组合。

本轮在 UI6 交付停止，未开始 P5.V-ART 或 P5.6。
