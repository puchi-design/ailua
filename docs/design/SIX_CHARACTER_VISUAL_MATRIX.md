# 六人官方头像视觉矩阵

本轮只生产成人角色主头像与一张同身份表情变体，不制作完整立绘、大量 CG 或 P5.6。统一高质感乙游／轻写实二次元绘画，脸部清晰，六人分别生成，不用 SVG 或程序图形替代正式头像。

## 名称与持久身份

| 中文名 | Public asset slug | 持久 characterId | 默认路线 | 年龄 |
| --- | --- | --- | --- | --- |
| 贺闻川 | hewenchuan | hewenchuan | romance | 28 |
| 周见野 | zhoujianye | zhoujianye | romance | 23 |
| 裴叙白 | peixubai | peixubai | romance | 27 |
| 苏晚宁 | suwanning | mira | friendship | 25 |
| 许朝颜 | xuchaoyan | yuna | friendship | 24 |
| 宋知微 | songzhiwei | noa | friendship | 26 |

女生素材目录使用新的 public slug；旧存档持久 ID 保持 mira／yuna／noa，不新增对应女性身份。路线仅为官方默认值，不限制用户自定义关系。

## 六项强制区分

| 人物 | 发型／发色 | 脸型与眉眼 | 服装 | 主色调 | 场景／时间 | 表情与镜头 |
| --- | --- | --- | --- | --- | --- | --- |
| 贺闻川 | 深咖啡棕、整洁左侧分、额头露出 | 成熟宽颌、直眉、微内收深棕杏眼 | 炭灰剪裁大衣、暖褐衬衫 | 暖棕／灰／米白 | 木质建筑工作室、黄昏 | 正面平视、稳、闭唇、目光温和 |
| 周见野 | 栗棕铜色、短凌乱右扫刘海 | 年轻菱形脸、眉锋活跃、上挑琥珀眼 | 黑机能夹克、橙色帽里、炭灰 T 恤 | 橙／黑／银 | livehouse 门外、夕阳 | 轻微低机位、向镜头靠近、露少许牙的笑 |
| 裴叙白 | 灰棕、偏长左落刘海、耳际稍长 | 瘦长椭圆脸、细直眉、窄灰棕眼 | 石板蓝细针织半高领、颈部工作耳机 | 灰蓝／深蓝／冷银 | 录音窗边、蓝夜 | 较远胸像、身体轻转、眼神落镜头旁、冷静 |
| 苏晚宁 | 奶茶棕、长发微卷、略偏左分 | 柔圆椭圆脸、软眉、微下垂榛棕眼 | 浅灰紫开衫、燕麦色内搭 | 奶白／浅紫／米杏 | 家中窗边、晨光、茶与花 | 微倾头、安静闭唇轻笑、柔和正视 |
| 许朝颜 | 暖栗红棕、高马尾、活泼侧刘海 | 短心形成人脸、眉扬、圆杏眼 | 珊瑚橘轻夹克、奶白内搭、边缘青蓝相机带 | 珊瑚／奶橘／青蓝 | 咖啡街、上午阳光 | 如朋友抓拍、明显露齿笑、轻微侧靠 |
| 宋知微 | 冷深棕、利落齐下颌至肩短直发、右分露耳 | 略长有棱角椭圆脸、直细眉、窄橄榄棕眼 | 墨绿结构领外套、深灰衬衫 | 墨绿／深灰／雾蓝 | 书店、晚间阅读灯 | 平视宽肩胸像、克制审视、微妙嘴角 |

配饰集中于周见野的一枚小耳钉、裴叙白的工作耳机；不让所有人同时戴项链／耳钉。皮肤有自然暖冷变化，不采用六人统一瓷白模板。镜头均满足圆裁安全，但距离、肩向和神情分别设计；不重复同一三分之四帅脸。

## 主头像生产

- 六条独立英文 Prompt：`docs/design/SIX_CHARACTER_AVATAR_PROMPTS.jsonl`。
- 模型：用户指定服务 `https://www.vibework.live/v1` 的 `gpt-image-2`；请求 `1024x1024`、`high`、PNG。
- 只调用 imagegen 技能 bundled CLI。当前 bundled batch CLI 的 no-augment 为全局参数，执行 `generate-batch` 必须传 `--no-augment`；JSONL 的 `no_augment=true` 是审核意图，不能认为它单独改变脚本行为。
- 原图：`output/imagegen/six-characters/raw/<public-slug>-main-v1.png`。
- 正式：`app/src/main/assets/characters/<public-slug>/avatar_main.webp`，512×512 RGB、WebP quality 95。
- PNG 备份：`output/imagegen/six-characters/final/<public-slug>/avatar_main.png`，1024×1024。
- 若服务不遵从像素尺寸，保留真实原图与实际尺寸，按 EXIF 正向后的中央等比正方形裁切规范化；不得声称实际返回了请求尺寸。

## 次级头像

先真正看六张主图，筛掉同质脸，再用各自通过的主图作为参考编辑生成 `<public-slug>-alt-v1.png`。必须保持同一人物脸型、发型、肤色与服装识别，不能重新生成六个陌生人。

| 人物 | 只改变的表情／姿态方向 |
| --- | --- |
| 贺闻川 | 极轻微笑，视线略往下但眼睛仍可辨 |
| 周见野 | 笑更明显，稍靠近镜头，保持成年与自然比例 |
| 裴叙白 | 视线偏开，极淡的笑 |
| 苏晚宁 | 比主图略温暖的笑 |
| 许朝颜 | 灿烂笑，保留高马尾和活泼眼型 |
| 宋知微 | 克制浅笑或冷静注视 |

正式 `avatar_alt.webp` 同为 512×512 RGB quality 95。Alt 的 Prompt 记录和参考 main 原图 SHA 必须进入 manifest；主图替换后不能让旧 alt 冒充由新主图编辑。默认 Alt 记录在本机 `output/imagegen/six-characters/alt-prompts.jsonl`，各行包含 public `characterId`、持久 `storageCharacterId`、`variant=alt`、`out`、`prompt`、`reference_image` 和请求编辑前计算的 `reference_sha256`；也可用 `--alt-prompts` 指向版本化记录。

## 本地准备命令

使用 `.tools/imagegen-venv/Scripts/python.exe scripts/prepare_character_avatar_assets.py`，其后选择：

- `audit`：实际完整解码 12 个预期 PNG，拒绝占位或近空白；不会以文件存在判断成功。
- `normalize --variants main --require-complete`：六主图规范化，保留原图，记录 source→WebP／PNG 的 SHA 绑定。
- `normalize --variants alt --require-complete`：六参考编辑变体规范化。
- `board`：六主图板、六变体板、48／64／96 native 像素圆裁板；文字仅放图外。
- `manifest`：更新 `SIX_CHARACTER_AVATAR_ASSET_MANIFEST.json`，未绑定最新原图的旧资源保持 pending。

版本重生时在本机 `output/imagegen/six-characters/selection.json` 用 `{ "<public-slug>": { "main": "<public-slug>-main-v2.png", "alt": "<public-slug>-alt-v2.png" } }` 显式选择，Prompt 记录的 `out` 同步相应版本；不删除原图和旧尝试。

## 真正看图的验收

1. 先看六张主图并排，再看六张 alt 并排，确认同一人身份一致、六人之间明显不同。
2. 看 48／64／96 像素圆形裁切。必须保留发型识别、眼睛、鼻口、下巴；背景不抢脸，暗色角色不糊成黑块。
3. 正式图不嵌圆框，不用白圈、透明玻璃圈、标题、Logo、签名或假英文。
4. manifest 初始 `visualReview.status=pending`。只有 root 实际看过原图、圆裁板和 UI 后才能记录通过，脚本不自动宣布审美合格。
5. AI 来源、Prompt、原图与资源 SHA、规范化过程持续记录。`unverified-provider-output-terms` 说明服务条款未核验；视觉状态不能解释为商用权利认证。
