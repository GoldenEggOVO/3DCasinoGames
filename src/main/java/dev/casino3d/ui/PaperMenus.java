package dev.casino3d.ui;

import dev.casino3d.Language;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/** Native renderer; callbacks are code-owned and always re-enter the main thread. */
public final class PaperMenus {
    public static final Duration LIFETIME = Duration.ofMinutes(5);
    private PaperMenus() {}

    public static void open(Plugin plugin, Player player, MenuView view,
                            BiConsumer<String, Map<String, String>> handler) {
        if (!plugin.getConfig().getBoolean("menu-enabled", true)) {
            player.sendMessage(Language.component("menu.disabled"));
            return;
        }
        BiConsumer<String, Map<String, String>> dispatch = (action, values) -> {
            if (!plugin.isEnabled()) return;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (plugin.isEnabled() && player.isOnline()
                        && plugin.getConfig().getBoolean("menu-enabled", true))
                    handler.accept(action, values);
            });
        };
        var body = view.body().stream().map(b -> DialogBody.plainMessage(b.text(), b.width())).toList();
        var inputs = view.inputs().stream().map(i -> DialogInput.text(i.id(), 300,
                i.label(), true, i.value(), i.maxLength(), null)).toList();
        var buttons = view.buttons().stream().map(b -> button(player, b, view, dispatch)).toList();
        var base = DialogBase.create(view.title(), null, true, false,
                DialogBase.DialogAfterAction.NONE, body, inputs);
        player.showDialog(Dialog.create(factory -> factory.empty().base(base).type(
                DialogType.multiAction(buttons, button(player, view.exit(), view, dispatch), view.columns()))));
    }

    private static ActionButton button(Player player, MenuView.Button button, MenuView view,
                                        BiConsumer<String, Map<String, String>> dispatch) {
        DialogAction action = button.action() == null ? null : DialogAction.customClick((response, audience) -> {
            if (!(audience instanceof Player actor) || !actor.getUniqueId().equals(player.getUniqueId())) return;
            Map<String, String> values = new HashMap<>();
            for (var input : view.inputs()) {
                String value = response.getText(input.id());
                if (value != null) {
                    if (value.length() > input.maxLength()) return;
                    values.put(input.id(), value);
                }
            }
            dispatch.accept(button.action(), Map.copyOf(values));
        }, ClickCallback.Options.builder().uses(1).lifetime(LIFETIME).build());
        return ActionButton.create(button.text(), button.tooltip(), button.width(), action);
    }
}
