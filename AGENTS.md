# AILUA 项目记忆

## 当前施工与冻结基线

- 分支 `local-pass3c-ai-runtime`。UIR-0→2 首轮基线为 `161fa130f683d0c85368971e384583d410731aec`。
- UIR-3A→3D 已完成：三套正式图标候选、Y2K 图标原型、同一 Workspace 真机对照。新任务书：[P5V_UIR3D_6_TASKBOOK.md](docs/design/P5V_UIR3D_6_TASKBOOK.md) 已授权继续 UIR-3D→6；基线为已同步远端的 `9ee6887eb77fd3b2f709c6e60eb7e16a72461e99`。
- 用户后续明确要求“持续推进解除无用门禁”：取消将生图服务商用授权证明作为 UI 施工前置条件，继续 UIR-3D→6。资产仍记录指定服务、Prompt、原图/资源哈希及 `unverified-provider-output-terms`；`production` 表示视觉整合状态，不能解释为商用权利已认证。此前停止状态已撤销，见 `docs/design/P5_V_UI_PRODUCTIZATION_REPORT.md`。
- 该基线的 Home 视觉骨架、Widget 位置、Character Widget 信息结构、Workspace 网格/Pager/Drag/Folder/Dock 数据结构、Character/World/Chat/Memory/Call/关系 Runtime 和数据库 schema 冻结。
- 2026-10-04 用户拒绝 Soft/Rain 的书桌、雨窗 AI 摆拍方向，随后也拒绝摄影海岸；最新明确要求传统油画壁纸多张。不要恢复这些已拒绝壁纸。新候选为玫瑰花丛、湖畔花田、粉色花枝、林间小路、睡莲、花园午后；不生成正式角色脸。
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
- 系统 C 盘临时空间不足时，Robolectric 可在创建 userDataDir/native library 解压时失败。2026-10-04 已用本机忽略文件 `.tools/qa-tmp/test-temp.init.gradle` 给 Test 设置 H 盘 `java.io.tmpdir`，不删测试、不清用户文件；620/620 与 Debug/androidTest 构建通过。以后先检查实际错误原因，再复用该本机 init 脚本。

## 生图实验与复用

- 用户已明确允许用 CLI/API 生产图标和壁纸。当前会话没有内置 image_gen，使用 imagegen 技能自带 `C:/Users/fan/.codex/skills/.system/imagegen/scripts/image_gen.py`；不要修改该脚本或另写 SDK 生图 runner。
- 独立 Python：`.tools/imagegen-venv/Scripts/python.exe`，已装 openai 与 Pillow。
- 验证过的用户指定服务：`https://www.vibework.live/v1`；模型名 `gpt-image-2`。通过 `OPENAI_BASE_URL` 指向该服务，不能误将其密钥发送到默认 OpenAI endpoint。
- 本机复用凭证在 `.tools/imagegen/credential.dpapi`，Windows 本机用户 DPAPI 加密，`.tools/` 已被 Git 忽略。使用本机用户上下文解密；权限配置可能切换，应服从会话当前 permissions，managed 沙箱的 DPAPI 身份不兼容时使用本机用户执行。仅用 ConvertTo-SecureString 解密并设置子进程环境；不输出密钥、不写入明文文档/日志/Git，结束清理进程环境。
- 首次实测约 32.4 秒；请求 1024×2208、medium，实际返回 853×1844。输出 `output/imagegen/soft-home-trial-v1.png`、对应 `.prompt.txt` / `.metadata.json`。图片已真实打开查看，不是程序插画。
- 服务不一定遵从请求像素尺寸；始终检查真实 dimensions、格式与 alpha，保留原图，记录规范化方法和 hash。
- 每个独立图标单独 prompt，使用 bundled CLI generate-batch；生成后看原图与拼图并做真机检查。来源写明 AI生成/指定服务，未知商用条款不能声称已验证。
- 本轮可生产 UI 图标与背景，不开始正式男主脸/角色 ART。
- 72 个独立图标原图在 `output/imagegen/uir3/raw/`（本机保留，Git 忽略）；正式候选位于 `app/src/main/res/drawable-nodpi/ti_*.webp`。实际均返回 1254×1254，按比例规范化为 256×256 RGB lossless WebP；Y2K 先 nearest 到 32px 再 nearest 到 256px，不加透明玻璃底。
- Prompt 已版本化：`docs/design/UIR3_ICON_PROMPTS.jsonl`；资产/原图/prompt 哈希和来源在 `docs/design/UIR3_ICON_ASSET_MANIFEST.json`。CLI 网络生成仍只调用技能 bundled 脚本；`scripts/prepare_production_icons.py` 仅做 prompts/audit/normalize/board/manifest，不自行调 API。
- 恢复中断批次先运行 `prepare_production_icons.py audit`，只补有效 PNG 清单缺项；不以文件存在判断成功（请求过程中可能出现 1 字节占位）。

## UIR-3D → 6 最终施工记录（2026-10-04）

- UIR-3D/E+4 资产 checkpoint `19524d1`、UI5 Center/Lab checkpoint `9dac45b`、UI6 真机与命中矩形修复 checkpoint `3ca945d` 已推送原分支。正式清单 `docs/design/THEME_ASSET_MANIFEST.json` v3 为 120 项：108 图标、6 主题壁纸、6 油画图库壁纸，120 production / 0 pending。
- 用户否决的书桌、雨窗、摄影海岸均未进入正式资产；Soft 使用玫瑰油画，Rain 使用湖畔花田。六油画原图实际 941×1672（睡莲 940×1672），等比中央裁剪为 1080×2340 lossless RGB WebP，原图和未采用历史本机保留。消费者油画入口置于壁纸页首屏；正式男主 ART 未开始。
- 离线最终 622/622，Debug/androidTest build 成功。Android 10 的六主题/108 bitmap、四入口/预览/应用、硬件返回、9种实际导出回导、Widget resize、长按拖动恢复、系统面板、Chat 输入（未发送）与 Living 回归通过。完整证据在 `dist/qa/P5.V-UIProduct/`。
- Workspace 的完整 CellLayout 与被裁剪的命中 bounds 曾不一致，现仅修 UI 上报完整布局矩形，并用已有 Pager 可见 bounds 限制命中；数据模型、DropResolver/FolderDropResolver 和拖拽解析规则不变。
- AILUA schema1 导出保留 basePreset；可选 iconStyle/wallpaperStyle 通过已有 metadata 传递，UI 校验 catalog 后恢复已有选择字段。稀疏/向量图标允许导出，Y2K 回导按实际来源 nearest；跨亮暗壁纸混搭保留文字/状态栏/锁屏前景。
- Soft/Midnight 均经真实进程强停、重启验证主题和外部图标持久化，并恢复用户原设置；schema/Workspace/Widget/Chat/Memory/角色/受保护 prefs 完整15项对照一致。临时 QA 备份在正常结束及失败恢复时清理。
- 真机性能必须唤醒手机、确认 AILUA 前台并解锁虚拟锁屏后再量；零帧样本无效。非 instrumentation 的6次350ms页面手势：Soft282帧/11 jank（3.90%），Midnight279/13（4.66%），只作 Android10 Debug 观察。API31+ Haze仍无真机，不能宣称验证。
- 图片异步解码后再截预览；可点击父级会合并 Image 的语义标签，QA加载检查需 `useUnmergedTree=true`。最后07/08补截只更新真实加载后的图片，没有修图。
- `dist/AILUA-P5.V-UIProduct-debug.apk` 与手机已装 APK SHA 完全相同；`dist/AILUA-oil-wallpapers.zip` 包含6张1080×2340 PNG。最终报告与机器摘要位于 `docs/design/`；最终提交/远端HEAD在报告提交后记录于忽略目录 `dist/qa/P5.V-UIProduct/delivery-receipt.json`。

## 本机图片工具硬规则

含 view_image 的工具轮只能调用一个 view_image，不能同时调用 shell、读文件、apply_patch 或第二个 view_image。先生成/截图，下一独立轮再看图。否则本机代理可能永久损坏会话。
