# 验证与复现

## 0.5.6-preview · 2026-09-26

| 检查 | 结果 |
| --- | --- |
| JDK 25 / Maven package | 121 项测试通过，0 失败、0 错误、0 跳过 |
| Python 资源、几何、打包回归 | 37 项通过 |
| 原版模型重导出 | 110 个模型；文件摘要与 0.5.5 一致；不需要本地字体 |
| 原资源素材 | 871 个文件通过原 SHA-256 基线校验 |
| 干净 Purpur 26.2 | 首启与两次重启均正常退出，三阶段通过 |
| 实体射线命中 | 1542 个按钮 / 格子取样通过，覆盖 0 / 37 / 90 / 180 度朝向 |
| 实际 Display 表面审计 | 15 个机器 / 状态快照未发现同向共面重叠 |

测试 JAR SHA-256：`c346032a8fc6341cae452699ae939b27b7f4b3b4bc911b1d7a818ffbdbbee928`。

### 隔离服验证内容

测试服只安装 ServerCasino 和专用探针，无 CraftEngine、Vault、AuthMe、ServerGames、ServerBoards、ServerMenu、KaMenu 或客户端资源包；只绑定回环地址。

1. 不预写 Casino 配置，确认新安装自动使用 vanilla。创建、操作、删除全部 12 种机器，检查显示实体与清理；检查原生根菜单和机器设置各调用一次 Dialog。
2. 保存 Slots、Mines、Dragon Tower 后重启。配置只写 `menu-enabled: false`，不写外观键，验证缺省值仍为 vanilla。三台机器恢复后按钮可操作，菜单不会调用 Dialog，随后删除机器。
3. 再次重启，确认被删除的机器没有重新出现。

探针还检查 Blackjack 发牌 / 盖牌、Keno 选中状态、按钮按压、变换矩阵及多朝向命中。规则差分、经济待核对与保存语义由 Java 回归覆盖。

### 自动检查的边界

这次未重复高级 `resource-pack` 模式的完整服务器流程；其通用接口保留并有相关单元及资源引用测试。Vault / AuthMe 的真实第三方集成、跨版本客户端与多人高密度性能未在本轮验收。

本地服务端探针使用代理玩家，能检查 Dialog 构造与调用、交互事件和实体数据，不能证明真实客户端渲染或操作手感。模型预览为 Blender 几何渲染；Minecraft 实际画面与体验由维护者进服验收。本次未修改正式服。

## 复现命令

```sh
mvn -B -ntp package
python -m pip install -r tools/requirements-dev.txt
python -m unittest discover -s tools -p "test_*.py"
python tools/export_vanilla_models.py
git diff --exit-code -- src/main/resources/vanilla-models.json
```

准备自己已接受 EULA 的 Purpur 26.2 模板目录：包含 `purpur-2622.jar`、`eula.txt`、`libraries/`、`cache/`、`versions/26.2/purpur-26.2.jar`。探针复制基础服务端文件，在本项目 `reports/vanilla-runtime/run-*` 创建独立测试服，不复制模板插件和世界，也不修改模板。设置 `JAVA_HOME` 为 JDK 25，运行：

```sh
python tools/vanilla-probe/run.py --server-template /path/to/prepared-purpur --port 25597
python tools/vanilla-preview/audit_surfaces.py reports/vanilla-runtime/run-TIMESTAMP/plugins/CasinoVanillaProbe/preview-snapshots reports/vanilla-runtime/run-TIMESTAMP/surface-audit.json
```

将 `run-TIMESTAMP` 替换为探针输出的目录。查看 `result.json`、三个 `phase-*.log` 与 `surface-audit.json`；任一阶段失败都应先定位，不可只以服务端进程启动作为通过。

GitHub Actions 自动运行 Maven、Python 回归及原版模型重导出检查。它不代替本地服务端探针或真实客户端验收。
