# 3DCasinoGames

[![Build](https://github.com/GoldenEggOVO/3DCasinoGames/actions/workflows/ci.yml/badge.svg)](https://github.com/GoldenEggOVO/3DCasinoGames/actions/workflows/ci.yml)

面向 **Paper / Purpur 26.2、Java 25** 的独立 3D Casino 插件。运行时名称为 `ServerCasino`，主命令 `/casino`。当前版本 **0.5.6-preview**。

**只安装一个 JAR，就能显示和操作完整的原版方块机器。无需 CraftEngine、客户端资源包或客户端模组。** 实体机器为免费练习，不扣款、不发放金币。

![原版机器模型预览](docs/images/vanilla-machines.png)

*预览由服务器导出的 Display 几何在 Blender 中渲染；使用方块代表色，并非 Minecraft 客户端截图。*

## 下载与安装

1. 从 [Releases](https://github.com/GoldenEggOVO/3DCasinoGames/releases) 下载 `server-casino-0.5.6-preview.jar`。
2. 停服并备份 `plugins/ServerCasino` 与世界，将 JAR 放进 `plugins/`；同一插件只保留一个版本。
3. 启动服务器，使用 `/casino` 打开原生 Dialog，或用 `/casino create blackjack` 创建机器。

新安装默认 `machine-appearance: vanilla`。**旧配置若写有 `machine-appearance: resource-pack`，请改成 `vanilla` 后重启**，才能切换到无资源包模型。存档目录与命令名保持不变。

Vault 和 AuthMe 是可选集成。无需 ServerGames、ServerBoards、ServerMenu 或 KaMenu。完整配置、升级和备份步骤见 [安装说明](docs/installation.md)。

## 游戏与外观

包含 12 种实体机器：Blackjack、Mines、Crash、Plinko、Slots、Duck Race、Wheel of Fortune、Money Wheel、Penguin Cross、Keno、Hilo、Dragon Tower。

- 由原有模型几何转换的机身、旋转片段圆边、纯色圆角按钮。
- Blackjack 包含 52 张原版牌及牌背，右下角标记旋转 180°。
- Mines 前倾控制台、窄立式红色龙塔、往返运动的 Hilo 指针。
- 原生 Dialog 管理菜单、实体按钮、练习下注、自定义布局及持久机器。
- 所有模型随 JAR 提供；普通资源包接口保留给高级自定义外观，详见 [自定义模型](docs/custom-models.md)。

每位玩家每种游戏可同时放置一台机器。机器不会因离线或超时消失；创建、修改练习下注与删除会保存到 `placements.json`，在重启或区块重新加载后恢复。当前对局和动画不跨重启恢复。详情及预览见 [外观说明](docs/vanilla-visual-review.md)。

## 命令与权限

| 命令 | 用途 |
| --- | --- |
| `/casino` | 原生管理菜单 |
| `/casino create <game> [skin-id]` | 创建免费练习机器 |
| `/casino bet <game> <1-100>` | 修改自己的机器练习下注；需靠近且处于空闲状态 |
| `/casino remove [game]` | 删除自己指定类型或全部机器 |
| `/casino reload-models` | 校验并重载自定义模型定义 |

`/casino-demo` 保留相同管理子命令。`casino.use` 默认允许；`casino.machine` 默认仅 OP 拥有。Shift＋右键机器打开设置。Shift＋F 不属于本插件。

`menu-enabled: false` 并重启可关闭 Dialog。命令、实体按钮、保存恢复和结算服务继续工作；菜单入口显示指令提示。

## 经济与旧记录

实体机器始终免费，不调用经济扣款或发奖。保留 Vault / `EconomyProvider` 扩展及旧金币记录核对，不提供菜单新开金币局。金额使用整数 cents；不确定的转账保持待核对状态。

管理员核对经济插件证据后，可在控制台执行 `casino resolve <玩家UUID> <对局UUID> applied|not-applied`；Mines 使用 `casino mines resolve ...`。命令本身不转账，不能猜测结果或删除存档绕过待核对状态。

## 开发与文档

```sh
mvn -B -ntp package
python -m pip install -r tools/requirements-dev.txt
python -m unittest discover -s tools -p "test_*.py"
```

使用 JDK 25、Maven 3.9+；首次构建需联网获取公开依赖。安装 `target/server-casino-*.jar`，不要安装 `original-*.jar`。不需要模型生成器、Blender 或本地字体就能构建 JAR。

- [安装说明](docs/installation.md) · [验证与复现](docs/verification.md)
- [自定义模型](docs/custom-models.md) · [扩展接口](docs/architecture.md)
- [资源工具](docs/resources.md) · [更新记录](CHANGELOG.md) · [贡献说明](CONTRIBUTING.md)

源码许可为 [GPL-3.0](LICENSE)。第三方来源与素材归属边界见 [THIRD_PARTY.md](THIRD_PARTY.md)。仓库不包含生产配置、第三方插件 JAR、私有字体或测试服务器二进制。
