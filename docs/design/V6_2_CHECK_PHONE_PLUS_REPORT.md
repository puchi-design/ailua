# AILUA V6.2 Check Phone+ 实施报告

日期：2026-10-06
基线：`local-pass3c-ai-runtime` / `2d7712e8217fed9d9abccbc66a102043b7153214`

## 实施范围

- 保留现有 `projectCheckPhone` 和 `CheckPhoneData`，增加只读 `projectCheckPhonePlus`。它以角色 ID 过滤持久化的 `LifeEvent`、`GalleryRepository.assets`、`CallStateEngine.callHistory`、`AiluaLocalStore.savedWorldPlan`、关系与恋爱记录。不新增数据库表或另一套世界运行时。
- 查手机首页改为七个可打开分区：搜索、未发草稿、照片、通话、音乐、角色备忘和虚拟 App 使用记录。特殊关系阶段还可看已有的浏览和收藏记录。点击照片会定位到原相册中的该张照片，并将相册限定在该角色；角色通话留在当前角色的只读列表。
- 所有分区使用现有 `AiluaScreenScaffold`、`AiluaSectionHeader` 和 `LocalAiluaTheme`。没有播放器、发送草稿、真实 Android 使用统计或用户 Memo 内容。
- 六位官方角色各自保留原稳定 ID，并把原先相同的占位未发草稿改为六条符合各自人设的初始内容；已有角色数据、聊天和旧存档未迁移。

## 数据来源与持久化

1. 搜索、草稿、角色备忘、音乐：已发生的角色 `LifeEvent.metadata` 优先；搜索另有少量根据真实近期地点、事件、职业、天气和世界时间得出的规则投影，内部标记来源。未发生但已保存的 `WorldPlan` 中明确的 `draft` 可形成未发草稿；只有明确 `note` 元数据且尚未触发的计划可形成标为“待办”的备忘。
2. 照片：复用 `projectGalleryAssets`、Gallery 的角色资产和 `PHOTO` 事件。用户导入照片及其他角色照片不会进入当前角色的手机。
3. 通话：复用已完成或拒接的 `CallStateEngine.callHistory`，按角色 ID 隔离。
4. 使用记录：只从实际角色事件推断相应的虚拟应用操作，例如明确的 `search_query`、`note`、`music_title`、`PHOTO`、动态、日记或通话事件。无对应事实就显示空状态，不使用 Android `UsageStats`。
5. 官方 authored seed 只作没有运行时事实时的低优先级初始内容。普通导入卡如果没有相关事实或种子，对应分区为空，不自动伪造其搜索、草稿或音乐。旧 Mira fallback 仍由原投影保留以兼容历史引用。

用户编辑官方卡并保留同一 ID 时，扩展投影不再附带原官方手机种子；已有以该稳定 ID 存储的真实事件仍照常读取。规则推测的搜索保留独立来源标记，不写入运行时事实。

所有运行时条目使用原有事件、相册、通话或世界计划的持久化 ID。杀进程后从这些记录重新投影，不依赖 Compose 临时状态。

## 隐私

`projectPhonePrivacy` 只读取已有的 `RomanceReducer.stage` 或 `RelationshipState.stage`，不新增好感阈值。初识可看搜索和使用记录；熟悉可看照片、通话、音乐；亲近可看草稿和备忘；更深关系可看已有的浏览和收藏。紧张关系回落到基础可见范围。角色手机的阅读行为不会进入聊天、世界计划或主动联系的角色 AI Prompt。

## 验证

- `CheckPhoneProjectionTest`、`CheckPhonePlusProjectionTest`、`CharacterDraftProjectionTest`、`SearchHistoryProjectionTest`、`CheckPhoneVisibilityTest` 定向运行：**28/28 通过**。覆盖角色隔离、官方/导入卡种子、世界事件变更、持久化重投影、计划草稿与备忘、真实相册和通话复用、关系阶段与隐私锁。
- 首轮编译曾被并行施工中的 SQLDelight 富消息字段不一致阻断；富消息代码补齐后，最终定向测试编译与执行通过。
- 最终全量 `:app:testDebugUnitTest`：**696/696 通过**；Debug APK 和 AndroidTest APK 构建成功，`git diff --check` 通过。AndroidTest APK 含 Android 10 两角色界面与临时 QA 数据清理用例。

## Android 10 真机验收

- 设备：小米 M2007J22C，Android 10 / API 29。`adb install -r` 覆盖安装通过；未清理用户数据。手工打开贺闻川、许朝颜和苏晚宁的手机，三人的搜索内容各不相同。许朝颜搜索详情显示摄影、展览与公寓附近晚餐等条目；使用记录显示实际角色事件投影。截图在 `dist/qa/V6.1-V6.2/checkphone-he.png`、`checkphone-he-search.png`、`checkphone-xu.png`、`checkphone-xu-search.png`、`checkphone-xu-usage.png`。
- 当前关系阶段下，草稿、照片、通话、音乐和备忘显示锁与“暂时无法查看”，没有泄漏内容；`checkphone-drafts-locked.png` 和 `instrumented-checkphone-xu-drafts.png` 留证。贺闻川与许朝颜的受控 Compose 真机界面用例通过，截图为 `instrumented-checkphone-he.png`、`instrumented-checkphone-xu.png`。
- 强制停止 AILUA 后重新启动，许朝颜仍是当前角色，搜索摘要和隐私状态均恢复，见 `checkphone-restart-xu.png`。测试后已通过 UI 将主屏角色恢复成原来的苏晚宁，手机 USB 常亮设置恢复原值 `0`。最终 `adb logcat` 未见 AILUA 的 `FATAL EXCEPTION`。
- 已锁分区的照片跳转、通话、备忘内容无法在当前用户关系阶段做真机可见性验收；投影与分区逻辑由离线测试覆盖。当前没有新的角色照片或通话事实时，相应页面不制造样本充数。

## 已知界限

- 未配置 AI 世界规划时，搜索仍可随真实生活事件、地点和天气产生少量规则记录；草稿、音乐等部分分区可能只有官方初始内容或为空。不会用固定假记录填满页面。
- 已有 Gallery 对没有真实图片资源的 `PHOTO` 事实可能使用其原有视觉 fallback；Check Phone 不制造另一份假照片。
- 草稿是角色未发出的文本，没有“发送”操作；音乐只是最近记录，没有虚假的播放按钮。通话时间沿用已有 `CallSession` 的记录格式。
