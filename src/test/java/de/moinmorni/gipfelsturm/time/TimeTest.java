package de.moinmorni.gipfelsturm.time;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import de.moinmorni.gipfelsturm.course.Box;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Stoppuhr, Zwischenzeiten und Anzeige. */
class TimeTest {

    @Test
    void clockShowsMinutesSecondsMillis() {
        assertEquals("0:00,000", TimeFormat.clock(0, "de"));
        assertEquals("0:05,250", TimeFormat.clock(5250, "de"));
        assertEquals("1:05,007", TimeFormat.clock(65007, "de"));
        assertEquals("1:05.007", TimeFormat.clock(65007, "en"));
        assertEquals("1:00:00,001", TimeFormat.clock(3_600_001, "de"));
    }

    @Test
    void differencesHaveSigns() {
        assertEquals("+0,452", TimeFormat.diff(452, "de"));
        assertEquals("−1,203", TimeFormat.diff(-1203, "de"));
        assertEquals("±0,000", TimeFormat.diff(0, "de"));
        assertEquals("+1:02,500", TimeFormat.diff(62500, "de"));
        assertEquals("−0.050", TimeFormat.diff(-50, "en"));
    }

    @Test
    void limitsAreShort() {
        assertEquals("0:45", TimeFormat.limit(45000, "de"));
        assertEquals("1:05,5", TimeFormat.limit(65500, "de"));
        assertEquals("0:23.25", TimeFormat.limit(23250, "en"));
    }

    @Test
    void runClockCountsTicksAndFractions() {
        RunClock c = new RunClock(100, 0.5);
        assertEquals(0, c.at(100, 0.5));
        assertEquals(50, c.at(101, 0.5));
        assertEquals(25, c.at(101, 0.0));
        assertEquals(1000, c.at(120, 0.5));
        assertEquals(1013, c.at(120, 0.76));
        // Anzeige während des Laufs in ganzen Ticks
        assertEquals(500, c.now(110));
        // Kein negativer Wert
        assertEquals(0, new RunClock(10, 0.9).at(10, 0.1));
    }

    @Test
    void entryFractionFindsTheMomentOfArrival() {
        Box zone = new Box(10, 100, 0, 11, 101, 1);
        // Von x=9 nach x=11 in einem Tick: Eintritt bei der Hälfte
        assertEquals(0.5, zone.entry(9, 100.5, 0.5, 11, 100.5, 0.5), 1e-9);
        // schon drin: 0
        assertEquals(0.0, zone.entry(10.5, 100.5, 0.5, 10.6, 100.5, 0.5), 1e-9);
        // daneben: −1
        assertEquals(-1, zone.entry(9, 105, 0.5, 11, 105, 0.5), 1e-9);
        // von oben landen: Eintritt, sobald die Höhe stimmt (101 liegt auf halbem Weg von 103 nach 99)
        assertEquals(0.5, zone.entry(10.5, 103, 0.5, 10.5, 99, 0.5), 1e-9);
    }

    @Test
    void splitsCompareWithBest() {
        List<Long> best = List.of(10_000L, 20_000L, 30_000L);
        assertEquals(-500L, Splits.delta(best, 1, 19_500));
        assertEquals(1200L, Splits.delta(best, 2, 31_200));
        assertNull(Splits.delta(best, 3, 40_000));
        assertNull(Splits.delta(List.of(), 0, 5_000));
        assertEquals("#5fd068", Splits.colour(-1));
        assertEquals("#ff6b5b", Splits.colour(1));
        assertEquals("#f5c542", Splits.colour(0));
    }
}
