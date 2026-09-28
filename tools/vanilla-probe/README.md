# Cross-version runtime probe

Use JDK 25 and a prepared Paper/Purpur template containing an accepted `eula.txt`, the bootstrap JAR, `libraries/`, `cache/` and `versions/`. Build the plugin once with `mvn package`, then run the same artifact against each template.

```powershell
python tools/vanilla-probe/run.py --server-template PATH_TO_1218_TEMPLATE --server-version 1.21.8 --server-jar server.jar --port 25597 --java-home PATH_TO_JDK25
python tools/vanilla-probe/run.py --server-template PATH_TO_262_TEMPLATE --server-version 26.2 --server-jar purpur-2622.jar --port 25598 --java-home PATH_TO_JDK25
python tools/vanilla-probe/run.py --server-template PATH_TO_263_TEMPLATE --server-version 26.3 --server-jar purpur-2642.jar --port 25599 --java-home PATH_TO_JDK25
python tools/vanilla-probe/run.py --server-template PATH_TO_PAPER263_TEMPLATE --server-version 26.3 --server-jar paper-26.3-42.jar --port 25600 --java-home PATH_TO_JDK25
```

Each invocation creates a new loopback-only server under `reports/vanilla-runtime/`. It copies only the plugin and disposable probe, creates a fresh test world, and stops the server after each of three phases. Each invocation has isolated probe compilation outputs. Concurrent runs must use different ports and the same unchanged plugin artifact.

The probe asserts the actual Minecraft version before testing machines. `result.json` records the requested server version, bootstrap and plugin digests, exit codes and pass markers. Compare `tested_jar_sha256` across runs to verify that the same JAR was tested. A compilation failure, incorrect runtime version, missing pass marker or nonzero server exit is a failure.

Coverage includes all 12 machines, native Dialog construction/calls with a simulated player, permission-filtered command completion, display state, ray targeting, feedback settlement, menus disabled, custom language fallback, saved placements and deletion across restarts. This does not replace real-client visual, audio or mouse-interaction acceptance.
