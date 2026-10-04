# AILUA P5.V 六人阵容首轮实施报告

日期：2026-10-05。基线：`local-pass3c-ai-runtime` 的 `6bd6e1df042e744fbbaa0b02d307e24d94e7bf29`。

## 身份和兼容性

| 用户看到的角色 | 持久化 characterId | 公开素材目录 | 官方默认关系 |
| --- | --- | --- | --- |
| 贺闻川 | `hewenchuan` | `hewenchuan` | 恋爱 |
| 周见野 | `zhoujianye` | `zhoujianye` | 恋爱 |
| 裴叙白 | `peixubai` | `peixubai` | 恋爱 |
| 苏晚宁 | `mira` | `suwanning` | 友情 |
| 许朝颜 | `yuna` | `xuchaoyan` | 友情 |
| 宋知微 | `noa` | `songzhiwei` | 友情 |

三位女生沿用原持久化 ID，没有新增同名第二身份，也没有迁移数据库外键。旧 `yan`、`yeo` 可按存档 ID 解析，但不会加入新用户默认阵容。旧故事、信件、照片和事件保留原 ID 和正文；有已保存引用时仍可阅读，不进入新用户默认内容池。旧聊天正文若本来包含旧姓名或旧 Noa 的设定，仍作为历史原文保留。新版人物名称只投影到界面作者标签，历史内容未改写。

## 人物与视觉资产

- 六张完整 Character Card V2，标准字段与 `extensions.ailua` 的 `identity / relationship / behavior / speech / initiative / life / visual` 七组完整。每张卡有八种情境对话示例。男性与女性的职业、生活节奏、说话方式、主动频率及关系边界分别设定。
- 六张正式主头像与六张同一人物的表情变体。原图实际为 1254×1254 PNG；统一规范化为 512×512 RGB WebP，并保留 1024×1024 PNG 备份。正式 WebP 位于 `app/src/main/assets/characters/<public-slug>/`。
- 已逐张审阅主图、变体、48/64/96 像素圆裁板。六人发型、脸型、服装、主色、场景和神情可区分；变体保持同一人。审阅板：`docs/design/six-character-assets/six-main-review.png`、`six-alt-review.png`、`six-circle-native-review.png`。最终 UI 是否合乎用户审美仍以实机截图和用户反馈为准。
- 来源为用户指定 `https://www.vibework.live/v1` 的 `gpt-image-2`；仅用 imagegen 技能内置 CLI 调用。Prompt、实际原图尺寸、编辑参考 SHA、原图与成品 SHA 在 `docs/design/SIX_CHARACTER_AVATAR_ASSET_MANIFEST.json`，12 项视觉整合状态为 production。服务输出的商用条款未独立核验，记录为 `unverified-provider-output-terms`，production 不代表商用权利已认证。
- 可独立审阅的 33 项压缩包：`dist/AILUA-six-character-avatars.zip`，包含 12 WebP、12 PNG 备份、六张卡及资产文档。

## 接入范围

沿用现有 Character Registry、Runtime、Workspace、数据库 schema 和 Theme 链。`CharacterAssetResolver` 仅解析官方稳定 ID 到公开素材目录，用户导入的本地图优先；`CharacterPortrait` 统一解码和圆裁，失败时才使用 fallback。联系人、会话列表、聊天顶部、详情页、Home、来电、Moments、群聊成员与相关通知入口均改用新头像或公开名字。新手页同时列出三位“心动对象”与三位“我的朋友”，保留用户自定义角色入口。旧恋爱故事归档，新默认故事为六人街区开场。

## 验证

- 离线 `:app:testDebugUnitTest`：**637/637 通过，118 套件、零跳过**。`:app:assembleDebug` 与 `:app:assembleDebugAndroidTest` 成功。
- `dist/AILUA-P5.V-SixCharacters-debug.apk` SHA-256：`793A7949F2929BB596F1AC159CECE7A54B624B41646103D47D918D744EBB644E`。
- 小米 M2007J22C / Android 10 / API 29 已使用 `adb install -r` 覆盖安装。安装前后原用户数据库 schema、3 页/18 图标/1 文件夹、Widget、原聊天/变体/记忆、受保护配置共 15 组检查完全一致。
- 最终六人真机页面巡检与截图：**待完成**。第一次脚本在 Android 系统锁屏期间没有进入测试方法，七分钟后超时；零张页面截图，未声称通过。原始数据复核通过。设备当前需要用户解锁，解锁后继续真实页面检查。
- 一次完整离线测试遇到旧 `ImportedPixelIconTest` 的 Windows 临时目录 rename 偶发失败；该测试单独重跑通过，随后完整 637/637 重跑通过。未借此修改 Theme 逻辑。

本轮未开始 P5.6、长篇剧情、CG 或全身立绘。真机截图目录：`dist/qa/P5.V-SixCharacters/device/`；该目录中的 `private/` 含旧存档快照，仅留本机，不提交。
