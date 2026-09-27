# 开发结构与扩展接口

插件入口为 `dev.casino3d.CasinoPlugin`。`game/<game>/` 内的 Round 只负责规则和结果，Machine 控制器只负责对应游戏的实体与动画。`machine/` 统一管理创建、射线点击、按钮按压、菜单入口和清理；`model/` 负责不可变外观定义、校验和重载。

`CasinoRuntime` 拥有金币存档服务与结算定时任务，菜单关闭也正常运行。`CasinoMenus` 仅负责机器管理界面和会话；`RoundRecoveryMenu` 仅处理已有存档对局，不能开始新的菜单对局。关闭菜单时不创建菜单对象。`economy.Wallet` 是内部结算访问契约；公共经济扩展仍使用 `api.EconomyProvider`。

持久化服务保留原 JSON 字段和交易恢复语义。不要用练习机器 Round 替换持久化交易状态，也不要绕过金币确认回调调用经济提供者。

## 经济接口

其他插件可以使用 Bukkit `ServicesManager` 注册 `dev.casino3d.api.EconomyProvider`。接口以 UUID 和整数最小金额单位操作，返回值区分成功、失败、不可用和结果不确定。具体方法及返回值契约见接口 Javadoc。Vault 适配器使用最低优先级，允许更高优先级的明确提供者覆盖。

练习流程不调用经济服务。经济不可用时禁止金币开局；转账不确定时保留待核对记录。Vault 与本地文件不能实现跨系统原子事务，不可通过自动重试来假装恰好一次转账。

## 模型解析接口

注册 `dev.casino3d.api.MachineModelResolver`，实现 `ItemStack resolve(String namespacedModel)`。仅显式配置的外部命名空间会调用此接口；内置 `3dcasino:` 模型始终使用 JAR 内原版 Display 几何，`material:` 直接使用原版材料。返回 `null` 时使用原版材料回退；返回的物品会复制，插件不修改提供者的缓存实例。

模型定义详见 [custom-models.md](custom-models.md)。注册服务和机器操作应在服务器主线程进行；不要在解析方法里阻塞网络或磁盘。API 目前是 beta，修改公共接口时需说明兼容影响。

## 回归与维护

测试中的 `Frozen*Round` 是冻结的 0.3.3 行为基线，只用于与新规则逐动作对比，不会进入插件 JAR。修改玩法应同时更新明确的行为测试；不能悄悄修改冻结基线使差分测试通过。

内置几何保存在 `vanilla-models.json`；修改后阅读 [resources.md](resources.md) 并进行客户端外观验收。内置按钮位置及尺寸由 `BuiltinLayouts` 和对应控制器的原版布局覆盖共同确定，测试需要覆盖实际定义而非重复写一套期望实现。

`machine/PlacementStore` 独立保存机器布置和模型快照，使用临时文件同步后原子替换；对局状态不写入布置文件。`MachineManager` 按所有者与游戏类型索引，区分保存记录与已加载实体。区块或世界卸载只卸载实体，显式删除才写入记录变更。

语言由 `Language` 加载，默认内置英文。启动只生成缺少的语言文件；菜单和实体文本使用命名键及占位符。服务使用稳定错误键，边界统一翻译；翻译内容不参与动作、概率或资金判断。见 [languages.md](languages.md)。

## 文本与菜单基础

`ui.MessageText` 只解析模板中的显示样式，普通参数使用 `Tag.inserting(Component.text(...))`。`Language.component` 是显示边界，`Language.text` 是日志与规则纯文本边界。禁止把替换后的字符串重新当模板解析。

`ui.MenuView` 是不可变的标题、正文、输入框、按钮和关闭按钮描述。`PaperMenus` 只负责转换到 Paper Dialog，内部不再生成临时 YAML。`MenuSessions` 使用单调时钟、玩家 UUID、令牌和单次消费；回调再检查插件启用、菜单开关、主线程、权限及具体机器状态。五分钟过期，退出、重载和停止时清理。

`VanillaDisplay.sync` 在确认模型、语言、位置、变换和发光均未变化后快速返回。文字重载只更新现有 TextDisplay；机器控制器在安全刷新点更新动态读数。保存的模型定义也经过记录构造校验，不能通过 JSON 恢复绕过 YAML 校验。详见 [Tabletop 对齐说明](tabletop-alignment.md)。
