package de.moinmorni.gipfelsturm.course;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Baukasten für Strecken: Ein Zeiger steht auf dem aktuellen Pad und schaut in eine Richtung. Jeder Schritt legt den
 * nächsten Pad relativ dazu an – Lücke (Blöcke Luft dazwischen), Höhe (Blöcke), Seitversatz (+ = rechts) und Größe
 * (Länge in Laufrichtung × Breite). So bleiben die Strecken im Code lesbar und die Prüfung weiß genau, was gemeint ist.
 *
 * <pre>
 * b.start(3, 3);
 * b.jump(1, 0, 0, 1, 1);      // ein Block Lücke, gleiche Höhe, 1×1
 * b.jump(2, 1, -1, 2, 2);     // zwei Lücke, einen hoch, einen nach links, 2×2
 * b.checkpoint(2, 0, 0, 3, 3);
 * b.turnRight();
 * b.climb(5, 2, 2, Blk.full("stone"));
 * b.finish(2, 0, 0, 3, 3);
 * </pre>
 */
public final class CourseBuilder {

    private final Plan plan;
    private final String id;
    private final Difficulty difficulty;
    private final List<Pad> pads = new ArrayList<>();
    private final List<Course.Hazard> hazards = new ArrayList<>();
    private final Dir startDir;
    private Dir dir;
    private Pad cur;
    private int alternate;
    private Blk padA = Blk.WHITE;
    private Blk padB = Blk.LIGHT_BLUE;
    /** Decke über den nächsten Sprüngen (Kopfstoß-Gang): Abstand zur Oberseite des Absprungs, null = keine. */
    private Double ceiling;
    private Blk ceilingBlk;
    /** Wird unter jeden neuen Pad gebaut (Säule, Fels, Kette …). */
    private Consumer<Pad> under = p -> { };
    private String nameDe = "";
    private String nameEn = "";
    private String textDe = "";
    private String textEn = "";
    private String icon = "minecraft:white_concrete";
    private String biome = "minecraft:meadow";
    private Course.Medals medals;

    public CourseBuilder(Plan plan, String id, Difficulty difficulty, Dir dir) {
        this.plan = plan;
        this.id = id;
        this.difficulty = difficulty;
        this.dir = dir;
        this.startDir = dir;
    }

    // ---------- Angaben ----------

    public CourseBuilder name(String de, String en) {
        nameDe = de;
        nameEn = en;
        return this;
    }

    public CourseBuilder text(String de, String en) {
        textDe = de;
        textEn = en;
        return this;
    }

    public CourseBuilder icon(String item) {
        icon = item.contains(":") ? item : "minecraft:" + item;
        return this;
    }

    public CourseBuilder biome(String b) {
        biome = b.contains(":") ? b : "minecraft:" + b;
        return this;
    }

    /** Eigene Medaillen-Zeiten in Sekunden (sonst aus der geschätzten Laufzeit). */
    public CourseBuilder medals(double gipfel, double gold, double silber, double bronze) {
        medals = new Course.Medals(sec(gipfel), sec(gold), sec(silber), sec(bronze));
        return this;
    }

    /** Standard-Blöcke der Pads (abwechselnd). */
    public CourseBuilder pads(Blk a, Blk b) {
        padA = a;
        padB = b;
        return this;
    }

    public CourseBuilder under(Consumer<Pad> u) {
        under = u;
        return this;
    }

    public Plan plan() {
        return plan;
    }

    public Pad current() {
        return cur;
    }

    public Dir dir() {
        return dir;
    }

    public List<Pad> pads() {
        return pads;
    }

    // ---------- Strecke ----------

    /** Startfeld (Smaragd) mit hinterer Kante bei x/z, mittig auf der Linie, Oberseite bei {@code surfaceY}. */
    public CourseBuilder start(int x, int surfaceY, int z, int len, int width) {
        int y = surfaceY - 1;
        int[] r = rectFrom(dir.dx != 0 ? x : z, len, dir.dx != 0 ? z : x, width);
        add(new Pad(0, r[0], r[1], r[2], r[3], y, Blk.START, Pad.Role.START, null));
        return this;
    }

    public CourseBuilder jump(int gap, int dy, int side, int len, int width) {
        return jump(gap, dy, side, len, width, nextColour());
    }

    public CourseBuilder jump(int gap, int dy, int side, int len, int width, Blk blk) {
        return place(gap, dy, side, len, width, blk, Pad.Role.NORMAL, Pad.Arrival.jump(dir));
    }

    /** 1×1-Sprung geradeaus. */
    public CourseBuilder hop(int gap, int dy) {
        return jump(gap, dy, 0, 1, 1);
    }

    public CourseBuilder hop(int gap, int dy, int side) {
        return jump(gap, dy, side, 1, 1);
    }

    public CourseBuilder hop(int gap, int dy, int side, Blk blk) {
        return jump(gap, dy, side, 1, 1, blk);
    }

    public CourseBuilder checkpoint(int gap, int dy, int side, int len, int width) {
        return place(gap, dy, side, len, width, Blk.CHECKPOINT, Pad.Role.CHECKPOINT, Pad.Arrival.jump(dir));
    }

    public CourseBuilder finish(int gap, int dy, int side, int len, int width) {
        return place(gap, dy, side, len, width, Blk.FINISH, Pad.Role.FINISH, Pad.Arrival.jump(dir));
    }

    public CourseBuilder turnRight() {
        dir = dir.right();
        return this;
    }

    public CourseBuilder turnLeft() {
        dir = dir.left();
        return this;
    }

    /** Ab jetzt eine Decke über allen Sprüngen ({@code headroom} über der Absprung-Oberseite, 2,0 oder 2,5). */
    public CourseBuilder ceilingOn(double headroom, Blk blk) {
        ceiling = headroom;
        ceilingBlk = blk;
        return this;
    }

    public CourseBuilder ceilingOff() {
        ceiling = null;
        return this;
    }

    /**
     * Leiter: direkt vor dem Pad eine Leiter {@code h} Blöcke hoch an einer Wand; oben auf der Wand der nächste Pad
     * (Länge × Breite, beginnt auf der Wand).
     */
    public CourseBuilder climb(int h, int topLen, int topWidth, Blk wallBlk) {
        return climb(h, topLen, topWidth, wallBlk, Pad.Role.NORMAL, wallBlk);
    }

    public CourseBuilder climbToCheckpoint(int h, int topLen, int topWidth, Blk wallBlk) {
        return climb(h, topLen, topWidth, wallBlk, Pad.Role.CHECKPOINT, Blk.CHECKPOINT);
    }

    private CourseBuilder climb(int h, int topLen, int topWidth, Blk wallBlk, Pad.Role role, Blk topBlk) {
        int s = dir.dx + dir.dz;
        int far = farEdge(cur);
        int lat = latCenter(cur);
        int ladderAlong = far + s;
        int wallAlong = ladderAlong + s;
        List<long[]> ladder = new ArrayList<>();
        List<long[]> wall = new ArrayList<>();
        Blk ladderBlk = Blk.ladder(dir.back());
        for (int yy = cur.y + 1; yy <= cur.y + h; yy++) {
            int[] lc = cell(ladderAlong, lat);
            plan.set(lc[0], yy, lc[1], ladderBlk);
            ladder.add(new long[] {lc[0], yy, lc[1]});
        }
        for (int yy = cur.y - 2; yy <= cur.y + h; yy++) {
            int[] wc = cell(wallAlong, lat);
            plan.set(wc[0], yy, wc[1], yy == cur.y + h ? topBlk : wallBlk);
            wall.add(new long[] {wc[0], yy, wc[1]});
        }
        int[] r = rectFrom(wallAlong, topLen, lat, topWidth);
        Pad.Arrival arrival = new Pad.Arrival(Pad.Kind.LADDER, dir, ladder, wall, 0, 0);
        Pad p = new Pad(pads.size(), r[0], r[1], r[2], r[3], cur.y + h, topBlk, role, arrival);
        add(p);
        return this;
    }

    /**
     * Schleim: Direkt vor dem Pad liegt {@code drop} Blöcke tiefer ein Schleim-Feld (3×3); von dort federt man
     * {@code rise} Blöcke hinauf auf den nächsten Pad (Lücke {@code gap} hinter dem Schleim).
     */
    public CourseBuilder bounce(int drop, int rise, int gap, int len, int width) {
        place(0, -drop, 0, 3, 3, Blk.SLIME, Pad.Role.NORMAL, Pad.Arrival.jump(dir));
        return place(gap, rise, 0, len, width, nextColour(), Pad.Role.NORMAL,
                new Pad.Arrival(Pad.Kind.BOUNCE, dir, List.of(), List.of(), 0, 0));
    }

    /**
     * Neo: Zwischen diesem 1-breiten Pad und dem nächsten (gleiche Höhe, Lücke = Wanddicke) steht eine Wand, die zur
     * anderen Seite weiterläuft – man springt auf der Seite {@code passSide} (+1 rechts, −1 links) um ihr Ende herum.
     */
    public CourseBuilder neo(int thickness, int passSide, Blk wallBlk) {
        int s = dir.dx + dir.dz;
        int rs = dir.right().dx + dir.right().dz;
        int far = farEdge(cur);
        int lat = latCenter(cur);
        List<long[]> wall = new ArrayList<>();
        for (int t = 1; t <= thickness; t++) {
            for (int w = 0; w <= 2; w++) {
                int[] c = cell(far + s * t, lat - passSide * rs * w);
                for (int yy = cur.y - 1; yy <= cur.y + 4; yy++) {
                    plan.set(c[0], yy, c[1], wallBlk);
                    wall.add(new long[] {c[0], yy, c[1]});
                }
            }
        }
        Pad.Arrival arrival = new Pad.Arrival(Pad.Kind.NEO, dir, List.of(), wall, thickness, passSide);
        return place(thickness, 0, 0, 1, 1, nextColour(), Pad.Role.NORMAL, arrival);
    }

    /** Gefahr-Zone (Welt-Koordinaten, Blockzellen einschließlich). */
    public CourseBuilder hazard(int x0, int y0, int z0, int x1, int y1, int z1, String what) {
        hazards.add(new Course.Hazard(Box.cells(x0, y0, z0, x1, y1, z1), what));
        return this;
    }

    // ---------- Fertig ----------

    public Course build() {
        Pad first = pads.get(0);
        Pad last = pads.get(pads.size() - 1);
        Box b = null;
        for (Pad p : pads) {
            Box pb = Box.cells(p.x0, p.y, p.z0, p.x1, p.y, p.z1);
            b = b == null ? pb : b.union(pb);
        }
        Box bounds = new Box(b.minX() - 24, b.minY() - 34, b.minZ() - 24, b.maxX() + 24, b.maxY() + 24, b.maxZ() + 24);
        Course.Medals m = medals != null ? medals : autoMedals();
        reservePath();
        if (first.role != Pad.Role.START || last.role != Pad.Role.FINISH) {
            throw new IllegalStateException(id + ": Start und Ziel fehlen");
        }
        return new Course(id, nameDe, nameEn, textDe, textEn, difficulty, icon, pads, hazards, startDir, m, bounds,
                biome);
    }

    /** Geschätzte Zeit eines guten Laufs (ms): pro Sprung gut eine halbe Sekunde, Leitern, Schleim, Neos extra. */
    public long estimateMs() {
        double t = 0;
        for (int i = 1; i < pads.size(); i++) {
            Pad a = pads.get(i - 1);
            Pad p = pads.get(i);
            double d = Math.hypot(p.centerX() - a.centerX(), p.centerZ() - a.centerZ());
            t += switch (p.arrival.kind()) {
                case JUMP -> 0.55 + Math.max(0, d - 3.5) / 5.6 + (p.surface() > a.surface() + 0.5 ? 0.1 : 0);
                case LADDER -> 0.4 + (p.surface() - a.surface()) / 2.35;
                case BOUNCE -> 1.6;
                case NEO -> 0.9;
            };
        }
        return Math.round(t * 1000);
    }

    private Course.Medals autoMedals() {
        double est = estimateMs() / 1000.0;
        double k = switch (difficulty) {
            case LEICHT -> 1.15;
            case MITTEL -> 1.2;
            case SCHWER -> 1.25;
            case EXTREM -> 1.3;
        };
        double gipfel = halfUp(est * k);
        return new Course.Medals(sec(gipfel), sec(halfUp(gipfel * 1.3)), sec(halfUp(gipfel * 1.8)),
                sec(halfUp(gipfel * 2.6)));
    }

    private static double halfUp(double s) {
        return Math.ceil(s * 2) / 2.0;
    }

    private static long sec(double s) {
        return Math.round(s * 1000);
    }

    /** Sprungwege und Stehplatz freihalten, damit Landschaft nichts verstellt. */
    private void reservePath() {
        for (int i = 0; i < pads.size(); i++) {
            Pad p = pads.get(i);
            for (int x = p.x0 - 1; x <= p.x1 + 1; x++) {
                for (int z = p.z0 - 1; z <= p.z1 + 1; z++) {
                    for (int yy = p.y + 1; yy <= p.y + 4; yy++) {
                        plan.reserve(x, yy, z);
                    }
                }
            }
            if (i == 0) {
                continue;
            }
            Pad a = pads.get(i - 1);
            int x0 = Math.min(a.x0, p.x0) - 1;
            int x1 = Math.max(a.x1, p.x1) + 1;
            int z0 = Math.min(a.z0, p.z0) - 1;
            int z1 = Math.max(a.z1, p.z1) + 1;
            int y0 = Math.min(a.y, p.y) + 1;
            int y1 = Math.max(a.y, p.y) + 4;
            for (int x = x0; x <= x1; x++) {
                for (int z = z0; z <= z1; z++) {
                    for (int yy = y0; yy <= y1; yy++) {
                        plan.reserve(x, yy, z);
                    }
                }
            }
        }
    }

    // ---------- Geometrie ----------

    private Blk nextColour() {
        return (alternate++ % 2 == 0) ? padA : padB;
    }

    private CourseBuilder place(int gap, int dy, int side, int len, int width, Blk blk, Pad.Role role,
                                Pad.Arrival arrival) {
        if (cur == null) {
            throw new IllegalStateException(id + ": erst start(…)");
        }
        int s = dir.dx + dir.dz;
        int rs = dir.right().dx + dir.right().dz;
        int near = farEdge(cur) + s * (1 + gap);
        int lat = latCenter(cur) + side * rs;
        int[] r = rectFrom(near, len, lat, width);
        Pad from = cur;
        Pad p = new Pad(pads.size(), r[0], r[1], r[2], r[3], cur.y + dy, blk, role, arrival);
        add(p);
        if (ceiling != null) {
            roof(from, p);
        }
        return this;
    }

    /**
     * Decke über der Lücke und dem neuen Pad (nicht über dem Absprung – der liegt schon unter der vorigen Decke oder ist
     * der offene Eingang, über den man in den Gang hineinläuft). Quer einen Block breiter auf jeder Seite.
     */
    private void roof(Pad a, Pad b) {
        // Unterkante der Decke = Absprung-Oberseite + Abstand; eine obere Stufe hat ihre Unterkante 0,5 höher.
        double bottom = a.surface() + ceiling;
        int yy = (int) Math.round(bottom - ceilingBlk.shape().minY);
        int s = dir.dx + dir.dz;
        int from = farEdge(a) + s;
        int to = farEdge(b);
        int lat0 = Math.min(lat(a, true), lat(b, true)) - 1;
        int lat1 = Math.max(lat(a, false), lat(b, false)) + 1;
        for (int along = Math.min(from, to); along <= Math.max(from, to); along++) {
            for (int l = lat0; l <= lat1; l++) {
                int[] c = cell(along, l);
                plan.set(c[0], yy, c[1], ceilingBlk);
            }
        }
    }

    /** Quer-Ausdehnung eines Pads (kleinste bzw. größte Zelle). */
    private int lat(Pad p, boolean min) {
        if (dir.dx != 0) {
            return min ? p.z0 : p.z1;
        }
        return min ? p.x0 : p.x1;
    }

    private void add(Pad p) {
        for (int x = p.x0; x <= p.x1; x++) {
            for (int z = p.z0; z <= p.z1; z++) {
                plan.set(x, p.y, z, p.blk);
            }
        }
        pads.add(p);
        cur = p;
        under.accept(p);
    }

    /** Äußerste Zelle des Pads in Laufrichtung. */
    private int farEdge(Pad p) {
        return switch (dir) {
            case EAST -> p.x1;
            case WEST -> p.x0;
            case SOUTH -> p.z1;
            case NORTH -> p.z0;
        };
    }

    /** Mitte quer zur Laufrichtung (bei gerader Breite die kleinere Zelle). */
    private int latCenter(Pad p) {
        return dir.dx != 0 ? Math.floorDiv(p.z0 + p.z1, 2) : Math.floorDiv(p.x0 + p.x1, 2);
    }

    /** Zelle aus Lauf- und Querkoordinate. */
    private int[] cell(int along, int lat) {
        return dir.dx != 0 ? new int[] {along, lat} : new int[] {lat, along};
    }

    /** Rechteck: beginnt bei {@code near} in Laufrichtung, {@code len} lang, quer mittig um {@code lat}. */
    private int[] rectFrom(int near, int len, int lat, int width) {
        int s = dir.dx + dir.dz;
        int far = near + s * (len - 1);
        int l0 = lat - (width - 1) / 2;
        int l1 = l0 + width - 1;
        int[] a = cell(near, l0);
        int[] b = cell(far, l1);
        return new int[] {Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.max(a[0], b[0]), Math.max(a[1], b[1])};
    }
}
