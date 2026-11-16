package net.morni.gipfelsturm.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Bestenliste einer Strecke: eine Zeit pro Spieler, die schnellste zuerst (bei Gleichstand: wer zuerst da war). */
public final class Leaderboard {

    public record Entry(UUID uuid, String name, long ms, long when) {
    }

    private static final Comparator<Entry> ORDER = Comparator.comparingLong(Entry::ms).thenComparingLong(Entry::when)
            .thenComparing(Entry::name);

    private final Map<UUID, Entry> best = new HashMap<>();
    private List<Entry> sorted = List.of();

    /** Zeit eintragen; zählt nur, wenn sie besser ist als die bisherige. true = neue Bestzeit. */
    public boolean submit(UUID uuid, String name, long ms, long when) {
        Entry old = best.get(uuid);
        if (old != null && old.ms() <= ms) {
            if (!old.name().equals(name)) {
                best.put(uuid, new Entry(uuid, name, old.ms(), old.when()));
                resort();
            }
            return false;
        }
        best.put(uuid, new Entry(uuid, name, ms, when));
        resort();
        return true;
    }

    public void remove(UUID uuid) {
        if (best.remove(uuid) != null) {
            resort();
        }
    }

    public void clear() {
        best.clear();
        sorted = List.of();
    }

    private void resort() {
        List<Entry> list = new ArrayList<>(best.values());
        list.sort(ORDER);
        sorted = List.copyOf(list);
    }

    public List<Entry> top(int n) {
        return sorted.subList(0, Math.min(n, sorted.size()));
    }

    /** Platz (1 = erster), 0 ohne Zeit. */
    public int rank(UUID uuid) {
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).uuid().equals(uuid)) {
                return i + 1;
            }
        }
        return 0;
    }

    public Entry record() {
        return sorted.isEmpty() ? null : sorted.get(0);
    }

    public Entry of(UUID uuid) {
        return best.get(uuid);
    }

    public int size() {
        return sorted.size();
    }
}
