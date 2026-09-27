# Installation and upgrade

## Requirements

- Paper / Purpur 26.2 or 26.3, Java 25. Use the same JAR for both versions. The verified 26.3 server builds are experimental; see [the compatibility matrix](verification.md#cross-version-compatibility).
- One `3dcasino-0.6.0-beta.2.jar` from the [Beta release](https://github.com/GoldenEggOVO/3DCasinoGames/releases/tag/v0.6.0-beta.2).

The JAR includes vanilla BlockDisplay/TextDisplay/Interaction machines. No resource pack or client mod is required. Native Dialog requires a compatible Minecraft client. Cross-version proxies and Bedrock bridges need their own client acceptance tests.

## Fresh install

Stop the server, back up existing data/worlds, install one JAR and start. The generated `plugins/3dcasino/config.yml` selects:

```yaml
menu-enabled: true
language: en_US
```

Use `/3dcasino` or `/3dcasino create <game>`. Operators have `3dcasino.machine`; grant this with your permissions plugin for other builders. Players also need `3dcasino.use` (default true). Right-click buttons to play; Shift + right-click opens settings.

Game IDs: `blackjack`, `mines`, `crash`, `plinko`, `slots`, `duck_race`, `wheel_of_fortune`, `money_wheel`, `penguin_cross`, `keno`, `hilo`, `dragon_tower`.

## Breaking change from 0.5.x

The runtime identity is now `3dcasino`. The old `ServerCasino` data folder is neither imported nor overwritten. Old `/casino`, `/casino-demo`, permissions and Java package names are removed. Existing API consumers must recompile against `dev.casino3d`.

For a fresh installation, remove the old JAR while stopped and keep its data backup. Create new machines in the new namespace. There is no migration command or compatibility adapter.

If you need existing placements, work on a backup in an isolated server first. After the new plugin has generated its data folder, stop it. Copy the old `placements.json` and `machines/` into `plugins/3dcasino/` only after updating explicit model references from `casino:` to `3dcasino:`. Do not overwrite existing new placements. Preserve ownership UUIDs, world UUIDs and stakes. Built-in definitions without explicit model overrides use the new namespace directly.

Financial records need separate care: preserve `rounds/` unchanged; the former `casino-rounds/` directory is now `game-rounds/`. Copy records only while both versions are stopped, preserve their bytes, and do not merge competing records or discard pending transfers. Keep the original backup. Confirm restored machines and settlement states before installing on a live server.

Config files are not migrated. Use the freshly generated config and explicitly reapply your chosen settings. This release does not change other plugins or their resource packs.

## Optional integrations

| Integration | Purpose | When absent |
| --- | --- | --- |
| Vault + economy provider | Economy API / settlement records | Free machines work; unavailable transactions do not report success |
| AuthMe | Enforce login when installed | No login restriction from this integration |
| `EconomyProvider` | Custom economy service | Vault adapter or unavailable result |
| `MachineModelResolver` | Explicitly configured external custom item references | Vanilla material fallback |

Built-in models always use geometry bundled in the JAR. Existing `machine-appearance` entries in old configs are ignored; remove them when updating the file. An explicitly configured external custom item may need its own client assets, managed by its provider.

## Files and settings

- `config.yml`: menu switch and language selection.
- `languages/en_US.yml`, `languages/zh_CN.yml`: editable runtime messages; see [language configuration](languages.md).
- `machines/*.yml`: optional custom machine definitions; built-in machines need no files.
- `placements.json`: saved machines, owners, transforms, definition snapshots and practice stakes.
- `rounds/`, `game-rounds/`: persisted settlement records when present.

Set `menu-enabled: false` and restart to disable Dialog. Commands, buttons, persistence and console reconciliation remain available. Definition reload affects new machines; saved machines retain their definition snapshots. Active rounds and animations are not restored after restart.

Physical machines never deduct or award economy balances. Back up all plugin data and the matching worlds. Validate creation, interaction, deletion and restart recovery in an isolated server first. Minecraft visuals, fonts, frame rate and interaction feel require client acceptance.
