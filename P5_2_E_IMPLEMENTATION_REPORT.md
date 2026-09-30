# AILUA P5.2-E 完成

- Widget 与 App 共用 `DesktopItem`、Workspace 格网、拖动状态和 SQLDelight 数据库；World Clock、Character Living、Memory Echo、Bond 均可通过组件选择器添加。
- 组件类型使用 `sourceId`，每次添加生成独立实例 ID。支持 Registry 声明的离散尺寸、右下角拖拽或点击调整尺寸、预览合法性、删除、跨页拖动。
- v5→v6 迁移仅增加同库的一次性种入标记。首次打开将 World Clock 4×1 和 Character Living 4×2 放到首页，原有 App 在剩余格子重排；用户删除后不会再次种入。
- Life Bento、Hotseat、聊天、世界和关系数据没有改写。

## 验证

- `:app:testDebugUnitTest :app:assembleDebug --offline`：407 tests，79 suites，0 failures/errors。
- Android 10 小米 M2007J22C：默认组件显示；选择器添加第二个时钟；拖动与点击 resize；小弥 4×2 跨页、重启后仍在第二页、拖回后空页回收；删除测试组件；数据库位置与画面一致，图标 ID 无重复。
- 真机测试后恢复为单个普通桌面页、默认两个组件和原 13 个 App；AndroidRuntime 未见 FATAL EXCEPTION。

交付 APK：`dist/AILUA-P5.2-E-debug.apk`，SHA-256：`76193F846AB99FC5593BDB9573CB714DDD2A29E6FB7B6876DBF7C219A71154A5`。
真机画面：`dist/qa/P5.2-E-home.png`、`dist/qa/P5.2-E-second-page.png`。
