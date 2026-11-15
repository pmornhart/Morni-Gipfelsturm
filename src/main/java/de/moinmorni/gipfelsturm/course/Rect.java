package de.moinmorni.gipfelsturm.course;

/** Rechteck von oben gesehen (x/z in Blöcken, Kommazahlen). */
public record Rect(double minX, double maxX, double minZ, double maxZ) {

    public Rect expand(double r) {
        return new Rect(minX - r, maxX + r, minZ - r, maxZ + r);
    }

    public double width() {
        return maxX - minX;
    }

    public double depth() {
        return maxZ - minZ;
    }

    public double centerX() {
        return (minX + maxX) / 2;
    }

    public double centerZ() {
        return (minZ + maxZ) / 2;
    }

    /** Kürzester Abstand zweier Rechtecke (0, wenn sie sich berühren oder überlappen). */
    public double distance(Rect o) {
        double gx = Math.max(0, Math.max(o.minX - maxX, minX - o.maxX));
        double gz = Math.max(0, Math.max(o.minZ - maxZ, minZ - o.maxZ));
        return Math.hypot(gx, gz);
    }

    /** Echte Überlappung (Berühren zählt nicht – wie bei Minecrafts Kollision). */
    public boolean overlaps(Rect o) {
        return minX < o.maxX && maxX > o.minX && minZ < o.maxZ && maxZ > o.minZ;
    }

    public boolean contains(double x, double z) {
        return x > minX && x < maxX && z > minZ && z < maxZ;
    }

    public Rect union(Rect o) {
        return new Rect(Math.min(minX, o.minX), Math.max(maxX, o.maxX), Math.min(minZ, o.minZ), Math.max(maxZ, o.maxZ));
    }

    /** Die vier Ecken (für die Hülle eines Sprungs). */
    public double[][] corners() {
        return new double[][] {{minX, minZ}, {maxX, minZ}, {maxX, maxZ}, {minX, maxZ}};
    }
}
