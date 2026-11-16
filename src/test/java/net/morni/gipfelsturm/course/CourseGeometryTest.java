package net.morni.gipfelsturm.course;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.morni.gipfelsturm.courses.CourseCatalog;
import net.morni.gipfelsturm.courses.World;
import org.junit.jupiter.api.Test;

/** Alle Strecken gegen die Sprung-Regeln – und die Regeln selbst an kleinen Beispielen. */
class CourseGeometryTest {

    private static final World WORLD = World.create();

    @Test
    void eightCoursesWithRisingDifficulty() {
        List<Course> courses = WORLD.courses();
        assertEquals(8, courses.size());
        Set<String> ids = new HashSet<>();
        int last = -1;
        for (Course c : courses) {
            assertTrue(c.id.matches("[a-z0-9_]+"), "Kennung für Statistik-Schlüssel: " + c.id);
            assertTrue(ids.add(c.id), "doppelt: " + c.id);
            assertTrue(c.difficulty.ordinal() >= last, c.id + " ist leichter als die Strecke davor");
            last = c.difficulty.ordinal();
            assertFalse(c.nameDe.isBlank() || c.nameEn.isBlank() || c.textDe.isBlank() || c.textEn.isBlank());
        }
        for (Difficulty d : Difficulty.values()) {
            assertEquals(2, courses.stream().filter(c -> c.difficulty == d).count(), "zwei Strecken je Stufe: " + d);
        }
    }

    @Test
    void everyCourseFollowsTheJumpRules() {
        StringBuilder all = new StringBuilder();
        for (Course c : WORLD.courses()) {
            List<String> problems = CourseValidator.validate(c, WORLD.plan());
            problems.forEach(p -> all.append(c.id).append(": ").append(p).append('\n'));
        }
        assertEquals("", all.toString());
    }

    @Test
    void coursesDoNotOverlap() {
        assertEquals(List.of(), CourseValidator.separate(WORLD.courses(), List.of(WORLD.basecamp().bounds())));
    }

    @Test
    void noSceneryBridgesAGap() {
        // In jeder Lücke zwischen zwei Pads darf bis 3 Blöcke unter Pad-Höhe nichts Festes liegen, auf dem man
        // laufen könnte (außer der Neo-Wand) – sonst wäre die Lücke keine Lücke.
        StringBuilder found = new StringBuilder();
        for (Course c : WORLD.courses()) {
            for (int i = 1; i < c.pads.size(); i++) {
                Pad a = c.pads.get(i - 1);
                Pad b = c.pads.get(i);
                java.util.Set<Long> wall = new HashSet<>();
                b.arrival.wall().forEach(w -> wall.add(Plan.key((int) w[0], (int) w[1], (int) w[2])));
                for (int x = Math.min(a.x0, b.x0); x <= Math.max(a.x1, b.x1); x++) {
                    for (int z = Math.min(a.z0, b.z0); z <= Math.max(a.z1, b.z1); z++) {
                        if (a.covers(x, z) || b.covers(x, z)) {
                            continue;
                        }
                        for (int y = Math.min(a.y, b.y) - 3; y <= Math.max(a.y, b.y); y++) {
                            Blk blk = WORLD.plan().get(x, y, z);
                            if (blk.solid() && !wall.contains(Plan.key(x, y, z)) && !c.isPadCell(x, y, z)
                                    && b.arrival.kind() != Pad.Kind.LADDER) {
                                found.append(c.id).append(" zwischen Pad ").append(a.index).append(" und ")
                                        .append(b.index).append(": ").append(blk.id()).append(" bei ").append(x)
                                        .append(' ').append(y).append(' ').append(z).append('\n');
                            }
                        }
                    }
                }
            }
        }
        assertEquals("", found.toString());
    }

    @Test
    void checkpointsAndFallLines() {
        for (Course c : WORLD.courses()) {
            assertTrue(c.checkpointCount() >= 3 && c.checkpointCount() <= 8, c.id);
            for (int k = 0; k <= c.checkpointCount(); k++) {
                // Die Fallgrenze liegt unter jedem Pad, das man in diesem Abschnitt betreten muss.
                double fall = c.fallY(k);
                int section = 0;
                for (Pad p : c.pads) {
                    if (section == k) {
                        assertTrue(p.surface() - fall >= Course.FALL_MARGIN - 1e-9, c.id + " Pad " + p.index);
                    }
                    if (p.role == Pad.Role.CHECKPOINT) {
                        section++;
                        if (section == k) {
                            assertTrue(p.surface() - fall >= Course.FALL_MARGIN - 1e-9, c.id + " CP " + p.index);
                        }
                    }
                }
            }
        }
    }

    @Test
    void medalsAreOrderedAndReachable() {
        for (Course c : WORLD.courses()) {
            Course.Medals m = c.medals;
            assertTrue(m.gipfel() < m.gold() && m.gold() < m.silber() && m.silber() < m.bronze(), c.id);
            assertTrue(m.gipfel() >= c.pathLength() / CourseValidator.MAX_SPEED * 1000, c.id + " Gipfel zu schnell");
            assertTrue(c.plausibleMs() < m.gipfel(), c.id);
        }
    }

    @Test
    void mechanicsAppearAsDifficultyRises() {
        // Leicht: nichts Rutschiges oder Federndes; Schwer/Extrem: Schleim, Eis, Seelensand, Decken, Neos kommen vor.
        for (Course c : WORLD.courses()) {
            boolean special = c.pads.stream().anyMatch(p -> p.blk.surface() == Surface.SLIME
                    || p.blk.surface() == Surface.SOUL_SAND || p.arrival != null && p.arrival.kind() == Pad.Kind.NEO);
            if (c.difficulty == Difficulty.LEICHT) {
                assertFalse(special, c.id);
            }
        }
        assertTrue(any(Surface.SLIME));
        assertTrue(any(Surface.ICE));
        assertTrue(any(Surface.SOUL_SAND));
        assertTrue(WORLD.courses().stream().anyMatch(c -> c.pads.stream()
                .anyMatch(p -> p.arrival != null && p.arrival.kind() == Pad.Kind.NEO)));
        assertTrue(WORLD.courses().stream().anyMatch(c -> c.pads.stream()
                .anyMatch(p -> p.arrival != null && p.arrival.kind() == Pad.Kind.LADDER)));
    }

    private static boolean any(Surface s) {
        return WORLD.courses().stream().anyMatch(c -> c.pads.stream().anyMatch(p -> p.blk.surface() == s));
    }

    // ---------- Die Regeln an kleinen Beispielen ----------

    private static List<String> check(Difficulty d, java.util.function.Consumer<CourseBuilder> steps) {
        Plan plan = new Plan();
        CourseBuilder b = new CourseBuilder(plan, "probe", d, Dir.EAST).name("P", "P").text("t", "t");
        b.start(0, 100, 0, 3, 3);
        steps.accept(b);
        b.checkpoint(1, 0, 0, 2, 2).checkpoint(1, 0, 0, 2, 2).checkpoint(1, 0, 0, 2, 2).finish(1, 0, 0, 3, 3);
        b.medals(60, 70, 80, 90);
        return CourseValidator.validate(b.build(), plan);
    }

    @Test
    void acceptsSimpleJumps() {
        assertEquals(List.of(), check(Difficulty.LEICHT, b -> b.jump(2, 0, 0, 1, 1).jump(1, 1, 0, 1, 1)));
        assertEquals(List.of(), check(Difficulty.MITTEL, b -> b.jump(3, 0, 0, 1, 1).jump(2, 1, 0, 1, 1)
                .jump(4, -1, 0, 1, 1)));
    }

    @Test
    void rejectsJumpsBeyondTheLimits() {
        assertFalse(check(Difficulty.LEICHT, b -> b.jump(3, 0, 0, 1, 1)).isEmpty(), "Leicht: Lücke 3");
        assertFalse(check(Difficulty.LEICHT, b -> b.jump(2, 1, 0, 1, 1)).isEmpty(), "Leicht: 2 Lücke hoch");
        assertFalse(check(Difficulty.SCHWER, b -> b.jump(4, 0, 0, 1, 1)).isEmpty(), "flache 4");
        assertFalse(check(Difficulty.SCHWER, b -> b.jump(3, 1, 0, 1, 1)).isEmpty(), "3+1 auf Schwer");
        assertFalse(check(Difficulty.EXTREM, b -> b.jump(1, 2, 0, 1, 1)).isEmpty(), "zwei hoch");
        assertFalse(check(Difficulty.EXTREM, b -> b.jump(5, -1, 0, 1, 1)).isEmpty(), "5 abwärts");
    }

    @Test
    void threePlusOneNeedsRunUp() {
        assertEquals(List.of(), check(Difficulty.EXTREM, b -> b.jump(1, 0, 0, 3, 1).jump(3, 1, 0, 1, 1)));
        assertFalse(check(Difficulty.EXTREM, b -> b.jump(1, 0, 0, 1, 1).jump(3, 1, 0, 1, 1)).isEmpty());
    }

    @Test
    void diagonalJumpsCountTheRealDistance() {
        // 2 vor und 2 zur Seite (Ecke an Ecke) ist weiter als 2 geradeaus.
        assertFalse(check(Difficulty.LEICHT, b -> b.jump(1, 0, 0, 1, 1).jump(2, 0, 3, 1, 1)).isEmpty());
        assertEquals(List.of(), check(Difficulty.MITTEL, b -> b.jump(1, 0, 0, 1, 1).jump(2, 0, 3, 1, 1)));
    }

    @Test
    void blocksInTheWayAreFound() {
        List<String> p = check(Difficulty.MITTEL, b -> {
            b.jump(2, 0, 0, 1, 1);
            Pad cur = b.current();
            b.plan().set(cur.x1 + 2, cur.y + 1, cur.z0, Blk.full("stone"));
            b.jump(2, 0, 0, 1, 1);
        });
        assertTrue(p.stream().anyMatch(s -> s.contains("im Sprungweg")), p.toString());
    }

    @Test
    void headHittersLimitTheJump() {
        // In den Gang hinein läuft man (Lücke 0), darin geht es mit Lücke 1 weiter, hinaus ebenso.
        assertEquals(List.of(), check(Difficulty.SCHWER, b -> b.jump(2, 0, 0, 2, 1).ceilingOn(2.0, Blk.full("stone"))
                .jump(0, 0, 0, 1, 1).jump(1, 0, 0, 1, 1).jump(1, 0, 0, 1, 1).ceilingOff().jump(1, 0, 0, 1, 1)));
        assertFalse(check(Difficulty.SCHWER, b -> b.jump(2, 0, 0, 2, 1).ceilingOn(2.0, Blk.full("stone"))
                .jump(0, 0, 0, 1, 1).jump(2, 0, 0, 1, 1).ceilingOff().jump(1, 0, 0, 1, 1)).isEmpty(),
                "Kopfstoß mit Lücke 2");
        assertFalse(check(Difficulty.SCHWER, b -> b.jump(1, 0, 0, 1, 1).ceilingOn(2.0, Blk.full("stone"))
                .jump(2, 0, 0, 1, 1).ceilingOff().jump(1, 0, 0, 1, 1)).isEmpty(), "weit in den Gang hineinspringen");
        assertFalse(check(Difficulty.LEICHT, b -> b.jump(1, 0, 0, 2, 1).ceilingOn(2.0, Blk.full("stone"))
                .jump(0, 0, 0, 1, 1).jump(1, 0, 0, 1, 1).ceilingOff().jump(1, 0, 0, 1, 1)).isEmpty(),
                "Kopfstoß auf Leicht");
        // Mit 2,5 Platz geht Lücke 2.
        assertEquals(List.of(), check(Difficulty.SCHWER, b -> b.jump(2, 0, 0, 2, 1)
                .ceilingOn(2.5, Blk.topSlab("smooth_stone_slab")).jump(0, 0, 0, 1, 1).jump(2, 0, 0, 1, 1)
                .ceilingOff().jump(2, 0, 0, 1, 1)));
    }

    @Test
    void slimeMustBeHighEnough() {
        assertEquals(List.of(), check(Difficulty.SCHWER, b -> b.jump(1, 0, 0, 1, 1).bounce(6, 3, 1, 1, 1)));
        assertFalse(check(Difficulty.SCHWER, b -> b.jump(1, 0, 0, 1, 1).bounce(6, 5, 1, 1, 1)).isEmpty());
        assertFalse(check(Difficulty.SCHWER, b -> b.jump(1, 0, 0, 1, 1).bounce(3, 1, 1, 1, 1)).isEmpty());
    }

    @Test
    void neoNeedsTheRightLevel() {
        assertEquals(List.of(), check(Difficulty.MITTEL, b -> b.jump(1, 0, 0, 1, 1).neo(1, 1, Blk.full("stone"))));
        assertFalse(check(Difficulty.MITTEL, b -> b.jump(1, 0, 0, 1, 1).neo(2, 1, Blk.full("stone"))).isEmpty());
        assertFalse(check(Difficulty.LEICHT, b -> b.jump(1, 0, 0, 1, 1).neo(1, 1, Blk.full("stone"))).isEmpty());
        assertEquals(List.of(), check(Difficulty.EXTREM, b -> b.jump(1, 0, 0, 1, 1).neo(2, -1, Blk.full("stone"))));
    }

    @Test
    void ladderNeedsAWall() {
        assertEquals(List.of(), check(Difficulty.LEICHT, b -> b.jump(1, 0, 0, 1, 1).climb(6, 2, 2, Blk.full("stone"))));
    }

    @Test
    void iceLandingNeedsRoomToStop() {
        assertFalse(check(Difficulty.MITTEL, b -> b.jump(1, 0, 0, 1, 1, Blk.PACKED_ICE).turnLeft()
                .jump(1, 0, 0, 1, 1)).isEmpty());
        assertEquals(List.of(), check(Difficulty.MITTEL, b -> b.jump(1, 0, 0, 3, 1, Blk.PACKED_ICE)));
    }
}
