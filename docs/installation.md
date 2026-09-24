# ServerCasino 0.5.1-preview 独立安装

支持 Paper/Purpur 26.2、Java 25。ServerCasino JAR 是唯一必需的服务端插件；不需要 ServerGames、ServerBoards、ServerMenu 或 KaMenu。当前版本不自动迁移旧插件目录或导入其他菜单插件配置。

## 安装插件与菜单

1. 停服，备份现有 Casino JAR、`plugins/ServerCasino` 数据目录及世界；同一插件只保留一个版本。
2. 把 `server-casino-0.5.1-preview.jar` 放入 `plugins/`，启动服务器。默认 `menu-enabled: true`，`/casino` 打开 Paper 原生 Dialog；不需要另装菜单插件。玩家需要 `casino.use` 权限；创建、设置和删除机器还需 `casino.machine`，其默认仅 OP 拥有。
3. `/casino create <game> [skin-id]`、`/casino bet <game> <1-100>`、`/casino remove [game]`、`/casino reload-models` 管理机器。`/casino-demo` 保留相同管理子命令。Mines 通过实体机器游玩。Shift＋F 统一入口属于 ServerMenu，不随 Casino JAR 提供。
4. 若关闭 Dialog，设置 `plugins/ServerCasino/config.yml` 的 `menu-enabled: false` 并重启。`/casino` 与 Shift＋右键机器设置只显示命令提示；管理命令、实体按钮、机器存档恢复及已有资金记录处理继续运行。

## 完整显示机器模型

原生 Dialog 与机器逻辑只需 JAR。若要保持原有机壳、按钮、图案和动画模型外观，保持默认 `machine-appearance: resource-pack`，并让客户端加载包含 `assets/casino` 的 26.2 资源包。选择一种分发方式：

| 方式 | 文件 | 安装位置与用途 |
| --- | --- | --- |
| 不安装 CraftEngine | `casino-client-pack-26.2.zip` | 作为客户端资源包分发，或把 `assets/casino` 合并进现有的有效 26.2 客户端资源包；让玩家实际加载。 |
| 已安装 CraftEngine | `casino-craftengine.zip` | 解压其中 `resources/casino` 到 `plugins/CraftEngine/resources/casino`，按 CraftEngine 流程重新生成并分发合并后的客户端资源包。此 ZIP 本身不是客户端资源包 URL。 |

从源码分别运行 `python tools/package-client-pack.py` 与 `python tools/package-resources.py` 生成以上文件。两种包使用相同的 Casino 模型资产；不要让旧 Casino 内容包与新内容包同时被 CraftEngine 扫描。CraftEngine 配置使用 `item-model: casino:<id>` 指向现有 `assets/casino/items/*.json`，不要改成会重新生成映射的 `model` 配置。使用其他资源包系统时保留这些文件路径及命名空间；只装 JAR 时机器仍可操作，但模型可能显示为缺失或普通物品。

## 完全不安装资源包

在 `plugins/ServerCasino/config.yml` 中设置 `machine-appearance: vanilla`，然后重启。此模式只用原版方块、物品和文字显示机身、按钮、游戏状态、Blackjack 牌面及 Keno 数字；不需要 CraftEngine 或客户端资源包。所有 12 种机器的玩法、按钮命中区域、权限、练习资金行为及 `placements.json` 保持一致。它是全服设置；已有机器在重启恢复时按当前模式重新显示。原版外观比自定义资源模型简化，不能与其视觉效果相同。

## 可选集成与数据

Vault 和兼容经济提供者仅用于保留的经济接口及旧金币记录，不是免费实体游戏或 Dialog 的前提。AuthMe 存在时会检查登录状态；缺席时不阻止使用。CraftEngine 仅用于资源分发。其他插件可以通过 Bukkit `ServicesManager` 注册 `EconomyProvider` 或 `MachineModelResolver`；见 [架构说明](architecture.md)。

默认实体机器始终免费练习，不扣款、不发放余额。备份 `placements.json`、`rounds/`、`casino-rounds/` 及对应世界；不要删除待核对的资金记录。模型定义只影响新创建的机器；已放置机器保留定义快照。旧版数据不保证兼容，迁移前保留原环境备份并完成对局核对。

在隔离环境验证菜单、机器创建、交互、删除及重启恢复后再自行安装到目标服务器。自动服务端探针不能证明客户端实际模型、字体、按钮位置和手感；这些需进服验收。
