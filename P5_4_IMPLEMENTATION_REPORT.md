# P5.4-A → D 实施报告

交付日期：2026-10-03。分支：`local-pass3c-ai-runtime`。

- 起点：`00c3f88f032452261dcd65b3f7af0e54a33b75f9`。
- 最终代码 / APK / 真机验证 HEAD：`cb623f3d1448c1ed1cf4d223810f7ae046ee3089`；其后的交付提交仅增加本报告，完整交付 HEAD 见最终回复。
- APK：`H:/ailua/dist/AILUA-P5.4-debug.apk`，28,120,034 字节。
- APK SHA-256：`3ee4e8a15a12e1dd2d49756fe169d2b889bea5d2f1c86ee74f781bf407344f4c`，与最终 Gradle 输出一致。
- 截图及本地测试证据：`H:/ailua/dist/qa/P5.4/`。

## A — Folder：完成

数据库升级至 v7，加入文件夹元数据及成员归属。支持 App 叠放建夹、继续加入 App、重命名、内部排序、移出成员和剩一个成员时自动解散；文件夹可跨页移动并放入 Hotseat。文件夹仅容纳 App，不接受嵌套文件夹或 Widget。

中央区域悬停 300 ms 后进入建夹 / 加入反馈，普通移动继续复用现有 Workspace 逻辑。文件夹预览及内部图标走共享 IconResolver，适配 Milk / Glass / Diary / Mono 和外部图标。

文件夹内移出成员通过长按菜单“移到桌面”实现。按任务书允许的降级范围，本轮未实现从弹层直接连续拖至 Workspace 的手势接力。

## B — 现有 App Drawer 接入：完成

扩展已有 AppLibraryScreen，保留搜索、分类与完整应用目录。共享 LauncherAppCatalog 和路由解析，统一旧 ID 别名、标签及可用状态。长按可添加到当前桌面页，已有应用显示“已在桌面”；桌面、Hotseat、文件夹之间统一检查重复应用。目标页无空位时使用其他页面或创建新页。

本轮采用任务书允许的长按添加菜单，未实现 Drawer 至 Workspace 的连续拖拽接力。

## C — Home Edit：完成

桌面空白处长按打开编辑面板，提供完成、Widget、壁纸、主题、页面及桌面设置入口。壁纸入口直接进入主题中心壁纸页。支持名称显隐、图标大小及页面指示器显隐，独立持久化，不改变网格或重置布局。

## D — Page Manager：完成

复用已有页面后端，实现轻量缩略图、新建页、拖动排序、设置主屏和删除空页。非空页禁止直接删除，主屏受保护。页面重排后按稳定 ID 保持当前页面；启动按持久化的主屏恢复。Life Bento 保持固定特殊页，不参与普通页面管理。

## 测试与构建

统一执行离线 `testDebugUnitTest assembleDebug`；真机修复后再次执行同一检查。最终结果：**439 / 439 通过，90 个测试套件，0 failure / error / skip，Debug APK 构建成功**。

新增测试集中于 v6 → v7、文件夹事务与恢复、页面操作及 Drawer 添加去重等纯逻辑。最终日志：`dist/qa/P5.4/build-final.log`。

## Android 10 真机结果

设备：小米 M2007J22C，Android 10。ADB 使用绝对路径 `D:/mandapi-chat-v1/android-sdk/platform-tools/adb.exe`。通过 `install -r` 覆盖安装并保留原数据，未清除应用数据。

| 检查 | 结果与证据 |
| --- | --- |
| 安装、启动及旧布局 | PASS；安装前后桌面页面 / 项目一致，后续运行快照确认 schema v7 |
| 相册叠放记忆建夹、加入日记 | PASS；`folder-created.png` |
| 重命名、内部排序及点击启动 | PASS；`QA_Folder`，顺序为 gallery → diary → memories |
| 文件夹跨页及 Hotseat | PASS；`folder-page2.png`、`folder-hotseat.png` |
| 移出成员、剩一个自动解散 | PASS；另建临时文件夹验证，`folder-auto-dissolve.png` |
| Drawer 搜索、添加及重复保护 | PASS；搜索 map 添加 VirtualMap，重复添加显示“已在桌面” |
| Home Edit、标签及图标大小 | PASS；关闭名称、设置 1.1 倍、关闭指示器，`home-edit.png`、`display-settings.png` |
| 新建页、排序、主屏、删除规则 | PASS；新主屏位于第三个位置，非空页删除被阻止，额外空页可删除 |
| 重排保持当前页、Life Bento | PASS；`current-page-kept.png`、`life-bento.png` |
| 强停后重启持久化 | PASS；最终前后 Workspace 与 Preferences JSON 完全一致 |
| 四套 Skin 文件夹视觉 | PASS；`folder-four-skins.png` |
| 外部图标及混搭 | PASS；自制 MTZ 经真实 SAF 导入，G / D 图标在文件夹预览、弹层及 Drawer 生效，切换 Milk 并重启后仍保留 |

重启证据为 `final-before-restart-*` 与 `final-after-restart-*`：文件夹名称与成员顺序、页面顺序、唯一主屏、显示偏好及 Milk + MTZ 图标配置均一致。原 Widget 尺寸保持 4×1、4×2。

MTZ 样本为本地产生的 `fixtures/qa-folder.mtz`（两个图标，无壁纸），没有下载商业主题。本轮外部图标验证用于覆盖新增文件夹 / Drawer 的共享解析链路，不代表重新完成所有外部格式的完整兼容性测试。

## 修复的问题与日志范围

- 真机发现 Dialog / Sheet 弹出后，Android 原生状态栏与 AILUA 虚拟状态栏叠加。增加窗口显示及焦点变化后的状态栏处理，应用于文件夹、显示设置、页面管理和主题中心；覆盖安装后截图确认修复。
- 集成时修正文件夹拖动跟随与悬停反馈、合并后空尾页回收、应用别名去重，以及稳定页面 ID / 主屏加载 / Life Bento 特殊页状态处理。
- 真机操作中未观察到应用崩溃。`logcat-after-install.txt` 覆盖设备时间 10-02 14:09:26–14:09:57；按当次 AILUA PID 18896 筛查，未见 FATAL、应用资源 / 文件权限或 ZIP/XML parser 异常。存在 MIUI 厂商组件与 Firebase 默认配置警告，未伴随崩溃。收尾时原小米已断开，当前 ADB 仅见另一台未授权设备，因此未能补采最终完整应用日志；此启动日志不覆盖后续导入和重启全过程。

## 提交与交付状态

| 提交 | 内容 |
| --- | --- |
| `30c5d68` | P5.4-A Folder |
| `75f5ea9` | P5.4-B Drawer 接入 |
| `1f9ab85` | P5.4-C Home Edit |
| `7a69188` | P5.4-D Page Manager |
| `cb623f3` | 真机状态栏重叠修复 |

要求的七张截图均已保存：`folder-created.png`、`folder-open.png`、`folder-hotseat.png`、`app-drawer.png`、`home-edit.png`、`page-manager.png`、`home-after-restart.png`。

真机保留验收时的三页布局、第三页主屏、Hotseat 内 QA_Folder、显示偏好及 Milk + synthetic MTZ 图标，便于继续查看。数据库快照仅在本地 QA 目录，未加入 Git。原有 `.idea/` 与 `gradle/gradle-daemon-jvm.properties` 未纳入本轮提交。
