# AILUA P5.2 第一轮施工记录

日期：2026-09-30
分支：`local-pass3c-ai-runtime`
基线：`0803644e27211c02fc93bd5e6dcdf64750cfc13f`

## 本轮范围

按 P5.2 总纲最后的建议，本轮实现 P5.2-A Workspace Data Kernel 和 P5.2-B 单页 CellLayout/Drag。视觉素材未调整。

## 已完成

- 在现有 `ailua_chat.db` 中新增 `DesktopPage`、`DesktopItem` 和 SQLDelight v3→v4 migration；沿用进程内已有的 SQLDelight 连接。
- 增加 WorkspaceRepository、默认布局、旧 `HomeAppOrder` 首次迁移、页面与 item 位置 API，以及 `GridOccupancy` 校验。旧顺序键保留供回退。
- 首页 App 区使用数据驱动的单页 CellLayout，支持长按、拖动预览、目标格提示、周边图标让位、放下后单次事务保存，以及取消恢复。
- 普通模式按已用行收紧高度；编辑拖动时展开至六行。拖动期间暂停页面竖向滚动。

提交：`9a3cdfd`、`53129e8`；共享数据库连接修正见后续提交。

## 验证

- `:app:testDebugUnitTest :app:assembleDebug --offline` 通过：395 个测试、77 个测试套件、0 失败、0 错误。
- Android 10 小米 M2007J22C：保留应用数据安装；旧 v3 数据库升级后首页可打开；实际拖动图标到空格、强制停止并重开，位置保留；再拖回原格，位置恢复。
- 最终 APK 再次覆盖安装并启动成功；启动后 logcat 未发现 `FATAL EXCEPTION`。
- APK：`dist/AILUA-P5.2-AB-debug.apk`，SHA-256：`CE096DA1E32ECA16B9268F7DCD6942241DE090F25961120E02FE04899145B279`。

## 后续范围

跨页拖动、可编辑 Dock、Widget、Design System 和视觉素材属于 P5.2 后续阶段。现有第二页、固定 Dock 与原有功能继续运行。本轮真机检查覆盖单页拖动与重启持久化，尚未覆盖所有设备和手势边界。
