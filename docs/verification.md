# Verification — 0.6.0-beta.1

## Results

| Check | Result |
| --- | --- |
| JDK 25 / Maven package | 135 tests passed; no failures, errors or skipped tests |
| Python assets, geometry and packaging | 39 tests passed |
| Resource baseline | 871 files verified; namespace-only normalization for JSON, identical PNG bytes |
| Vanilla geometry exporter | 110 models; checked-in output reproduced |
| Clean Purpur 26.2 | Startup and two restarts passed; all processes exited normally |
| Ray targeting | 1857 samples passed across 0 / 37 / 90 / 180 degree placements |
| Display surface audit | 15 snapshots, zero same-facing coplanar overlaps |
| Namespace and Tab | Only new command, permission-filtered suggestions; invalid console UUID gives usage |
| Language | English default, generated files, edited machine label applied after restart, English fallback |

Release JAR SHA-256: `6248ab0d6e6b45d419af8f725510e5e7adf97dfae1bd7e43d3055aff3213d09d`.

The isolated server contains only the plugin and disposable test probe. CraftEngine, Vault, AuthMe, ServerGames, ServerBoards, ServerMenu and KaMenu are absent. It binds only to loopback and uses a copied test template, not a production server.

1. Fresh startup without pre-written config: vanilla default, both language files generated, native root and machine settings Dialog calls succeed. Create, operate and remove all 12 machines, check card dealing/hole-card reveal, display matrices, button positions and cleanup.
2. Save six machines: Slots, Mines, Dragon Tower, Hilo, Duck Race and Penguin Cross. Restart with menus disabled and a custom language file overriding one PLAY label. All six restore and play; the edited label is shown and missing keys fall back to English. Remove them.
3. Restart again; deleted machines remain absent.

Java regressions retain the frozen game-rule comparisons and economy/persistence tests. New checks cover language fallback, malformed YAML, placeholders, key coverage, validation messages, translation-independent outcomes, command suggestions and metadata. Visual changes also have geometry regressions.

## Display counts

Idle/default snapshots; totals include hidden item carriers and text displays, exclude Interaction entities. A Blackjack hand adds card entities while playing.

| Machine | Unique model IDs | BlockDisplays | All Displays |
| --- | ---: | ---: | ---: |
| Blackjack | 6 | 328 | 340 |
| Mines | 5 | 232 | 267 |
| Crash | 4 | 154 | 162 |
| Plinko | 2 | 277 | 294 |
| Slots | 2 | 70 | 84 |
| Duck Race | 10 | 323 | 338 |
| Wheel of Fortune | 4 | 744 | 770 |
| Money Wheel | 8 | 932 | 966 |
| Penguin Cross | 5 | 171 | 187 |
| Keno | 3 | 660 | 754 |
| Hilo | 6 | 257 | 281 |
| Dragon Tower | 4 | 274 | 304 |

Dragon Tower now uses 1056 fewer BlockDisplays than the earlier rounded hidden tiles. This count is not a measured client FPS improvement.

## Limits

The probe uses a proxy player and real server entities. It verifies server-side Dialog construction, events, placement recovery, geometry and ray selection. It does not verify actual client rendering, latency, fonts, FPS or interaction feel. Blender previews use representative colors for most blocks and local vanilla textures for Dragon Tower tile blocks; they are not Minecraft screenshots.

Actual third-party Vault/AuthMe integration, cross-version clients, high-density multiplayer load and the complete advanced resource-pack server workflow were not retested. Their interfaces and packaged resource references have automated coverage. Minecraft visual and interaction acceptance remains with the maintainer. No production server was modified.

## Reproduce

```sh
mvn -B -ntp package
python -m pip install -r tools/requirements-dev.txt
python -m unittest discover -s tools -p "test_*.py"
python tools/export_vanilla_models.py
git diff --exit-code -- src/main/resources/vanilla-models.json
```

Prepare a Purpur 26.2 template whose EULA you have accepted. It needs `purpur-2622.jar`, `eula.txt`, `libraries/`, `cache/` and `versions/26.2/purpur-26.2.jar`. Set `JAVA_HOME` to JDK 25, then:

```sh
python tools/vanilla-probe/run.py --server-template /path/to/prepared-purpur --port 25597
python tools/vanilla-preview/audit_surfaces.py reports/vanilla-runtime/run-TIMESTAMP/plugins/CasinoVanillaProbe/preview-snapshots reports/vanilla-runtime/run-TIMESTAMP/surface-audit.json
```

Use the printed run directory. Inspect all three `phase-*.log` files, `result.json` and `surface-audit.json`. A running Java process alone is not a passing test. GitHub Actions additionally runs Maven, Python checks and exporter consistency for the pushed commit.
