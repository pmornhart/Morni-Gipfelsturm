package net.morni.gipfelsturm.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.morni.gipfelsturm.courses.World;
import org.junit.jupiter.api.Test;

/** Medaillen-Grenzen: Gipfel ≤ Gold ≤ Silber ≤ Bronze, genau an der Grenze zählt die Medaille noch. */
class MedalTest {

    private static final Course.Medals M = new Course.Medals(20_000, 26_000, 36_000, 52_000);

    @Test
    void bestMedalForATime() {
        assertEquals(Medal.GIPFEL, Medal.of(19_000, M));
        assertEquals(Medal.GIPFEL, Medal.of(20_000, M));
        assertEquals(Medal.GOLD, Medal.of(20_001, M));
        assertEquals(Medal.GOLD, Medal.of(26_000, M));
        assertEquals(Medal.SILBER, Medal.of(30_000, M));
        assertEquals(Medal.BRONZE, Medal.of(52_000, M));
        assertNull(Medal.of(52_001, M));
        assertNull(Medal.of(0, M));
    }

    @Test
    void nextMedal() {
        assertEquals(Medal.BRONZE, Medal.next(null));
        assertEquals(Medal.SILBER, Medal.next(Medal.BRONZE));
        assertEquals(Medal.GIPFEL, Medal.next(Medal.GOLD));
        assertNull(Medal.next(Medal.GIPFEL));
    }

    @Test
    void namesInBothLanguages() {
        assertEquals("Gipfel", Medal.GIPFEL.name("de"));
        assertEquals("Summit", Medal.GIPFEL.name("en"));
        assertEquals("Silber", Medal.SILBER.name("de"));
        assertEquals("Silver", Medal.SILBER.name("en"));
    }

    @Test
    void autoMedalsGrowWithTheCourse() {
        // Gold, Silber, Bronze sind feste Vielfache der Gipfel-Zeit, auf halbe Sekunden aufgerundet.
        for (Course c : World.create().courses()) {
            Course.Medals m = c.medals;
            assertTrue(m.gold() >= m.gipfel() * 1.3 - 1 && m.gold() <= m.gipfel() * 1.3 + 500, c.id);
            assertTrue(m.bronze() >= m.gipfel() * 2.6 - 1, c.id);
            assertEquals(0, m.gipfel() % 500, c.id + " auf halbe Sekunden");
        }
    }
}
