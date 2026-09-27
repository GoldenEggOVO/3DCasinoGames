# Architecture and extension interfaces

The entry point is `dev.casino3d.CasinoPlugin`. Round classes in `game/<game>/` own rules and outcomes; machine controllers own game-specific entities and animations. `machine/` manages creation, ray targeting, button presses, menu entry points and cleanup. `model/` owns immutable appearance definitions, validation and reloading.

`CasinoRuntime` owns persisted settlement services and scheduled settlement work, which remain active when menus are disabled. `CasinoMenus` manages machine menus and sessions. `RoundRecoveryMenu` handles existing saved rounds; it cannot start new menu rounds. Menu objects are not created when menus are disabled. `economy.Wallet` is the internal settlement contract; `api.EconomyProvider` remains the public economy interface.

Preserve JSON fields and transaction recovery semantics. Do not replace persisted transaction state with practice-round state or bypass confirmation callbacks when calling an economy provider.

## Economy interface

Other plugins can register `dev.casino3d.api.EconomyProvider` through Bukkit's `ServicesManager`. The interface uses player UUIDs and integer minor currency units, and distinguishes success, failure, unavailability and uncertain results. See the interface Javadoc for method contracts. The Vault adapter registers at the lowest priority so an explicitly registered provider can override it.

Practice machines do not call economy services. Economy-backed starts must be rejected when the provider is unavailable; uncertain transfers remain pending reconciliation. Vault and local files cannot provide a shared atomic transaction. Automatic retries must not be treated as proof of exactly-once payment.

## Model resolver interface

Register `dev.casino3d.api.MachineModelResolver` and implement `ItemStack resolve(String namespacedModel)`. Only explicitly configured external namespaces invoke this service. Built-in `3dcasino:` models always use bundled vanilla display geometry; `material:` references use vanilla materials directly. A `null` result falls back to a vanilla material. Returned items are copied rather than modifying the provider's cached instance.

See [custom models](custom-models.md) for definitions. Register services and manipulate machines on the server main thread. Avoid blocking network or disk operations inside a resolver. The API is in beta; document compatibility implications when changing public interfaces.

## Regression tests and maintenance

`Frozen*Round` test classes preserve the 0.3.3 behavior baseline for action-by-action comparisons. They are not included in the plugin JAR. Changes to game rules need explicit behavioral tests; do not silently change the frozen baseline to make comparisons pass.

Bundled geometry lives in `vanilla-models.json`. Follow the [model tooling instructions](custom-models.md) and perform client visual acceptance after geometry edits. Built-in button positions and dimensions come from `BuiltinLayouts` and controller-specific vanilla layout overrides. Tests must exercise the actual definitions.

`machine/PlacementStore` saves placements and definition snapshots separately from rounds, syncing a temporary file before atomic replacement. `MachineManager` indexes machines by owner and game, distinguishing saved records from loaded entities. Chunk or world unloads remove loaded entities; explicit deletion also updates saved records.

`Language` loads messages with bundled English defaults and creates only missing language files. Menus and entity labels use stable keys and named placeholders. Translate service errors at presentation boundaries; translations never participate in action, probability or payment decisions. See [languages](languages.md).

## Text and menus

`ui.MessageText` parses template styles only. Ordinary parameters use `Tag.inserting(Component.text(...))`. `Language.component` is the formatted display boundary; `Language.text` provides plain text for logs and rule status. Never parse substituted dynamic text again as a template.

`ui.MenuView` describes immutable titles, body text, inputs, buttons and the close action. `PaperMenus` converts it to Paper Dialog without generating intermediate YAML. `MenuSessions` uses a monotonic clock, player UUIDs and single-use tokens. Callbacks recheck plugin/menu state, main-thread execution, permissions and machine-specific conditions. Sessions expire after five minutes and are cleared on logout, reload and shutdown.

`VanillaDisplay.sync` returns early when the model, language, position, transform and glow state are unchanged. Language reload updates existing TextDisplays; controllers refresh dynamic readouts at safe points. Saved definitions are validated by their record constructors, so JSON restoration cannot bypass YAML validation.
