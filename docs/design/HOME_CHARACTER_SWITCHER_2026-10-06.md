# Home 角色切换与 MandAPI 真机验收（2026-10-06）

## 交付

- Home 顶部固定展示当前角色头像、名字及切换箭头；即使用户从 Workspace 移除角色 Widget，入口仍在首屏。角色 Widget 与 LifeBento 的主头像、名字也可打开同一选择 Sheet。
- Sheet 以 `CharacterRegistry.getOfficialRomanceCharacters()` 和 `getOfficialFriendshipCharacters()` 分组展示六人正式头像、中文名、年龄、当前状态及选中勾选。其他角色收在“更多角色”。
- 选择仅调用 `CharacterContext.select(id)` 并收起 Sheet。Home 及其他角色页面沿用现有 `selectedId` 响应链，没有新活动角色状态源，也没有改写聊天、记忆、LifeEvent 或持久角色 ID。
- 新安装的默认角色仍是 `hewenchuan`；已有选择和旧存档继续保留，例如实机原选择 `mira`（苏晚宁）。
- AI 连接测试的等待上限由 20 秒增至 45 秒，以适应真实兼容接口首次响应。正式聊天传输逻辑未变。

## 验证

- `:app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest`：离线构建成功；单元测试 **708/708**，无失败或跳过。构建时将 `GRADLE_USER_HOME`、`TEMP`、`TMP`、`java.io.tmpdir` 指向 H 盘；C 盘当时剩余空间为 0，原构建因缓存写入失败。
- Android 10 / 小米 M2007J22C：`HomeCharacterSwitcherDeviceTest` **1/1**；打开首屏入口、选择另一位正式角色、验证选择持久化，最后恢复原 `mira`。本轮覆盖安装保留应用数据。
- 连接服务端：实查 `https://api.mandapi.com/v1/models` 确认模型 ID `deepseek-v4.1-flash`；桌面直接请求及 SSE 流请求成功；真机 HTTPS 到达、应用传输完成。真机 `MandApiProviderSetupTest#connectAndStoreApprovedMandApiProvider` 在新版 45 秒连接测试下 **1/1**，耗时 9.512 秒；已保存并启用 `mandapi_deepseek_v41_flash` 配置。
- API Key 仅保存在本机 Git 忽略的 DPAPI 文件和真机应用加密存储。仪表测试从应用私有一次性文件读取 Key，测试后该文件已删除；源码、测试包和报告均不含 Key。
- 最终 APK：`dist/AILUA-Home-Character-Switcher-debug.apk`；SHA-256 `F0F2F8468C9B1B515E8C6CED5A56D9EE7E34136093D17AE033108140E7FA4068`，与真机安装包相同。真机截图在 `dist/qa/Home-Character-Switcher/`。测试时修改的系统自动旋转设置已恢复为 `1`。

## 范围

真机已验证配置连接及保存；本轮没有通过聊天界面发送一条正式消息。API 31+ 的 Glass 真 Haze 仍无真机验证。
