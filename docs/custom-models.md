# 自定义机器外观

模型定义只为现有游戏配置外观和布局，不执行脚本，也不定义新规则。内置模型始终使用 JAR 内原版 Display 几何；外部命名空间的自定义物品仍可由可选解析服务提供。

内置模型 ID 解析为原版 Display 几何，`material:` 引用直接使用指定原版材料；其他模型 ID 在没有外部解析服务时使用原版材质回退。自定义坐标、按钮及模型定义快照保持生效。插件本身不提供或要求资源包。

内置默认定义由 `src/main/java/dev/casino3d/model/BuiltinLayouts.java` 和 `MachineDefinition.builtin(game)` 提供。外部定义放在服务器 `plugins/3dcasino/machines/*.yml`，每文件一台皮肤。缺省字段按 `game` 继承内置定义，因此只修改需要覆盖的项目。

```yaml
schema-version: 1
id: emerald-blackjack
game: blackjack
models:
  cabinet_blackjack: material:EMERALD_BLOCK
anchors:
  body:
    position: [0, 0, 0]
    rotation: [0, 0, 0]
    scale: 1
  playfield:
    position: [0, 0, 0]
    rotation: [0, 0, 0]
    scale: 1
parts:
  - model: material:EMERALD_BLOCK
    position: [0, 2.5, 0]
    rotation: [0, 0, 0]
    scale: 0.2
```

可直接复制 [emerald-blackjack.yml](emerald-blackjack.yml) 到上述目录。该示例用原版材料验证替换，无需额外贴图。如把材料改为外部模型 ID（例如 `my_pack:my_blackjack`），需由外部解析服务提供物品；其客户端素材也由该服务的管理员自行管理。模型配置不负责上传资源。

执行 `/3dcasino reload-models`，然后 `/3dcasino create blackjack emerald-blackjack`。加载先校验全部文件再替换注册表；失败保留之前有效定义。已经创建的机器保留原定义快照；删除后重新创建才使用新外观。

## 字段

| 字段 | 用途 |
| --- | --- |
| `schema-version` | 当前为整数 1 |
| `id` / `game` | 唯一皮肤 ID / 已有游戏 ID |
| `models` | 逻辑模型名到资源 ID 的映射；未覆盖时沿用 `3dcasino:<逻辑名>` |
| `anchors.body` | 机壳的局部变换 |
| `anchors.playfield` | 动态牌、球、轮盘等玩法局部坐标的共同变换 |
| `buttons` | 以该游戏支持的 action 为键的按钮覆盖 |
| `parts` | 额外静态部件，各含 `model, position, rotation, scale` |
| `settings-bounds` | 设置菜单射线区域 `[minX,minY,minZ,maxX,maxY,maxZ]` |

按钮值可以包含 `position: [x,y,z]`、`rotation: [pitch,yaw,roll]`、`width`、`height`、`depth`、`size` 与 `press`。前三个尺寸用于点击区域，`size` 用于视觉大小，`press` 为沿按钮面法线的按压距离。具体 action 和默认尺寸直接复制对应游戏的内置条目，不能随意添加玩法不支持的动作。按钮与机壳、动态挂点分别变换；修改 body 不会自动迁移按钮。

位置使用机器局部坐标与方块单位；整体朝向随机器放置 yaw。内置机壳模型底部为原点，正 Z 是前方。模型 JSON 使用 `8 + 物理坐标 × 4`，配合 FIXED ItemDisplay 的 scale 4；外来模型需按自身尺寸调整。rotation 数组为 pitch/yaw/roll，单位为度；四元数为 Z × Y × X，对点依次绕 X（pitch）、Y（yaw）、Z（roll）旋转，再做统一缩放和平移。不要把模型 JSON 的 display 旋转与机器配置旋转混为一谈。

所有坐标须为有限数；局部 position 每轴范围为 [-64, 64]，rotation 每轴范围为 [-360, 360] 度，统一缩放范围为 [0.001, 32]，按钮 width/height/depth/size 范围为 [0.001, 8]，press 范围为 [0, 1]。资源 ID 要满足 NamespacedKey 语法；`material:` 后为大写 Bukkit Material，且必须为非空气、可作为物品的材料。重复 ID、未知游戏/动作或非法值会报告对应文件和字段。创建后须检查四向摆放、卡牌/球与面板位置、按钮命中及按压方向。自动测试不能替代这些客户端验收。

## 内置模型与开发工具

插件从 JAR 内的 `vanilla-models.json` 读取 110 个模型，包括机器部件、52 张牌及牌背。它们由原版 BlockDisplay、TextDisplay 与原版材料显示；安装时只需要插件 JAR，不需要客户端资源包。

模型数据位于 `src/main/resources/vanilla-models.json`。修改几何后运行 Maven 和 Python 回归检查，并使用 [独立测试服](verification.md) 导出实际 Display 快照。`tools/vanilla-preview/` 中的脚本可生成预览，README 中的机器展示图由服务器导出的几何渲染，不能代替 Minecraft 客户端验收。

```sh
mvn -B -ntp package
python -m pip install -r tools/requirements-dev.txt
python -m unittest discover -s tools -p "test_*.py"
```

`python tools/package-source.py` 生成公开源码包，包含内置几何、文档和测试；不会包含旧客户端资源包、构建产物或运行报告。显式配置的第三方自定义物品由其提供者管理，不属于插件安装文件。

`python tools/refine_wheels.py` 仅重新生成转盘专用几何。修改后应检查四向摆放、按钮命中和动态部件位置。
