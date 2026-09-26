# 资源与生成工具

## 原版机器

运行时直接读取 JAR 中的 `vanilla-models.json`，使用原版方块和文字 Display，无需下载资源。`tools/export_vanilla_models.py` 从原模型几何及批准的贴图生成 110 个内置模型，包含 52 张牌与牌背。

```sh
python -m pip install -r tools/requirements-dev.txt
python tools/export_vanilla_models.py
python -m unittest discover -s tools -p "test_*.py"
```

生成器先对照已提交的源模型验证几何，再导出原版模型；不会覆盖原始资源包素材。普通 Maven 构建直接使用已提交的导出文件，不要求 Python 或字体。Blender 预览脚本位于 `tools/vanilla-preview/`，运行时实体快照的生成见 [验证说明](verification.md)。

## 原有素材与可选客户端资源包

批准的 JSON / PNG 现位于 `resource-pack/assets/3dcasino/`。PNG 字节和模型几何保持不变；JSON 仅替换命名空间为 `3dcasino`，校验时反向规范化该替换，`tools/resource-baseline.json` 校验 871 个保留素材；原来 4 个 CraftEngine 配置已经移除。不要为了测试通过而重写基线。

`python tools/package-client-pack.py` 生成 `target/3dcasino-client-pack-26.2.zip`，只包含普通 Minecraft `assets/3dcasino` 与 `pack.mcmeta`。固定排序、时间戳和 ZIP 编码使打包可重复。它只供主动选择 `resource-pack` 外观模式的管理员使用，默认安装不需要。

`python tools/export_original_bbmodels.py <输出目录>` 将原始 JSON / PNG 导出为 Blockbench 文件，第二个可选参数为本地原版 assets 目录，用于引用原版贴图的模型。修改几何后须同步检查原版导出与布局，不应通过重绘改变已认可的素材。

## 源码交付

`python tools/package-source.py` 生成 `target/3dcasino-source.zip`。包含源码、普通资源素材、文档、CI、生成工具和测试基线；排除构建产物、运行报告、本地字体、字体配置及 Python 缓存。打包不需要字体或第三方插件。

## 可选重绘

`build-casino-artwork.py`、`build-casino-panels.py`、`build-machine-ball.py`、`build-casino-cabinets.py` 和 `build-showcase-machines.py` 是美术开发工具，普通构建不调用它们。图像切片重绘需自行提供有权使用的 `artwork/casino-icons-source.png` 和 `artwork/casino-panels-source.png`；这些本地原图不包含在公开源码中。

需要文字的重绘工具使用 `asset_fonts.py`。复制 `tools/fonts.example.json` 为 `tools/fonts.local.json`，配置自己有权使用的 bold、symbols、cjk 字体。相对路径以配置文件目录为根，`THREEDCASINO_FONT_CONFIG` 可指定另一配置。无配置时明确失败，不静默替换字体。历史纹理使用过 Arial Bold、Segoe UI Symbol、Microsoft YaHei，字体程序不随项目分发。

重绘受字体、Python 和 Pillow 版本影响；需要精确复现时固定版本和字体 SHA-256。通常直接打包已提交的 PNG 即可复现原有资源包。每次有意修改素材后审阅差异并进服验收。
