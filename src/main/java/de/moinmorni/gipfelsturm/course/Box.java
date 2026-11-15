package de.moinmorni.gipfelsturm.course;

/** Quader in der Welt (Kommazahlen). */
public record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

    public static Box cells(int x0, int y0, int z0, int x1, int y1, int z1) {
        return new Box(Math.min(x0, x1), Math.min(y0, y1), Math.min(z0, z1), Math.max(x0, x1) + 1.0,
                Math.max(y0, y1) + 1.0, Math.max(z0, z1) + 1.0);
    }

    public boolean contains(double x, double y, double z) {
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public boolean intersects(Box o) {
        return minX < o.maxX && maxX > o.minX && minY < o.maxY && maxY > o.minY && minZ < o.maxZ && maxZ > o.minZ;
    }

    public Box union(Box o) {
        return new Box(Math.min(minX, o.minX), Math.min(minY, o.minY), Math.min(minZ, o.minZ), Math.max(maxX, o.maxX),
                Math.max(maxY, o.maxY), Math.max(maxZ, o.maxZ));
    }

    public Box grow(double r) {
        return new Box(minX - r, minY - r, minZ - r, maxX + r, maxY + r, maxZ + r);
    }

    /**
     * Wo auf der Strecke von a nach b (Anteil 0…1) geht es in den Quader hinein? −1, wenn gar nicht. Liegt a schon
     * drin, ist es 0. Damit misst der Zeitnehmer genauer als ein Spiel-Tick (50 ms).
     */
    public double entry(double ax, double ay, double az, double bx, double by, double bz) {
        double t0 = 0;
        double t1 = 1;
        double[] a = {ax, ay, az};
        double[] d = {bx - ax, by - ay, bz - az};
        double[] lo = {minX, minY, minZ};
        double[] hi = {maxX, maxY, maxZ};
        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1e-12) {
                if (a[i] < lo[i] || a[i] > hi[i]) {
                    return -1;
                }
                continue;
            }
            double u = (lo[i] - a[i]) / d[i];
            double v = (hi[i] - a[i]) / d[i];
            if (u > v) {
                double s = u;
                u = v;
                v = s;
            }
            t0 = Math.max(t0, u);
            t1 = Math.min(t1, v);
            if (t0 > t1) {
                return -1;
            }
        }
        return t0;
    }
}
