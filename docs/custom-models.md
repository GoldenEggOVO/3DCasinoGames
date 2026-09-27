# Custom machine appearance

Definitions configure appearance and layout for existing games. They do not execute scripts or add game rules. Built-in models use vanilla display geometry bundled in the JAR. Optional resolver services can supply explicitly configured external item models.

Built-in model IDs resolve to display geometry; `material:` references use the specified vanilla material directly. Other IDs fall back to vanilla materials when no external resolver supplies them. Custom coordinates, buttons and saved definition snapshots remain supported. The plugin does not provide or require a resource pack.

Defaults come from `src/main/java/dev/casino3d/model/BuiltinLayouts.java` and `MachineDefinition.builtin(game)`. Put external definitions in `plugins/3dcasino/machines/*.yml`, one skin per file. Omitted fields inherit the game's built-in definition.

## Example

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

Copy [emerald-blackjack.yml](emerald-blackjack.yml) into that directory to try the example. It replaces the cabinet model with an emerald block and adds a small emerald-block decoration. This demonstrates material replacement, not a finished alternative cabinet. It is not enabled automatically.

If you use an external model ID such as `my_pack:my_blackjack`, a resolver service must supply the item. Its administrator manages any client assets; machine definitions do not upload resources.

Run `/3dcasino reload-models`, then `/3dcasino create blackjack emerald-blackjack`. Reload validates all files before replacing the registry. Failure preserves the previous valid definitions. Existing machines retain their definition snapshots; remove and recreate them to apply a new appearance.

## Fields

| Field | Meaning |
| --- | --- |
| `schema-version` | Currently the integer `1` |
| `id` / `game` | Unique skin ID / existing game ID |
| `models` | Logical model names mapped to resource IDs; defaults use `3dcasino:<logical-name>` |
| `anchors.body` | Local cabinet transform |
| `anchors.playfield` | Shared transform for gameplay coordinates, including cards, balls and wheels |
| `buttons` | Overrides keyed by actions supported by the game |
| `parts` | Additional static parts with `model`, `position`, `rotation` and `scale` |
| `settings-bounds` | Settings-menu ray region: `[minX,minY,minZ,maxX,maxY,maxZ]` |

Buttons accept `position: [x,y,z]`, `rotation: [pitch,yaw,roll]`, `width`, `height`, `depth`, `size` and `press`. Width, height and depth define the hit region; size controls appearance; press is the travel distance along the button face normal. Use supported actions and default dimensions from the game's built-in definition. Buttons, cabinet and playfield transform independently: changing the body does not reposition buttons automatically.

## Coordinates and validation

Positions use local coordinates in block units, with the placement yaw providing world orientation. Built-in cabinets use their base as the origin and positive Z as the front. Model JSON uses `8 + physical coordinate × 4`, together with FIXED ItemDisplay scale 4; external models may need different sizing. Rotation arrays use pitch/yaw/roll in degrees. Quaternion composition is Z × Y × X: points rotate around X, then Y, then Z, followed by uniform scale and translation. Model JSON display rotation is separate from machine configuration rotation.

All numeric coordinates must be finite:

| Value | Allowed range |
| --- | --- |
| Local position, each axis | `[-64, 64]` |
| Rotation, each axis | `[-360, 360]` degrees |
| Uniform scale | `[0.001, 32]` |
| Button width / height / depth / size | `[0.001, 8]` |
| Button press | `[0, 1]` |

Resource IDs must satisfy `NamespacedKey` syntax. After `material:`, use an uppercase Bukkit Material that is non-air and can be represented as an item. Duplicate IDs, unknown games/actions and invalid values report the relevant file and field. Check placement in four directions, card/ball alignment, button targeting and press direction in a real client.

## Bundled models and development tools

`src/main/resources/vanilla-models.json` contains 110 models, including machine parts, 52 cards and a card back. They use vanilla BlockDisplay/TextDisplay entities and materials.

After geometry edits, run Maven and Python checks and use the [isolated runtime probe](verification.md) to export actual display snapshots. Scripts in `tools/vanilla-preview/` render those snapshots. README gallery images are geometry renders, not Minecraft screenshots, and do not replace client acceptance.

```sh
mvn -B -ntp package
python -m pip install -r tools/requirements-dev.txt
python -m unittest discover -s tools -p "test_*.py"
```

`python tools/package-source.py` builds the public source archive with geometry, documentation and tests. It excludes build outputs, runtime reports and private local assets. Explicit external item models are managed by their providers.

`python tools/refine_wheels.py` regenerates only wheel-specific geometry. After changes, check machine orientations, button targeting and moving-part alignment.
