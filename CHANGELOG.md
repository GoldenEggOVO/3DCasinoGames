# Changelog

## Unreleased

- Unify player-facing text with Adventure Components and style-only MiniMessage; insert ordinary parameters literally and retain legacy color/reset semantics.
- Validate language keys, placeholders and styles; add transactional `/3dcasino reload-language` with the operator-default `3dcasino.admin` permission. Refresh live labels and invalidate old menus without restarting rounds.
- Replace internal menu YAML round-trips with immutable typed Dialog descriptions; bound inputs and use monotonic, owner-bound, single-use sessions with expiry cleanup.
- Replace leftover resource-font recovery graphics with vanilla card/path/multiplier text.
- Fit machine labels using visible glyph advances and bold styles; clip oversized translations within existing label bounds.
- Skip unchanged composite-display traversal and repeated button-press pose writes. Validate saved model references, bounds and rotations before restoration.
- Document text/menu contracts for independent Tabletop alignment; retain rules, free practice, placement format and uncertain-transfer handling.

- Remove the bundled client resource pack and its appearance switch. Built-in machines always use the JAR's vanilla Display geometry; old `machine-appearance` config entries are ignored.
- Keep optional model resolution only for explicitly configured external custom item references. Remove pack-only generators, baselines and packaging checks.

## 0.6.0-beta.1

- Breaking rename: plugin/data folder, command, permissions and models use `3dcasino`; Java API uses `dev.casino3d`. Old aliases and automatic compatibility adapters are removed.
- Add permission-aware command completion, game-specific skin suggestions and owned-machine suggestions.
- Add editable `languages/en_US.yml` and `languages/zh_CN.yml`, selected through `config.yml`; English is the default for Dialog, machine labels and feedback.
- Dragon Tower tiles now use one gray terracotta block when hidden, emerald blocks when safe, and TNT when trapped. Idle tiles use 1056 fewer BlockDisplays.
- Add a rear support to the Hilo panel, move Penguin Cross buttons outward, and place Duck Race PLAY below buttons 2/3 with more table clearance.
- Remove the sloped panel behind Blackjack controls.
- Retain free practice, game rules, persistence, native Dialog, custom models and optional economy/AuthMe integration. No CraftEngine dependency.

## 0.5.6-preview

- 默认使用随 JAR 提供的原版 3D 机器；不需要 CraftEngine 或客户端资源包。
- 移除 CraftEngine softdepend、专用配置与内容包打包脚本；原始素材移至 `resource-pack/`，保留普通资源包和公共模型接口。
- 整理安装、升级、开发与验证文档，加入 GitHub Actions 构建和回归检查。
- 补充全新配置默认值、菜单开关与无依赖重启运行检查。

### 合并此前本地交付的 0.5.2–0.5.5 更新

- 全部 12 种原版机器，以及 Blackjack 的 52 张牌和牌背；保留原资源主要机身轮廓。
- 原生菜单移除“返回棋牌游戏”，保留独立管理命令。
- 修复重叠表面、按钮圆角伸缩失真、轮盘和龙塔点击区域、Keno 过高判定。
- Mines 改为前倾控制台；通用按钮统一纯色，Blackjack 纯色桌面，Keno 橙色选中格。
- Slots 移除按钮底托，Dragon Tower 改为深色格子与红色窄机身。
- Hilo 指针往返后落在真实结果；移除机器上方悬浮说明。
- 保留原有玩法、概率、免费练习资金行为、存档及经济扩展。

升级时保留旧数据备份。当前版本已移除 `machine-appearance` 配置，旧配置项会被忽略。

## 0.5.1-preview

独立插件与仓库的早期公开预览版本。历史发布记录见 GitHub Releases。
