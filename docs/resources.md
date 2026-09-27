# 内置原版模型与预览

插件从 JAR 内的 `vanilla-models.json` 读取 110 个模型，包括机器部件、52 张牌及牌背。它们由原版 BlockDisplay、TextDisplay 与原版材料显示；安装时只需要插件 JAR，不需要客户端资源包。

模型数据位于 `src/main/resources/vanilla-models.json`。修改几何后运行 Maven 和 Python 回归检查，并使用 [独立测试服](verification.md) 导出实际 Display 快照。`tools/vanilla-preview/` 中的脚本可生成预览，README 中的机器展示图由服务器导出的几何渲染，不能代替 Minecraft 客户端验收。

```sh
mvn -B -ntp package
python -m pip install -r tools/requirements-dev.txt
python -m unittest discover -s tools -p "test_*.py"
```

`python tools/package-source.py` 生成公开源码包，包含内置几何、文档和测试；不会包含旧客户端资源包、构建产物或运行报告。显式配置的第三方自定义物品由其提供者管理，不属于插件安装文件。
