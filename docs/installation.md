# 安装与升级

## 必需文件

- Paper / Purpur 26.2，Java 25。
- `plugins/server-casino-0.5.6-preview.jar`。

新安装使用内置原版 BlockDisplay / TextDisplay / Interaction 机器，模型随 JAR 提供。**无需 CraftEngine、资源包、客户端模组或其他自研插件。** 原生 Dialog 需要支持相应协议的客户端；跨版本代理或 Bedrock 桥接客户端的显示由管理员实测。

## 新安装

1. 停服并备份插件数据与世界；只保留一个 ServerCasino JAR。
2. 放入 JAR 并启动。默认配置如下：

   ```yaml
   menu-enabled: true
   machine-appearance: vanilla
   ```

3. `/casino` 打开机器管理菜单，`/casino create <game>` 创建机器。创建权限 `casino.machine` 默认仅 OP 拥有；管理者可用自己的权限插件授予。
4. 右键按钮操作；Shift＋右键打开设置。没有任何 Shift＋F 依赖。

支持的游戏 ID：`blackjack`、`mines`、`crash`、`plinko`、`slots`、`duck_race`、`wheel_of_fortune`、`money_wheel`、`penguin_cross`、`keno`、`hilo`、`dragon_tower`。

## 从旧版本升级

停服，备份原 JAR、`plugins/ServerCasino` 和对应世界后替换 JAR。**已有配置中显式的 `machine-appearance: resource-pack` 会保留；要使用原版模型，改为 `vanilla` 后重启。** 未设置该键时使用新的 vanilla 默认值。

无需删除或手工转换 `placements.json`。现有默认机器在恢复时采用新版原版几何；默认龙塔采用窄立式布局；自定义定义保留其布局快照。当前一局或动画不跨重启恢复。

本插件已去掉 CraftEngine 依赖及专用内容包。若旧服务器仍通过 CraftEngine 提供其他内容，应自行保留它们；本次插件升级不会修改其他插件或已分发资源包。

## 菜单开关

`menu-enabled: false` 并重启会关闭 Dialog，`/casino` 与机器设置入口改为命令提示。创建、下注设置、删除、模型重载、实体游戏、保存恢复及结算服务继续运行。已有资金记录的对局菜单随开关关闭，控制台核对功能仍可使用。

## 可选集成

| 集成 | 用途 | 缺少时 |
| --- | --- | --- |
| Vault + 经济插件 | 经济接口与旧金币记录 | 免费机器照常运行；不伪造经济成功 |
| AuthMe | 已安装时检查玩家登录状态 | 不阻止使用 |
| `EconomyProvider` | 自定义经济服务 | 使用可用的 Vault 适配或返回不可用 |
| `MachineModelResolver` | 高级资源包模式的物品解析 | 使用原版 `item_model` |

模型插件不是安装条件。若主动选用 `machine-appearance: resource-pack`，必须给客户端提供相应模型的普通 Minecraft 26.2 资源包。源码的 `python tools/package-client-pack.py` 可生成原有 Casino 客户端资源包。这是高级兼容选项，默认原版模式不使用它。

## 保存与验收

备份 `placements.json`、`rounds/`、`casino-rounds/` 及对应世界。机器布局持久化；单局进度不持久化。模型定义重载只影响新创建的机器，已保存机器保留定义快照。

实体机器免费练习，不扣款、不发放余额。旧资金记录不可删除以绕过核对；命令见 README。

先在本地或隔离服验证菜单、创建、交互、删除、重启恢复，再由管理员安装至目标服。原版曲面由多个旋转方块实体组成，大量机器的客户端帧率、材质、字体与操作手感需要实际客户端验收。
