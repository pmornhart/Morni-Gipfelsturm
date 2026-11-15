package de.moinmorni.gipfelsturm.courses;

import de.moinmorni.gipfelsturm.course.Blk;
import de.moinmorni.gipfelsturm.course.Box;
import de.moinmorni.gipfelsturm.course.Course;
import de.moinmorni.gipfelsturm.course.Plan;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Das Basislager: ein Platz auf einer Bergschulter mit Berghütte, Rauten-Mosaik in Weiß-Blau, einer Wegweiser-Station
 * pro Strecke (Schild zum Anklicken + Bestenliste darüber) und der Tafel „Strecke der Woche“ in der Mitte.
 */
public final class Basecamp {

    /** Eine Station: Schild (anklickbar) und Platz für die Bestenliste darüber. */
    public record Station(String course, int signX, int signY, int signZ, double boardX, double boardY, double boardZ) {
    }

    public static final int Y = 100;
    private static final int PLAZA = 17;
    private static final int ISLAND = 27;
    private static final int STATION_RADIUS = 13;

    private final List<Station> stations = new ArrayList<>();
    private final Box bounds = new Box(-120, 40, -120, 120, 190, 120);

    public double spawnX() {
        return 0.5;
    }

    public double spawnY() {
        return Y;
    }

    public double spawnZ() {
        return 10.5;
    }

    /** Blick nach Norden (zur Hütte und den Stationen). */
    public float spawnYaw() {
        return 180f;
    }

    /** Tafel „Strecke der Woche“ (Mitte über dem Mosaik). */
    public double[] weeklyBoard() {
        return new double[] {0.5, Y + 4.6, 0.5};
    }

    /** Begrüßungs-Tafel über dem Eingang der Hütte. */
    public double[] hutSign() {
        return new double[] {0.5, Y + 13.2, -15.4};
    }

    public List<Station> stations() {
        return stations;
    }

    public Box bounds() {
        return bounds;
    }

    /** Baut das Basislager in den Plan; eine Station je Strecke. */
    public void build(Plan plan, List<Course> courses) {
        island(plan);
        plaza(plan);
        hut(plan);
        for (int i = 0; i < courses.size(); i++) {
            station(plan, courses.get(i), i, courses.size());
        }
        decor(plan);
        Scenery.panorama(plan, 0, Y, 0, 75, 12, 4242);
    }

    // ---------- Insel und Platz ----------

    private void island(Plan plan) {
        SplittableRandom r = new SplittableRandom(99);
        for (int x = -ISLAND; x <= ISLAND; x++) {
            for (int z = -ISLAND - 6; z <= ISLAND; z++) {
                double d = Math.hypot(x, z * (z < 0 ? 0.82 : 1.0));
                double edge = ISLAND - 1.5 + r.nextDouble() * 2.5;
                if (d > edge) {
                    continue;
                }
                plan.set(x, Y - 1, z, Scenery.GRASS);
                int depth = (int) Math.round(34 * Math.pow(1 - d / (edge + 1), 0.8)) + 2;
                for (int k = 2; k <= depth; k++) {
                    Blk b = k <= 3 ? Scenery.DIRT : (r.nextInt(6) == 0 ? Scenery.ANDESITE
                            : (r.nextInt(11) == 0 ? Scenery.TUFF : Scenery.STONE));
                    plan.set(x, Y - k, z, b);
                }
                if (d > PLAZA + 1 && r.nextInt(5) == 0) {
                    String[] flowers = {"cornflower", "oxeye_daisy", "dandelion", "allium", "azure_bluet", "short_grass",
                        "short_grass", "poppy"};
                    plan.set(x, Y, z, Blk.deco(flowers[r.nextInt(flowers.length)]));
                }
            }
        }
    }

    private void plaza(Plan plan) {
        Blk paving = Blk.full("polished_andesite");
        Blk paving2 = Blk.full("stone_bricks");
        Blk gold = Blk.full("gold_block");
        Blk emerald = Blk.full("emerald_block");
        for (int x = -PLAZA; x <= PLAZA; x++) {
            for (int z = -PLAZA; z <= PLAZA; z++) {
                double d = Math.hypot(x + 0.5, z + 0.5);
                if (d > PLAZA) {
                    continue;
                }
                Blk b;
                if (d <= 6.5) {
                    // Rauten (bayerisches Muster): ein um 45° gedrehtes Schachbrett in Weiß und Hellblau. Die Breite muss
                    // ungerade sein – bei 2 hätten x+z und x−z dieselbe Parität, und es gäbe nur Streifen.
                    int u = Math.floorDiv(x + z + 1, 3);
                    int v = Math.floorDiv(x - z + 1, 3);
                    b = Math.floorMod(u + v, 2) == 0 ? Blk.WHITE : Blk.LIGHT_BLUE;
                } else if (d <= 7.5) {
                    b = (Math.floorMod(x * 7 + z * 3, 9) == 0) ? emerald : gold;
                } else if (d > PLAZA - 1) {
                    b = paving2;
                } else {
                    b = Math.floorMod(x * 31 + z * 17, 7) == 0 ? Blk.full("andesite") : paving;
                }
                plan.set(x, Y - 1, z, b);
                plan.remove(x, Y, z);
            }
        }
    }

    // ---------- Berghütte ----------

    private void hut(Plan plan) {
        int x0 = -9;
        int x1 = 9;
        int z0 = -27;
        int z1 = -17;
        Blk stone = Blk.full("cobblestone");
        Blk bricks = Blk.full("stone_bricks");
        Blk plaster = Blk.full("calcite");
        Blk beamY = Blk.full("dark_oak_log", "axis=y");
        Blk floor = Blk.full("spruce_planks");
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                plan.set(x, Y - 1, z, stone);
                plan.set(x, Y, z, floor);
                boolean edge = x == x0 || x == x1 || z == z0 || z == z1;
                boolean corner = (x == x0 || x == x1) && (z == z0 || z == z1);
                for (int y = Y + 1; y <= Y + 8; y++) {
                    if (!edge) {
                        if (y == Y + 5) {
                            plan.set(x, y, z, floor);
                        } else {
                            plan.remove(x, y, z);
                        }
                        continue;
                    }
                    boolean upper = y >= Y + 5;
                    Blk wall = corner ? beamY : (upper ? (Math.floorMod(x, 4) == 0 || Math.floorMod(z, 4) == 0
                            ? beamY : plaster) : (y == Y + 1 ? bricks : stone));
                    boolean windowRow = y == Y + 2 || y == Y + 3 || y == Y + 6 || y == Y + 7;
                    boolean windowCol = (z == z1 || z == z0) ? Math.floorMod(x, 4) == 2 && Math.abs(x) > 2
                            : Math.floorMod(z, 3) == 1;
                    if (!corner && windowRow && windowCol) {
                        wall = Blk.deco("glass_pane");
                    }
                    plan.set(x, y, z, wall);
                }
            }
        }
        // Tür mitten in der Front (Süden)
        plan.set(0, Y + 1, z1, Blk.deco("spruce_door[half=lower,facing=south,hinge=left]"));
        plan.set(0, Y + 2, z1, Blk.deco("spruce_door[half=upper,facing=south,hinge=left]"));
        // Balkon im Obergeschoss mit Geländer und Blumenkästen
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            plan.set(x, Y + 4, z1 + 1, Blk.topSlab("spruce_slab"));
            plan.set(x, Y + 5, z1 + 2, Blk.fence("spruce_fence"));
            if (Math.floorMod(x, 3) == 0) {
                plan.set(x, Y + 6, z1 + 2, Scenery.pot(Math.floorMod(x, 2) == 0 ? "red_tulip" : "blue_orchid"));
            }
        }
        for (int x = x0 + 1; x <= x1 - 1; x++) {
            plan.set(x, Y + 4, z1 + 2, Blk.topSlab("spruce_slab"));
        }
        // Satteldach (First in Ost-West-Richtung) mit Überstand
        int ridge = (z0 + z1) / 2;
        for (int k = 0; k <= 6; k++) {
            int y = Y + 9 + k;
            int zs = z1 + 2 - k;
            int zn = z0 - 2 + k;
            for (int x = x0 - 2; x <= x1 + 2; x++) {
                if (zs > ridge) {
                    plan.set(x, y, zs, Blk.of("spruce_stairs[facing=north]", de.moinmorni.gipfelsturm.course.Shape.FULL));
                }
                if (zn < ridge) {
                    plan.set(x, y, zn, Blk.of("spruce_stairs[facing=south]", de.moinmorni.gipfelsturm.course.Shape.FULL));
                }
                if (zs <= ridge || zn >= ridge) {
                    plan.set(x, y, ridge, Blk.slab("spruce_slab"));
                }
                // Giebel innen füllen
                for (int z = zn + 1; z < zs; z++) {
                    if (x == x0 || x == x1) {
                        plan.set(x, y, z, plaster);
                    } else if (x > x0 && x < x1) {
                        plan.remove(x, y, z);
                    }
                }
            }
            if (zs <= ridge) {
                break;
            }
        }
        // Kamin
        for (int y = Y + 1; y <= Y + 16; y++) {
            plan.set(x1 - 3, y, z0 + 2, Blk.full("bricks"));
        }
        plan.set(x1 - 3, Y + 17, z0 + 2, Blk.deco("campfire[lit=true,signal_fire=false]"));
        // Bänke vor der Hütte
        for (int x : new int[] {-6, -5, 5, 6}) {
            plan.set(x, Y, z1 + 3, Blk.of("spruce_stairs[facing=north]", de.moinmorni.gipfelsturm.course.Shape.FULL));
        }
        // Laternen an der Front
        plan.set(-2, Y + 3, z1 + 1, Blk.deco("lantern[hanging=false]"));
        plan.set(2, Y + 3, z1 + 1, Blk.deco("lantern[hanging=false]"));
        plan.set(-2, Y + 2, z1 + 1, Blk.full("spruce_planks"));
        plan.set(2, Y + 2, z1 + 1, Blk.full("spruce_planks"));
    }

    // ---------- Stationen ----------

    private void station(Plan plan, Course c, int i, int n) {
        double from = Math.toRadians(200);
        double to = Math.toRadians(340);
        double a = n == 1 ? Math.toRadians(270) : from + (to - from) * i / (n - 1);
        int x = (int) Math.round(Math.cos(a) * STATION_RADIUS);
        int z = (int) Math.round(Math.sin(a) * STATION_RADIUS);
        // Sockel in der Farbe der Schwierigkeit, Pfosten, Schild zur Platzmitte
        plan.set(x, Y - 1, z, Blk.full(c.difficulty.block));
        plan.set(x, Y, z, Blk.full("spruce_log", "axis=y"));
        double face = Math.atan2(-z, -x);
        int rotation = Math.floorMod((int) Math.round((Math.toDegrees(face) - 90) / 22.5), 16);
        plan.set(x, Y + 1, z, Blk.deco("spruce_sign[rotation=" + rotation + "]"));
        plan.sign(x, Y + 1, z, signLines(c));
        stations.add(new Station(c.id, x, Y + 1, z, x + 0.5, Y + 2.3, z + 0.5));
        // Laterne daneben
        double side = a + Math.PI / 2;
        int lx = x + (int) Math.round(Math.cos(side) * 1.6);
        int lz = z + (int) Math.round(Math.sin(side) * 1.6);
        if (Math.hypot(lx, lz) < PLAZA - 1) {
            plan.set(lx, Y, lz, Blk.fence("spruce_fence"));
            plan.set(lx, Y + 1, lz, Blk.deco("lantern[hanging=false]"));
        }
    }

    /** Schildtext: Name (lange Namen am Bindestrich umbrochen), Schwierigkeit, Hinweis. */
    static List<String> signLines(Course c) {
        String name = c.nameDe;
        String first = name;
        String second = "";
        int dash = name.indexOf('-');
        if (name.length() > 14 && dash > 0) {
            first = name.substring(0, dash + 1);
            second = name.substring(dash + 1);
        }
        return List.of(first, second, c.difficulty.stars() + " " + c.difficulty.de, "▶ Klick: Start");
    }

    // ---------- Schmuck ----------

    private void decor(Plan plan) {
        SplittableRandom r = new SplittableRandom(7);
        // Fichten am Rand der Insel
        for (int i = 0; i < 14; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            int d = PLAZA + 3 + r.nextInt(5);
            int x = (int) Math.round(Math.cos(a) * d);
            int z = (int) Math.round(Math.sin(a) * d);
            if (z < -15 && Math.abs(x) < 13) {
                continue; // nicht in die Hütte
            }
            Scenery.spruce(plan, x, Y, z, 6 + r.nextInt(4), false);
        }
        // Brunnen-Trog im Südwesten
        int bx = -10;
        int bz = 9;
        for (int x = bx; x <= bx + 3; x++) {
            for (int z = bz; z <= bz + 1; z++) {
                boolean edge = x == bx || x == bx + 3;
                plan.set(x, Y, z, edge ? Blk.full("spruce_log", "axis=z") : Blk.WATER);
                plan.set(x, Y - 1, z, Blk.full("spruce_planks"));
            }
        }
        plan.set(bx + 1, Y, bz - 1, Blk.full("spruce_planks"));
        plan.set(bx + 1, Y + 1, bz - 1, Blk.fence("spruce_fence"));
        plan.set(bx + 1, Y + 2, bz - 1, Blk.full("spruce_log", "axis=z"));
        // Fahnenmasten (weiß-blau) an der Hütte
        for (int x : new int[] {-13, 13}) {
            for (int y = Y; y <= Y + 9; y++) {
                plan.set(x, y, -15, Blk.fence("spruce_fence"));
            }
            plan.set(x, Y + 10, -15, Blk.deco("light_blue_banner[rotation=0]"));
        }
        // Felsen und ein kleines Gipfelkreuz hinter der Hütte
        Scenery.rock(plan, -20, Y + 1, -24, 4, Scenery.STONE, Scenery.ANDESITE, 11);
        Scenery.rock(plan, 19, Y + 1, -21, 3, Scenery.STONE, Scenery.TUFF, 12);
        for (int y = Y + 4; y <= Y + 10; y++) {
            plan.decorate(-20, y, -24, Blk.full("dark_oak_log", "axis=y"));
        }
        for (int x = -22; x <= -18; x++) {
            plan.decorate(x, Y + 8, -24, Blk.full("dark_oak_log", "axis=x"));
        }
    }
}
