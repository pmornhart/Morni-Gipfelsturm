package net.morni.gipfelsturm.time;

/**
 * Stoppuhr eines Laufs in Server-Ticks (50 ms) mit Bruchteilen: Wann genau innerhalb eines Ticks jemand losläuft oder
 * ankommt, rechnet der Zeitnehmer aus der Bewegung zwischen zwei Ticks nach. So ist die Zeit fair (Server-Ruckler
 * zählen nicht) und trotzdem auf die Millisekunde.
 */
public final class RunClock {

    public static final double MS_PER_TICK = 50.0;

    private final long startTick;
    private final double startFraction;

    /**
     * @param tick     Tick, in dem der Start erkannt wurde
     * @param fraction wo zwischen dem vorigen und diesem Tick (0…1) der Start lag
     */
    public RunClock(long tick, double fraction) {
        this.startTick = tick;
        this.startFraction = clamp(fraction);
    }

    /** Zeit bis zu einem Ereignis im Tick {@code tick} beim Anteil {@code fraction}. */
    public long at(long tick, double fraction) {
        double ticks = (tick - startTick) + (clamp(fraction) - startFraction);
        return Math.max(0, Math.round(ticks * MS_PER_TICK));
    }

    /** Laufende Anzeige (ganze Ticks). */
    public long now(long tick) {
        return at(tick, startFraction);
    }

    public long startTick() {
        return startTick;
    }

    private static double clamp(double f) {
        return Double.isNaN(f) ? 1.0 : Math.max(0, Math.min(1, f));
    }
}
