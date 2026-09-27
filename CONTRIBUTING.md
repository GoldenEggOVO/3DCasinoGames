# Contributing

Use **JDK 25, Maven 3.9+ and Python 3.12+**. A normal build does not depend on other workspace projects, local fonts or model plugins.

```sh
mvn -B -ntp package
python -m pip install -r tools/requirements-dev.txt
python -m unittest discover -s tools -p "test_*.py"
```

After changing models, review `src/main/resources/vanilla-models.json`, run the Java and Python regressions, and inspect a fresh server Display snapshot and Minecraft client preview.

## Changes and pull requests

Describe the behavior changed, the tests run and any compatibility impact. Include before/after previews for visual or interaction changes, and distinguish server-side probe results from actual Minecraft client acceptance.

Keep the English and [Simplified Chinese README](README.zh-CN.md) aligned when changing installation, commands or features. Machine showcase images should come from actual server Display snapshots and be identified as rendered previews.

Do not commit runtime worlds, credentials, third-party JARs, private fonts or local font configuration files.

## Behavior and compatibility

Cover game rules, amounts and recovery semantics with behavioral tests. Visual changes must preserve gameplay and payment behavior. The public API is still in beta; document breaking changes explicitly.

See [architecture and interfaces](docs/architecture.md) for the code layout and API, [verification](docs/verification.md) for isolated-server checks, and [model tools](docs/custom-models.md) for asset generation and preview workflows.
