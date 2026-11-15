package de.moinmorni.gipfelsturm.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Strecke der Woche: Wechsel Montag 0:00 deutscher Zeit, fest aus der Wochennummer, jede Strecke kommt dran. */
class WeeklyRotationTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private static Instant berlin(int y, int mo, int d, int h, int mi, int s) {
        return LocalDateTime.of(y, mo, d, h, mi, s).atZone(BERLIN).toInstant();
    }

    @Test
    void changesExactlyAtMondayMidnightBerlinWinter() {
        // 16.11.2026 ist ein Montag (Winterzeit, UTC+1)
        Instant sunday = berlin(2026, 11, 15, 23, 59, 59);
        Instant monday = berlin(2026, 11, 16, 0, 0, 0);
        assertEquals(WeeklyRotation.weekNumber(sunday) + 1, WeeklyRotation.weekNumber(monday));
        assertNotEquals(WeeklyRotation.index(sunday, 8), WeeklyRotation.index(monday, 8));
        assertEquals("2026-W46", WeeklyRotation.weekId(sunday));
        assertEquals("2026-W47", WeeklyRotation.weekId(monday));
        // In UTC ist der Wechsel schon Sonntag 23:00
        assertEquals(WeeklyRotation.weekNumber(monday), WeeklyRotation.weekNumber(Instant.parse("2026-11-15T23:00:00Z")));
        assertEquals(WeeklyRotation.weekNumber(sunday), WeeklyRotation.weekNumber(Instant.parse("2026-11-15T22:59:59Z")));
    }

    @Test
    void changesAtMondayMidnightBerlinSummer() {
        // 15.06.2026 ist ein Montag (Sommerzeit, UTC+2)
        assertEquals(WeeklyRotation.weekNumber(Instant.parse("2026-06-14T21:59:59Z")) + 1,
                WeeklyRotation.weekNumber(Instant.parse("2026-06-14T22:00:00Z")));
    }

    @Test
    void sameWholeWeekIncludingDstChange() {
        // Woche mit Zeitumstellung (So 25.10.2026): Montag 19.10. bis Sonntag 25.10. dieselbe Strecke
        int first = WeeklyRotation.index(berlin(2026, 10, 19, 0, 0, 0), 8);
        for (int day = 19; day <= 25; day++) {
            for (int h = 0; h < 24; h += 5) {
                assertEquals(first, WeeklyRotation.index(berlin(2026, 10, day, h, 30, 0), 8), day + ". " + h + " Uhr");
            }
        }
        assertNotEquals(first, WeeklyRotation.index(berlin(2026, 10, 26, 0, 0, 1), 8));
    }

    @Test
    void deterministicAndCoversEveryCourse() {
        Instant start = berlin(2026, 1, 5, 12, 0, 0);
        Set<Integer> seen = new HashSet<>();
        int last = -1;
        for (int w = 0; w < 8; w++) {
            Instant t = start.plusSeconds(w * 7L * 24 * 3600);
            int i = WeeklyRotation.index(t, 8);
            assertEquals(i, WeeklyRotation.index(t, 8));
            assertNotEquals(last, i, "zweimal hintereinander dieselbe Strecke");
            last = i;
            seen.add(i);
        }
        assertEquals(8, seen.size());
        // Reihenfolge wechselt die Schwierigkeit: nie zwei Nachbarn (gleiche Stufe) direkt hintereinander
        int[] order = WeeklyRotation.order(8);
        for (int k = 1; k < order.length; k++) {
            assertNotEquals(order[k - 1] / 2, order[k] / 2);
        }
    }

    @Test
    void nextChangeIsNextMonday() {
        assertEquals(berlin(2026, 11, 16, 0, 0, 0), WeeklyRotation.nextChange(berlin(2026, 11, 15, 10, 0, 0)));
        assertEquals(berlin(2026, 11, 23, 0, 0, 0), WeeklyRotation.nextChange(berlin(2026, 11, 16, 0, 0, 0)));
    }
}
