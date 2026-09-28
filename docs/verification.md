# Verification — 0.6.0-beta.2

## Cross-version compatibility

The unchanged published beta.2 JAR was tested on all three servers below on 2026-09-27, using Java 25. Every run used plugin digest `5bcbfc4e68c5d2ca98687e20b106406be03cccd5e89aea0ef4f3a2c54b60a77a`.

| Server | Build | Three phases | Local evidence directory under `reports/vanilla-runtime/` |
| --- | --- | --- | --- |
| Purpur 26.2 | 2622 | Passed | `run-20260927T092857122591Z` |
| Purpur 26.3 | 2642 (experimental) | Passed | `run-20260927T092700249294Z` |
| Paper 26.3 | 42 (alpha) | Passed | `run-20260927T092832854840Z` |

Each run checks the actual Minecraft version, all 12 machines, 1,857 targeting samples in its first phase, feedback, Dialog construction/calls, command completion, language overrides, menus on/off, persistence and deletion across restarts. Each runtime contains only Casino and the disposable probe. The Python suite also passed all 20 tests after the runner changes.

The plugin keeps its Paper 26.2 compilation dependency and `api-version: '26.2'` minimum. There is no separate 26.3 artifact or version-specific gameplay branch. See [probe commands](../tools/vanilla-probe/README.md) to reproduce the checks. 26.3 upstream builds are still experimental; real-client rendering, sound and interaction acceptance remain separate from these server-side checks.

## Beta.2 release validation

- Fresh release build: 159 Java tests and 20 Python tests passed, with no skipped Java tests.
- The exact `3dcasino-0.6.0-beta.2.jar` passed all three clean Purpur phases: all 12 machines, feedback, menu toggles, targeting, saved-machine restoration and deletion across restarts.
- Local evidence: `reports/vanilla-runtime/run-20260927T091003258657Z/result.json`, including the tested artifact digest.
- Client visuals, sound mixing and high-density multiplayer performance remain separate in-game acceptance checks.

## Backward compatibility experiment

A separate local test build compiled the unchanged production Java sources against Purpur 1.21.8 build 2497 libraries, with `api-version: '1.21.8'`. The same artifact passed all three runtime phases on Purpur 1.21.8, 1.21.9, 1.21.10, 1.21.11, 26.1.2, 26.2 and 26.3 using Java 25. Older native Paper builds and Java 21 were not tested. This experiment does not change the published beta.2 JAR or its declared support.

Lowering only the metadata while retaining the 26.2 compile dependencies produced an Adventure `TextComponent.Builder.build()` linkage error on older servers. Recompiling with the older dependencies resolved it without changing production Java. Compilation against 1.21.7 fails because `Player.closeDialog()` is unavailable; that version needs implementation changes.

Local evidence and compilation instructions are in `reports/backward-compat/summary.md`; each successful run records the same test artifact digest. The probe also now completes feedback for a randomly dealt natural Blackjack before forcing a replacement test hand, avoiding an unrelated pending-result assertion.

## Validation scope and limits

The three-phase probe creates and operates all 12 machines, saves six placements, restores and operates them with menus disabled, removes them, and confirms they remain absent after another restart. It also checks card reveals, display initialization, language reload/fallback and feedback settlement, including doubled Blackjack stakes, early Crash cashouts and concurrent Plinko balls.

The beta.2 surface audit covered 30 runtime snapshots with no same-facing coplanar overlaps. Java tests cover game rules, economy/persistence, language validation, menu callbacks and model definitions.

The probe uses a simulated player and real server entities. It does not validate client rendering, fonts, audio, latency, FPS or interaction feel. Optional Vault/AuthMe integrations, cross-version clients and high-density multiplayer performance require separate acceptance.

## Reproduce

```sh
mvn -B -ntp package
python -m pip install -r tools/requirements-dev.txt
python -m unittest discover -s tools -p "test_*.py"
```

For clean Paper/Purpur 26.2 and 26.3 runs, follow the [runtime probe instructions](../tools/vanilla-probe/README.md). Compare the recorded plugin digests to verify that each server tested the same JAR.

To audit exported model surfaces:

```sh
python tools/vanilla-preview/audit_surfaces.py reports/vanilla-runtime/run-TIMESTAMP/plugins/CasinoVanillaProbe/preview-snapshots reports/vanilla-runtime/run-TIMESTAMP/surface-audit.json
```

Inspect all three phase logs, `result.json` and the surface audit. GitHub Actions also runs Maven and Python checks for each pushed commit.
