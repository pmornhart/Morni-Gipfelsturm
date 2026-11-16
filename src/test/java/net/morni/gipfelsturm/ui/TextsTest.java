package net.morni.gipfelsturm.ui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Jeder Text gibt es auf Deutsch und Englisch, mit denselben Platzhaltern, und er lässt sich lesen. */
class TextsTest {

    private static final Pattern PLACEHOLDER = Pattern.compile("<(time|course|n|total|delta|best|medal|diff|rank|"
            + "player|what|leader|sec|place|list|left|badge|state|date|finishes|attempts|falls|medals|d|h|m)>");

    @Test
    void everyTextInBothLanguages() {
        for (Map.Entry<String, String[]> e : Texts.all().entrySet()) {
            String de = e.getValue()[0];
            String en = e.getValue()[1];
            assertFalse(de.isBlank(), e.getKey());
            assertFalse(en.isBlank(), e.getKey());
            assertTrue(placeholders(de).equals(placeholders(en)), "Platzhalter unterschiedlich: " + e.getKey());
            // MiniMessage muss sich lesen lassen (Platzhalter stehen leer da)
            Texts.MM.deserialize(de);
            Texts.MM.deserialize(en);
        }
    }

    @Test
    void noForeignBrandsInTexts() {
        for (Map.Entry<String, String[]> e : Texts.all().entrySet()) {
            for (String t : e.getValue()) {
                String low = t.toLowerCase(java.util.Locale.ROOT);
                for (String banned : new String[] {"mojang", "hypixel", "nintendo", "mario", "minecraft"}) {
                    assertFalse(low.contains(banned), e.getKey() + " nennt " + banned);
                }
            }
        }
    }

    private static Set<String> placeholders(String s) {
        Set<String> out = new TreeSet<>();
        Matcher m = PLACEHOLDER.matcher(s);
        while (m.find()) {
            out.add(m.group(1));
        }
        return out;
    }
}
