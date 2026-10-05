# AILUA V6.1 Rich Message 实施记录

状态：V6.1 已接入并完成 Android 10 真机受控交互及跨进程持久化验收。真实模型自然生成富消息仍需用户配置聊天 Provider 后验证。

## 范围

- 沿用 `ChatRepository`、`ChatTurn`、`ChatVariant` 和 `ChatGenerationRuntime`。SQLDelight 从 v8 增量升级到 v9，为既有 `chat_variant` 加入可空的有序富消息 JSON、引用消息 ID 与引用预览；旧聊天行原样保留。
- 第一版支持文字、引用、表情、虚拟位置、礼物、虚拟红包、虚拟转账。没有余额、支付接口或真钱路径。
- 模型可输出短指令，`RichMessageParser` 安全解析；非法金额、空内容、未知表情以及超限指令保留为普通文字。单轮最多两条富消息，其中金融型最多一条。
- 中文模型常见的 `【红包：30：晚饭钱】`、`［表情：偷看］` 也能按相同规则解析；非法的全角指令仍原样显示。
- 用户操作通过现有仓库原子更新卡片状态；重复操作无效。只允许 COMPLETE 的当前活动回复版本被操作，避免重生成后的旧卡片被误触。
- 富消息操作产生确定 ID 的 `LifeEvent`，被重复观察时去重；接受类互动再交由现有 Romance reducer 的轻量 `GOOD_EVENT` 处理。退回转账是中性私密记录，不增加关系值。聊天页重新观察已完成的卡片时补记证据，覆盖 SQL 提交后进程中断的窗口。
- 引用 ID 必须属于当前会话；第一版只引用角色消息，引用预览随用户消息持久化，后续 Prompt History 自然表述引用关系，不要求模型猜 ID。
- 纯富消息卡片也可长按引用：输入区使用“红包 · 晚饭钱”等可读摘要，不显示原始模型指令；发送时沿用现有引用 ID 与预览持久化链路。
- 浏览角色手机的用户活动从聊天、世界计划和主动行为的角色 AI 输入中排除；原私密记录仍持久化用于用户侧证据。
- 红包打开、转账收下或退回、礼物收下的最终状态现在进入下一轮聊天 Prompt History；角色不会仅看到当初“发出卡片”的记录。待处理卡片不会被写成已收下。

## 已验证

- `:app:testDebugUnitTest` 离线全量通过：**701/701 单测**，Debug APK 与 AndroidTest APK 构建成功。
- `RichMessageParserTest`、`RichMessagePersistenceTest`、`RichMessageStateTransitionTest`、`RichMessagePromptHistoryTest`、`RichMessageEvidenceTest`、`ChatMigrationTest` 专项通过。测试覆盖 v8→v9 迁移、文件数据库关闭重开、非法指令、生成结果入库、打开/接受/退回、重复点击、跨会话引用隔离、隐藏手机查看记录，以及完成状态进入下一轮 Provider 请求。
- 使用现有 Fake Provider 的生成流程回归用例确认：带中文括号和冒号的模型回复会落库为红包卡片，正文不残留原始指令。这验证协议兼容性，不替代真实 Provider 的自然生成验收。
- Android 10 小米 M2007J22C 上 `adb install -r` 覆盖安装成功并启动到 AILUA 虚拟锁屏和桌面；没有清除用户数据。真机仪表用例 `richCardsQuoteAndPersistedActionsUseOnlyTemporaryQaSession` 通过：普通文字和纯红包卡片均能长按引用，红包打开、转账退回、位置回调、表情渲染、重开 SQL 驱动后的状态与引用均核对成功。卡片引用截图见 `dist/qa/V6.1-V6.2/rich-quote-card.png`；其它截图见同目录的 `instrumented-rich-cards.png` 和 `instrumented-rich-reopened.png`。
- 独立跨进程用例先在唯一 `qa_v61_*` 会话中写入红包 `OPENED`、转账 `ACCEPTED` 和引用关系，再执行 `am force-stop`、启动 AILUA，由新仪表进程打开同一磁盘数据库复核，全部通过。`V61QaCleanupTest` 随后通过，临时会话已删除；原角色和用户记录未改写。
- 真机截图暴露 Android 10 不支持原「偷看」及「抱抱」所用的新 Unicode 表情；已换为 Android 10 可正常显示的字符，并重新装包截图确认。
- `git diff --check` 通过。

## 真机验证范围与限制

- UI 用例使用 Debug 专用隔离 Activity 和合成角色会话，避免 MIUI 阻止仪表框架启动 Activity；生产 Release 不包含该 Activity。该 Activity 的截图包含 Android 原生状态栏和导航栏，不能作为正式 App 的边缘视觉验收图。位置卡片被「回到最新消息」浮层遮住中心时，测试改点卡片未遮挡区域，回调通过。这是测试点击坐标问题，不是位置卡片解析失败。
- 跨进程用例直接通过现有 `ChatRepository` 写入受控富消息，验证真实磁盘与进程重启；真实用户聊天 Provider 未配置，因此不能宣称模型会自然发出红包、转账等指令，也未将模拟会话写入用户的正式角色聊天。
- Android 10 最终真机用例：Rich UI 1/1、Check Phone UI 1/1、跨进程准备和验证各 1/1、清理 1/1。最终 `adb logcat` 未见 AILUA 的 `FATAL EXCEPTION`。测试前手机的 USB 常亮设置为 `0`，结束时已恢复为 `0`。
- 最新 APK 覆盖安装后 AILUA 正常启动，Rich UI 与 Check Phone UI 真机用例 **2/2**、QA 清理 **1/1** 通过。交付 Debug APK：`dist/AILUA-V6.1-V6.2-debug.apk`，SHA-256 `6463212a2a044ff98d19a363a592d94fc3da25d13e901ec6266a5f455ed7320f`；设备上已装 APK 的 SHA-256 完全一致。Debug QA Activity 仅接受持有 `android.permission.DUMP` 的启动方，Release 不包含它。
