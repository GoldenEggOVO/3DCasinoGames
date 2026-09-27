package dev.casino3d;

import dev.casino3d.ui.MenuView;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MenuViewTest {
    private final Component text = Component.text("Label");
    private final MenuView.Button close = new MenuView.Button(text, null, 230, "close");

    @Test void rejectsInvalidInputsBeforeRendering() {
        assertThrows(IllegalArgumentException.class, () -> new MenuView.Input("a", text, "123", 2));
        assertThrows(IllegalArgumentException.class, () -> new MenuView.Body(text, 0));
        var input = new MenuView.Input("stake", text, "10", 3);
        assertThrows(IllegalArgumentException.class,
                () -> new MenuView(text, List.of(), List.of(input, input), List.of(), close, 1));
    }

    @Test void sourceListChangesCannotChangeAnAlreadyBuiltMenu() {
        var buttons = new ArrayList<MenuView.Button>();
        buttons.add(close);
        var view = new MenuView(text, List.of(), List.of(), buttons, close, 1);
        buttons.clear();
        assertEquals(1, view.buttons().size());
        assertThrows(UnsupportedOperationException.class, () -> view.buttons().clear());
    }
}
