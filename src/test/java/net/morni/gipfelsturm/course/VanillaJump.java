package net.morni.gipfelsturm.course;

/**
 * Tick-genaue Nachrechnung eines Sprungs in Minecraft (Java Edition, ohne Effekte), nur für die Tests: Sie belegt, dass
 * die Grenzen in {@link JumpRules} innerhalb dessen liegen, was man mit normaler Bewegung schafft.
 *
 * <p>Bewegung pro Tick: Geschwindigkeit · Reibung + Beschleunigung (Boden 0,1·1,3·0,98 beim Sprinten, Luft 0,02·1,3·0,98),
 * Reibung Boden 0,6·0,91, Luft 0,91; Sprung: 0,42 nach oben, Sprint-Sprung +0,2 nach vorn; Schwerkraft: (vy − 0,08)·0,98.
 * Landen: Minecraft prüft die Höhe zuerst – man landet nur, wenn man im Tick davor schon über dem Ziel war.
 */
final class VanillaJump {

    static final double GROUND_SPRINT = 0.1 * 1.3 * 0.98;
    static final double GROUND_WALK = 0.1 * 0.98;
    static final double AIR_SPRINT = 0.02 * 1.3 * 0.98;
    static final double AIR_WALK = 0.02 * 0.98;

    private VanillaJump() {
    }

    /** Geschwindigkeit nach dem Tick bei Dauerlauf auf dem Boden (Reibung, Bremsfaktor des Blocks). */
    static double steady(double slip, boolean sprint, double speedFactor) {
        double acc = (sprint ? GROUND_SPRINT : GROUND_WALK) * Math.pow(0.6 / slip, 3);
        double w = 0;
        for (int i = 0; i < 400; i++) {
            w = (w + acc) * speedFactor * slip * 0.91;
        }
        return w;
    }

    /**
     * Waagerechte Flugstrecke (Spieler-Mitte), bis man auf Höhe {@code dy} landen kann. NaN, wenn man die Höhe nie
     * erreicht.
     *
     * @param ceiling Decke über der Absprung-Oberseite (Infinity = keine)
     */
    static double travel(double dy, boolean sprint, double ceiling, double slip, double speedFactor) {
        double acc = (sprint ? GROUND_SPRINT : GROUND_WALK) * Math.pow(0.6 / slip, 3);
        double w = steady(slip, sprint, speedFactor);
        double y = 0;
        double vy = 0.42;
        double x = 0;
        double d = w + (sprint ? 0.2 : 0) + acc;
        double ny = y + vy;
        if (ny > ceiling - 1.8) {
            ny = ceiling - 1.8;
            vy = 0;
        }
        x += d;
        double peak = ny;
        y = ny;
        double vx = d * speedFactor * slip * 0.91;
        vy = (vy - 0.08) * 0.98;
        double air = sprint ? AIR_SPRINT : AIR_WALK;
        for (int t = 2; t < 400; t++) {
            d = vx + air;
            ny = y + vy;
            if (ny > ceiling - 1.8) {
                ny = ceiling - 1.8;
                vy = 0;
            }
            peak = Math.max(peak, ny);
            if (vy < 0 && ny <= dy && y >= dy) {
                return x;
            }
            if (vy < 0 && ny < dy && peak < dy) {
                return Double.NaN;
            }
            x += d;
            y = ny;
            vx = d * 0.91;
            vy = (vy - 0.08) * 0.98;
        }
        return Double.NaN;
    }

    static double sprint(double dy) {
        return travel(dy, true, Double.POSITIVE_INFINITY, 0.6, 1.0);
    }

    static double walk(double dy) {
        return travel(dy, false, Double.POSITIVE_INFINITY, 0.6, 1.0);
    }

    /** Höchster Punkt eines Sprungs (Füße). */
    static double peak() {
        double y = 0;
        double vy = 0.42;
        double best = 0;
        for (int t = 0; t < 40; t++) {
            y += vy;
            best = Math.max(best, y);
            vy = (vy - 0.08) * 0.98;
        }
        return best;
    }

    /** Wie hoch man nach einem Fall aus {@code drop} Blöcken auf Schleim zurückfedert. */
    static double bounce(double drop) {
        double y = drop;
        double vy = 0;
        while (y + vy > 0) {
            y += vy;
            vy = (vy - 0.08) * 0.98;
        }
        vy = -vy;
        y = 0;
        double best = 0;
        for (int t = 0; t < 200 && vy > -0.5; t++) {
            y += vy;
            best = Math.max(best, y);
            vy = (vy - 0.08) * 0.98;
        }
        return best;
    }
}
