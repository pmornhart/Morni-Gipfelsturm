package net.morni.gipfelsturm.course;

import java.util.ArrayList;
import java.util.List;

/**
 * Eine fertige Strecke: Pads in Reihenfolge (Start … Checkpoints … Ziel), Gefahr-Zonen, Fallgrenzen je Abschnitt und
 * Medaillen-Zeiten.
 */
public final class Course {

    /** Bereich, in den man nicht fallen darf (Gletscherspalte, Wasser …) – zurück zum Checkpoint. */
    public record Hazard(Box box, String what) {
    }

    /** Zeiten für die Medaillen in Millisekunden (Gipfel ist die beste). */
    public record Medals(long gipfel, long gold, long silber, long bronze) {
    }

    public final String id;
    public final String nameDe;
    public final String nameEn;
    public final String textDe;
    public final String textEn;
    public final Difficulty difficulty;
    /** Item für Menüs, z. B. {@code minecraft:blue_ice}. */
    public final String icon;
    public final List<Pad> pads;
    public final List<Hazard> hazards;
    /** Blickrichtung am Start. */
    public final Dir startDir;
    public final Medals medals;
    /** Platz der Strecke in der Welt (Pads und Landschaft). */
    public final Box bounds;
    /** Biom der Gegend (für Farben von Gras und Laub, Schnee-Partikel). */
    public final String biome;

    private final List<Integer> checkpoints = new ArrayList<>();
    private final double[] fallY;
    private final java.util.Set<Long> padCells = new java.util.HashSet<>();

    /** Fallgrenze: so weit unter dem tiefsten Pad eines Abschnitts beginnt das Zurücksetzen. */
    public static final double FALL_MARGIN = 3.0;

    public Course(String id, String nameDe, String nameEn, String textDe, String textEn, Difficulty difficulty,
                  String icon, List<Pad> pads, List<Hazard> hazards, Dir startDir, Medals medals, Box bounds,
                  String biome) {
        this.id = id;
        this.nameDe = nameDe;
        this.nameEn = nameEn;
        this.textDe = textDe;
        this.textEn = textEn;
        this.difficulty = difficulty;
        this.icon = icon;
        this.pads = List.copyOf(pads);
        this.hazards = List.copyOf(hazards);
        this.startDir = startDir;
        this.medals = medals;
        this.bounds = bounds;
        this.biome = biome;
        for (Pad p : pads) {
            if (p.role == Pad.Role.CHECKPOINT) {
                checkpoints.add(p.index);
            }
            for (int x = p.x0; x <= p.x1; x++) {
                for (int z = p.z0; z <= p.z1; z++) {
                    padCells.add(Plan.key(x, p.y, z));
                }
            }
        }
        // Abschnitt k: vom Checkpoint k (0 = Start) bis einschließlich zum nächsten Checkpoint bzw. Ziel.
        fallY = new double[checkpoints.size() + 1];
        int section = 0;
        double low = Double.MAX_VALUE;
        for (Pad p : pads) {
            low = Math.min(low, p.surface());
            if (p.role == Pad.Role.CHECKPOINT || p.role == Pad.Role.FINISH) {
                fallY[section] = low - FALL_MARGIN;
                section++;
                low = p.surface();
            }
        }
    }

    public String name(String lang) {
        return "en".equals(lang) ? nameEn : nameDe;
    }

    public String text(String lang) {
        return "en".equals(lang) ? textEn : textDe;
    }

    public Pad start() {
        return pads.get(0);
    }

    public Pad finish() {
        return pads.get(pads.size() - 1);
    }

    /** Pads der Checkpoints in Reihenfolge (ohne Start und Ziel). */
    public List<Pad> checkpoints() {
        return checkpoints.stream().map(pads::get).toList();
    }

    public int checkpointCount() {
        return checkpoints.size();
    }

    /** Fallgrenze, solange der letzte erreichte Checkpoint {@code reached} ist (0 = noch keiner). */
    public double fallY(int reached) {
        return fallY[Math.max(0, Math.min(reached, fallY.length - 1))];
    }

    /** Tiefste Fallgrenze der ganzen Strecke. */
    public double lowestFallY() {
        double low = Double.MAX_VALUE;
        for (double f : fallY) {
            low = Math.min(low, f);
        }
        return low;
    }

    /** Länge des Weges von Pad-Mitte zu Pad-Mitte (waagerecht, Leitern senkrecht dazu). */
    public double pathLength() {
        double len = 0;
        for (int i = 1; i < pads.size(); i++) {
            Pad a = pads.get(i - 1);
            Pad b = pads.get(i);
            len += Math.hypot(b.centerX() - a.centerX(), b.centerZ() - a.centerZ());
            if (b.arrival.kind() == Pad.Kind.LADDER) {
                len += b.surface() - a.surface();
            }
        }
        return len;
    }

    /**
     * Schnellste denkbare Zeit (ms): der ganze Weg mit 12 Blöcken pro Sekunde – schneller als jeder echte Lauf (Sprint-
     * Sprünge schaffen gut 7). Wer schneller im Ziel ist, hat geschummelt (Teleport, Fliegen).
     */
    public long plausibleMs() {
        return Math.round(pathLength() / 12.0 * 1000);
    }

    /** Gehört der Block zu einem Pad der Strecke (Sprungblock, Start, Checkpoint, Ziel)? */
    public boolean isPadCell(int x, int y, int z) {
        return padCells.contains(Plan.key(x, y, z));
    }

    /** Medaille für eine Zeit (null = keine). */
    public Medal medal(long ms) {
        return Medal.of(ms, medals);
    }
}
