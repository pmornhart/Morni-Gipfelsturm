package de.moinmorni.gipfelsturm.course;

/**
 * Die Regeln, nach denen jede Strecke geprüft wird: Welche Sprünge schafft man mit normaler Bewegung (Java Edition,
 * ohne Effekte, ohne Tricks)? Alle Werte sind Flugstrecken der Spieler-Mitte in Blöcken – bei einem geraden Sprung
 * zwischen vollen Blöcken ist die Lücke 0,6 größer (der Spieler ist 0,6 breit und darf an beiden Kanten überstehen).
 *
 * <p>Die Grenzen stammen aus der Bewegungsphysik von Minecraft (Sprung 0,42 nach oben, Schwerkraft 0,08, Luftwiderstand
 * 0,98/0,91, Sprint-Sprung +0,2) – nachgerechnet in {@code JumpPhysicsTest}, das jede Grenze gegen eine Tick-für-Tick-
 * Simulation prüft. Jede Grenze lässt einen Sicherheitsabstand: fair heißt hier „schaffbar ohne Pixel-Glück“.
 *
 * <table>
 *   <caption>Erlaubte Flugstrecke (Lücke) je Höhenunterschied</caption>
 *   <tr><th>Höhe (Ziel − Absprung)</th><th>Leicht (gehen)</th><th>Mittel/Schwer (sprinten)</th><th>Extrem</th></tr>
 *   <tr><td>über +1,25</td><td colspan="3">unmöglich (höchster Sprung: 1,2522)</td></tr>
 *   <tr><td>+0,5 … +1</td><td>0,4 (Lücke 1)</td><td>1,4 (Lücke 2)</td><td>1,4; mit 3 Blöcken Anlauf 2,4 („3+1“)</td></tr>
 *   <tr><td>0 … +0,5</td><td>0,4 (Lücke 1)</td><td>2,4 (Lücke 3)</td><td>2,4</td></tr>
 *   <tr><td>−1 … 0</td><td>1,4 (Lücke 2)</td><td>2,4 (Lücke 3)</td><td>2,4</td></tr>
 *   <tr><td>−1 und tiefer</td><td>1,4 (Lücke 2)</td><td>3,4 (Lücke 4)</td><td>3,4</td></tr>
 * </table>
 *
 * <p>Eine flache Lücke von 4 ist bewusst verboten: Ohne Schwung aus einem vorigen Sprung reicht es knapp nicht
 * (Simulation: 3,34 Flugstrecke, nötig 3,4).
 *
 * <p>Sonderfälle: Decke über dem Absprung („Kopfstoß“), Seelensand (bremst, 14/16 hoch), Eis (Landefläche muss zum
 * Abbremsen reichen), Schleim (federt), Leiter, Neo (Sprung um eine Wand herum).
 */
public final class JumpRules {

    /** Wer muss es schaffen? Leicht = auch ohne Sprinten, Extrem = mit gutem Timing. */
    public enum Tier {
        WALK, SPRINT, EXPERT
    }

    /** Höchster Sprung (Spieler-Füße): 1,2522 Blöcke. */
    public static final double JUMP_PEAK = 1.2522;
    /** Spielergröße und halbe Breite. */
    public static final double PLAYER_HEIGHT = 1.8;
    public static final double HALF_WIDTH = 0.3;
    /** Bis zu dieser Höhe steigt man ohne Springen hinauf (Treppen, Stufen). */
    public static final double STEP_UP = 0.6;
    /** Mit weniger Platz über dem Kopf ist der Sprung gebremst. */
    public static final double FREE_HEADROOM = PLAYER_HEIGHT + JUMP_PEAK;
    /** Anlauf (Blöcke) für den „3+1“ auf Extrem. */
    public static final double RUNUP_FOR_THREE_PLUS_ONE = 3;
    /** So weit muss eine Eis-Landefläche in Sprungrichtung reichen (Blöcke), wenn man danach nicht weiterläuft. */
    public static final double ICE_STOP_LENGTH = 3;

    private JumpRules() {
    }

    /**
     * Höchste erlaubte Flugstrecke für einen Sprung (ohne Decke). {@code NaN}, wenn der Sprung gar nicht geht.
     *
     * @param dy    Oberseite Ziel minus Oberseite Absprung
     * @param from  Untergrund beim Absprung
     * @param runup Anlauf auf dem Absprung-Block in Sprungrichtung (Blöcke)
     */
    public static double maxTravel(Tier tier, double dy, Surface from, double runup) {
        if (from == Surface.SOUL_SAND) {
            // Seelensand bremst auch den Absprung (Faktor 0,4): Simulation +0,125 → 1,78, +1,125 → 1,21.
            if (tier == Tier.WALK || dy > 1.125 + 1e-9) {
                return Double.NaN;
            }
            return dy <= 0.2 ? 1.4 : 0.4;
        }
        if (dy > 1.0 + 1e-9) {
            return Double.NaN;
        }
        if (tier == Tier.WALK) {
            if (dy > 0) {
                return 0.4;
            }
            return 1.4;
        }
        if (dy > 0.5) {
            return tier == Tier.EXPERT && runup >= RUNUP_FOR_THREE_PLUS_ONE ? 2.4 : 1.4;
        }
        if (dy > -1.0 + 1e-9) {
            return 2.4;
        }
        return 3.4;
    }

    /**
     * Höchste Flugstrecke unter einer Decke (Abstand Decke – Absprung-Oberseite {@code headroom}). {@code NaN}, wenn
     * man dort gar nicht springen kann. Unter einer Decke geht es nur geradeaus oder abwärts.
     */
    public static double maxTravelUnderCeiling(Tier tier, double headroom, double dy) {
        if (headroom >= FREE_HEADROOM) {
            return Double.POSITIVE_INFINITY;
        }
        if (tier == Tier.WALK || dy > 1e-9 || headroom < 2.0) {
            return Double.NaN;
        }
        // Simulation: Decke 2,0 → 0,77 Flugstrecke, Decke 2,5 → 1,63.
        return headroom < 2.5 ? 0.4 : 1.4;
    }

    /**
     * Wie hoch federt Schleim zurück, wenn man aus {@code drop} Blöcken Höhe darauf fällt? Werte aus der Simulation
     * (Abrundung auf den nächstkleineren Eintrag).
     */
    public static double bouncePeak(double drop) {
        double[][] table = {{3, 2.75}, {4, 3.27}, {5, 4.37}, {6, 4.96}, {8, 6.19}, {10, 7.47}, {12, 8.13}, {16, 10.84}};
        double peak = 0;
        for (double[] row : table) {
            if (drop + 1e-9 >= row[0]) {
                peak = row[1];
            }
        }
        return peak;
    }

    /** Sicherheitsabstand nach oben beim Schleim: so viel tiefer als der höchste Punkt muss das Ziel liegen. */
    public static final double BOUNCE_MARGIN = 1.2;
    /** Flugstrecke nach einem Schleim-Sprung zum Ziel (Blöcke, großzügig – man ist lange in der Luft). */
    public static final double BOUNCE_TRAVEL = 1.4;
    /** Mindest-Fallhöhe auf Schleim, damit es überhaupt nennenswert hinauf geht. */
    public static final double BOUNCE_MIN_DROP = 4;
    /** Leitern: so hoch dürfen sie sein. */
    public static final int LADDER_MAX = 24;
    /** Neo: so dick darf die Wand sein, um die man herumspringt. */
    public static int neoMaxThickness(Tier tier) {
        return switch (tier) {
            case WALK -> 0;
            case SPRINT -> 1;
            case EXPERT -> 2;
        };
    }
}
