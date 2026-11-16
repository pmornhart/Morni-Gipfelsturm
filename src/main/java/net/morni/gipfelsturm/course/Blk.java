package net.morni.gipfelsturm.course;

/**
 * Ein Block für den Bauplan: Block-Zustand als Text (wie im Spiel, z. B. {@code minecraft:spruce_stairs[facing=east]}),
 * seine Kollisionsform und wie er sich unter den Füßen verhält. Bewusst ohne Bukkit, damit die Prüfung der Strecken
 * ohne Server läuft.
 */
public record Blk(String data, Shape shape, Surface surface) {

    public static final Blk AIR = new Blk("minecraft:air", Shape.NONE, Surface.NORMAL);

    // Farben der Parkour-Sprache (wie in der Lobby): weiß/hellblau = Sprungblöcke, Smaragd = Start, Gold = Checkpoint,
    // Diamant = Ziel.
    public static final Blk WHITE = full("white_concrete");
    public static final Blk LIGHT_BLUE = full("light_blue_concrete");
    public static final Blk START = full("emerald_block");
    public static final Blk CHECKPOINT = full("gold_block");
    public static final Blk FINISH = full("diamond_block");

    public static final Blk PACKED_ICE = new Blk("minecraft:packed_ice", Shape.FULL, Surface.ICE);
    public static final Blk BLUE_ICE = new Blk("minecraft:blue_ice", Shape.FULL, Surface.ICE);
    public static final Blk SLIME = new Blk("minecraft:slime_block", Shape.FULL, Surface.SLIME);
    public static final Blk SOUL_SAND = new Blk("minecraft:soul_sand", Shape.SOUL_SAND, Surface.SOUL_SAND);
    public static final Blk WATER = new Blk("minecraft:water", Shape.NONE, Surface.WATER);

    /** Voller Block (Name ohne „minecraft:“). */
    public static Blk full(String id) {
        return new Blk(ns(id), Shape.FULL, Surface.NORMAL);
    }

    /** Block mit Zustand, z. B. {@code spruce_log[axis=y]}. */
    public static Blk full(String id, String state) {
        return new Blk(ns(id) + "[" + state + "]", Shape.FULL, Surface.NORMAL);
    }

    /** Ohne Kollision: Blumen, Fackeln, Laub-Teppich … */
    public static Blk deco(String data) {
        return new Blk(ns(data), Shape.NONE, Surface.NORMAL);
    }

    public static Blk slab(String id) {
        return new Blk(ns(id) + "[type=bottom]", Shape.SLAB, Surface.NORMAL);
    }

    public static Blk topSlab(String id) {
        return new Blk(ns(id) + "[type=top]", Shape.TOP_SLAB, Surface.NORMAL);
    }

    /** Einzelner Zaun-Pfosten (ohne Verbindung zu Nachbarn). */
    public static Blk fence(String id) {
        return new Blk(ns(id), Shape.FENCE, Surface.NORMAL);
    }

    /** Einzelner Mauer-Pfosten. */
    public static Blk wall(String id) {
        return new Blk(ns(id) + "[up=true]", Shape.WALL, Surface.NORMAL);
    }

    /** Einzelnes Eisengitter. */
    public static Blk bars() {
        return new Blk("minecraft:iron_bars", Shape.PANE, Surface.NORMAL);
    }

    /** Senkrechte Kette. */
    public static Blk chain() {
        return new Blk("minecraft:iron_chain[axis=y]", Shape.CHAIN, Surface.NORMAL);
    }

    /** Leiter, die in Richtung {@code facing} schaut (die Wand ist auf der anderen Seite). */
    public static Blk ladder(Dir facing) {
        return new Blk("minecraft:ladder[facing=" + facing.id() + "]", Shape.NONE, Surface.CLIMB);
    }

    public static Blk of(String data, Shape shape) {
        return new Blk(ns(data), shape, Surface.NORMAL);
    }

    public boolean solid() {
        return shape.solid();
    }

    /** Name ohne Zustand, z. B. {@code minecraft:ladder}. */
    public String id() {
        int i = data.indexOf('[');
        return i < 0 ? data : data.substring(0, i);
    }

    private static String ns(String id) {
        return id.contains(":") ? id : "minecraft:" + id;
    }
}
