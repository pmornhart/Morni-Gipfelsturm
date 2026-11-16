package de.moinmorni.gipfelsturm.course;

/** Himmelsrichtung, in die eine Strecke gerade läuft (Minecraft: Osten = +x, Süden = +z). */
public enum Dir {
    EAST(1, 0), SOUTH(0, 1), WEST(-1, 0), NORTH(0, -1);

    private static final Dir[] ORDER = values();

    public final int dx;
    public final int dz;

    Dir(int dx, int dz) {
        this.dx = dx;
        this.dz = dz;
    }

    /** Rechts davon (wer nach Osten schaut, hat Süden rechts). */
    public Dir right() {
        return ORDER[(ordinal() + 1) % ORDER.length];
    }

    public Dir left() {
        return ORDER[(ordinal() + ORDER.length - 1) % ORDER.length];
    }

    public Dir back() {
        return ORDER[(ordinal() + 2) % ORDER.length];
    }

    /** Blickrichtung für Minecraft (Süden 0°, Westen 90°, Norden 180°, Osten −90°). */
    public float yaw() {
        return switch (this) {
            case SOUTH -> 0f;
            case WEST -> 90f;
            case NORTH -> 180f;
            case EAST -> -90f;
        };
    }

    /** Name für Block-Zustände wie {@code facing=east}. */
    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
