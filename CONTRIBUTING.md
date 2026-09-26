# 参与开发

使用 JDK 25、Maven 3.9+ 与 Python 3.12+。普通构建不依赖其他工作区项目、本地字体或模型插件。

```sh
mvn -B -ntp package
python -m pip install -r tools/requirements-dev.txt
python -m unittest discover -s tools -p "test_*.py"
```

更改模型后运行 `python tools/export_vanilla_models.py`，审阅 `vanilla-models.json` 差异；保留资源基线，只有确认要更新批准外观时才更改其摘要。

提交时说明改变的行为、测试结果和兼容性影响。视觉或交互变更请附前后预览，并区分服务端探针与真实客户端验收。不要提交运行世界、凭据、第三方 JAR、私有字体或本地字体配置。

规则、金额与恢复语义应有行为测试；不应为了修改外观而改变玩法或扣款方式。公共 API 仍处于 preview。代码目录与接口见 [架构](docs/architecture.md)，隔离服测试方法见 [验证](docs/verification.md)。
