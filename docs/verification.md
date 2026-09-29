# Verification — 0.6.0-beta.3

## Cross-version compatibility

Minimum supported server version: **Paper / Purpur 1.21.8**. **Java 25 is required**, including on older Minecraft versions. One JAR is used across the tested versions.

The release is compiled against the pinned official Paper 1.21.8 API. This also resolves the older-server Adventure linkage error without changing production Java or gameplay. Version 1.21.7 is unsupported because the implementation uses `Player.closeDialog()`.

## Release validation

- 159 Java tests passed, with no failures, errors or skips.
- 20 Python tests passed.
- The exact release JAR was exercised in the following clean servers using Java 25:

| Server | Build | Three-phase probe |
| --- | --- | --- |
| Paper 1.21.8 | 60 | Passed |
| Paper 26.3 | 42 (alpha) | Passed |
| Purpur 1.21.8 | 2497 | Passed |
| Purpur 1.21.9 | 2505 | Passed |
| Purpur 1.21.10 | 2535 | Passed |
| Purpur 1.21.11 | 2568 | Passed |
| Purpur 26.1.2 | 2592 | Passed |
| Purpur 26.2 | 2622 | Passed |
| Purpur 26.3 | 2642 (experimental) | Passed |

Native Paper builds between the two listed Paper versions were not separately tested. Untested future releases are not guaranteed compatible. The 26.3 results apply to the listed experimental upstream builds.

## Scope and limits

Each three-phase run creates and operates all 12 machines, checks 1,857 targeting samples, feedback, Dialog calls, command completion and language overrides. It then restores six saved placements with menus disabled, operates and deletes them, and confirms they remain absent after another restart. It also checks card reveals and feedback settlement, including doubled Blackjack stakes, early Crash cashouts and concurrent Plinko balls.

The probe uses a simulated player and real server entities. Client rendering, fonts, audio, latency, FPS and interaction feel require in-game acceptance. Optional Vault/AuthMe integrations, cross-version clients and high-density multiplayer performance are outside this compatibility matrix.

Local receipts are stored under `reports/vanilla-runtime/run-TIMESTAMP/result.json`; they record the server build, plugin digest and evidence for each phase. Release-run console logs are under `reports/beta3/`. These generated reports are not distributed with the plugin.

## Reproduce

### Multiplayer development build — 0.6.0-beta.4-SNAPSHOT

On September 29, 2026, the same development JAR passed 168 Java tests, 20 Python tests and all three clean-server phases on Paper 1.21.8 build 60, Purpur 26.2 build 2622 and Paper 26.3 build 42 (alpha). This is separate from the published beta.3 matrix above.

The added server probes exercise non-owner public play, exclusive player control, personal rounds and feedback, non-owner OP settings, occupied-machine stake rejection and Plinko batches across all 12 games. They verify logout handover and persisted Keno preferences/totals after restart. Unit tests cover preparation reservation, idle expiry, unfinished-session reset, corrupt-file preservation and retaining fresh in-memory data when the saved profile is stale.

Receipts are retained locally under `reports/multiplayer/final-results.json`. The probe uses simulated players and real server entities; actual Minecraft client multiplayer interaction and visual acceptance remain pending. Other versions in the beta.3 matrix have not been rerun for this development build.

```sh
mvn -B -ntp package
python -m pip install -r tools/requirements-dev.txt
python -m unittest discover -s tools -p "test_*.py"
```

Follow the [runtime probe instructions](../tools/vanilla-probe/README.md) with the desired prepared Paper/Purpur server. Inspect all three phase logs and `result.json`, and compare plugin digests to confirm that every server tested the same JAR.

To audit exported model surfaces:

```sh
python tools/vanilla-preview/audit_surfaces.py reports/vanilla-runtime/run-TIMESTAMP/plugins/CasinoVanillaProbe/preview-snapshots reports/vanilla-runtime/run-TIMESTAMP/surface-audit.json
```

GitHub Actions also runs Maven and Python checks for each pushed commit.
