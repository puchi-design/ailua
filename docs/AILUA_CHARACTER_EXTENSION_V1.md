# AILUA Character Extension v1

本文记录 CHAR-A→D 已实现的角色卡协议、编辑方式与运行时接入。模型位于 `data/model/AiluaCharacterExtension.kt`，读写入口为 `data/codec/AiluaCharacterExtensionCodec.kt`。

## 1. 标准 V2 与 AILUA 命名空间

角色卡使用 `spec: "chara_card_v2"`、`spec_version: "2.0"`。以下标准字段继续保留各自用途：

```text
name / description / personality / scenario / first_mes / mes_example
creator_notes / system_prompt / post_history_instructions / alternate_greetings
character_book / tags / creator / character_version / extensions
```

AILUA 新增的七组行为字段位于 `data.extensions.ailua`。现有 `data.id`、`data.avatar_reference` 兼容字段继续保留。

V2 的 `extensions` 可包含任意 JSON 值，编辑器应保留未知扩展；书本与条目各自也有扩展对象。协议依据：[Character Card V2 一手规范](https://github.com/malfoyslastname/character-card-spec-v2/blob/main/spec_v2.md)。

`CharacterCardData.extensions` 使用 `kotlinx.serialization.json.JsonObject`，默认 `{}`。旧存档的字符串 map 本来也是 JSON object，因而可以直接读取。保真指 JSON 值、类型及结构保留，不承诺空格、缩进或原始字节完全相同。

`LosslessJsonElementSerializer` / `LosslessJsonObjectSerializer` 还保留合法 JSON 数字的原始字面量，包括超过 `Long` 或 `Double` 范围的大数、`0.0100` 的末尾零、`-0` 和指数写法。数字经过 JSON 数字语法校验后按字面量写入，避免默认 serializer 经浮点数转换造成精度损失。字符串仍正常转义。卡片导出、直接模型 serializer、本地卡片列表、书本、条目以及高级 JSON 预览共用这条保真路径。

下面是 `data.extensions` 的片段；第三方对象、数组、布尔和 `null` 都会保留：

```json
{
  "vendor.example": {
    "enabled": true,
    "options": ["voice-a", null, { "gain": 0.75 }]
  },
  "ailua": {
    "schema": 1,
    "identity": { "gender": "custom", "occupation": "修复师" },
    "initiative": { "message_frequency": "medium", "call_frequency": "low" },
    "life": { "sleep_window": "23:40-07:10" },
    "future_group": { "keep": [1, false] }
  }
}
```

保真范围包括角色扩展对象，以及标准 `character_book` 与其条目的原始 JSON。未建模的卡片根级和 `data` 级未知字段仍由 `ignoreUnknownKeys` 忽略，不应把这些位置当作私有字段的保存入口。

## 2. 七组模型与默认值

根模型为 `AiluaCharacterExtension`，`schema: Int = 1`，七组均有默认实例。JSON 使用下划线字段名，Kotlin 属性使用驼峰名。

除表中单独注明的数字外，字符串默认 `""`、字符串列表默认 `[]`、映射默认 `{}`。缺失性别或关系类型不会自动补成 `male` 或 `romance`。

| JSON 分组 / Kotlin 类型 | 字段与类型 |
| --- | --- |
| `identity` / `AiluaIdentity` | `gender: String`；`age: Int? = null`；`occupation: String`；`height_cm: Int? = null`；`birthday: String` |
| `relationship` / `AiluaRelationship` | `route_type`、`initial_relation`、`affection_style`、`attachment_style`、`physical_distance`: `String`；`jealousy: Double = 0.0`；`possessiveness: Double = 0.0`；`confession_threshold: Double = 0.5` |
| `behavior` / `AiluaBehavior` | `core_desire: String`；`flaws`、`blind_spots`、`boundaries`、`vulnerabilities`、`care_patterns`、`flirt_patterns`、`jealousy_patterns`、`conflict_patterns`: `List<String>` |
| `speech` / `AiluaSpeech` | `sentence_length`、`emoji_frequency`: `String`；`pet_names`、`verbal_tics`、`forbidden_phrases`、`tone`: `List<String>` |
| `initiative` / `AiluaInitiative` | `message_frequency`、`call_frequency`、`photo_frequency`、`moment_frequency`: `String`；`preferred_triggers: List<String>` |
| `life` / `AiluaLife` | `home`、`workplace`、`sleep_window`: `String`；`hobbies`、`social_circle`: `List<String>` |
| `visual` / `AiluaVisual` | `asset_pack`、`default_outfit`、`avatar`、`portrait`: `String`；`expressions: Map<String, String>` |

例如 `height_cm` 对应 `heightCm`，`core_desire` 对应 `coreDesire`，`preferred_triggers` 对应 `preferredTriggers`。性别、关系路线和描述字段使用自由字符串，允许用户导入或创建任意性别与关系类型。

### 字段容错

- 不支持的分组形状按该组默认值读取；一组字段损坏不会清空其他有效分组。
- 字符串字段只接受 JSON 字符串；列表只读取其中的字符串元素；表情映射只读取字符串值。
- `age` 读取可解析且不小于零的整数；`height_cm` 读取大于零的整数，否则为 `null`。
- 三个关系数值要求有限且位于 `[0, 1]`；非法值使用该字段默认值。
- `birthday`、语气、路线等字符串没有额外枚举验证。频率和作息虽为字符串，但需要遵守下一节的机器格式才能影响调度。
- 读取不会修改原始 `JsonObject`。容错后的模型是供 UI 和运行时使用的视图。

## 3. Codec API 与未来版本保护

```kotlin
fun read(data: CharacterCardData): AiluaCharacterExtension
fun readOrNull(data: CharacterCardData): AiluaCharacterExtension?
fun canEdit(data: CharacterCardData): Boolean
fun hasFutureSchema(data: CharacterCardData): Boolean
fun write(extensions: JsonObject, value: AiluaCharacterExtension): JsonObject
fun writeChanges(
    extensions: JsonObject,
    before: AiluaCharacterExtension,
    after: AiluaCharacterExtension,
): JsonObject
```

| 输入状态 | `readOrNull` | `read` | `canEdit` / `write` |
| --- | --- | --- | --- |
| 没有 `ailua` | `null` | 中性默认模型 | 可编辑；显式写入时创建 v1 |
| `ailua` 为 object，schema 缺失或可识别为 1 | 按字段容错读取 v1 | 同左 | 可编辑并深合并 |
| 未来 schema、无法识别的 schema、`ailua` 为其他形状 | `null` | 中性默认模型 | 不可普通编辑；写入返回原扩展 |

`hasFutureSchema` 检查可解析为整数且大于 1 的 schema；它不替代 `canEdit` 对所有不支持形状的判断。写入值自身 `schema != 1` 时，`write` 同样返回原扩展。

`write` 用于完整写入新建模型。它只更新 AILUA 命名空间，以递归合并保留第三方扩展、AILUA 内未知分组及分组内未知键；已建模的字段按传入模型写入，数组整体替换。

**编辑已有卡片使用 `writeChanges`**：先读取容错视图，再修改目标字段，将前后视图的差异应用于原始 JSON。它只更新发生变化的字段，未改动的非法或暂不支持的已知字段也保持原样。例如原始 `identity.age` 为对象、`speech` 为布尔值时，只修改职业不会把这两个字段替换成默认值。修改数组时整体替换目标数组；修改嵌套对象时递归更新改变的键。没有差异时不写入，没有命名空间时首次有效修改创建 schema 1；不支持的 schema 或命名空间形状仍受保护。

```kotlin
val current = AiluaCharacterExtensionCodec.read(card.data)
val changed = current.copy(identity = current.identity.copy(occupation = "摄影师"))
val extensions = AiluaCharacterExtensionCodec.writeChanges(card.data.extensions, current, changed)
val saved = card.copy(data = card.data.copy(extensions = extensions))
```

普通 V2 卡与未来 schema 卡的运行时分支使用 `readOrNull`，因此不会因为默认模型而自动插入行为提示词或收紧规划配额。

## 4. 频率与作息格式

四个 frequency 字段规范写为小写 token：`low`、`medium`、`high`；禁用可写 `off`、`none` 或 `never`。运行时忽略大小写，但不负责去除首尾空白或解释中文长句。说明性文字应放入行为、语气或触发情境字段。

### 主动消息

`message_frequency` 在用户设置基础上收紧规则：

| 值 | 有效设置 |
| --- | --- |
| `off` / `none` / `never` | 关闭自动主动消息 |
| `low` | 间隔至少 18 小时；每日上限取用户上限与 1 的较小值 |
| `medium` | 间隔至少 8 小时；每日上限取用户上限与 2 的较小值 |
| `high`、空值或未知字符串 | 保留用户原设置 |

角色频率不会打开用户关闭的开关，也不会缩短用户设定的更长间隔、提高用户上限或改写免打扰时段。消息间隔、每日次数和免打扰沿用真实设备时间；角色睡眠额外参考虚拟世界时钟。开发控制台显式 `force` 路径沿用原有跳过时序限制的行为。

### 来电、照片、动态的候选计划配额

配额按角色与虚拟世界日期计算，覆盖已有待执行计划、本批已接受计划和账本中已发生的相应事件：

| 值 | `call` | `photo` | `moment` |
| --- | ---: | ---: | ---: |
| `off` / `none` / `never` | 0 | 0 | 0 |
| `low` | 1 | 1 | 1 |
| `medium` | 2 | 3 | 2 |
| `high` | 4 | 6 | 4 |
| 空值、未知值、无受支持的 AILUA 扩展 | 不增加额外配额限制 | 不增加额外配额限制 | 不增加额外配额限制 |

这些是额外上限，候选计划仍须通过原有 `WorldPlanValidator`、联系冷却与事件数量限制；`high` 不保证当天一定产生表中的数量。来电次数统计计划产生的 `pulse_call_plan_*` 事实，同一通电话后续的结束摘要不重复占用配额。手动通话摘要不作为新的自主来电计划计数。

### 睡眠

`sleep_window` 使用固定 `HH:mm-HH:mm`，例如 `23:40-07:10`。中间必须是 ASCII `-`，不是 `–` 或 `—`。小时范围 00–23，分钟范围 00–59。

区间包含开始、不包含结束，支持跨午夜。相同起止、空值或非法格式按“没有可用睡眠窗口”处理。睡眠期间筛掉新的来电、照片和动态候选；日常其他类型不由这条筛选规则统一禁止。fallback 规划可在该时段选择 `SLEEP`。

## 5. 运行时实际接入

| 入口 | 已实现行为 |
| --- | --- |
| `PromptAssembler` | 读取受支持的 AILUA 扩展，插入 `ailua_behavior` 角色块；仅选取明确支持的字段，第三方 JSON、未知键和视觉路径不会整包拼入提示词。 |
| `CharacterBehaviorRuntime.prompt` | 提供身份、关系倾向、缺点、边界、四类行为、说话方式及生活线索。`route_type == "romance"` 时，将三个关系数值转为低/中/高文字提示。 |
| `ProactiveMessageEngine` | 默认从 `CharacterContext` 获取当前角色；使用该角色卡生成提示词，并应用主动消息频率与睡眠规则。会话、事件及通知仍写入该角色原有链路。 |
| `WorldActionPlanner` | 将主动程度、作息、地点、爱好与社交圈作为规划上下文；生成候选后交给 `CharacterWorldPolicy` 筛选。配额使用完整已传入账本，提示词中的近期事实可截短。 |
| `CharacterWorldPolicy` | 只筛选新候选，不修改输入动作、已有计划或已发生事实；按角色、日期、类型统计配额。 |
| `WorldPlanRuntime` fallback | 照片偏好改变候选类型列表中的出现次数；`medium/high` 动态偏好加入动态候选；睡眠时选择休息事件。普通卡和未来 schema 卡沿用旧的日常类型列表，包括 `SLEEP`。 |
| 角色资料投影 | 自定义及保存后的卡片可使用职业、语气、工作地/居所、初始关系作为资料显示。`CharacterPortrait` 优先使用显式照片覆盖或 `avatar_reference` URI，再考虑对应 variant 的 `visual.avatar` / `visual.portrait` URI；本次仅加载本地 content/file/resource URI。没有可用图片时，旧 Mira/Yuna 使用原头像，其余角色使用名字占位。 |

关系数值目前是提示词倾向，没有新增独立的好感、嫉妒或告白状态机；`confession_threshold` 不会自动判定恋爱成立或触发告白。`preferred_triggers`、爱好与社交圈是生成上下文，没有为每个字符串创建独立触发器。

电话仍由既有 `CallStateEngine` 执行接听、稍后、结束等状态转换，本次没有改写通话状态机。照片与动态通过既有世界事件及投影链路产生；本协议不包含图片生成或新相册存储系统。

## 6. Lorebook 与存储兼容

`WorldBookSerializer` / `LoreEntrySerializer` 保留现有 Kotlin `WorldBook` / `LoreEntry` API，并在 JSON 层桥接标准 V2：

- 读取标准 `scan_depth`、`token_budget`、`recursive_scanning`、条目 `keys` / `secondary_keys` 等字段，也读取旧内部 camelCase 格式。
- 标准书本和条目的 `sourceJson` 保留未支持的可选字段、未知键及嵌套扩展；未编辑内容再导出时保留这些值，编辑投影字段时仅更新对应字段。
- 新建或旧内部格式的书本导出为标准字段；内部字符串 ID、角色/地点限制、激活模式等专属元数据放入书本或条目的 `extensions.ailua`。已有标准数字条目 ID 保持其 JSON 类型。
- 这些规则位于模型 serializer，直接通过 `CharacterCard.serializer()` 存储也有效，不依赖仅在 SAF 导入时执行一次转换。
- `AiluaLocalStore` 的自定义卡片继续使用原存储键。`decodeList` 按卡解码，单张无法解码的记录会被跳过，其余有效记录继续恢复。

这层桥接保证 wire 数据保存，不表示所有第三方 lorebook 可选字段都已成为新的运行时激活规则。

## 7. Character Creator 两种模式

`CreatorDraft` 持有完整加载卡片。修改普通字段使用 `copy`，只在用户编辑行为字段时写入 AILUA 扩展，避免保存时重建卡片导致 PHI、候选开场白、书本或第三方扩展丢失。

- **普通模式**：角色照片、名字、自由性别、年龄、职业、关系、人物简介、性格、说话感觉、日常爱好、表达喜欢与吃醋的行为、主动频率、相遇场景和第一句话。不会显示关系数值面板。
- **高级模式**：标准 V2 字段、system prompt、PHI、alternate greetings JSON、lorebook JSON、extensions JSON、作者与版本、头像引用，以及模板、预览、SAF 导入导出。
- 高级 JSON 修改保存在草稿中；无效内容保留以便修正，不会悄悄清空原字段。切回普通模式前应用并校验 JSON 修改。
- 对未来 schema 或不支持的扩展形状，普通模式停用行为字段编辑，仍可修改人物简介等标准字段；原扩展保留，高级模式可查看或显式编辑 JSON。
- JSON 文件仍通过系统文件选择器按 UTF-8 读写。用户选择照片时使用 SAF 并取得持久读取权限。

`visual` 是未来资产引用协议；codec 本身不读文件、不联网，也不自动安装素材包。角色照片选择使用现有显示链路；正式角色立绘、表情包及完整服装资产尚未制作。

## 8. 官方阵容与 Yan / Mira 迁移

官方恋爱入口顺序为 `yan`、`yeo`、`noa`。`yan` 与 `yeo` 使用新 ID；现有 `mira`、`yuna` 保留为可选角色，未把其历史记录改挂到男性角色名下。`noa` 保留原 ID；已有会话和世界事实不因官方设定更新而重写。

新增角色的初始生活事实均发生于默认世界时间 21:30 之前；后续修复台、选片、档案、来电和休息安排属于 `OfficialDemoWorldPlan`，不预先写入已发生事件。冷启动时，仅在无已保存计划、无可用 provider 且当前选择官方角色的情况下，将当天尚未经过的官方日程转换为带已保存世界日期的 `WorldPlan`，先持久化再恢复调度队列。这样 `futureActions()` 与首次 `maybePlan()` 读取同一计划，不会因未来队列为空而立即用 fallback 覆盖官方日程。已有计划优先，旧角色的 legacy 日程分支保留；错过的官方时间点不会重播或自动挪到次日。

`CharacterSelectionPolicy.resolve` 的选择规则：

1. 已有非空 `selected_id`：保持原值，包括导入或自定义角色 ID。
2. 没有保存选择，但检测到旧聊天数据库、已完成 onboarding，或原 `ailua_os_store` 非空：使用兼容角色 `mira`。
3. 没有上述旧数据：新安装默认 `yan`。

`CharacterContext.init` 在新聊天数据库初始化前执行，并立即保存首次选择结果，避免首次欢迎页中途退出后被误识别为旧存档。普通聊天或资料导航不会自动改写选择；欢迎页确认选择、工坊的“保存并预览”，以及资料页的“在主屏显示”会显式调用 `CharacterContext.select`。

欢迎页优先呈现三位官方角色，并提供其他角色及导入入口。Registry 保留用户导入/保存卡片的优先级，包括使用已有内建 ID 的编辑；自定义性别与关系路线不受官方男性阵容限制。

## 9. 相关回归测试

协议与接入的回归用例位于：

- `CharacterCardCompatibilityTest.kt`：嵌套扩展、数字字面量保真、完整 V2 字段保留、旧字符串 map、标准/旧 lorebook、直接 serializer、预览与列表恢复。
- `AiluaCharacterExtensionCodecTest.kt`：七组字段、局部容错、未知键深合并、未来 schema 保护。
- `CharacterBehaviorRuntimeTest.kt`：提示词隔离、官方差异、用户限制、睡眠、候选配额及输入数据不变。
- `ProactiveMessageTest.kt`：显式角色注入、旧 Mira 流程、Yan 提示词及会话/通知归属。
- `CharacterSelectionPolicyTest.kt`、`OfficialCharactersTest.kt`、`CreatorDraftTest.kt`：选择兼容、阵容与编辑草稿。

本文不记录测试通过数量或真机结果；验证状态以本次构建与 QA 报告为准。
