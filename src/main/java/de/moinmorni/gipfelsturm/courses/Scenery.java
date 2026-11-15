package de.moinmorni.gipfelsturm.courses;

import de.moinmorni.gipfelsturm.course.Blk;
import de.moinmorni.gipfelsturm.course.Plan;
import java.util.SplittableRandom;

/**
 * Landschaft aus dem Code: schwebende Almen, Felsen, Fichten, Berggipfel, Wolken. Alles über {@link Plan#decorate}, also
 * nie in Sprungwegen. Zufall mit festem Startwert – jeder Bau sieht gleich aus.
 */
public final class Scenery {

    public static final Blk GRASS = Blk.full("grass_block");
    public static final Blk DIRT = Blk.full("dirt");
    public static final Blk COARSE = Blk.full("coarse_dirt");
    public static final Blk STONE = Blk.full("stone");
    public static final Blk ANDESITE = Blk.full("andesite");
    public static final Blk TUFF = Blk.full("tuff");
    public static final Blk CALCITE = Blk.full("calcite");
    public static final Blk SNOW = Blk.full("snow_block");
    public static final Blk POWDER = Blk.deco("snow[layers=2]");
    public static final Blk SPRUCE_LOG = Blk.full("spruce_log", "axis=y");
    public static final Blk SPRUCE_LEAVES = Blk.full("spruce_leaves", "persistent=true");
    public static final Blk CLOUD = Blk.full("white_concrete_powder");
    public static final Blk CLOUD_LIGHT = Blk.full("white_wool");

    private static final String[] FLOWERS = {"poppy", "dandelion", "cornflower", "oxeye_daisy", "azure_bluet",
        "allium", "short_grass", "short_grass", "short_grass", "lily_of_the_valley"};

    private Scenery() {
    }

    /** Schwebende Alm: oben Gras mit Blumen, darunter Erde und Fels, nach unten spitz. */
    public static void meadowIsland(Plan p, int cx, int topY, int cz, int radius, long seed) {
        island(p, cx, topY, cz, radius, seed, GRASS, true, false);
    }

    /** Schneebedeckte Insel (Gletscher, Grat). */
    public static void snowIsland(Plan p, int cx, int topY, int cz, int radius, long seed) {
        island(p, cx, topY, cz, radius, seed, SNOW, false, true);
    }

    private static void island(Plan p, int cx, int topY, int cz, int radius, long seed, Blk top, boolean flowers,
                               boolean snowy) {
        SplittableRandom r = new SplittableRandom(seed);
        int depth = radius + 3 + r.nextInt(radius + 1);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.hypot(dx, dz) + r.nextDouble() * 0.8;
                if (d > radius) {
                    continue;
                }
                int x = cx + dx;
                int z = cz + dz;
                // Tiefe nimmt nach außen ab: unten spitz wie ein umgedrehter Kegel.
                int down = (int) Math.round(depth * (1 - d / (radius + 0.5)));
                p.decorate(x, topY, z, top);
                for (int k = 1; k <= Math.max(1, down); k++) {
                    Blk b = k <= 2 ? (snowy ? SNOW : DIRT) : (r.nextInt(5) == 0 ? ANDESITE : STONE);
                    p.decorate(x, topY - k, z, b);
                }
                if (flowers && r.nextInt(4) == 0) {
                    p.decorate(x, topY + 1, z, Blk.deco(FLOWERS[r.nextInt(FLOWERS.length)]));
                } else if (snowy && r.nextInt(3) == 0) {
                    p.decorate(x, topY + 1, z, POWDER);
                }
            }
        }
        int trees = radius >= 4 ? 1 + r.nextInt(Math.max(1, radius / 3)) : 0;
        for (int i = 0; i < trees; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            double d = r.nextDouble() * (radius - 2);
            spruce(p, cx + (int) Math.round(Math.cos(a) * d), topY + 1, cz + (int) Math.round(Math.sin(a) * d),
                    5 + r.nextInt(4), snowy);
        }
    }

    /** Fichte: Stamm und kegelförmige Krone. */
    public static void spruce(Plan p, int x, int y, int z, int height, boolean snowy) {
        for (int k = 0; k < height; k++) {
            p.decorate(x, y + k, z, SPRUCE_LOG);
        }
        int crownStart = 2;
        for (int k = crownStart; k <= height; k++) {
            int rad = Math.max(0, (height - k) / 2 + (k % 2 == 0 ? 1 : 0));
            rad = Math.min(rad, 3);
            for (int dx = -rad; dx <= rad; dx++) {
                for (int dz = -rad; dz <= rad; dz++) {
                    if (Math.abs(dx) + Math.abs(dz) > rad + 1 || (dx == 0 && dz == 0 && k < height)) {
                        continue;
                    }
                    p.decorate(x + dx, y + k, z + dz, SPRUCE_LEAVES);
                }
            }
        }
        p.decorate(x, y + height, z, SPRUCE_LEAVES);
        p.decorate(x, y + height + 1, z, snowy ? POWDER : SPRUCE_LEAVES);
    }

    /** Felsbrocken (unregelmäßige Kugel). */
    public static void rock(Plan p, int cx, int cy, int cz, int radius, Blk main, Blk accent, long seed) {
        SplittableRandom r = new SplittableRandom(seed);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    double d = Math.sqrt(dx * dx + dy * dy * 1.6 + dz * dz) + r.nextDouble();
                    if (d <= radius) {
                        p.decorate(cx + dx, cy + dy, cz + dz, r.nextInt(4) == 0 ? accent : main);
                    }
                }
            }
        }
    }

    /**
     * Berggipfel in der Ferne: Kegel aus Fels mit Schneekappe, nur die Hülle (spart Blöcke).
     */
    public static void peak(Plan p, int cx, int baseY, int cz, int radius, int height, long seed) {
        SplittableRandom r = new SplittableRandom(seed);
        double[] wobble = new double[16];
        for (int i = 0; i < wobble.length; i++) {
            wobble[i] = 0.8 + r.nextDouble() * 0.4;
        }
        for (int k = 0; k <= height; k++) {
            double rad = radius * (1 - (double) k / (height + 1));
            int ri = (int) Math.ceil(rad * 1.2);
            for (int dx = -ri; dx <= ri; dx++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    double ang = Math.atan2(dz, dx);
                    int wi = (int) Math.floor((ang + Math.PI) / (2 * Math.PI) * wobble.length) % wobble.length;
                    double edge = rad * wobble[wi];
                    double d = Math.hypot(dx, dz);
                    if (d > edge || d < edge - 2.2) {
                        continue;
                    }
                    int y = baseY + k;
                    boolean snowLine = k > height * 0.62 || (k > height * 0.45 && r.nextInt(3) == 0);
                    Blk b = snowLine ? SNOW : (r.nextInt(6) == 0 ? ANDESITE : (r.nextInt(9) == 0 ? TUFF : STONE));
                    p.decorate(cx + dx, y, cz + dz, b);
                }
            }
        }
    }

    /** Flache Wolke. */
    public static void cloud(Plan p, int cx, int y, int cz, int rx, int rz, long seed) {
        SplittableRandom r = new SplittableRandom(seed);
        for (int dx = -rx; dx <= rx; dx++) {
            for (int dz = -rz; dz <= rz; dz++) {
                double e = (double) (dx * dx) / (rx * rx) + (double) (dz * dz) / (rz * rz);
                if (e + r.nextDouble() * 0.25 > 1) {
                    continue;
                }
                p.decorate(cx + dx, y, cz + dz, r.nextInt(3) == 0 ? CLOUD_LIGHT : CLOUD);
                if (e < 0.35 && r.nextInt(2) == 0) {
                    p.decorate(cx + dx, y + 1, cz + dz, CLOUD_LIGHT);
                }
            }
        }
    }

    /** Ring aus Bergen und Wolken um einen Mittelpunkt (für Strecken und Basislager). */
    public static void panorama(Plan p, int cx, int baseY, int cz, int distance, int count, long seed) {
        SplittableRandom r = new SplittableRandom(seed);
        for (int i = 0; i < count; i++) {
            double a = (Math.PI * 2 * i) / count + r.nextDouble() * 0.4;
            int d = distance + r.nextInt(distance / 3 + 1);
            int rad = 14 + r.nextInt(12);
            int h = 30 + r.nextInt(30);
            peak(p, cx + (int) (Math.cos(a) * d), baseY - 25, cz + (int) (Math.sin(a) * d), rad, h, seed + i);
        }
        for (int i = 0; i < count; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            int d = 20 + r.nextInt(distance);
            cloud(p, cx + (int) (Math.cos(a) * d), baseY - 12 - r.nextInt(14), cz + (int) (Math.sin(a) * d),
                    4 + r.nextInt(6), 3 + r.nextInt(5), seed * 31 + i);
        }
    }

    /** Ein Blumentopf mit Pflanze. */
    public static Blk pot(String plant) {
        return Blk.deco("potted_" + plant);
    }
}
