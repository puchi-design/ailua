# P5.V-CHAR A–D 实施与真机报告

日期：2026-10-03。分支：`local-pass3c-ai-runtime`。基线：`942e696f09ed6634985569563ab83f9417e545b5`。

## 交付范围

本轮完成角色卡兼容、AILUA 行为扩展、三位官方男性角色和角色工坊/欢迎页。此前 UI0–UI6 已完成并推送；本轮沿用其公共组件和全局 Theme Runtime。未制作正式角色图片，未开始 P5.6。

| 模块 | 实施结果 | 主要证据 |
| --- | --- | --- |
| CHAR-A | `extensions` 改为 arbitrary JSON object；标准 V2 字段保留；标准/旧内部 lorebook 桥接；未知第三方扩展与数值字面量保真 | `CharacterCardCompatibilityTest`、`LosslessJsonSerializers`、`CharacterBookSerializers` |
| CHAR-B | `data.extensions.ailua` schema 1，identity / relationship / behavior / speech / initiative / life / visual 七组；未来 schema 保留；普通编辑仅更新改变的字段 | `AiluaCharacterExtensionCodecTest`、`CreatorDraftTest`、协议文档 |
| 运行时接入 | Prompt 加载人物行为及私有 lore；主动消息读取当前角色并遵守用户频率上限/睡眠；世界计划使用人物作息、生活偏好及电话/照片/动态配额 | `CharacterBehaviorRuntimeTest`、`CharacterLoreResolverTest`、`ProactiveMessageTest`、`WorldPlanTest` |
| CHAR-C | 沈砚 26 岁：克制年上旧书修复师；周野 22 岁：直球摄影师；诺亚 28 岁：清冷慢热档案学者。各有欲望、缺点、软肋、表达/嫉妒/冲突方式、语气、生活和世界联系 | `OfficialCharacters`、三份导出卡、真机资料截图 |
| CHAR-D | 工坊普通/高级两模式；完整 V2/JSON/SAF/复制/保存；照片持久读取权限；Activity 重建保存草稿和待导出快照；欢迎页优先三位男主并保留其他角色和导入入口 | `CreatorDraftTest`、`CharacterSelectionPolicyTest`、真机 smoke |

协议细节见 [AILUA_CHARACTER_EXTENSION_V1.md](docs/AILUA_CHARACTER_EXTENSION_V1.md)。兼容字段依据 [Character Card V2 原始规范](https://github.com/malfoyslastname/character-card-spec-v2/blob/main/spec_v2.md)；保真指 JSON 值、类型及数字字面量，输出缩进不承诺与输入逐字节相同。未建模的 V2 根级额外字段不属于扩展保真范围。

## 旧数据与角色选择

- 新安装默认 `yan`；已有 `selected_id` 原样保留；没有该偏好但有旧数据库/旧存储的升级用户沿用 `mira`。
- `mira`、`yuna` 仍可选择。`noa` 保留 ID；原聊天、记忆、旧世界事实和已保存角色覆盖不重写。
- 查看资料和聊天不自动改当前主角色。资料页“在主屏显示”、欢迎页确认及工坊“保存并预览”才显式切换。
- 自定义/导入角色允许任意性别和关系类型。三位男主使用中性姓名占位视觉；本地照片 URI 可替换，未借用小弥女性头像作为男性占位。

## 本轮发现并修复

1. kotlinx JSON 默认编码会把超大整数转为 Double、收缩小数字面量。扩展、lorebook、直接 serializer、本地卡列表及高级 JSON 预览统一使用保真编码，测试包括 `123456789012345678901234567890`、`0.0100`、`-0`、`1e400`。
2. 容错读取后的普通字段编辑会覆盖未理解的原始已知字段。现在只回写实际修改的叶子，保留未编辑的原始 JSON。
3. 工坊旋转/SAF 重建会丢草稿或待导出卡。加入可恢复状态；保留未完成 JSON 文本，导入校验从恢复卡重新计算。
4. 官方阵容排序变化会让离线续排只选三位男主。候选现在优先当前角色，再分配给事件较少的其他角色，Mira/Yuna 继续有生活事件。
5. 新日记种子原时间晚于默认世界钟。调整为已发生时点，并加默认时钟断言。
6. 官方初始日程仅在内存中，首次自动续排可能覆盖。现在先保存带日期的初始计划；已有计划仍优先，错过的时点不重放。

另修正两项旧测试的种子假设：日记断言按作者过滤；最新事件测试使用隔离角色并逆序插入，实际验证时间排序。

## 构建与离线测试

最终命令：

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug assembleDebugAndroidTest --offline --no-configuration-cache --no-daemon --max-workers=1
```

临时目录使用 `dist/qa/P5.V-CHAR/temp`。结果：**517/517 PASS，103 suites，0 failure/error/skip**；Debug 与 instrumentation APK 构建成功。

证据：`dist/qa/P5.V-CHAR/build.log`、`unit-results.json`。首轮失败日志留在 `build-first-run.log`；修复中间版本日志另存，未覆盖最终结果。

## 真机验证

设备：**小米 M2007J22C，Android 10 / API 29**，1080×2340。ADB：`D:\mandapi-chat-v1\android-sdk\platform-tools\adb.exe`。

主 APK 使用 `install -r` 覆盖安装，未清数据。最终 APK 运行 `P5VCharacterSmokeTest`：**1/1 PASS**，真实 MainActivity 路由。MIUI 的 ActivityScenario 启动由宿主脚本使用相同 MAIN/LAUNCHER Intent 协助。

| 检查 | 结果 |
| --- | --- |
| 旧版本没有 selected_id、已有聊天数据库的真实升级 | PASS，启动仍为 Mira，首次写入兼容选择 |
| 工坊普通/高级模式 | PASS，普通模式隐藏技术字段，高级模式保留 V2/PHI/JSON |
| 模板未保存修改、Activity 重建及模式切换 | PASS，名称和高级模式保留，原卡未写入 |
| JSON 预览 | PASS，显示修改后的草稿 |
| 三位官方男主资料页 | PASS，从联系人真实入口打开，查看未改变主角色 |
| 资料选择 Yan → Home | PASS，Home 消费新选择；测试 finally 恢复原 Mira |
| Android 10 照片 SAF | PASS，实际打开 DocumentsUI 并取消返回；未读取个人照片 |
| Android 10 V2 文件导出 | PASS，实际导出 Yan 卡，14,846 bytes；解析结果与完整官方卡相等 |
| Android 10 V2 文件读取 | PASS，经系统文件选择器读回导出文件，显示导入确认和开场白；取消确认，未覆盖官方卡 |
| 覆盖安装/强制停止/重启后的布局 | PASS，3 页、18 项、1 文件夹完整行数据相等，含 Widget span、Hotseat rank |
| 重启后的角色与外观 | PASS，Mira 与 Milk + Cream、图标缩放/标签等设置保留 |
| Logcat | 本次应用 UID 日志未检出 FATAL、ANR、资源缺失、权限、文件、序列化或 SQLite 异常 |

证据：`instrumentation.log`、`character-smoke-events.txt`、`verification-summary.json`、`device-logcat.txt`、前后 workspace/preferences JSON。数据库快照和设备日志仅留本机 QA 目录。

新安装 Yan 分支由选择策略单测覆盖；本机未清除旧存档模拟新安装。第三方嵌套扩展的导入/编辑/保存/导出保真由离线回归覆盖。没有调用真实 AI Provider 做三位角色长对话评测，没有生成正式立绘或音频。

## 交付物

- APK：`dist/AILUA-P5.V-CHAR-debug.apk`
- SHA-256：`6d81cc9e2885e70d7b82bd1091b0e27d1210d041ed07e6e7ddffb42f0b44472d`
- 截图目录：`dist/qa/P5.V-CHAR/`；已查看总览 `character-review.jpg`。
- 截图：`01-creator-simple.png`、`02-creator-advanced.png`、`03-creator-json-preview.png`、`04-profile-yan.png`、`05-profile-yeo.png`、`06-profile-noa.png`、`07-home-yan.png`、`08-photo-saf-picker.png`、`09-saf-import-preview.png`。
- 官方卡：同目录 `official-yan.json`、`official-yeo.json`、`official-noa.json`。
- 手机 QA 导出：`Download/AILUA-QA/qa-character-yan-20261003.json`；仅测试导出文件，未写入新用户角色。

`07-home-yan.png` 保留用户原本的默认页布局，因此不是铺满 Widget 的演示桌面。本轮没有改 Workspace 拖拽/Folder/Resize、Chat streaming/Memory 存储或 Call engine。此前 UI5/6 已完成，无需重复批量迁移；停在本轮交付，等待下一份明确任务书。
