package net.morni.gipfelsturm.course;

/**
 * Kollisionsform eines Blocks innerhalb seiner Zelle (0…1). Damit rechnet die Prüfung der Strecken: Wo kann man stehen,
 * wie hoch ist die Oberseite, was ist im Weg?
 */
public enum Shape {
    /** Keine Kollision: Luft, Blumen, Fackeln, Schilder, Banner, Leitern (an Leitern klettert man). */
    NONE(0, 0, 0, 0, 0, 0),
    FULL(0, 1, 0, 1, 0, 1),
    /** Untere Stufe. */
    SLAB(0, 1, 0, 1, 0, 0.5),
    /** Obere Stufe (als niedrige Decke). */
    TOP_SLAB(0, 1, 0, 1, 0.5, 1),
    /** Seelensand ist nur 14/16 hoch. */
    SOUL_SAND(0, 1, 0, 1, 0, 0.875),
    CARPET(0, 1, 0, 1, 0, 0.0625),
    /** Zaun-Pfosten ohne Verbindung: 4/16 breit, 1,5 hoch. */
    FENCE(0.375, 0.625, 0.375, 0.625, 0, 1.5),
    /** Mauer-Pfosten ohne Verbindung: 8/16 breit, 1,5 hoch. */
    WALL(0.25, 0.75, 0.25, 0.75, 0, 1.5),
    /** Eisengitter ohne Verbindung: 2/16 breit. */
    PANE(0.4375, 0.5625, 0.4375, 0.5625, 0, 1),
    /** Kette (senkrecht): 3/16 breit. */
    CHAIN(0.40625, 0.59375, 0.40625, 0.59375, 0, 1),
    /** Laterne (stehend). */
    LANTERN(0.3125, 0.6875, 0.3125, 0.6875, 0, 0.5625);

    public final double minX;
    public final double maxX;
    public final double minZ;
    public final double maxZ;
    public final double minY;
    public final double maxY;

    Shape(double minX, double maxX, double minZ, double maxZ, double minY, double maxY) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.minY = minY;
        this.maxY = maxY;
    }

    public boolean solid() {
        return this != NONE;
    }

    /** Kollisionsquader des Blocks an Zelle x/y/z (null ohne Kollision). */
    public Box box(int x, int y, int z) {
        if (!solid()) {
            return null;
        }
        return new Box(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
    }
}
