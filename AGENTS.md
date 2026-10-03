# AILUA 项目记忆

## 当前施工与冻结基线

- 分支 `local-pass3c-ai-runtime`。UIR-0→2 首轮基线为 `161fa130f683d0c85368971e384583d410731aec`。
- UIR-3A→3D 已完成：三套正式图标候选、Y2K 图标原型、同一 Workspace 真机对照。新任务书：[P5V_UIR3D_6_TASKBOOK.md](docs/design/P5V_UIR3D_6_TASKBOOK.md) 已授权继续 UIR-3D→6；基线为已同步远端的 `9ee6887eb77fd3b2f709c6e60eb7e16a72461e99`。
- 新任务书第43节规定资源合法来源无法确认时停止。当前停在开工商用授权核查：服务公开条款将输出权利关联到上游条款，仍缺该服务图片商用授权证据；已向用户请求条款/书面授权或其已有授权来源。不要擅自将候选资产改标production。状态与恢复范围见 `docs/design/P5_V_UI_PRODUCTIZATION_REPORT.md`。
- 该基线的 Home 视觉骨架、Widget 位置、Character Widget 信息结构、Workspace 网格/Pager/Drag/Folder/Dock 数据结构、Character/World/Chat/Memory/Call/关系 Runtime 和数据库 schema 冻结。
- 继续原 ThemeStore→ThemeSelection→ThemeResolver→AiluaThemeRuntime 与 IconResolver；图标资产通过既有 iconStyleId 选择，不创建新 Launcher/Theme/Icon Engine。
- 用户 manual / 外部图标源优先，切换主题不能清用户覆盖。`ThemeCatalog.DEFAULT_ID=milk` 与四个 legacy theme 保留。

## 真机与证据

- ADB：`D:/mandapi-chat-v1/android-sdk/platform-tools/adb.exe`。已有小米 `pnhykzpzzxq8lbs8` / M2007J22C / Android 10 API 29。每轮查询设备，不假定一直在线。
- 首轮证据在 `dist/qa/P5.V-UI-Theme/`；587 离线测试通过，Folder/Widget resize/覆盖安装与重启后的布局和已有聊天行已核对。
- API 29 的 Glass 是 tint + highlight fallback。API 31+ 真 Haze 尚未实测，不能宣称 Glass Production Verified。
- 用户反馈的玻璃图标白圈、透明层厚灰框、矩形孔已返修。Glass 不加 native elevation；背板和前景分为 sibling。
- 原 `.idea/` 与 `gradle/gradle-daemon-jvm.properties` 是本机既有未跟踪文件，不提交或还原。
- UIR-3 真机证据：`dist/qa/P5.V-UIR3/device/`；最终离线 605/605、Android 10 实机 72 bitmap 解码/透明度检查与四套生产 UI 切换/Folder 截图通过。覆盖安装和冷启动后 3 页/18 item/1 Folder、Widget 尺寸、设置及聊天/记忆行保持一致。
- 官方 bitmap 由实际解析来源携带 mask：前三套 squircle，Y2K NONE + nearest；manual/外部图保留原形。缺失 Y2K 回退 Default 时使用 Default 的 mask。Home 与预览共用 shape 函数。
- ADB 首次启动必须使用本机用户身份，沙箱另起 daemon 会导致密钥身份不匹配和 unauthorized；遇到这种情况以本机用户重启 ADB 服务。测试前显式打开 AILUA，保持前台，避免 MIUI/系统通知面板让 ActivityScenario 挂起。

## 生图实验与复用

- 用户已明确允许用 CLI/API 生产图标和壁纸。当前会话没有内置 image_gen，使用 imagegen 技能自带 `C:/Users/fan/.codex/skills/.system/imagegen/scripts/image_gen.py`；不要修改该脚本或另写 SDK 生图 runner。
- 独立 Python：`.tools/imagegen-venv/Scripts/python.exe`，已装 openai 与 Pillow。
- 验证过的用户指定服务：`https://www.vibework.live/v1`；模型名 `gpt-image-2`。通过 `OPENAI_BASE_URL` 指向该服务，不能误将其密钥发送到默认 OpenAI endpoint。
- 本机复用凭证在 `.tools/imagegen/credential.dpapi`，Windows 本机用户 DPAPI 加密，`.tools/` 已被 Git 忽略。加密和解密均使用本机用户（网络调用的 require_escalated 上下文），沙箱用户的 DPAPI 上下文不兼容。仅用 ConvertTo-SecureString 解密并设置子进程环境；不输出密钥、不写入明文文档/日志/Git，结束清理进程环境。
- 首次实测约 32.4 秒；请求 1024×2208、medium，实际返回 853×1844。输出 `output/imagegen/soft-home-trial-v1.png`、对应 `.prompt.txt` / `.metadata.json`。图片已真实打开查看，不是程序插画。
- 服务不一定遵从请求像素尺寸；始终检查真实 dimensions、格式与 alpha，保留原图，记录规范化方法和 hash。
- 每个独立图标单独 prompt，使用 bundled CLI generate-batch；生成后看原图与拼图并做真机检查。来源写明 AI生成/指定服务，未知商用条款不能声称已验证。
- 本轮可生产 UI 图标与背景，不开始正式男主脸/角色 ART。
- 72 个独立图标原图在 `output/imagegen/uir3/raw/`（本机保留，Git 忽略）；正式候选位于 `app/src/main/res/drawable-nodpi/ti_*.webp`。实际均返回 1254×1254，按比例规范化为 256×256 RGB lossless WebP；Y2K 先 nearest 到 32px 再 nearest 到 256px，不加透明玻璃底。
- Prompt 已版本化：`docs/design/UIR3_ICON_PROMPTS.jsonl`；资产/原图/prompt 哈希和来源在 `docs/design/UIR3_ICON_ASSET_MANIFEST.json`。CLI 网络生成仍只调用技能 bundled 脚本；`scripts/prepare_production_icons.py` 仅做 prompts/audit/normalize/board/manifest，不自行调 API。
- 恢复中断批次先运行 `prepare_production_icons.py audit`，只补有效 PNG 清单缺项；不以文件存在判断成功（请求过程中可能出现 1 字节占位）。

## 本机图片工具硬规则

含 view_image 的工具轮只能调用一个 view_image，不能同时调用 shell、读文件、apply_patch 或第二个 view_image。先生成/截图，下一独立轮再看图。否则本机代理可能永久损坏会话。
