package de.moinmorni.gipfelsturm.course;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Prüft eine Strecke gegen {@link JumpRules}: Jeder Schritt muss mit normaler Bewegung schaffbar sein, nichts darf im
 * Weg stehen, Start/Checkpoints/Ziel stimmen, Medaillen sind erreichbar. Läuft in den Tests und beim Start des Plugins
 * (eine fehlerhafte Strecke wird gemeldet).
 */
public final class CourseValidator {

    /** Geringster Abstand einer Gipfel-Zeit zur schnellsten möglichen Bewegung (Blöcke pro Sekunde). */
    public static final double MAX_SPEED = 7.2;

    private CourseValidator() {
    }

    public static List<String> validate(Course c, Plan plan) {
        List<String> out = new ArrayList<>();
        List<Pad> pads = c.pads;
        if (pads.size() < 2 || pads.get(0).role != Pad.Role.START || pads.get(pads.size() - 1).role != Pad.Role.FINISH) {
            out.add("Start oder Ziel fehlt");
            return out;
        }
        int cps = c.checkpointCount();
        if (cps < 3 || cps > 8) {
            out.add("3 bis 8 Checkpoints nötig, sind " + cps);
        }
        for (Pad p : pads) {
            if (!p.blk.solid()) {
                out.add(at(p) + "Pad ohne festen Block");
            }
            if (p.cells() > 1 && p.blk.shape() != Shape.FULL && p.blk.shape() != Shape.SLAB
                    && p.blk.shape() != Shape.SOUL_SAND) {
                out.add(at(p) + "Pfosten-Pads nur einzeln");
            }
            if (p.role != Pad.Role.START && p.role != Pad.Role.FINISH && p.index > 0 && p.index < pads.size() - 1
                    && (p.blk == Blk.START || p.blk == Blk.FINISH)) {
                out.add(at(p) + "Start-/Zielblock mitten in der Strecke");
            }
            headroom(p, plan, out);
        }
        for (int i = 1; i < pads.size(); i++) {
            Pad a = pads.get(i - 1);
            Pad b = pads.get(i);
            switch (b.arrival.kind()) {
                case JUMP -> jump(c, a, b, plan, out, i + 1 < pads.size() ? pads.get(i + 1) : null);
                case LADDER -> ladder(a, b, plan, out);
                case BOUNCE -> bounce(i >= 2 ? pads.get(i - 2) : null, a, b, plan, out);
                case NEO -> neo(c, a, b, plan, out);
            }
        }
        for (Course.Hazard h : c.hazards) {
            for (Pad p : pads) {
                if (h.box().intersects(p.zone())) {
                    out.add(at(p) + "liegt in der Gefahr-Zone " + h.what());
                }
            }
        }
        Course.Medals m = c.medals;
        if (!(m.gipfel() < m.gold() && m.gold() < m.silber() && m.silber() < m.bronze())) {
            out.add("Medaillen-Zeiten nicht aufsteigend");
        }
        long fastest = Math.round(c.pathLength() / MAX_SPEED * 1000);
        if (m.gipfel() < fastest) {
            out.add(String.format(Locale.ROOT, "Gipfel-Zeit %d ms unter der schnellsten möglichen (%d ms)",
                    m.gipfel(), fastest));
        }
        if (c.plausibleMs() >= m.gipfel()) {
            out.add("Plausibilitäts-Grenze über der Gipfel-Zeit");
        }
        return out;
    }

    /** Mehrere Strecken dürfen sich nicht überlappen. */
    public static List<String> separate(List<Course> courses, List<Box> others) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < courses.size(); i++) {
            for (int j = i + 1; j < courses.size(); j++) {
                if (courses.get(i).bounds.intersects(courses.get(j).bounds)) {
                    out.add(courses.get(i).id + " überlappt " + courses.get(j).id);
                }
            }
            for (Box o : others) {
                if (courses.get(i).bounds.intersects(o)) {
                    out.add(courses.get(i).id + " überlappt das Basislager");
                }
            }
        }
        return out;
    }

    private static String at(Pad p) {
        return "Pad " + p.index + " (" + p.x0 + " " + p.y + " " + p.z0 + "): ";
    }

    /** Über jedem Pad 1,8 Blöcke Platz zum Stehen. */
    private static void headroom(Pad p, Plan plan, List<String> out) {
        Rect r = p.support();
        Box room = new Box(r.minX(), p.surface(), r.minZ(), r.maxX(), p.surface() + JumpRules.PLAYER_HEIGHT, r.maxZ());
        List<long[]> hits = plan.solidIn(room, k -> false);
        if (!hits.isEmpty()) {
            long[] h = hits.get(0);
            out.add(at(p) + "kein Platz zum Stehen (Block bei " + h[0] + " " + h[1] + " " + h[2] + ")");
        }
    }

    private static void jump(Course c, Pad a, Pad b, Plan plan, List<String> out, Pad next) {
        JumpRules.Tier tier = c.difficulty.tier;
        double dy = b.surface() - a.surface();
        double travel = a.stand().distance(b.stand());
        Dir d = b.arrival.dir();
        double headroom = corridor(a, b, plan, out, Set.of());
        if (dy > JumpRules.JUMP_PEAK) {
            out.add(at(b) + String.format(Locale.ROOT, "zu hoch (%.2f)", dy));
            return;
        }
        if (travel <= 1e-9 && dy <= JumpRules.STEP_UP) {
            return; // einfach hinübergehen
        }
        double limit = JumpRules.maxTravel(tier, dy, a.blk.surface(), a.length(d));
        if (headroom < JumpRules.FREE_HEADROOM) {
            limit = Math.min(limit, JumpRules.maxTravelUnderCeiling(tier, headroom, dy));
        }
        if (Double.isNaN(limit)) {
            out.add(at(b) + String.format(Locale.ROOT, "Sprung so nicht möglich (Höhe %.2f, Decke %.2f, %s)", dy,
                    headroom, a.blk.surface()));
        } else if (travel > limit + 1e-9) {
            out.add(at(b) + String.format(Locale.ROOT, "zu weit: %.2f statt höchstens %.2f (Höhe %.2f)", travel,
                    limit, dy));
        }
        if (b.blk.surface() == Surface.ICE && b.length(d) < JumpRules.ICE_STOP_LENGTH
                && (next == null || next.arrival.kind() != Pad.Kind.JUMP || next.arrival.dir() != d)) {
            out.add(at(b) + "Eis-Landung zu kurz zum Bremsen");
        }
        if (b.blk.surface() == Surface.SLIME && (next == null || next.arrival.kind() != Pad.Kind.BOUNCE)) {
            out.add(at(b) + "Schleim ohne Absprung danach");
        }
        if (tier == JumpRules.Tier.WALK && (a.blk.surface() == Surface.SOUL_SAND || b.blk.surface() == Surface.SLIME)) {
            out.add(at(b) + "auf Leicht kein Seelensand/Schleim");
        }
    }

    private static void ladder(Pad a, Pad b, Plan plan, List<String> out) {
        List<long[]> ladder = b.arrival.ladder();
        int h = ladder.size();
        if (h < 2 || h > JumpRules.LADDER_MAX) {
            out.add(at(b) + "Leiter " + h + " hoch");
            return;
        }
        long[] first = ladder.get(0);
        if (first[1] != a.y + 1 || ladder.get(h - 1)[1] != b.y) {
            out.add(at(b) + "Leiter reicht nicht vom Pad bis oben");
        }
        for (long[] l : ladder) {
            if (plan.get((int) l[0], (int) l[1], (int) l[2]).surface() != Surface.CLIMB) {
                out.add(at(b) + "Leiter fehlt bei " + l[0] + " " + l[1] + " " + l[2]);
            }
            Dir d = b.arrival.dir();
            Blk behind = plan.get((int) l[0] + d.dx, (int) l[1], (int) l[2] + d.dz);
            if (!behind.solid()) {
                out.add(at(b) + "Leiter ohne Wand dahinter");
            }
        }
        // Die Leiter muss direkt am Pad hängen (man läuft hinein) und oben an den nächsten Pad grenzen.
        Rect cellRect = new Rect(first[0], first[0] + 1, first[2], first[2] + 1);
        if (a.support().distance(cellRect) > 1e-9 || b.support().distance(cellRect) > 1e-9) {
            out.add(at(b) + "Leiter nicht zwischen den Pads");
        }
    }

    private static void bounce(Pad before, Pad slime, Pad b, Plan plan, List<String> out) {
        if (slime.blk.surface() != Surface.SLIME || before == null) {
            out.add(at(b) + "Absprung ist kein Schleim");
            return;
        }
        double drop = before.surface() - slime.surface();
        double rise = b.surface() - slime.surface();
        if (drop < JumpRules.BOUNCE_MIN_DROP) {
            out.add(at(b) + "Fall auf den Schleim zu niedrig (" + drop + ")");
        }
        double peak = JumpRules.bouncePeak(drop);
        if (rise > peak - JumpRules.BOUNCE_MARGIN) {
            out.add(at(b) + String.format(Locale.ROOT, "Schleim reicht nicht: %.2f hoch, federt bis %.2f", rise, peak));
        }
        double travel = slime.stand().distance(b.stand());
        if (travel > JumpRules.BOUNCE_TRAVEL) {
            out.add(at(b) + "nach dem Schleim zu weit (" + travel + ")");
        }
        corridor(slime, b, plan, out, Set.of());
    }

    private static void neo(Course c, Pad a, Pad b, Plan plan, List<String> out) {
        int t = b.arrival.neoThickness();
        if (t < 1 || t > JumpRules.neoMaxThickness(c.difficulty.tier)) {
            out.add(at(b) + "Neo mit Wanddicke " + t + " auf " + c.difficulty.de + " nicht erlaubt");
        }
        if (a.cells() != 1 || b.cells() != 1 || Math.abs(b.surface() - a.surface()) > 1e-9) {
            out.add(at(b) + "Neo nur zwischen 1×1-Pads gleicher Höhe");
        }
        Dir d = b.arrival.dir();
        int side = b.arrival.passSide();
        Dir pass = side > 0 ? d.right() : d.left();
        // Die Wand muss den geraden Weg wirklich versperren …
        for (int k = 1; k <= t; k++) {
            int x = a.x0 + d.dx * k;
            int z = a.z0 + d.dz * k;
            for (int y = a.y + 1; y <= a.y + 3; y++) {
                if (!plan.get(x, y, z).solid()) {
                    out.add(at(b) + "Neo-Wand hat ein Loch");
                }
                // … und auf der Seite, auf der man herumspringt, muss frei sein.
                if (plan.get(x + pass.dx, y, z + pass.dz).solid()) {
                    out.add(at(b) + "Neo: Seite zum Herumspringen ist zu");
                }
            }
        }
        Set<Long> wall = new HashSet<>();
        for (long[] w : b.arrival.wall()) {
            wall.add(Plan.key((int) w[0], (int) w[1], (int) w[2]));
        }
        corridor(a, b, plan, out, wall);
    }

    /**
     * Sucht feste Blöcke im Sprungweg (Hülle der beiden Standflächen, vom tieferen Pad bis Sprunghöhe über dem
     * höheren). Was über Kopfhöhe des Absprungs liegt, ist eine Decke (Rückgabe: kleinster Abstand), alles darunter ist
     * ein Fehler.
     */
    private static double corridor(Pad a, Pad b, Plan plan, List<String> out, Set<Long> ignore) {
        // Hindernisse zählen, wenn sie die Hülle der Standflächen (ohne Überstand) schneiden: Ein Block schräg neben
        // einem Pad engt nur ein, man kann immer noch mittig springen. Abstände rechnen dagegen mit Überstand.
        Rect ra = a.support();
        Rect rb = b.support();
        double[][] hull = Hull.of(ra, rb);
        double lo = Math.min(a.surface(), b.surface());
        double hi = Math.max(a.surface(), b.surface()) + JumpRules.FREE_HEADROOM;
        Rect bb = ra.union(rb);
        Box search = new Box(bb.minX(), lo, bb.minZ(), bb.maxX(), hi, bb.maxZ());
        double headroom = Double.POSITIVE_INFINITY;
        for (long[] cell : plan.solidIn(search, k -> ignore.contains(k))) {
            int x = (int) cell[0];
            int y = (int) cell[1];
            int z = (int) cell[2];
            if ((a.covers(x, z) && y <= a.y) || (b.covers(x, z) && y <= b.y)) {
                continue; // der Pad selbst und was darunter steht
            }
            Box box = plan.get(x, y, z).shape().box(x, y, z);
            if (!Hull.overlaps(hull, new Rect(box.minX(), box.maxX(), box.minZ(), box.maxZ()))) {
                continue;
            }
            double above = box.minY() - a.surface();
            if (above >= JumpRules.PLAYER_HEIGHT - 1e-9) {
                headroom = Math.min(headroom, above);
            } else {
                out.add(at(b) + "Block im Sprungweg bei " + x + " " + y + " " + z);
                return headroom;
            }
        }
        return headroom;
    }

    /** Konvexe Hülle zweier Rechtecke und Überlappung mit einem Rechteck (Trennachsen-Test). */
    static final class Hull {
        private Hull() {
        }

        static double[][] of(Rect a, Rect b) {
            List<double[]> pts = new ArrayList<>(List.of(a.corners()));
            pts.addAll(List.of(b.corners()));
            pts.sort((p, q) -> p[0] != q[0] ? Double.compare(p[0], q[0]) : Double.compare(p[1], q[1]));
            List<double[]> lower = new ArrayList<>();
            for (double[] p : pts) {
                while (lower.size() >= 2 && cross(lower.get(lower.size() - 2), lower.get(lower.size() - 1), p) <= 0) {
                    lower.remove(lower.size() - 1);
                }
                lower.add(p);
            }
            List<double[]> upper = new ArrayList<>();
            for (int i = pts.size() - 1; i >= 0; i--) {
                double[] p = pts.get(i);
                while (upper.size() >= 2 && cross(upper.get(upper.size() - 2), upper.get(upper.size() - 1), p) <= 0) {
                    upper.remove(upper.size() - 1);
                }
                upper.add(p);
            }
            lower.remove(lower.size() - 1);
            upper.remove(upper.size() - 1);
            lower.addAll(upper);
            return lower.toArray(new double[0][]);
        }

        private static double cross(double[] o, double[] a, double[] b) {
            return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
        }

        /** Echte Überlappung (Berühren zählt nicht). */
        static boolean overlaps(double[][] poly, Rect r) {
            double[][] rect = r.corners();
            if (!separates(poly, rect, 1, 0) && !separates(poly, rect, 0, 1)) {
                for (int i = 0; i < poly.length; i++) {
                    double[] p = poly[i];
                    double[] q = poly[(i + 1) % poly.length];
                    double nx = -(q[1] - p[1]);
                    double nz = q[0] - p[0];
                    if (separates(poly, rect, nx, nz)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        private static boolean separates(double[][] a, double[][] b, double ax, double az) {
            double aMin = Double.MAX_VALUE;
            double aMax = -Double.MAX_VALUE;
            for (double[] p : a) {
                double v = p[0] * ax + p[1] * az;
                aMin = Math.min(aMin, v);
                aMax = Math.max(aMax, v);
            }
            double bMin = Double.MAX_VALUE;
            double bMax = -Double.MAX_VALUE;
            for (double[] p : b) {
                double v = p[0] * ax + p[1] * az;
                bMin = Math.min(bMin, v);
                bMax = Math.max(bMax, v);
            }
            double eps = 1e-9 * Math.max(1, Math.hypot(ax, az));
            return aMax <= bMin + eps || bMax <= aMin + eps;
        }
    }
}
