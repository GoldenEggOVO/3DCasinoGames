# Editable languages

On first startup the plugin creates `plugins/3dcasino/config.yml` and `languages/en_US.yml`, `languages/zh_CN.yml`. English is the default for native Dialogs, machines and feedback. Select a filename without `.yml` using `language: en_US`. The language is server-wide.

## Edit and reload

Copy a generated file to `languages/my_language.yml`, edit it, and set `language: my_language`. Existing files are never overwritten. Back up custom translations before upgrading.

Run `/3dcasino reload-language` after editing. Console may always run it; players need `3dcasino.use` and `3dcasino.admin` (admin defaults to operators), and must pass the optional authentication check. Tab suggestions obey those permissions. This command reads only the language selection from disk; other configuration changes still require a restart.

Successful reload closes old menu sessions and refreshes existing machine labels without rebuilding geometry or ending a round. Dynamic busy-machine readouts refresh at their next safe update. Failed reload retains the complete previous language; check console diagnostics for the filename and message key.

## Keys, values and styles

Use stable keys and quoted strings (flat dotted keys or equivalent nested YAML):

```yaml
"menu.close": "<dark_gray>[ <red>Close Menu <dark_gray>]"
"models.showcase_button_play.0": "PLAY"
"machine.stake-saved": "<green>Practice stake set to {amount}."
```

Named placeholders such as `{amount}` must retain their spelling. A translation may omit a placeholder, but cannot invent one absent from bundled English. Ordinary parameter values are literal text: `<red>`, `&c` and `{another}` inside a value are not interpreted. `\n` in a double-quoted YAML value starts a new line.

Menus, chat, ActionBar and editable model labels use the same Adventure Component renderer. Allowed MiniMessage features are colors (including hex), decorations, reset, newline, gradient and rainbow. Click, hover, font, insertion and dynamic lookup tags are not allowed in language templates. Actions remain code-owned. Omitted closing style tags are allowed; explicit recognized closing tags must match an open style. Unknown angle-bracket command arguments such as `<game>` remain literal.

Existing `&` and `§` formatting accepts uppercase/lowercase codes, `&#RRGGBB` and expanded `§x§R§R§G§G§B§B`. Legacy colors reset earlier decorations; for example `&lBold &aGreen` makes only the first segment bold. Prefer MiniMessage for new text. Plain `Close` is now rendered literally; put the complete desired bracket/color style in `menu.close`.

Label fitting measures visible text, inherited bold, wide characters and explicit line breaks. Very long labels use an ellipsis and at most two lines instead of shrinking indefinitely. Exceptionally narrow areas may reduce the ellipsis further to remain within bounds. Metrics approximate the default Minecraft font; a client's custom font can differ. Keep button translations short. `models.<model-id>.<index>` changes text, not hitboxes or model geometry.

## Validation and fallback

Bundled English defines valid keys and placeholders. At startup, valid selected-file entries override editable English, then bundled English. Missing keys fall back normally. Non-string values, unknown keys, unknown placeholders and invalid recognized styles produce per-key warnings and are skipped. Missing files, malformed YAML and invalid locale names warn and fall back; language-folder I/O failure uses bundled English. User files remain unchanged.

Reload is transactional: **any warning in either editable English or the selected language rejects the candidate**. Missing keys alone are not warnings. Fix reported errors and reload again. Locale filenames accept letters, digits, `_` and `-`, starting with a letter, up to 64 characters.

## Developer contract

`Language.component(key, pairs...)` returns formatted Components for player-facing output. `Language.text(key, pairs...)` returns plain text for logs, errors and rule status. A caller may explicitly supply a Component parameter to retain its structure; never deserialize a player name or arbitrary parameter as MiniMessage. Language files cannot declare callbacks.

Translations do not change probabilities, practice stakes, payouts, ownership or action availability. Card ranks/suits and game/action IDs remain symbols or identifiers. External models may contain painted lettering outside this interface. See [Tabletop alignment](tabletop-alignment.md).
