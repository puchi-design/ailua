# Character Runtime R1–R4

## 单一投影边界

`CharacterRuntimeResolver.resolve(characterId)` 每次读取 Registry 当前卡，用户同 ID 编辑立即生效。`resolve(CharacterCardData)` 供纯 Prompt 组装及测试。返回不可变 `CharacterRuntimeProfile`，七组分别为 identity、relationship、behavior、speech、initiative、life、visual。

Codec 和角色工坊仍处理 wire 数据；业务模块读取投影。角色卡本身不被投影回写，未知第三方扩展和未来 schema 沿用 CHAR 的 JSON 保真路径。只在确定为当前官方原卡时标记 OFFICIAL，不能凭一个 `yan` ID 覆盖用户自定义人设。

普通 V2 或未知 schema 的默认：消息 medium；电话、照片、动态、信件 low；睡眠 `00:00-08:00`；路线 companion。标签只作为风格参考，不据此判定性别、恋爱或共同经历。显式任意 gender / route_type 保留，只有 `romance` 启用恋爱阶段。未知触发器不执行；只有未知触发器时使用保守默认，显式零权重保持关闭。

## 追加的兼容字段

所有字段仍放在 schema 1 的 `data.extensions.ailua`，不会升级或重写未知未来 namespace。

| 字段 | 默认/含义 |
| --- | --- |
| `relationship.progression_style` | `balanced`；官方分别 `slow_trust`、`expressive`、`reserved` |
| `initiative.letter_frequency` | 缺失按 low 投影 |
| `initiative.trigger_weights` | snake_case trigger → 0..1 权重；缺失使用 user_absent/shared_memory 保守默认 |
| `initiative.max_text_burst` | 1，限定 1..3；周野为 3，一次完成/联系可呈现多个短意群 |

频率投影：none=0，very_low=.06，low=.18，low_medium=.35，medium=.5，medium_high=.65，high=.8。这些是合格机会中的决策参数，不是每秒随机率，也不承诺固定发送数量。联系人总开关、间隔、每日上限、静默时段、作息和重复检查优先。

## Prompt

保留 V2 description、personality、scenario、mes_example、system_prompt、PHI 及既有 lore/memory/history 组装顺序。行为层逐项生成有界自然语言，不放完整 JSON 或视觉字段。普通卡的 fallback 块可在预算不足时让出。

三位官方卡各有八组独立示例：普通聊天、疲惫、高兴、深夜、嫉妒、误会、久未联系、暧昧升温。共同约束减少泛化安慰、逐条总结和建议；语境确有需要时允许关心。各自的称呼、停顿、幽默和冲突方式来自人物卡。

`PromptAssemblyInput.relationshipInstructions` 是当前用户与该角色关系的定性投影，不包含数字或完整存储记录。私聊与主动行为使用它；群聊保留 NPC 之间的公开关系，不导入其他角色的私密用户关系。

## 主动行为

触发器：user_absent、late_night、morning、rain、birthday、shared_memory、recent_conflict、recent_good_event、location_change。只有实际时间、天气、互动/记忆/位置事实满足时才成为候选；角色权重决定是否反应。生日指角色资料里的生日，不伪称用户生日。

角色与 30 分钟时间窗产生稳定抽样；重启或反复 tick 不重新抽签。成功联系记录 window/evidence 元数据，避免同一事实反复提醒。Provider 没有成功内容就不投递。用户限制和角色类型额度同时生效。

| 类型 | 实际投影 |
| --- | --- |
| TEXT | 私聊回复；周野可拆短意群，整次只计一个联系额度 |
| PHOTO | 私人照片事实 → Gallery，引用 `character_asset:<pack>/gallery/<trigger>`，尚无正式图像时仍是素材槽位 |
| CALL_INVITE | 询问是否方便的私聊邀请，不自动接通或拨号 |
| LETTER | 持久世界事实 → Mailbox；重启可恢复已投递信件和已读标记 |
| MOMENT | 公开生活动态，不传用户私聊/私人关系提示作为公开内容素材 |

计划排入、主动完成、自动日程执行共享配额。队列执行重新读取角色设置，并保护正在进行的通话，避免旧排程绕过当前限制。

## 关系证据

独立 `RomanceRecord` 持久化到 `romance_evidence_v1_json`；保留旧 NPC 关系、bond 和历史，不把它们换算成已经恋爱的事实。五维 familiarity / trust / attraction / intimacy / tension 均限定 0..1，仅内部使用。

有效来源：重要记忆、至少十分钟且三种非重复实质双向交流、用户主动且实际持续至少六十秒的通话、首次读信、完成共同剧情、双方说开冲突、在已互认后提及的实际纪念日。普通问候、刷屏、重生成与失败的助手回复不制造关系增长。

事件 ID 去重，重复内容指纹不给重复收益；同日同类收益按 1 / .25 / .05 / 0 衰减。迟到旧事件不会复活已解决冲突或覆盖较新的边界。双方明确互认才允许 ROMANTIC / COMMITTED，且仍须多维条件；非恋爱路线不会自动变成恋爱。

沈砚信任权重大、吸引慢；周野吸引和亲密增长较快、紧张波动较大；诺亚熟悉增长慢，信任建立后亲密增长明显。冲突重联冷静期分别为 12 / 4 / 24 小时，之后仍须通过正常主动触发和限额。用户主动发话时仍正常回应。

嫉妒需要明确 romance 路线、足够关系阶段、卡片倾向和具体行为、近期实际其他角色互动。不会向角色暴露另一段私聊内容，也不把互动等同于背叛。禁止盘问、辱骂、强迫、威胁和限制用户交友。

明确意愿识别采用保守的声明句和有界模式，排除假设、引用与歧义；不是通用情感分类器。用户已发送的明确非恋爱边界不依赖助手回复成功。不能识别的语境留给正常对话，不凭猜测升级关系。

本轮没有 ART、正式生图、音频生成或 P5.6。验证结果与交付物见根目录本轮实施报告。
