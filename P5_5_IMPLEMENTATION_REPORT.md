# P5.5-A → D 实施报告

日期：2026-10-03。分支：`local-pass3c-ai-runtime`。起点：`e0ba22deec55edbcc8fb133b3dd26ca091bc15ff`。

## P5.5-A — Lock Screen

完成统一 `VirtualSystemUiHost`、Controller、Provider；系统层覆盖 NavHost，不作为普通导航页面。Theme Runtime 提升到全局，并增加锁屏、通知、控制中心及持续活动的四套 Skin token。

完成 onboarding 后默认冷启动锁屏；同进程返回、Activity 重建不会重复冷启动锁定。支持上滑解锁、通讯 / 相册快捷入口、真实设备时间、AILUA 世界时间与角色状态，最多三条未读通知预览。锁屏壁纸与 Home 共用选源。来电临时置顶，接听后现有通话页面可操作；手动锁屏及通话结束遵守原锁定状态。

## P5.5-B — Notification Center

完成持久化通知仓库与 Graph；`source_key` 唯一去重，seen 与 dismissed 独立，清除保留去重历史。新事件通过无重播的 SharedFlow 投递，重启历史不会再次弹横幅。

主动消息仅在真实聊天、LifeEvent 与发送状态写入成功后通知；Mailbox 仅对本次新送达信件通知。支持通知列表、点击路由、滑动删除、清除全部、未读状态及约四秒 Heads-Up。锁屏、系统面板、专注、来电或应用失焦时抑制横幅，通知仍可持久化。

## P5.5-C — Control Center

完成网络 / 电量状态、深色、专注、静音、虚拟亮度、锁屏、主题及角色入口；通知中心提供四个快捷项。控制状态用 SharedPreferences 持久化。亮度仅为 AILUA 绘制遮罩，不修改 Android 系统设置；专注不阻止消息生成。

## P5.5-D — Live Activity

复用 `CallStateEngine.currentCall` 投影 CONNECTED 通话；支持状态栏胶囊、计时、展开卡片、返回通话、结束，以及锁屏 / 通知中心摘要。结束后自动消失；来电继续使用既有 IncomingCallScreen，不产生重复普通通知。模型可容纳多项活动，本轮实际接入 CALL。

## DB schema

- 新增 `VirtualNotification.sq` 与 `7.sqm`，schema **v7 → v8**；锁屏、Control 与 Live Activity 不新增数据库表。
- 本轮连接设备原安装为 **v3**，覆盖安装升级到 v8，未清除原应用数据。
- `upgrade-preservation.json` 确认原聊天会话、23 条聊天、23 个变体、4 条记忆及 1 条提取游标全部保留；启动增加会话未影响原记录。

## Tests

- 最终统一离线 `testDebugUnitTest assembleDebug`：**451 / 451 通过，96 suites，0 failure / error / skip**；`dist/qa/P5.5/build-final-glass.log` 为 `BUILD SUCCESSFUL in 2m 46s`。
- 关键新增覆盖：迁移、持久化去重、seen / dismiss、主动消息及来信接入、锁定状态、专注抑制、通话活动投影。
- Android instrumentation：主安装两项 PASS；已有信件全部送达，Mailbox 项按设计 SKIP，未清除送达历史。
- 另建仅更换 applicationId 的隔离 QA 安装，调用同一 WorldHeartbeat / Mailbox 链路推进 59 分钟，悠奈信件真实新送达与通知 PASS：`instrumentation-isolated-mail.log`、`isolated/results/`。
- 新增 Glass 明暗 / Palette / 白色底图文字对比度回归；安全区域修复后 Mono 通话测试 PASS，最终 APK 主动消息测试再次 PASS。再次清除全部因已有通知而跳过，首次完整轮已 PASS。

## APK

- 交付路径：`H:/ailua/dist/AILUA-P5.5-debug.apk`。
- 最终 APK 大小：**28,252,678 字节**。
- SHA-256：`8e91154642c50366bd53e009d326fc6d4daa7ed7199efb183aa8fd577e1d34ab`；与真机已安装的 `base.apk` 一致。
- 截图与本地证据：`H:/ailua/dist/qa/P5.5/`。

## 真机 smoke

实际设备为用户更换后的 **realme RMX6699 / Android 16 / API 36**，ADB 覆盖安装并保留数据。本轮 **未在原小米 Android 10 上实测**，不能将 Android 16 结果记作 Android 10 PASS。

| 检查 | 结果 |
| --- | --- |
| 冷启动锁屏、解锁、锁屏通知 / 控制中心、手动锁屏 | PASS |
| 主动消息 → 持久化 → Heads-Up → 点击 Chat | PASS；仅 Provider 输出使用标注的本地 instrumentation fixture，实际执行生产消息写入与通知链路 |
| 专注模式、滑动删除、清除全部 | PASS；清除只在活跃通知均为本次 QA 产物时执行 |
| 网络 / 电量、亮度、深色、静音 | PASS；虚拟控制状态与偏好持久化 |
| 锁屏来电优先级、接听、胶囊、锁屏摘要、展开结束 | PASS；沿用真实 CallStateEngine 与既有接听 UI |
| Mailbox 新送达 | 主安装 SKIP；隔离 QA 安装 PASS，不重置用户信件历史 |
| 强停重启 | PASS；Workspace、Widget 尺寸、主题 / 控制偏好及三条通知字段一致；两条已清除、一条未读保留，source_key 无重复 |
| Milk 锁屏 / Diary 控制中心 / Mono 通话胶囊 | PASS |
| Glass 浅色通知中心 | PASS；修复后标题、日期、通知与清除按钮可读，支持与外部壁纸混搭 |
| 外部壁纸与 SAF 返回不重锁 | PASS；使用项目 synthetic `.ailuatheme`，SAF → 预览 → 导入应用 → Home / Lock → 强停重开，壁纸保留；未下载商业样本 |
| 最终应用 logcat | PASS；应用 UID 持续日志与最终快照未发现 FATAL、AndroidRuntime、SQLiteException、IllegalStateException、权限、ZIP / XML、文件读取异常；MAIL assumption SKIP 不是应用崩溃 |

关键截图：`lockscreen.png`、`notification-shade.png`、`heads-up.png`、`control-center.png`、`live-call.png`、`live-call-lockscreen.png`。持久化证据为 `before-restart-*`、`after-restart-*`；QA 消息明确标注 local fixture，未把 fixture 内容描述为联网 AI 生成。

补充截图：`saf-return-unlocked.png`、`external-wallpaper-home.png`、`external-wallpaper-restart-lockscreen.png`、`isolated/results/mail-delivered.png`。测试后删除本轮导入的 synthetic 主题，壁纸 / 图标正常 fallback；卸载独立 QA App / Test，保留原应用及其数据。最终原应用为 Milk / 浅色，测试通知历史保留。

## 实际修复

- 补齐既有 CallScreen 的虚拟状态栏，使已接听通话可显示胶囊。
- 系统面板 / 锁屏遮蔽下层页面的无障碍语义，避免隐藏页面重复暴露控件；打开面板时收起输入焦点与键盘。
- Android 顶边下拉可揭示原生状态栏但不触发窗口焦点变更；在系统表面状态变化后重新隐藏，并将虚拟界面统一放入安全区域，避开 Android 16 挖孔与导航区域。
- 独立 Dialog 失焦期间抑制不可见的 Heads-Up；保持通知落库。
- 修正 SQLDelight 变更查询返回类型；Mailbox 测试明确区分初始化已送达的 Mira 历史信件与两封真正的新送达信件。
- Glass 固定浅色前景却继承了浅色背景，导致通知标题接近白字白底；统一深色系统背景并加强卡片 / 锁屏遮罩，保留半透明亮边，真机浅色模式复测通过。

## 已知限制

- 这是 AILUA 内部系统层，不接管 Android Keyguard、通知读取、真实网络开关或系统亮度。
- 原生 Dialog / 系统文件选择器使用独立窗口；此时横幅被抑制，通知照常落库但不会稍后重播。主题面板等原生模态窗口需先关闭，再使用下层状态栏手势。
- 来电没有新增 missed timeout；Live Activity 本轮只实现 CALL；主题快捷入口回 Home。
- Android 10 兼容性保留 API 24 基线及无需 RenderEffect 的视觉实现，但本轮实机证据仅覆盖 Android 16。

## HEAD

阶段提交：A `452d952`；B `3382487`；C `edea962`；D `3838309`。

最终代码 / APK 验证 HEAD：`cb810f95d19ecc10b37e0be0ab3b650776df94b1`。其后仅提交本报告；包含报告的最终交付 SHA 与推送结果见交付消息，可用 `git rev-parse HEAD` 核对。停在 P5.5，未开始 P5.6。
