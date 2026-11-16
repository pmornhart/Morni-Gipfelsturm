package net.morni.gipfelsturm.course;

import java.util.List;

/**
 * Eine Station der Strecke, auf der man steht: Rechteck aus Blöcken (x0…x1, z0…z1) auf Höhe {@code y}. Dazu, wie man
 * vom vorigen Pad hierher kommt ({@link Arrival}).
 */
public final class Pad {

    public enum Role {
        START, NORMAL, CHECKPOINT, FINISH
    }

    /** Wie man diesen Pad vom vorigen aus erreicht. */
    public enum Kind {
        /** Normaler Sprung (oder Schritt). */
        JUMP,
        /** Leiter hinauf; der Pad liegt oben auf der Wand. */
        LADDER,
        /** Vom Schleim-Pad davor hinauf gefedert. */
        BOUNCE,
        /** Um eine Wand herum gesprungen. */
        NEO
    }

    /**
     * Ankunft: Art, Richtung und die Zellen, die dazugehören (Leiter + Wand, Wand beim Neo, Decke beim Kopfstoß).
     */
    public record Arrival(Kind kind, Dir dir, List<long[]> ladder, List<long[]> wall, int neoThickness, int passSide) {
        public static Arrival jump(Dir dir) {
            return new Arrival(Kind.JUMP, dir, List.of(), List.of(), 0, 0);
        }
    }

    public final int index;
    public final int x0;
    public final int z0;
    public final int x1;
    public final int z1;
    public final int y;
    public final Blk blk;
    public final Role role;
    public final Arrival arrival;

    public Pad(int index, int x0, int z0, int x1, int z1, int y, Blk blk, Role role, Arrival arrival) {
        this.index = index;
        this.x0 = Math.min(x0, x1);
        this.z0 = Math.min(z0, z1);
        this.x1 = Math.max(x0, x1);
        this.z1 = Math.max(z0, z1);
        this.y = y;
        this.blk = blk;
        this.role = role;
        this.arrival = arrival;
    }

    /** Höhe der Oberseite (wo die Füße stehen). */
    public double surface() {
        return y + blk.shape().maxY;
    }

    /** Fläche, auf der man Halt hat (Kollision der Oberseite). */
    public Rect support() {
        Shape s = blk.shape();
        return new Rect(x0 + s.minX, x1 + s.maxX, z0 + s.minZ, z1 + s.maxZ);
    }

    /** Wo die Spieler-Mitte sein darf, um noch auf dem Pad zu stehen. */
    public Rect stand() {
        return support().expand(JumpRules.HALF_WIDTH);
    }

    /** Mitte der Oberseite. */
    public double centerX() {
        return (x0 + x1 + 1) / 2.0;
    }

    public double centerZ() {
        return (z0 + z1 + 1) / 2.0;
    }

    public int cells() {
        return (x1 - x0 + 1) * (z1 - z0 + 1);
    }

    /** Länge in Richtung {@code d} (Blöcke). */
    public int length(Dir d) {
        return d.dx != 0 ? x1 - x0 + 1 : z1 - z0 + 1;
    }

    /** Bereich über dem Pad, in dem man steht (für Checkpoints und Ziel). */
    public Box zone() {
        Rect r = stand();
        double s = surface();
        return new Box(r.minX(), s - 0.05, r.minZ(), r.maxX(), s + 1.0, r.maxZ());
    }

    public boolean covers(int x, int z) {
        return x >= x0 && x <= x1 && z >= z0 && z <= z1;
    }
}
