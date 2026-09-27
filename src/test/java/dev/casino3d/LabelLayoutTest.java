package dev.casino3d;

import dev.casino3d.ui.LabelLayout;
import dev.casino3d.ui.MessageText;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LabelLayoutTest {
    @Test void boldAndMixedStylesFitWithinBounds() {
        assertEquals(28, LabelLayout.width(MessageText.render("<bold>WWWW")));
        assertEquals(26, LabelLayout.width(MessageText.render("WW<bold>WW")));
        for (String value : new String[] {"<bold>WWWW", "WW<bold>WW", "<bold>" + "W".repeat(300), "W\nW\nW"}) {
            var fit = LabelLayout.fit(MessageText.render(value), .02f, .2f);
            assertTrue(LabelLayout.width(fit.text()) * .025f * fit.scale() <= .02001f);
        }
    }
    @Test void formattingHasNoWidthAndWideCharactersHaveMoreWidth() {
        assertEquals(LabelLayout.width("PLAY"), LabelLayout.width(MessageText.plain(MessageText.render("<red>PLAY"))));
        assertTrue(LabelLayout.width("WWWW") > LabelLayout.width("iiii"));
        assertTrue(LabelLayout.width("中文") > LabelLayout.width("ii"));
        assertEquals(LabelLayout.width("PLAY"), LabelLayout.width("PLAY\ni"));
    }

    @Test void longTranslationsHaveBoundedSizeAndUseVisibleEllipsis() {
        var fit = LabelLayout.fit(MessageText.render("<red>" + "W".repeat(300)), .5f, .2f);
        assertTrue(fit.scale() >= .35f);
        assertTrue(MessageText.plain(fit.text()).endsWith("…"));
        assertTrue(LabelLayout.width(MessageText.plain(fit.text())) * .025f * fit.scale() <= .50001f);
    }
}
