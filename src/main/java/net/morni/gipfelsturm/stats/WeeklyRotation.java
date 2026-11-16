package net.morni.gipfelsturm.stats;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;

/**
 * Strecke der Woche: wechselt jeden Montag um 0:00 Uhr deutscher Zeit (Europe/Berlin). Welche Strecke dran ist, folgt
 * fest aus der Nummer der Woche – auf jedem Server und nach jedem Neustart dieselbe – in einer gemischten Reihenfolge, so
 * dass in {@code n} Wochen jede Strecke einmal dran ist.
 */
public final class WeeklyRotation {

    public static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    /** Woche 0: Montag, 5. Januar 2026. */
    private static final LocalDate EPOCH = LocalDate.of(2026, 1, 5);

    private WeeklyRotation() {
    }

    /** Fortlaufende Nummer der Woche (seit Woche 0, Montag 0:00 Berlin). */
    public static long weekNumber(Instant now) {
        LocalDate day = now.atZone(BERLIN).toLocalDate();
        LocalDate monday = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        return Math.floorDiv(ChronoUnit.DAYS.between(EPOCH, monday), 7);
    }

    /** Welche Strecke (Index 0…n−1) in dieser Woche dran ist. */
    public static int index(Instant now, int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("keine Strecken");
        }
        long week = weekNumber(now);
        int[] order = order(n);
        return order[(int) Math.floorMod(week, (long) n)];
    }

    /**
     * Gemischte, aber feste Reihenfolge: Schritt k zeigt auf (k · s) mod n mit s teilerfremd zu n – so wechseln sich
     * leichte und schwere Strecken ab.
     */
    static int[] order(int n) {
        int step = 1;
        for (int s = n / 2 + 1; s < n; s++) {
            if (gcd(s, n) == 1) {
                step = s;
                break;
            }
        }
        int[] out = new int[n];
        for (int k = 0; k < n; k++) {
            out[k] = (int) ((long) k * step % n);
        }
        return out;
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    /** Kennung der Woche wie „2026-W47“ (ISO-Woche, Berlin). */
    public static String weekId(Instant now) {
        ZonedDateTime z = now.atZone(BERLIN);
        return String.format("%d-W%02d", z.get(IsoFields.WEEK_BASED_YEAR), z.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR));
    }

    /** Wann die Woche endet (nächster Montag 0:00 Berlin). */
    public static Instant nextChange(Instant now) {
        LocalDate day = now.atZone(BERLIN).toLocalDate();
        LocalDate next = day.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        return next.atStartOfDay(BERLIN).toInstant();
    }
}
