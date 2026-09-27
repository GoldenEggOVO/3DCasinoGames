package dev.casino3d;

import dev.casino3d.ui.MessageText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MessageTextTest {
    @Test void validatesClosingStylesWithoutRequiringEveryOpeningTagToClose() {
        assertThrows(IllegalArgumentException.class, () -> MessageText.validate("<red>Hello</blue>"));
        assertThrows(IllegalArgumentException.class, () -> MessageText.validate("Hello</#oops>"));
        assertThrows(IllegalArgumentException.class, () -> MessageText.validate("<red>Hello</red:blue>"));
        assertDoesNotThrow(() -> MessageText.validate("<red>Hello <bold>world</bold> <game>"));
        assertDoesNotThrow(() -> MessageText.validate("<red><bold>Hello</red>"));
    }
    @Test void parametersAreLiteralAndNeverBecomeActionsOrRecursiveParameters() {
        String value = "<click:run_command:'/op me'><red>&c{name}\\$";
        var actual = MessageText.render("<green>Hello {name}", "name", value);
        assertEquals("Hello " + value, MessageText.plain(actual));
        assertFalse(actual.toString().contains("RunCommandClickEvent"));
        assertEquals("X {name}", MessageText.plain(MessageText.render("{a} {b}",
                "a", Component.text("X", NamedTextColor.RED), "b", "{name}")));
    }

    @Test void legacyColorsResetDecorationsAndAcceptBothPrefixesAndUppercase() {
        var actual = MessageText.render("&LA§CA&rB");
        var expected = Component.empty().append(Component.text("A").decorate(TextDecoration.BOLD))
                .append(Component.text("A", NamedTextColor.RED)).append(Component.text("B"));
        assertEquals(expected.compact(), actual.compact());
        assertEquals("Hello", MessageText.plain(MessageText.render("&#00ff00Hello")));
    }

    @Test void plainOutputPreservesLiteralParametersButRemovesTemplateFormatting() {
        assertEquals("Hi <red>name", MessageText.plain(MessageText.render("<green>Hi {name}", "name", "<red>name")));
        assertEquals("<game>", MessageText.plain(MessageText.render("<game>")));
        assertThrows(IllegalArgumentException.class, () -> MessageText.validate("<click:run_command:'/op me'>X"));
        assertThrows(IllegalArgumentException.class, () -> MessageText.validate("<#oops>Text"));
        assertThrows(IllegalArgumentException.class, () -> MessageText.validate("<red"));
    }
}
