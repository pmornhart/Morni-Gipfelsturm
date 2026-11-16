package net.morni.gipfelsturm.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Die Regeln in {@link JumpRules} gegen die nachgerechnete Bewegung von Minecraft. */
class JumpPhysicsTest {

    /** So viel Luft muss jede erlaubte Grenze zur echten Grenze lassen (Blöcke). */
    private static final double FAIR = 0.08;

    @Test
    void jumpPeakIsAboutOneAndAQuarter() {
        assertEquals(1.2522, VanillaJump.peak(), 0.001);
        assertTrue(JumpRules.JUMP_PEAK <= VanillaJump.peak() + 1e-6);
    }

    @Test
    void sprintLimitsAreReachable() {
        double[] heights = {1.0, 0.75, 0.5, 0.25, 0.0, -0.5, -0.99, -1.0, -2.0, -6.0};
        for (double dy : heights) {
            double limit = JumpRules.maxTravel(JumpRules.Tier.SPRINT, dy, Surface.NORMAL, 1);
            double real = VanillaJump.sprint(dy);
            assertTrue(real >= limit + FAIR, "Sprint dy=" + dy + ": erlaubt " + limit + ", schaffbar " + real);
        }
    }

    @Test
    void walkLimitsAreReachableWithoutSprinting() {
        double[] heights = {1.0, 0.5, 0.25, 0.0, -0.5, -1.0, -3.0};
        for (double dy : heights) {
            double limit = JumpRules.maxTravel(JumpRules.Tier.WALK, dy, Surface.NORMAL, 1);
            double real = VanillaJump.walk(dy);
            assertTrue(real >= limit + FAIR, "Gehen dy=" + dy + ": erlaubt " + limit + ", schaffbar " + real);
        }
    }

    @Test
    void threePlusOneIsTightButPossible() {
        double limit = JumpRules.maxTravel(JumpRules.Tier.EXPERT, 1.0, Surface.NORMAL, 3);
        assertEquals(2.4, limit, 1e-9);
        assertTrue(VanillaJump.sprint(1.0) >= limit + FAIR, "3+1 mit Anlauf");
        // ohne Anlauf (und unter Mittel/Schwer) gibt es ihn nicht
        assertEquals(1.4, JumpRules.maxTravel(JumpRules.Tier.EXPERT, 1.0, Surface.NORMAL, 1), 1e-9);
        assertEquals(1.4, JumpRules.maxTravel(JumpRules.Tier.SPRINT, 1.0, Surface.NORMAL, 5), 1e-9);
    }

    @Test
    void flatFourIsNotAllowedBecauseItNeedsMomentum() {
        // Lücke 4 = Flugstrecke 3,4 – ohne Schwung reicht es nicht.
        assertTrue(VanillaJump.sprint(0.0) < 3.4, "flache 4 ohne Schwung: " + VanillaJump.sprint(0.0));
        assertTrue(JumpRules.maxTravel(JumpRules.Tier.EXPERT, 0.0, Surface.NORMAL, 10) < 3.4);
        // abwärts geht die 4
        assertTrue(VanillaJump.sprint(-1.0) >= 3.4 + FAIR);
    }

    @Test
    void tooHighIsImpossible() {
        assertTrue(Double.isNaN(VanillaJump.sprint(1.3)));
        assertTrue(Double.isNaN(JumpRules.maxTravel(JumpRules.Tier.EXPERT, 1.3, Surface.NORMAL, 10)));
    }

    @Test
    void headHittersAreReachable() {
        for (double head : new double[] {2.0, 2.2, 2.5, 2.8, 3.0}) {
            double limit = JumpRules.maxTravelUnderCeiling(JumpRules.Tier.SPRINT, head, 0.0);
            double real = VanillaJump.travel(0.0, true, head, 0.6, 1.0);
            assertTrue(real >= limit + FAIR, "Decke " + head + ": erlaubt " + limit + ", schaffbar " + real);
        }
        assertTrue(Double.isNaN(JumpRules.maxTravelUnderCeiling(JumpRules.Tier.SPRINT, 1.9, 0.0)));
        assertTrue(Double.isNaN(JumpRules.maxTravelUnderCeiling(JumpRules.Tier.SPRINT, 2.0, 1.0)));
    }

    @Test
    void soulSandLimitsAreReachable() {
        // Seelensand: 14/16 hoch, bremst um 0,4 – auf einen vollen Block gleicher Zelle (+0,125) bzw. eins höher (+1,125).
        double flat = VanillaJump.travel(0.125, true, Double.POSITIVE_INFINITY, 0.6, 0.4);
        double up = VanillaJump.travel(1.125, true, Double.POSITIVE_INFINITY, 0.6, 0.4);
        assertTrue(flat >= JumpRules.maxTravel(JumpRules.Tier.SPRINT, 0.125, Surface.SOUL_SAND, 1) + FAIR, "flach " + flat);
        assertTrue(up >= JumpRules.maxTravel(JumpRules.Tier.SPRINT, 1.125, Surface.SOUL_SAND, 1) + FAIR, "hoch " + up);
    }

    @Test
    void slimeBounceTableIsConservative() {
        for (double drop : new double[] {3, 4, 5, 6, 8, 10, 12, 16}) {
            double real = VanillaJump.bounce(drop);
            assertTrue(real + 0.05 >= JumpRules.bouncePeak(drop), "Schleim aus " + drop + ": Tabelle "
                    + JumpRules.bouncePeak(drop) + ", echt " + real);
        }
    }

    @Test
    void neoAroundAWallIsReachable() {
        // Um eine Wand der Dicke t herum: Flugstrecke t + 0,6 (geradeaus an der Kante entlang).
        assertTrue(VanillaJump.sprint(0.0) >= JumpRules.neoMaxThickness(JumpRules.Tier.EXPERT) + 0.6 + 0.5);
        assertTrue(VanillaJump.sprint(0.0) >= JumpRules.neoMaxThickness(JumpRules.Tier.SPRINT) + 0.6 + 1.0);
    }
}
