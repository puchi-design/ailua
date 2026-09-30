# AILUA P5.2-CD 施工与真机验收

日期：2026-09-30

基线：`local-pass3c-ai-runtime` 的远端 `a2a5f451168604dc9f9192fc7da478fe3ad5aef5`，开工前已用 `git ls-remote` 核对。AB 三个提交未改写。

## 完成范围

- 沿用 AB 的 `DesktopPage` / `DesktopItem` SQLDelight 表和数据库连接。原表已具备容器、页、cell、span、类型字段；新增 v4→v5 数据迁移，将旧 `DOCK` 行更新为 `HOTSEAT`。未创建第二套桌面数据库。
- Pager 的普通页数量来自 Workspace 数据；Life Bento 保留为最后一个特殊页，不进入 Workspace 数据库，也不是 Drop Target。
- 拖动状态、图标 overlay 和坐标命中提升到首页顶层。拖动时关闭 Pager 普通滑动；边缘连续停留 450ms 才翻页。末页边缘可生成临时空页；取消时不入库，真正落下时与图标位置一起事务提交。
- 同一 `DropResolver` 处理页内让位、跨页、Hotseat 重排及 Workspace ↔ Hotseat。Hotseat 五个 slot 来自 `DesktopItem(HOTSEAT)`，可留空；桌面项拖入满槽会拒绝，不挤走原项。
- 事务提交后回收尾部空普通页，始终保留至少一个普通桌面页。Life Bento 仅机械拆到 `special/LifeBentoPage.kt`，UI 内容未重写。

## 自动验证

- `:app:testDebugUnitTest :app:assembleDebug --offline` 通过：403 个测试、78 个测试套件、0 失败、0 错误。
- 新增覆盖：SQLDelight v4→v5 数据迁移、跨页移动与重启读取、边缘页选择、自动建页、尾部空页回收、取消预览不写库、碰撞让位、Hotseat 移出与空槽移入、满槽拒绝、Hotseat 重排。

## Android 10 真机

机型：小米 M2007J22C。测试前备份应用数据库；覆盖安装保留数据。拖动结束后强制停止并读取设备数据库核对，而不只依赖截图。

- Hotseat“消息”移出到桌面空格，重启后保持；从桌面拖回空 Hotseat slot，重启后保持。
- Hotseat 0/2 号位交换后重启，画面和数据库一致；测试完恢复默认顺序。
- 拖动中按 Back、在非法区域抬手：页面与图标位置未写入变化。
- 长按“相册”→右边缘停留→自动建第二普通页→继续拖动落下；强制停止并重启，第二页画面与数据库一致。Life Bento 仍可作为第三个特殊页打开。
- 连续右→左→右跨页后放下：设备数据库仍为 13 个唯一图标，无重复或丢失。再将“相册”拖回第一页，空尾页回收。测试完恢复为原来的单页、默认 Hotseat。
- 最终 APK 再次覆盖安装并启动，logcat 未发现 `FATAL EXCEPTION`。

连续多段手势使用设备自带 UiAutomator 执行；设备拒绝安装测试 APK，因此未将设备专用坐标脚本放入正式应用源码。

## 交付

- `dist/AILUA-P5.2-CD-debug.apk`，SHA-256：`61F4EA8D90324A4473AE1A3F230B245DD4837E4CC63811B0E0AF1A0F79BC8FAC`
- 真机截图：`dist/qa/P5.2-CD-second-page.png`、`dist/qa/P5.2-CD-Life-Bento.png`

Widget、Folder、Theme V2 和视觉素材不属于本轮范围。
