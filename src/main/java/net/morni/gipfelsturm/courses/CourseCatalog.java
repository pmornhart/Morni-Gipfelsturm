package net.morni.gipfelsturm.courses;

import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;
import net.morni.gipfelsturm.course.Blk;
import net.morni.gipfelsturm.course.Course;
import net.morni.gipfelsturm.course.CourseBuilder;
import net.morni.gipfelsturm.course.Difficulty;
import net.morni.gipfelsturm.course.Dir;
import net.morni.gipfelsturm.course.Pad;
import net.morni.gipfelsturm.course.Plan;

/**
 * Die acht Strecken von Gipfelsturm – eigene Entwürfe, von leicht nach extrem. Jede Strecke liegt in ihrer eigenen
 * Gegend der Welt (500 Blöcke auseinander) und hat ihre eigene Landschaft.
 *
 * <ol>
 *   <li>Almwiesen-Lauf (Leicht): große Felder, Trittsteine, eine Leiter auf den Heustadel.</li>
 *   <li>Gletscherspalten (Leicht): von Eisturm zu Eisturm über Spalten, Eis-Felder zum Rutschen.</li>
 *   <li>Kletterwand (Mittel): Leitern und Felsbänder eine Wand hinauf, erster Neo.</li>
 *   <li>Felsgrat (Mittel): schmaler Grat mit Mauer-Pfosten, Stufen und Sprüngen über Abgründe.</li>
 *   <li>Seilbahn-Masten (Schwer): Masten, Gondel-Dächer, Zaun-Pfosten, Schleim.</li>
 *   <li>Eishöhle (Schwer): Kopfstoß-Gänge, Blaueis, Seelensand-Matsch, eiskaltes Wasser.</li>
 *   <li>Gipfelkreuz-Sprint (Extrem): „3+1“, Vierer-Sprünge, Neo um dicke Wände, Gitter-Pfosten.</li>
 *   <li>Wetterturm (Extrem): rund um einen hohen Turm hinauf bis zur Wetterstation.</li>
 * </ol>
 */
public final class CourseCatalog {

    /** Abstand der Strecken in der Welt (x). */
    public static final int SPACING = 500;
    /** Höhe der Start-Oberseite. */
    public static final int BASE_Y = 100;

    static final Blk STONE_PAD = Blk.full("mossy_cobblestone");
    static final Blk HAY = Blk.full("hay_block", "axis=y");
    static final Blk PLANKS = Blk.full("spruce_planks");
    static final Blk SNOW = Blk.full("snow_block");
    static final Blk ANDESITE = Blk.full("polished_andesite");
    static final Blk STONE = Blk.full("stone");
    static final Blk STONE_BRICKS = Blk.full("stone_bricks");
    static final Blk IRON = Blk.full("iron_block");
    static final Blk RED = Blk.full("red_concrete");

    private CourseCatalog() {
    }

    /** Alle Strecken in Reihenfolge der Schwierigkeit; schreibt Pads und Landschaft in den Plan. */
    public static List<Course> build(Plan plan) {
        List<Course> out = new ArrayList<>();
        out.add(almwiese(plan, x(0)));
        out.add(gletscher(plan, x(1)));
        out.add(kletterwand(plan, x(2)));
        out.add(felsgrat(plan, x(3)));
        out.add(seilbahn(plan, x(4)));
        out.add(eishoehle(plan, x(5)));
        out.add(gipfelkreuz(plan, x(6)));
        out.add(wetterturm(plan, x(7)));
        return out;
    }

    private static int x(int i) {
        return SPACING * (i + 1);
    }

    // =====================================================================================================
    // 1. Almwiesen-Lauf (Leicht)
    // =====================================================================================================

    static Course almwiese(Plan plan, int ox) {
        CourseBuilder b = new CourseBuilder(plan, "almwiese", Difficulty.LEICHT, Dir.EAST)
                .name("Almwiesen-Lauf", "Meadow Run")
                .text("Gemütlich über die Almwiesen: große Felder, kleine Lücken – ideal zum Reinkommen.",
                        "A relaxed hop across alpine meadows: big blocks, small gaps – perfect to warm up.")
                .icon("pink_tulip").biome("meadow");
        b.start(ox, BASE_Y, 0, 3, 3);
        b.jump(1, 0, 0, 2, 2);
        b.jump(1, 0, 0, 2, 2);
        b.jump(2, 0, 0, 2, 2);
        b.jump(1, 1, 0, 2, 2);
        b.jump(1, 0, 1, 1, 1);
        b.jump(1, 0, -1, 1, 1);
        b.jump(2, 0, 0, 2, 2);
        b.checkpoint(1, -1, 0, 3, 3);
        // Trittsteine über den Bach
        b.turnLeft();
        b.jump(1, 0, 0, 1, 2, STONE_PAD);
        Pad bach0 = b.current();
        b.jump(1, 0, 0, 1, 1, STONE_PAD);
        b.jump(1, 0, 1, 1, 1, STONE_PAD);
        b.jump(1, 0, -1, 1, 1, STONE_PAD);
        b.jump(1, 0, 0, 1, 2, STONE_PAD);
        Pad bach1 = b.current();
        b.jump(1, 0, 0, 2, 2);
        b.jump(1, 1, 0, 2, 2);
        b.jump(1, 1, 0, 2, 2);
        b.checkpoint(1, 0, 0, 3, 3);
        // Heustadel: Leiter aufs Dach, über Heuballen wieder hinunter
        b.turnRight();
        b.jump(1, 0, 0, 1, 1);
        Pad stadel = b.current();
        b.climb(5, 2, 2, PLANKS);
        b.jump(1, 0, 0, 2, 2, HAY);
        b.jump(2, -1, 0, 2, 2, HAY);
        b.jump(2, -1, 1, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.jump(2, -1, 0, 2, 2);
        b.checkpoint(1, 0, 0, 3, 3);
        b.turnRight();
        b.jump(1, 0, 0, 2, 2);
        b.jump(2, 0, 0, 1, 1);
        b.jump(1, 1, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.jump(2, 0, -1, 2, 2);
        b.jump(1, 1, 0, 2, 2);
        b.finish(1, 0, 0, 3, 3);
        Course c = b.build();

        // Landschaft: eine große Alm tief unter der Strecke (die Bäume bleiben unter den Sprüngen), ein Bach quer
        // darüber, Heustadel, Almhütte am Ziel.
        long seed = 101;
        double mx = 0;
        double mz = 0;
        for (Pad p : c.pads) {
            mx += p.centerX();
            mz += p.centerZ();
        }
        mx /= c.pads.size();
        mz /= c.pads.size();
        int almY = BASE_Y - 17;
        Scenery.meadowIsland(plan, (int) mx, almY, (int) mz, 30, seed);
        for (int z = (int) mz - 30; z <= (int) mz + 30; z++) {
            int bx = (int) bach0.centerX() + (int) Math.round(Math.sin(z * 0.25) * 2);
            for (int dx = -1; dx <= 1; dx++) {
                if (plan.get(bx + dx, almY, z).solid()) {
                    plan.set(bx + dx, almY, z, Blk.WATER);
                    plan.remove(bx + dx, almY + 1, z);
                }
            }
        }
        stadel(plan, stadel);
        Pad fin = c.finish();
        almhuette(plan, fin.x1 + 3, fin.y, fin.z0 - 1);
        Scenery.panorama(plan, (int) c.start().centerX() + 20, BASE_Y, 0, 90, 9, seed);
        return c;
    }

    /** Holzwände rund um die Leiter: ein kleiner Heustadel mit Satteldach (die Leiter-Wand ist seine Giebelwand). */
    private static void stadel(Plan plan, Pad base) {
        int x0 = base.x1 + 2;
        int z0 = base.z0 - 3;
        for (int x = x0; x <= x0 + 6; x++) {
            for (int z = z0; z <= z0 + 6; z++) {
                boolean edge = x == x0 || x == x0 + 6 || z == z0 || z == z0 + 6;
                for (int y = base.y - 2; y <= base.y + 3; y++) {
                    if (edge && !(x == x0 && z == base.z0)) {
                        plan.decorate(x, y, z, y % 2 == 0 ? PLANKS : Blk.full("stripped_spruce_log", "axis=y"));
                    }
                }
                plan.decorate(x, base.y - 3, z, PLANKS);
            }
        }
    }

    /** Kleine Almhütte (Dekoration neben dem Ziel). */
    private static void almhuette(Plan plan, int x0, int y, int z0) {
        Blk wall = Blk.full("stripped_spruce_log", "axis=y");
        Blk base = Blk.full("cobblestone");
        for (int x = x0; x <= x0 + 6; x++) {
            for (int z = z0; z <= z0 + 5; z++) {
                plan.decorate(x, y, z, base);
                boolean edge = x == x0 || x == x0 + 6 || z == z0 || z == z0 + 5;
                if (edge) {
                    for (int k = 1; k <= 3; k++) {
                        boolean window = k == 2 && (x == x0 + 3 || z == z0 + 2) && !(x == x0 || x == x0 + 6);
                        plan.decorate(x, y + k, z, window ? Blk.deco("glass_pane") : (k == 1 ? base : wall));
                    }
                }
            }
        }
        for (int k = 0; k <= 3; k++) {
            for (int x = x0 - 1 + k; x <= x0 + 7 - k; x++) {
                for (int z = z0 - 1; z <= z0 + 6; z++) {
                    plan.decorate(x, y + 4 + k, z, Blk.full("dark_oak_planks"));
                }
            }
        }
        plan.decorate(x0 + 5, y + 8, z0 + 4, Blk.full("bricks"));
        plan.decorate(x0 + 5, y + 9, z0 + 4, Blk.deco("campfire[lit=true,signal_fire=false]"));
        plan.decorate(x0 + 3, y + 1, z0 - 1, Blk.deco("flower_pot"));
    }

    // =====================================================================================================
    // 2. Gletscherspalten (Leicht)
    // =====================================================================================================

    static Course gletscher(Plan plan, int ox) {
        CourseBuilder b = new CourseBuilder(plan, "gletscher", Difficulty.LEICHT, Dir.EAST)
                .name("Gletscherspalten", "Glacier Crevasses")
                .text("Von Eisturm zu Eisturm über tiefe Spalten – auf dem Eis rutscht man weit!",
                        "From ice tower to ice tower over deep crevasses – ice makes you slide!")
                .icon("blue_ice").biome("frozen_peaks")
                .pads(SNOW, Blk.LIGHT_BLUE);
        SplittableRandom r = new SplittableRandom(202);
        b.under(p -> serac(plan, p, r));
        b.start(ox, BASE_Y, 0, 3, 3);
        b.jump(1, 0, 0, 2, 3);
        b.jump(2, 0, 0, 2, 2);
        b.jump(2, -1, 0, 2, 2);
        b.jump(1, 0, 1, 2, 2);
        b.jump(2, 0, 0, 3, 3, Blk.PACKED_ICE);
        b.jump(1, 0, 0, 2, 2);
        b.checkpoint(2, 0, 0, 3, 3);
        b.turnRight();
        b.jump(1, 1, 0, 2, 2);
        b.jump(1, 1, 0, 2, 2);
        b.jump(2, 0, -1, 1, 2);
        b.jump(2, 0, 0, 1, 1);
        b.jump(1, 0, 1, 1, 1);
        b.jump(2, -1, 0, 3, 3, Blk.PACKED_ICE);
        b.checkpoint(1, 0, 0, 3, 3);
        b.turnLeft();
        b.jump(2, 0, 0, 1, 1);
        b.jump(1, 1, 0, 1, 1);
        b.jump(2, 0, 0, 2, 2);
        b.jump(2, -1, -1, 2, 2);
        b.jump(1, 0, 0, 3, 3, Blk.PACKED_ICE);
        b.jump(1, 0, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1);
        b.checkpoint(1, 1, 0, 3, 3);
        b.turnLeft();
        b.jump(1, 0, 0, 2, 2);
        b.jump(2, 0, 1, 1, 1);
        b.jump(1, 1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1);
        b.jump(2, -1, 0, 2, 2);
        b.jump(1, 0, 0, 3, 3, Blk.PACKED_ICE);
        b.finish(1, 1, 0, 3, 3);
        Course c = b.build();
        long seed = 202;
        // Gletscher-Ränder links und rechts, tief unten Spalten
        for (Pad p : c.pads) {
            if (p.index % 3 == 0) {
                Scenery.snowIsland(plan, (int) p.centerX() + 9, p.y - 14, (int) p.centerZ() + 7, 6, seed + p.index);
                Scenery.snowIsland(plan, (int) p.centerX() - 8, p.y - 16, (int) p.centerZ() - 9, 5, seed * 3 + p.index);
            }
        }
        Scenery.panorama(plan, (int) c.start().centerX() + 15, BASE_Y + 10, 0, 85, 11, seed);
        return c;
    }

    /** Eisturm unter einem Pad: Säule aus Packeis und Schnee, nach unten schmaler, mit Eiszapfen-Spitzen. */
    private static void serac(Plan plan, Pad p, SplittableRandom r) {
        int depth = 14 + r.nextInt(8);
        for (int k = 1; k <= depth; k++) {
            int shrink = k * 2 / depth;
            for (int x = p.x0 + Math.min(shrink, (p.x1 - p.x0) / 2); x <= p.x1 - Math.min(shrink, (p.x1 - p.x0) / 2); x++) {
                for (int z = p.z0 + Math.min(shrink, (p.z1 - p.z0) / 2); z <= p.z1 - Math.min(shrink, (p.z1 - p.z0) / 2); z++) {
                    Blk blk = r.nextInt(5) == 0 ? Blk.full("blue_ice") : (k < 3 ? SNOW : Blk.full("packed_ice"));
                    plan.decorate(x, p.y - k, z, blk);
                }
            }
        }
    }

    // =====================================================================================================
    // 3. Kletterwand (Mittel)
    // =====================================================================================================

    static Course kletterwand(Plan plan, int ox) {
        CourseBuilder b = new CourseBuilder(plan, "kletterwand", Difficulty.MITTEL, Dir.EAST)
                .name("Kletterwand", "Climbing Wall")
                .text("Leitern und schmale Felsbänder eine steile Wand hinauf – mit dem ersten Neo um eine Kante.",
                        "Ladders and narrow ledges up a steep wall – with your first neo around an edge.")
                .icon("ladder").biome("stony_peaks");
        b.start(ox, BASE_Y, 0, 3, 3);
        b.jump(1, 0, 0, 2, 2);
        b.jump(2, 1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.climb(6, 2, 1, STONE);
        b.jump(2, 0, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.checkpoint(2, 0, 0, 2, 2);
        b.turnLeft();
        b.jump(2, 0, 0, 1, 1);
        b.jump(3, 0, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.neo(1, 1, ANDESITE);
        b.jump(2, 0, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.checkpoint(2, 0, 0, 2, 2);
        b.turnRight();
        b.jump(1, 0, 0, 1, 1);
        b.climb(7, 1, 1, STONE);
        b.jump(2, 0, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.jump(3, -1, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.checkpoint(2, 0, 0, 2, 2);
        b.turnRight();
        b.jump(2, 0, 0, 1, 1);
        b.jump(3, 0, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.climbToCheckpoint(6, 2, 2, STONE);
        b.turnLeft();
        b.jump(2, 1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1);
        b.jump(3, 0, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.finish(2, 0, 0, 3, 3);
        Course c = b.build();
        long seed = 303;
        // Das Felsmassiv, um das die Strecke herumklettert (innen im „U“ der Strecke, Sprungwege bleiben frei).
        int mx0 = Integer.MAX_VALUE;
        int mx1 = Integer.MIN_VALUE;
        int mz0 = Integer.MAX_VALUE;
        int mz1 = Integer.MIN_VALUE;
        for (Pad p : c.pads) {
            if (p.index > 6) {
                mx0 = Math.min(mx0, p.x0);
                mx1 = Math.max(mx1, p.x1);
                mz0 = Math.min(mz0, p.z0);
                mz1 = Math.max(mz1, p.z1);
            }
        }
        Scenery.massif(plan, mx0 + 3, mz0 + 2, mx1 - 18, mz1 - 3, BASE_Y - 30, BASE_Y + 34, seed);
        for (Pad p : c.pads) {
            if (p.index % 3 == 0) {
                Scenery.rock(plan, (int) p.centerX(), p.y - 7, (int) p.centerZ() + 6, 3, Scenery.STONE,
                        Scenery.ANDESITE, seed + p.index);
            }
        }
        Scenery.panorama(plan, (int) c.start().centerX() + 10, BASE_Y + 10, 0, 85, 9, seed);
        return c;
    }

    // =====================================================================================================
    // 4. Felsgrat (Mittel)
    // =====================================================================================================

    static Course felsgrat(Plan plan, int ox) {
        Blk post = Blk.wall("cobblestone_wall");
        Blk slab = Blk.slab("andesite_slab");
        CourseBuilder b = new CourseBuilder(plan, "felsgrat", Difficulty.MITTEL, Dir.EAST)
                .name("Felsgrat", "Rocky Ridge")
                .text("Ein schmaler Grat hoch über dem Tal: Mauer-Pfosten, halbe Stufen und weite Sprünge.",
                        "A narrow ridge high above the valley: wall posts, half steps and long jumps.")
                .icon("cobblestone_wall").biome("stony_peaks")
                .pads(ANDESITE, Blk.WHITE);
        SplittableRandom r = new SplittableRandom(404);
        b.under(p -> spire(plan, p, r));
        b.start(ox, BASE_Y, 0, 3, 3);
        b.jump(2, 0, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1, post);
        b.jump(2, 0, 0, 1, 1, post);
        b.jump(2, 0, 0, 2, 1);
        b.jump(3, 0, 0, 1, 1);
        b.jump(1, 0, 1, 1, 1, slab);
        b.jump(1, 0, 0, 1, 1);
        b.checkpoint(2, 0, 0, 2, 2);
        b.turnRight();
        b.jump(3, -1, 0, 1, 1);
        b.jump(2, 0, 1, 1, 1);
        b.jump(2, 0, -1, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1, post);
        b.jump(2, 0, 0, 1, 1);
        b.checkpoint(3, 0, 0, 2, 2);
        b.turnLeft();
        b.jump(1, 0, 0, 1, 1, slab);
        b.jump(1, 0, 0, 1, 1);
        b.jump(1, 1, 0, 1, 1, slab);
        b.jump(1, 0, 0, 1, 1);
        b.jump(3, 0, 0, 1, 1);
        b.jump(2, 0, 1, 1, 1, post);
        b.jump(2, 0, 0, 1, 1, post);
        b.jump(2, 0, -1, 1, 1);
        b.checkpoint(4, -1, 0, 2, 2);
        b.turnLeft();
        b.jump(2, 1, 0, 1, 1);
        b.jump(2, 1, 1, 1, 1);
        b.jump(2, 0, -1, 1, 1);
        b.jump(3, -1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1, post);
        b.jump(2, 0, 0, 1, 1);
        b.finish(3, 0, 0, 3, 3);
        Course c = b.build();
        long seed = 404;
        Scenery.panorama(plan, (int) c.start().centerX() + 10, BASE_Y + 5, 0, 80, 10, seed);
        for (Pad p : c.pads) {
            if (p.index % 5 == 2) {
                Scenery.snowIsland(plan, (int) p.centerX() + 10, p.y - 18, (int) p.centerZ() - 10, 6, seed + p.index);
            }
        }
        return c;
    }

    /** Felsnadel unter einem Grat-Pad. */
    private static void spire(Plan plan, Pad p, SplittableRandom r) {
        int depth = 10 + r.nextInt(10);
        int cx = p.x0;
        int cz = p.z0;
        for (int k = 1; k <= depth; k++) {
            int rad = k / 6;
            for (int dx = -rad; dx <= rad + (p.x1 - p.x0); dx++) {
                for (int dz = -rad; dz <= rad + (p.z1 - p.z0); dz++) {
                    Blk blk = r.nextInt(4) == 0 ? Scenery.ANDESITE : (r.nextInt(7) == 0 ? Scenery.TUFF : Scenery.STONE);
                    plan.decorate(cx + dx, p.y - k, cz + dz, blk);
                }
            }
        }
    }

    // =====================================================================================================
    // 5. Seilbahn-Masten (Schwer)
    // =====================================================================================================

    static Course seilbahn(Plan plan, int ox) {
        Blk fence = Blk.fence("dark_oak_fence");
        CourseBuilder b = new CourseBuilder(plan, "seilbahn", Difficulty.SCHWER, Dir.EAST)
                .name("Seilbahn-Masten", "Cable Car Pylons")
                .text("Über Masten, Gondel-Dächer und Zaun-Pfosten der alten Seilbahn – mit Schleim-Sprung.",
                        "Across the pylons, gondola roofs and fence posts of the old cable car – with a slime bounce.")
                .icon("iron_chain").biome("windswept_hills");
        b.start(ox, BASE_Y, 0, 3, 3);
        b.jump(2, 0, 0, 1, 1);
        b.jump(3, 0, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.jump(2, 0, 0, 2, 2, RED);
        Pad gondel1 = b.current();
        b.jump(2, 0, 0, 1, 1, fence);
        b.jump(2, 0, 0, 1, 1, fence);
        b.jump(2, 0, 0, 1, 1);
        b.checkpoint(3, 0, 0, 2, 2);
        Pad mast1 = b.current();
        b.jump(1, 0, 0, 1, 1);
        b.climb(8, 2, 2, IRON);
        b.jump(3, 0, 0, 1, 1);
        b.jump(4, -1, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.jump(3, 0, 0, 2, 2, RED);
        Pad gondel2 = b.current();
        b.checkpoint(2, 0, 0, 2, 2);
        b.turnRight();
        b.jump(2, 0, 0, 1, 1, fence);
        b.jump(2, 0, 0, 1, 1, fence);
        b.jump(2, 1, 0, 1, 1);
        b.bounce(6, 3, 1, 2, 2);
        b.jump(3, 0, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.checkpoint(3, 0, 0, 2, 2);
        b.turnLeft();
        b.jump(3, 0, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1, fence);
        b.jump(2, 1, 0, 1, 1);
        b.jump(4, -1, 0, 1, 1);
        b.jump(3, 0, 0, 2, 2, RED);
        b.checkpoint(2, 0, 0, 2, 2);
        b.jump(2, 0, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1, fence);
        b.jump(2, 0, 0, 1, 1);
        b.finish(3, 0, 0, 3, 3);
        Course c = b.build();
        gondola(plan, gondel1);
        gondola(plan, gondel2);
        pylon(plan, mast1);
        long seed = 505;
        List<Pad> masts = new ArrayList<>();
        masts.add(c.start());
        for (Pad p : c.pads) {
            if (p.role == Pad.Role.CHECKPOINT) {
                pylon(plan, p);
                masts.add(p);
            }
        }
        masts.add(c.finish());
        // Tragseile hoch über der Strecke von Mast zu Mast
        for (int i = 1; i < masts.size(); i++) {
            cable(plan, masts.get(i - 1), masts.get(i));
        }
        Scenery.panorama(plan, (int) c.start().centerX() + 20, BASE_Y, 0, 90, 10, seed);
        for (int i = 0; i < 6; i++) {
            Pad p = c.pads.get(i * 5 % c.pads.size());
            Scenery.meadowIsland(plan, (int) p.centerX() + 6, p.y - 22, (int) p.centerZ() + 8, 6, seed + i);
        }
        return c;
    }

    /** Gondel unter einem Dach-Pad: Kabine mit Fenstern, Bügel nach oben. */
    private static void gondola(Plan plan, Pad roof) {
        for (int x = roof.x0 - 1; x <= roof.x1 + 1; x++) {
            for (int z = roof.z0 - 1; z <= roof.z1 + 1; z++) {
                boolean edge = x == roof.x0 - 1 || x == roof.x1 + 1 || z == roof.z0 - 1 || z == roof.z1 + 1;
                for (int k = 1; k <= 3; k++) {
                    Blk blk = edge ? (k == 2 ? Blk.full("glass") : Blk.full("white_concrete")) : Blk.AIR;
                    if (blk != Blk.AIR) {
                        plan.decorate(x, roof.y - k, z, blk);
                    }
                }
                plan.decorate(x, roof.y - 4, z, Blk.full("red_concrete"));
            }
        }
    }

    /** Seil (Ketten) zwischen zwei Masten: je ein Seilmast neben dem Pad, oben das leicht durchhängende Seil. */
    private static void cable(Plan plan, Pad a, Pad b) {
        int[] pa = {(int) a.centerX(), a.y + 7, a.z1 + 2};
        int[] pb = {(int) b.centerX(), b.y + 7, b.z1 + 2};
        for (int[] m : new int[][] {pa, pb}) {
            for (int y = m[1] - 6; y < m[1]; y++) {
                plan.decorate(m[0], y, m[2], Blk.full("iron_block"));
            }
        }
        int steps = Math.max(Math.abs(pb[0] - pa[0]), Math.abs(pb[2] - pa[2]));
        boolean alongX = Math.abs(pb[0] - pa[0]) >= Math.abs(pb[2] - pa[2]);
        Blk chain = new Blk("minecraft:iron_chain[axis=" + (alongX ? "x" : "z") + "]",
                net.morni.gipfelsturm.course.Shape.CHAIN, net.morni.gipfelsturm.course.Surface.NORMAL);
        for (int k = 0; k <= steps; k++) {
            double t = steps == 0 ? 0 : (double) k / steps;
            double sag = Math.sin(Math.PI * t) * Math.min(3, steps / 12.0);
            int x = (int) Math.round(pa[0] + (pb[0] - pa[0]) * t);
            int z = (int) Math.round(pa[2] + (pb[2] - pa[2]) * t);
            int y = (int) Math.round(pa[1] + (pb[1] - pa[1]) * t - sag);
            plan.decorate(x, y, z, chain);
        }
    }

    /** Stahlmast unter einem Pad. */
    private static void pylon(Plan plan, Pad top) {
        int x = (top.x0 + top.x1) / 2;
        int z = (top.z0 + top.z1) / 2;
        for (int k = 1; k <= 26; k++) {
            plan.decorate(x, top.y - k, z, k % 5 == 0 ? IRON : Blk.full("iron_block"));
            if (k % 4 == 0) {
                plan.decorate(x + 1, top.y - k, z, Blk.bars());
                plan.decorate(x - 1, top.y - k, z, Blk.bars());
            }
        }
    }

    // =====================================================================================================
    // 6. Eishöhle (Schwer)
    // =====================================================================================================

    static Course eishoehle(Plan plan, int ox) {
        Blk ceilingBlk = Blk.full("packed_ice");
        CourseBuilder b = new CourseBuilder(plan, "eishoehle", Difficulty.SCHWER, Dir.EAST)
                .name("Eishöhle", "Ice Cave")
                .text("Tief im Gletscher: niedrige Decken (Kopfstoß!), Blaueis zum Rutschen, zäher Matsch und "
                        + "eiskaltes Wasser.", "Deep in the glacier: low ceilings (head hitters!), blue ice to slide on, "
                        + "sticky mud and ice-cold water.")
                .icon("packed_ice").biome("ice_spikes")
                .pads(Blk.full("light_blue_concrete"), Blk.WHITE);
        b.start(ox, BASE_Y, 0, 3, 3);
        b.jump(2, 0, 0, 1, 1);
        b.jump(2, 0, 0, 2, 1);
        // Kopfstoß-Gang: hineinlaufen, dann mit kurzen, flachen Sprüngen unter der Decke durch.
        b.ceilingOn(2.0, ceilingBlk);
        b.jump(0, 0, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.ceilingOff();
        b.jump(1, 0, 0, 1, 1);
        b.checkpoint(2, 0, 0, 2, 2);
        b.jump(1, 0, 0, 4, 1, Blk.BLUE_ICE);
        b.jump(2, 0, 0, 4, 1, Blk.BLUE_ICE);
        b.jump(3, 0, 0, 4, 1, Blk.BLUE_ICE);
        b.jump(2, 0, 0, 3, 3);
        b.checkpoint(2, 0, 0, 2, 2);
        b.turnLeft();
        b.jump(1, 0, 0, 3, 1, Blk.SOUL_SAND);
        b.jump(1, 0, 0, 1, 1);
        b.jump(1, 0, 0, 2, 1, Blk.SOUL_SAND);
        b.jump(1, 0, 0, 1, 1);
        b.jump(2, 0, 0, 2, 1);
        b.ceilingOn(2.5, Blk.topSlab("smooth_stone_slab"));
        b.jump(0, 0, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1);
        b.ceilingOff();
        b.jump(2, 0, 0, 1, 1);
        b.checkpoint(2, 0, 0, 2, 2);
        b.turnRight();
        b.jump(2, 0, 0, 1, 1);
        b.bounce(5, 3, 1, 1, 1);
        b.jump(2, 0, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.checkpoint(2, 0, 0, 2, 2);
        b.jump(2, 0, 0, 2, 1);
        b.ceilingOn(2.0, ceilingBlk);
        b.jump(0, 0, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.ceilingOff();
        b.jump(1, 0, 0, 3, 1, Blk.BLUE_ICE);
        b.jump(3, 0, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.finish(2, 0, 0, 3, 3);
        // Eiskaltes Wasser auf dem Höhlenboden: Wer hineinfällt, muss zurück zum Checkpoint.
        int low = b.pads().stream().mapToInt(p -> p.y).min().orElse(BASE_Y);
        int wy = low - 3;
        int x0 = b.pads().stream().mapToInt(p -> p.x0).min().orElse(ox) - 8;
        int x1 = b.pads().stream().mapToInt(p -> p.x1).max().orElse(ox) + 8;
        int z0 = b.pads().stream().mapToInt(p -> p.z0).min().orElse(0) - 8;
        int z1 = b.pads().stream().mapToInt(p -> p.z1).max().orElse(0) + 8;
        b.hazard(x0, wy - 30, z0, x1, wy, z1, "Wasser");
        Course c = b.build();
        cave(plan, c);
        for (Pad p : c.pads) {
            for (int x = p.x0 - 3; x <= p.x1 + 3; x++) {
                for (int z = p.z0 - 3; z <= p.z1 + 3; z++) {
                    plan.decorate(x, wy, z, Blk.WATER);
                    plan.decorate(x, wy - 1, z, Blk.full("packed_ice"));
                }
            }
        }
        return c;
    }

    /** Höhlen-Hülle aus Eis um die Strecke (nur Wände und Decke, innen frei). */
    private static void cave(Plan plan, Course c) {
        SplittableRandom r = new SplittableRandom(606);
        java.util.Set<Long> inside = new java.util.HashSet<>();
        for (Pad p : c.pads) {
            for (int x = p.x0 - 4; x <= p.x1 + 4; x++) {
                for (int z = p.z0 - 4; z <= p.z1 + 4; z++) {
                    for (int y = p.y - 4; y <= p.y + 7; y++) {
                        inside.add(Plan.key(x, y, z));
                    }
                }
            }
        }
        for (long k : inside) {
            int x = Plan.x(k);
            int y = Plan.y(k);
            int z = Plan.z(k);
            for (int[] d : new int[][] {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}}) {
                long n = Plan.key(x + d[0], y + d[1], z + d[2]);
                if (!inside.contains(n)) {
                    Blk blk = r.nextInt(9) == 0 ? Blk.full("sea_lantern")
                            : (r.nextInt(3) == 0 ? Blk.full("blue_ice") : Blk.full("packed_ice"));
                    plan.decorate(x + d[0], y + d[1], z + d[2], blk);
                }
            }
        }
    }

    // =====================================================================================================
    // 7. Gipfelkreuz-Sprint (Extrem)
    // =====================================================================================================

    static Course gipfelkreuz(Plan plan, int ox) {
        Blk fence = Blk.fence("spruce_fence");
        Blk post = Blk.wall("stone_brick_wall");
        CourseBuilder b = new CourseBuilder(plan, "gipfelkreuz", Difficulty.EXTREM, Dir.EAST)
                .name("Gipfelkreuz-Sprint", "Summit Cross Sprint")
                .text("Der Sprint zum Gipfelkreuz: 3+1-Sprünge, Vierer über den Abgrund, Neo um dicke Wände – nur für "
                        + "Könner.", "The sprint to the summit cross: 3+1 jumps, four-block gaps over the abyss, neos "
                        + "around thick walls – experts only.")
                .icon("lightning_rod").biome("snowy_slopes");
        b.start(ox, BASE_Y, 0, 3, 3);
        b.jump(3, 0, 0, 1, 1);
        b.jump(3, 0, 0, 3, 1);
        b.jump(3, 1, 0, 1, 1);
        b.jump(4, -1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1, fence);
        b.jump(2, 0, 0, 1, 1, fence);
        b.jump(2, 0, 0, 1, 1);
        b.checkpoint(3, 0, 0, 1, 1);
        b.neo(2, -1, STONE_BRICKS);
        b.jump(3, 0, 0, 3, 1);
        b.jump(3, 1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1, post);
        b.jump(2, 1, 0, 1, 1);
        b.checkpoint(4, -1, 0, 1, 1);
        b.turnRight();
        b.jump(1, 0, 0, 3, 1, Blk.SOUL_SAND);
        b.jump(2, 0, 0, 1, 1);
        b.jump(1, 1, 0, 1, 1, Blk.SOUL_SAND);
        b.jump(1, 1, 0, 1, 1);
        b.jump(3, 0, 0, 1, 1);
        b.checkpoint(4, -1, 0, 1, 1);
        b.turnLeft();
        b.jump(2, 0, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.bounce(8, 4, 1, 1, 1);
        b.jump(2, 0, 0, 1, 1, fence);
        b.jump(2, -1, 0, 1, 1);
        b.jump(2, 0, 0, 5, 1, Blk.PACKED_ICE);
        b.jump(3, 0, 0, 1, 1);
        b.checkpoint(3, 0, 0, 1, 1);
        b.neo(1, 1, STONE_BRICKS);
        b.jump(3, 0, 0, 3, 1);
        b.jump(3, 1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1, Blk.bars());
        b.jump(2, 0, 0, 1, 1);
        b.checkpoint(2, 1, 0, 1, 1);
        b.jump(4, -1, 0, 1, 1);
        b.jump(3, 0, 0, 3, 1);
        b.jump(3, 1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1, post);
        b.jump(2, 1, 0, 1, 1);
        b.finish(3, 0, 0, 5, 5);
        Course c = b.build();
        Pad fin = c.finish();
        summitCross(plan, fin.x0 + 2, fin.y + 1, fin.z0 - 3);
        long seed = 707;
        for (Pad p : c.pads) {
            if (p.index % 4 == 0) {
                Scenery.peak(plan, (int) p.centerX(), p.y - 40, (int) p.centerZ() + 18, 10, 30, seed + p.index);
            }
        }
        Scenery.panorama(plan, (int) c.start().centerX() + 30, BASE_Y, 0, 100, 12, seed);
        return c;
    }

    /** Großes Gipfelkreuz aus Holz mit kleinem Dach (neben dem Ziel). */
    private static void summitCross(Plan plan, int x, int y, int z) {
        Blk log = Blk.full("dark_oak_log", "axis=y");
        Blk beam = Blk.full("dark_oak_log", "axis=x");
        for (int k = 0; k < 11; k++) {
            plan.decorate(x, y + k, z, log);
        }
        for (int dx = -3; dx <= 3; dx++) {
            plan.decorate(x + dx, y + 7, z, dx == 0 ? log : beam);
        }
        plan.decorate(x, y + 11, z, Blk.deco("dark_oak_slab[type=bottom]"));
        plan.decorate(x, y + 7, z + 1, Blk.deco("lantern[hanging=false]"));
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                plan.decorate(x + dx, y - 1, z + dz, Blk.full("cobblestone"));
            }
        }
    }

    // =====================================================================================================
    // 8. Wetterturm (Extrem)
    // =====================================================================================================

    static Course wetterturm(Plan plan, int ox) {
        Blk bars = Blk.bars();
        CourseBuilder b = new CourseBuilder(plan, "wetterturm", Difficulty.EXTREM, Dir.EAST)
                .name("Wetterturm", "Weather Tower")
                .text("Rund um den alten Wetterturm hinauf bis zur Station ganz oben – Schleim, Leitern, Eis und "
                        + "Gitter-Pfosten.", "Up and around the old weather tower to the station at the very top – "
                        + "slime, ladders, ice and bar posts.")
                .icon("lightning_rod").biome("windswept_gravelly_hills");
        b.start(ox, BASE_Y, 0, 3, 3);
        b.jump(3, 0, 0, 1, 1);
        b.jump(2, 1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1, bars);
        b.jump(2, 0, 0, 1, 1);
        b.climb(10, 1, 1, STONE_BRICKS);
        b.jump(3, 0, 0, 1, 1);
        b.checkpoint(2, 1, 0, 1, 1);
        b.turnLeft();
        b.jump(3, 0, 0, 3, 1);
        b.jump(3, 1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1, Blk.fence("spruce_fence"));
        b.jump(2, 0, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.bounce(6, 3, 1, 1, 1);
        b.jump(3, 0, 0, 1, 1);
        b.checkpoint(2, 1, 0, 1, 1);
        b.turnLeft();
        b.jump(2, 0, 0, 4, 1, Blk.BLUE_ICE);
        b.jump(3, 0, 0, 4, 1, Blk.BLUE_ICE);
        b.jump(3, 0, 0, 1, 1);
        b.jump(1, 1, 0, 1, 1, bars);
        b.jump(2, 0, 0, 1, 1);
        b.neo(2, 1, STONE_BRICKS);
        b.checkpoint(3, 0, 0, 1, 1);
        b.turnLeft();
        b.jump(1, 0, 0, 1, 1);
        b.climb(9, 1, 1, STONE_BRICKS);
        b.jump(3, 0, 0, 3, 1);
        b.jump(3, 1, 0, 1, 1);
        b.jump(4, -1, 0, 1, 1);
        b.jump(2, 0, 0, 1, 1, Blk.wall("stone_brick_wall"));
        b.jump(2, 1, 0, 1, 1);
        b.checkpoint(3, 0, 0, 1, 1);
        b.turnRight();
        b.jump(2, 0, 0, 1, 1, bars);
        b.jump(2, 0, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.bounce(8, 4, 1, 1, 1);
        b.jump(3, 0, 0, 3, 1);
        b.jump(3, 1, 0, 1, 1);
        b.checkpoint(3, 0, 0, 1, 1);
        b.turnRight();
        b.jump(2, 0, 0, 1, 1, Blk.fence("spruce_fence"));
        b.jump(2, 0, 0, 1, 1);
        b.jump(1, 0, 0, 1, 1);
        b.climb(8, 1, 1, STONE_BRICKS);
        b.jump(2, 1, 0, 1, 1);
        b.jump(3, 0, 0, 1, 1);
        b.finish(2, 1, 0, 5, 5);
        Course c = b.build();
        Pad fin = c.finish();
        weatherStation(plan, fin);
        long seed = 808;
        // Der Wetterturm selbst steht in der Mitte der ersten Runde.
        Pad cp1 = c.checkpoints().get(0);
        Pad cp2 = c.checkpoints().get(1);
        Scenery.tower(plan, (cp1.x0 + c.start().x0) / 2 + 10, (cp2.z0 + c.start().z0) / 2, BASE_Y - 20, BASE_Y + 34,
                seed);
        for (Pad p : c.pads) {
            if (p.index % 6 == 3) {
                Scenery.rock(plan, (int) p.centerX() + 7, p.y - 14, (int) p.centerZ() + 7, 4, Scenery.STONE,
                        Scenery.TUFF, seed + p.index);
            }
        }
        Scenery.panorama(plan, (int) c.start().centerX() + 20, BASE_Y + 10, 20, 100, 12, seed);
        return c;
    }

    /** Wetterstation auf dem Zielplatz: Häuschen, Windmesser, Blitzableiter. */
    private static void weatherStation(Plan plan, Pad fin) {
        int x = fin.x1 + 2;
        int z = fin.z0;
        int y = fin.y;
        for (int dx = 0; dx <= 3; dx++) {
            for (int dz = 0; dz <= 4; dz++) {
                plan.decorate(x + dx, y, z + dz, STONE_BRICKS);
                boolean edge = dx == 0 || dx == 3 || dz == 0 || dz == 4;
                for (int k = 1; k <= 3 && edge; k++) {
                    plan.decorate(x + dx, y + k, z + dz, k == 2 && dz == 2 ? Blk.deco("glass_pane") : Blk.full("white_concrete"));
                }
                plan.decorate(x + dx, y + 4, z + dz, Blk.full("light_blue_concrete"));
            }
        }
        for (int k = 5; k <= 9; k++) {
            plan.decorate(x + 1, y + k, z + 2, Blk.full("iron_bars"));
        }
        plan.decorate(x + 1, y + 10, z + 2, Blk.deco("lightning_rod"));
        plan.decorate(x + 2, y + 9, z + 2, Blk.deco("end_rod[facing=east]"));
        plan.decorate(x, y + 9, z + 2, Blk.deco("end_rod[facing=west]"));
    }
}
