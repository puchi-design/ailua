# AILUA 小手机应用实用性审计与首轮修复

基线：`local-pass3c-ai-runtime` / `0d15252f39b3fa5ec500642e5af0aebd2de53c28`。本轮沿用既有 Launcher、导航、角色与世界运行时；没有改 Workspace 或数据库 schema。

## 逐项审计结论

原应用库声明 24 个入口。其中 15 个有路由，9 个点开只能看介绍或“规划中”。`reality_bridge` 属于接线错误：真正的 `RealityBridgeScreen` 已经存在，但库入口没有路由。剩余 8 个概念入口当前没有可使用的页面：`world_3d`、`dreamscape`、`game_center`、`shortcut_bridge`、`wechat_assistant`、`agent_tasks`、`black_market`、`age_gated_zone`。本轮把它们从“已安装应用”视图收起，保留原目录元数据供以后实现；没有制造新的假页面。

| 已有入口 | 原来的断点 | 本轮处理 |
| --- | --- | --- |
| 现实连接 | 有页面，但应用库图标无路由 | 接到既有 `REALITY`；权限说明按实际能力写 |
| 相册 | Photo Picker 的临时 URI 和导入列表只存内存，重启后图片消失 | 复制到应用私有目录；持久索引；重新打开可见、查看、确认删除；读写错误可见 |
| 动态 | 喜欢状态只在当前 Compose 画面有效 | 按帖子 ID 保存喜欢/取消喜欢；保留原有评论事件 |
| 日记 | 所有条目共用当前画面的一位喜欢状态 | 按角色和条目保存，重开仍正确 |
| 世界书 | 开关只切换列表副本，聊天仍读取原始设定 | 官方条目开关持久保存，聊天激活读取同一结果；自定义卡隔离 |
| 角色手机 | “最近在听”的播放按钮仅变化图标，完全不播放 | 移除虚假的播放控制；保留真实的虚拟生活记录投影 |
| 通话 | 静音、扬声器按钮只变 UI 状态；没有音频传输 | 移除无效控制，明确标注当前为剧情通话且无实时语音 |

新增一个个人**备忘录**，提供新建、编辑、搜索、删除与重启回读。没有预填演示内容；保存失败会保留编辑中的文字。它用独立 `notes` ID 和现有导航加入应用库，未启用旧 `agent_tasks` 的 AI 执行承诺。应用库现显示 17 个有路由的入口：原有 15 个、接通的现实连接和新增备忘录。

个人备忘录、用户导入照片和社交喜欢记录写在本机私有目录；本轮把这些新增存储从 Android 旧版 Auto Backup、Android 12+ 云备份和设备迁移规则中排除，避免“仅本机”内容被系统备份传出。
相册可在自己的列表显示选中图片的文件名；跨应用世界事件只记录“导入一张照片”的通用事实，不把文件名送进聊天 Prompt。

## GitHub 对照

- [AetherSuite 相册实现](https://github.com/convergence-human-technology/AetherSuite/blob/2dbddd4253aa03a68915d898089207b1a1182231/aether-gallery/src/main/kotlin/com/aether/gallery/GalleryApp.kt) 与 [笔记实现](https://github.com/convergence-human-technology/AetherSuite/blob/2dbddd4253aa03a68915d898089207b1a1182231/aether-notes/src/main/kotlin/com/aether/notes/NotesApp.kt)：对照“列表 → 详情/编辑 → 保存/删除 → 重开”闭环。仓库 [MIT 许可](https://github.com/convergence-human-technology/AetherSuite/blob/main/LICENSE)。本轮独立实现 AILUA 数据层和 Compose 页面，没有直接复制代码或图像。
- [Google Jetchat 的会话](https://github.com/android/compose-samples/blob/main/Jetchat/app/src/main/java/com/example/compose/jetchat/conversation/Conversation.kt) 与 [输入交互](https://github.com/android/compose-samples/blob/main/Jetchat/app/src/main/java/com/example/compose/jetchat/conversation/UserInput.kt)：用作聊天操作闭环的对照，未修改既有 Chat Runtime。
- [xiao-shouji 应用目录](https://github.com/jiuyi777/xiao-shouji/tree/main/src/apps)：仅比较虚拟手机应有的应用种类与操作结果。仓库根目录未核实到明确可复用许可，本轮没有复制其代码、文本或素材。

## 边界与剩余工作

- 剧情通话仍不是双向语音电话；角色手机的歌曲仍是世界记录，不是可播放媒体。UI 已避免暗示这些控制可用。
- 官方相册里既有的程序绘制照片仍是内容占位；用户自己导入的照片已经有真实文件保存闭环。正式角色图片素材属于独立 ART 阶段。
- 未实现的 8 个概念入口只有目录元数据，不在普通用户的已安装应用库中，也不会被加入桌面。
- Android 10 端需覆盖安装后实测备忘录和相册导入、重启回读；ADB 解锁状态与证据记录在本轮交付说明中。

## 本轮验证（2026-10-05）

- `:app:testDebugUnitTest`：651/651 通过，0 失败、0 跳过；覆盖新增的相册副本、动态/日记反应、世界书开关和备忘录存储/大草稿。
- `:app:assembleDebug` 与 `:app:assembleDebugAndroidTest`：成功。
- 小米 M2007J22C / Android 10 API 29：`adb install -r` 成功，原 `firstInstallTime` 仍为 2026-09-27；最终 APK 可启动，进程存活，启动时 logcat 未见 `FATAL EXCEPTION`。
- 机主仍处于系统锁屏（`deviceLocked=1`，焦点 `StatusBar`），所以备忘录编辑、Photo Picker 选择及重启回读 **尚无真机 UI 通过结论**。代码与 APK 已交付，解锁后应补这三项操作证据。
