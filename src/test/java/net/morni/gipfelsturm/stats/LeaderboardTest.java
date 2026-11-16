package net.morni.gipfelsturm.stats;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Bestenliste und gespeicherte Spielerdaten. */
class LeaderboardTest {

    private final UUID a = UUID.randomUUID();
    private final UUID b = UUID.randomUUID();
    private final UUID c = UUID.randomUUID();

    @Test
    void onlyBetterTimesCount() {
        Leaderboard l = new Leaderboard();
        assertTrue(l.submit(a, "Anna", 30_000, 1));
        assertFalse(l.submit(a, "Anna", 31_000, 2));
        assertTrue(l.submit(a, "Anna", 29_000, 3));
        assertEquals(29_000, l.of(a).ms());
        assertEquals(1, l.size());
    }

    @Test
    void sortedFastestFirstTiesByWhoWasFirst() {
        Leaderboard l = new Leaderboard();
        l.submit(a, "Anna", 30_000, 5);
        l.submit(b, "Bert", 25_000, 6);
        l.submit(c, "Carl", 30_000, 4);
        List<Leaderboard.Entry> top = l.top(10);
        assertEquals(List.of("Bert", "Carl", "Anna"), top.stream().map(Leaderboard.Entry::name).toList());
        assertEquals(1, l.rank(b));
        assertEquals(3, l.rank(a));
        assertEquals(0, l.rank(UUID.randomUUID()));
        assertEquals("Bert", l.record().name());
        assertEquals(2, l.top(2).size());
    }

    @Test
    void renameKeepsTime() {
        Leaderboard l = new Leaderboard();
        l.submit(a, "Anna", 30_000, 1);
        l.submit(a, "Anni", 40_000, 2);
        assertEquals("Anni", l.top(1).get(0).name());
        assertEquals(30_000, l.top(1).get(0).ms());
    }

    @Test
    void playerRecordRoundTrip() {
        PlayerRecord r = new PlayerRecord(a, "Anna");
        r.attempts = 7;
        r.falls = 12;
        r.finishes = 3;
        PlayerRecord.CourseStats s = r.course("almwiese");
        s.best = 21_449;
        s.bestWhen = 1_794_750_707_156L;
        s.splits.addAll(List.of(7_467L, 14_050L, 17_752L));
        s.finishes = 2;
        r.enterWeek("2026-W47", "almwiese");
        r.weekBest = 22_000;
        r.weekDone = true;
        r.hideOthers = true;
        r.ghost = false;
        Map<String, Object> m = r.toMap();
        PlayerRecord back = PlayerRecord.fromMap(a, m);
        assertEquals("Anna", back.name);
        assertEquals(7, back.attempts);
        assertEquals(12, back.falls);
        assertEquals(21_449, back.best("almwiese"));
        assertEquals(List.of(7_467L, 14_050L, 17_752L), back.course("almwiese").splits);
        assertTrue(back.weeklySummit("2026-W47"));
        assertFalse(back.weeklySummit("2026-W48"));
        assertTrue(back.hideOthers);
        assertFalse(back.ghost);
    }

    @Test
    void newWeekForgetsWeeklyValues() {
        PlayerRecord r = new PlayerRecord(a, "Anna");
        r.enterWeek("2026-W47", "almwiese");
        r.weekBest = 20_000;
        r.weekDone = true;
        r.enterWeek("2026-W47", "almwiese");
        assertTrue(r.weekDone);
        r.enterWeek("2026-W48", "felsgrat");
        assertFalse(r.weekDone);
        assertEquals(0, r.weekBest);
        assertEquals("felsgrat", r.weekCourse);
    }
}
