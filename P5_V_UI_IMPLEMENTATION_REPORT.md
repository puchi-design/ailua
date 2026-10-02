# P5.V-UI0–UI4 交付与真机视觉复核

## 状态

- 基线：`local-pass3c-ai-runtime` / `57e83641223cc72f1a29042497b33231176e2bed`，开工时已与远端一致，包含完成的 P5.5。
- UI0–UI4 已实施，改动保留在工作区，尚未提交或推送，等待四页视觉方向确认。
- 未迁移 UI5，未开始 P5.6，未修改角色人设或制作正式角色素材。
- APK：`dist/AILUA-P5.V-UI0-4-debug.apk`。
- APK SHA-256：`2b29db22355e1ef4d1c586e73bd6e8ee7c175cb64d78a98a03b810d7167060a5`。

## 实施内容

1. 沿用 P5.5 已提升至 App Root 的唯一 Theme Runtime，扩展 surface、shape、layout、text 语义规格。完整映射 Material3 的颜色、字体与形状，包括菜单使用的 surfaceContainer 系列颜色。
2. 字号统一为 30 / 20 / 17 / 15 / 13 / 11；主要圆角统一为 10 / 16 / 22 / pill。
3. 新增七个公共组件：AiluaScreenScaffold、AiluaTopBar、AiluaSectionHeader、AiluaSurface、AiluaChip、AiluaMediaFrame、CharacterPortrait。Portrait 继续使用现有 AiluaAvatar，未引入新角色资产。
4. Virtual Chrome 移除状态栏主题切换及品牌文字，保留时间、通知、网络、电量、Live Activity 与左右下拉入口。迁移页面顶部返回逐页返回，底部指示条返回主屏。
5. LifeBento 改为角色主视觉、消息/生活/相册三个入口、纯列表今日动态；同步收敛 Home Widget、Dock 和编辑面板的表现。
6. Chat 拆出五个 UI 文件；移除头部羁绊等级；气泡取消边框/阴影；复制、保存记忆、重新生成、收藏、回复分支放入长按菜单；使用统一 Composer。
7. Living 保留 300dp Hero 与事件列表，主视图移除 RPG 指标；Profile 改为角色视觉、介绍、关系记忆、最近事件。Living 现有投影未提供事件日期，因此使用“最近动态”，避免把历史事件标成“今天”。

Workspace 拖拽、Folder、Pager、Resize、持久化、App Drawer、ChatViewModel、Memory、World Runtime、Call Engine 未修改。

## 验证

设备：小米 M2007J22C，Android 10 / API 29，1080×2340。ADB 使用绝对路径正常连接。APK 使用 `install -r` 覆盖安装，没有清除应用数据。

| 检查 | 结果与证据 |
| --- | --- |
| 离线单测 | 451/451；96 suites，0 failure/error/skip；`build-final.log` |
| 最终 Debug 构建 | PASS；`build-delivery.log` |
| Chat 真机 instrumentation | 1/1 PASS；`instrumentation.log` |
| 四页导航及绘制 | PASS；Home / Chat / Living / Profile 截图 |
| Chat 气泡与长按 | PASS；显示复制、保存记忆、重新生成、收藏；未执行生成请求 |
| 回复分支 | PASS；切换已有分支，UI 与数据库 activeVariantId 一致 |
| Composer / IME | PASS；键盘打开后输入框和发送按钮可见；`chat-ime.png` |
| 顶部返回 / 底部 Home | PASS；深层 Profile → Living 后点击底部指示条返回主屏 |
| 通知 / 控制中心 | PASS；从 Living 状态栏左右下拉均正常 |
| Milk / Glass 主题传播 | PASS；页面及菜单随全局 Runtime 更新，Glass 菜单文字可读 |
| 覆盖安装与重启保留布局 | PASS；3 pages、18 items、folder 的完整行数据与安装前一致，含位置、Widget span 与 Hotseat rank |
| 重启保留外观 | PASS；重启前后外观设置一致；`verification-summary.json` |
| 应用日志 | 本次采集未发现 FATAL、ANR 或资源/文件/解析异常；`device-logcat.txt` |

真机检查修正了“全部消息可见时仍显示回到最新消息”的条件，并在最终 APK 上复测通过。还补回 Living 返回按钮原测试标识。

## 截图

目录：`dist/qa/P5.V-UI/`。

- `core-pages-review.png`：Home LifeBento / Chat / Living / Profile 四页对照。
- `home.png`：保留原布局的 Workspace 页。
- `home-life-bento.png`、`chat.png`、`living.png`、`profile.png`：四页原始真机截图。
- `profile-details.png`：关系记忆和最近事件。
- `chat-ime.png`、`chat-glass-menu.png`：键盘与 Glass 菜单。
- `instrumentation-artifacts/`：长按菜单、回复分支截图及创建数据 ID 清单。

## 测试数据和范围

- 小米安装前聊天记录为空。本次通过原 SqlDelightChatRepository 写入 4 条明确标记“［UI QA］”的本地消息，并添加 1 个备选回复，用于视觉与交互检查。没有调用外部 AI，也没有清除会话。这些测试消息保留供查看，ID 已存档。
- 四页交付截图使用 Milk + Cream。通过原主题中心调整了外观以便对照；原设置备份在 `before-install-preferences.json`，未删除已有导入主题。桌面图标大小、标签开关和布局没有重置。
- 本轮验证范围是 UI 与对应交互，没有重新验证真实 AI Provider、语音生成或通话引擎。
- 正式角色素材仍为后续工作；未迁移页面保留现有实现，仅共享全局 Theme Runtime。

下一步仅等待四页视觉确认。
