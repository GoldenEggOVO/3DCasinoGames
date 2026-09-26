# Editable languages

On first startup, the plugin creates:

```text
plugins/3dcasino/
  config.yml
  languages/
    en_US.yml
    zh_CN.yml
```

The default is English, including native Dialog menus, machine text and command feedback. To select Chinese, set `language: zh_CN` in `config.yml` and restart. The filename is used without `.yml`. The setting is server-wide, not per-player.

## Customize

Edit either generated file, or copy one to a new filename such as `my_language.yml` and set `language: my_language`. Restart after editing. Existing language files are never overwritten during startup. Back them up before upgrading.

Entries use stable keys and quoted strings:

```yaml
"menu.close": "<dark_gray>[ <red>Close Menu <dark_gray>]"
"models.cabinet_button_play.0": "PLAY"
"models.showcase_button_play.0": "PLAY"
"machine.stake-saved": "§aPractice stake set to {amount}."
```

Keep named placeholders such as `{amount}`, `{game}` and `{error}` when translating an entry. Their spelling is part of the interface. Values inserted into placeholders are not recursively expanded. Use `\n` inside double quotes for a line break. Keep command names, permission nodes, game IDs and callback/action identifiers unchanged; those are not translated.

Dialog entries use MiniMessage formatting by default and continue to accept existing `&` color codes. The close button uses `<dark_gray>` brackets and `<red>` text; older `Close` and `关闭` values still render with that style. Chat messages use `§` color codes. Physical machine labels are plain text; their size is fitted to the available label area, so short translations are easier to read. Keys `models.<model-id>.<index>` refer to each label in an embedded vanilla model. They do not modify hitboxes or gameplay.

Missing or non-string entries fall back to the editable `en_US.yml`, then bundled English. A missing file, invalid filename or malformed YAML logs a warning and uses English fallback; your file remains unchanged. Filename selection accepts letters, digits, `_` and `-`, starting with a letter; it cannot traverse parent folders.

The English file shipped in the JAR is the complete key reference. New keys added in later versions still work through fallback even if your existing file does not contain them.

## Scope

Editable strings cover menus, command feedback, machine labels/readouts, game result text and expected user errors. Numbers, card ranks/suits and game/action IDs are identifiers or symbols. Language changes do not change odds, stakes, payouts, saved ownership or action availability.

Built-in machines use editable TextDisplay labels. External custom models may contain painted lettering that language files cannot change.
