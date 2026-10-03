# P5.V-R1 → R4 实施报告

2026-10-03，分支 `local-pass3c-ai-runtime`。开工基线 `c5dbaf57d76ca63e1948a4f9cdb897e427362f46`（UI 与 CHAR 已完成）。

## 交付

| 阶段 | Commit | 结果 |
| --- | --- | --- |
| R1 | `470afbd` | 七组 `CharacterRuntimeProfile`，统一 resolver；官方/导入/普通 V2 与未知扩展投影，保留原始 V2 的未知字段。普通卡不自动推断恋爱或性别。 |
| R2 | `95d1759` | 行为指令接入现有 V2 Prompt；沈砚、周野、诺亚各八组对话示例与不同的语言、冲突、推进节奏；减少公式化安慰。 |
| R3 | `a44f2f8` | 九类有证据触发器、五类主动内容、角色频率；稳定机会窗、额度、作息、静默、去重与提交前重验。照片使用资产槽位，信件可重启恢复。 |
| R4 | `46738af` | 内部五维关系记录与阶段、事件驱动、用户边界、冷静期、条件嫉妒；私聊、用户真实通话、首次读信、记忆和剧情结局接入。 |

R3 checkpoint 同时纳入 R4 的最小关系仓库与 Prompt 字段，供主动行为读取冲突/边界证据；R4 checkpoint 接入其余证据生产路径。

## 关键行为

- `CharacterRuntimeResolver` 每次读取当前角色卡，用户覆写官方 ID 仍按用户卡处理；`extensions.ailua` 以外的 V2 字段保持原样。普通酒馆卡使用保守 companion 默认，明确 `route_type` 可为 romance、friendship、family、companion、custom 等任意值。
- Prompt 保留 description、personality、scenario、exampleMessages、systemPrompt、postHistoryInstructions，以及原有记忆/世界上下文；追加有界的角色行为与关系指令，不整份注入 JSON。群聊只传公开路线与边界，不披露私聊冲突和其他角色互动。
- 主动内容 `TEXT`、`PHOTO`、`CALL_INVITE`、`LETTER`、`MOMENT` 各走现有聊天、Gallery、Mailbox、Moments 等投影。照片只引用 `character_asset:` 槽位；通话邀请只是一条消息，不自动拨号。用户收藏照片不计入角色主动拍照额度。
- 新的 `RomanceRecord` 独立存储于 `romance_evidence_v1_json`，旧 bond/NPC 关系不换算成已经恋爱。事件 ID 和内容指纹去重、同日同类收益衰减；普通问候、重生成、失败 AI 回复和短通话不增加关系。用户明确拒绝恋爱在其发言成功保存后立即生效，即使随后 AI 失败。
- 用户点击“打电话”会建立引擎拥有的通话 session。关系推进仅认用户发起、实际连接至少 60 秒的通话；旧的空通话展示路径已修正。

更细的字段与行为约定见 [运行时说明](docs/CHARACTER_RUNTIME_R1_R4.md)。

## 验证

- 离线：`testDebugUnitTest` **579/579** 通过；`assembleDebug`、`assembleDebugAndroidTest` 构建通过。测试覆盖 V2 fallback、Prompt 预算、主动决策及竞态、关系事件去重/边界/持久化接线。
- 设备：小米 `M2007J22C`、Android 10；使用 ADB 覆盖安装，未清应用数据。真实 `MainActivity` instrumentation **1/1** 通过：三位男主 Chat 路由与 Prompt 区分、沈砚 Living、用户发起短通话、独立关系记录保存/重载；测试结束恢复原角色“小弥”并删除隔离的关系记录。
- 强制停止后启动：先出现锁屏，解锁后桌面正常。测试前后 Workspace **3 页、18 项、1 文件夹**与 Milk/Cream 主题设置逐项一致。短通话在历史/生活事件中留下真实 QA 记录，未增加关系数值。
- 设备 logcat 按应用 UID 检索，无 `FATAL EXCEPTION`、`AndroidRuntime`、`SQLiteException` 或 `SecurityException`。
- 截图与日志：[QA 目录](dist/qa/P5.V-R/)；包括三位男主 Chat、沈砚 Living/Call、重启锁屏和解锁桌面。APK：[Debug APK](dist/AILUA-P5.V-R1-4-debug.apk)，SHA-256 `308B788BCE0DDE9BB306FC2467E583CF49FFACBE142362A67D2C3BE5F12C3A37`。

设备测试只验证真实页面路由、Prompt 组装和本地持久化，没有调用线上 LLM，因此不能把三人实际生成的对话效果宣称为已验收。正式角色图片、生成照片、ART 与 P5.6 均未开始。
