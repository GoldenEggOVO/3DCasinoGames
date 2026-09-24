# 0.5.1-preview 原版外观验证（2026-09-24）

- Java 回归：113 项通过，无失败、错误或跳过；Python 资源回归：21 项通过。
- 全新隔离 Purpur 26.2 只加载 ServerCasino 与验证探针，没有 CraftEngine、ServerGames、ServerBoards、ServerMenu、KaMenu，也没有客户端资源包。`machine-appearance: vanilla` 下，12 种机器均完成创建、原版显示实体及无自定义 item-model 检查、按钮交互和删除；Keno 先选择数字再启动游戏。首次启动、机器恢复后的重启、删除后的再次重启均通过，退出码均为 0。
- 默认 `machine-appearance: resource-pack` 的隔离服探针也通过：12 种机器、自定义模型接口与按钮、原生 Dialog、`menu-enabled: false`、存档恢复和删除后重启均通过，三次启动退出码均为 0。
- 两种模式验证的 JAR SHA256 均为 `af643fcee29c0ca9cd10c0aceffd6e13038f74e3cf533fde52440e5d43bce6d2`。原版模式证据位于 `server-casino/reports/vanilla-runtime/run-20260924T184908797480Z/result.json` 及三个阶段日志；资源包模式证据位于工作区 `reports/casino-public-runtime/run-20260924T185036737597Z/result.json`、`restart-2.json`、`restart-3.json` 及日志。

以上是服务端模拟玩家及实体状态验证；真实 Minecraft 客户端的原版外观、可读性、按钮位置与操作手感仍由玩家进服验收。未部署正式服。

## 0.5.0-preview 独立安装复核

- 结构回归确认插件管理器不再作为公共可写字段暴露，机器菜单不持有结算服务或定时任务。
- Java 回归：110 项通过，无失败、错误或跳过。新增插件元数据回归，拒绝其他自研插件的硬依赖与软依赖。
- Python 资源回归：21 项通过；新增独立客户端包的 26.2 格式、完整 Casino 资产及可重复打包检查。默认资源几何与像素除命名空间外保持基线一致。
- 关闭菜单后实际重启确认菜单对象未创建，独立结算服务仍可推进已有对局；机器恢复及删除后再次重启检查通过。
- 全新隔离 Purpur 26.2 仅加载 Casino 与验证探针；探针明确检查 Vault、AuthMe、CraftEngine、ServerGames、ServerBoards、ServerMenu、KaMenu 均不存在时正常启用。服务端首启与两次重启退出码均为 0。
- `/casino` 入口正常；`/casino mines` 不再打开菜单，也不生成 Mines 的 `menu.yml`。旧 `/mines`、`/plinko-demo` 未注册；当前权限拒绝检查通过。
- `menu-enabled: false` 下 `/casino` 与机器设置不弹窗；12 种机器的 create、bet、remove 指令及模型重载通过。非法金额与无权限修改被拒绝，Mines 对局中下注保持不变。
- 12 种机器同时放置、离线保留、超过 20 分钟不清理、区块显示卸载与恢复通过。
- 同一隔离服务器存档实际重启后，12 台机器的位置、朝向、下注与自定义模型快照一致；重复恢复不增加实体，未加载机器可删除；删除后再次重启确认不会重新出现。
- 历史对局处理界面不再提供新开局、参数设置或金币模式切换入口。
- 原生 Dialog 构造、12 种机器四方向放置、设置回调、旧会话拒绝与实体清理通过。
- 自定义 Blackjack 皮肤验证了模型 ID、机壳/游戏区域挂点、按钮位置与三轴旋转、右键射线开局、沿表面法线按压及回弹。
- 练习运行验证中经济服务调用次数为 0；持久化资金失败与待核对恢复通过单元回归。

上述为自动化服务端验证。真实 Java/Bedrock 客户端的外观、字体及交互手感仍需进服验收；真实 Vault 经济提供者端到端测试和 Modrinth 发布审查不包含在此次结果中。该版本未部署到正式服。

本次隔离服原始证据位于工作区 `reports/casino-public-runtime/run-20260924T181535009899Z/` 的 `result.json`、`restart-2.json`、`restart-3.json` 与服务端日志。探针使用模拟玩家验证 Dialog 构造和事件路由，不代表真实客户端成功加载资源包。客户端包已校验其内容，尚未通过真实客户端确认模型显示。
Purpur 在受限本地环境中记录了系统指标读取和 Mojang 公钥联网警告；三个阶段均完成插件验证并正常退出。
