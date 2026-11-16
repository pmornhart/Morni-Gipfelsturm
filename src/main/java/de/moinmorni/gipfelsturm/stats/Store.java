package de.moinmorni.gipfelsturm.stats;

import de.moinmorni.gipfelsturm.course.Course;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Lokale Daten: ein YAML pro Spieler ({@code spieler/<uuid>.yml}), daraus die Bestenlisten aller Strecken und der
 * Woche. Gespeichert wird im Hintergrund, atomar (erst Hilfsdatei, dann umbenennen) – ein Absturz zerstört nichts.
 * Dazu die Wege der Bestzeiten als „Geist“ ({@code geister/<uuid>/<strecke>.bin}).
 */
public final class Store {

    private final Path dir;
    private final Logger log;
    private final Map<UUID, PlayerRecord> records = new HashMap<>();
    private final Map<String, Leaderboard> boards = new LinkedHashMap<>();
    private final Leaderboard weekly = new Leaderboard();
    private final ExecutorService io = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Gipfelsturm-Speichern");
        t.setDaemon(true);
        return t;
    });
    private String weekId = "";
    private String weekCourse = "";
    private boolean ghostDefault = true;

    public Store(Path dir, Logger log, Collection<Course> courses) {
        this.dir = dir;
        this.log = log;
        for (Course c : courses) {
            boards.put(c.id, new Leaderboard());
        }
    }

    /** Alle Spieler laden und Bestenlisten aufbauen. */
    public void load() throws IOException {
        Path players = dir.resolve("spieler");
        Files.createDirectories(players);
        try (var files = Files.list(players)) {
            for (Path f : files.filter(p -> p.getFileName().toString().endsWith(".yml")).toList()) {
                String name = f.getFileName().toString();
                try {
                    UUID uuid = UUID.fromString(name.substring(0, name.length() - 4));
                    YamlConfiguration y = new YamlConfiguration();
                    y.loadFromString(Files.readString(f, StandardCharsets.UTF_8));
                    PlayerRecord r = PlayerRecord.fromMap(uuid, toMap(y));
                    records.put(uuid, r);
                } catch (IllegalArgumentException | InvalidConfigurationException e) {
                    log.warning("Spielerdatei " + name + " ist kaputt und wird übersprungen: " + e.getMessage());
                }
            }
        }
        rebuildBoards();
    }

    private void rebuildBoards() {
        boards.values().forEach(Leaderboard::clear);
        for (PlayerRecord r : records.values()) {
            r.courses.forEach((id, s) -> {
                Leaderboard b = boards.get(id);
                if (b != null && s.best > 0) {
                    b.submit(r.uuid, r.name, s.best, s.bestWhen);
                }
            });
        }
        rebuildWeekly();
    }

    /** Woche (neu) setzen: Wochenbestenliste nur aus Zeiten dieser Woche auf der Strecke der Woche. */
    public void setWeek(String id, String course) {
        if (id.equals(weekId) && course.equals(weekCourse)) {
            return;
        }
        weekId = id;
        weekCourse = course;
        rebuildWeekly();
    }

    private void rebuildWeekly() {
        weekly.clear();
        for (PlayerRecord r : records.values()) {
            if (r.weekId.equals(weekId) && r.weekCourse.equals(weekCourse) && r.weekBest > 0) {
                weekly.submit(r.uuid, r.name, r.weekBest, r.weekBestWhen);
            }
        }
    }

    public String weekId() {
        return weekId;
    }

    public String weekCourse() {
        return weekCourse;
    }

    /** Geist für neue Spieler an oder aus (config.yml). */
    public void ghostDefault(boolean on) {
        ghostDefault = on;
    }

    public PlayerRecord record(UUID uuid, String name) {
        PlayerRecord r = records.computeIfAbsent(uuid, u -> {
            PlayerRecord n = new PlayerRecord(u, name);
            n.ghost = ghostDefault;
            return n;
        });
        if (name != null && !name.equals(r.name)) {
            r.name = name;
        }
        r.enterWeek(weekId, weekCourse);
        return r;
    }

    /** Ohne Anlegen (für /gipfel stats anderer Spieler). */
    public PlayerRecord find(UUID uuid) {
        return records.get(uuid);
    }

    public PlayerRecord findByName(String name) {
        for (PlayerRecord r : records.values()) {
            if (r.name.equalsIgnoreCase(name)) {
                return r;
            }
        }
        return null;
    }

    public Leaderboard board(String course) {
        return boards.get(course);
    }

    public Leaderboard weekly() {
        return weekly;
    }

    /** Anzahl Wochen-Gipfel dieser Woche. */
    public int weeklySummits() {
        int n = 0;
        for (PlayerRecord r : records.values()) {
            if (r.weeklySummit(weekId)) {
                n++;
            }
        }
        return n;
    }

    /** Neue Zeit in Liste übernehmen (nach einer neuen Bestzeit). */
    public void submit(PlayerRecord r, String course, long ms, long when) {
        Leaderboard b = boards.get(course);
        if (b != null) {
            b.submit(r.uuid, r.name, ms, when);
        }
        if (course.equals(weekCourse) && r.weekBest > 0) {
            weekly.submit(r.uuid, r.name, r.weekBest, r.weekBestWhen);
        }
    }

    /** Zeiten eines Spielers löschen (alle oder eine Strecke). */
    public void reset(PlayerRecord r, String course) {
        if (course == null) {
            r.courses.clear();
            r.weekBest = 0;
            r.weekDone = false;
            r.attempts = 0;
            r.falls = 0;
            r.finishes = 0;
        } else {
            r.courses.remove(course);
            if (course.equals(r.weekCourse)) {
                r.weekBest = 0;
                r.weekDone = false;
            }
        }
        rebuildBoards();
        save(r);
    }

    /** Im Hintergrund speichern (Inhalt wird jetzt erstellt, geschrieben wird danach). */
    public void save(PlayerRecord r) {
        YamlConfiguration y = new YamlConfiguration();
        r.toMap().forEach(y::set);
        String text = y.saveToString();
        Path file = dir.resolve("spieler").resolve(r.uuid + ".yml");
        io.execute(() -> writeAtomic(file, text.getBytes(StandardCharsets.UTF_8)));
    }

    // ---------- Geister ----------

    /** Weg der Bestzeit speichern (x/y/z pro Tick). */
    public void saveGhost(UUID uuid, String course, float[] path) {
        Path file = dir.resolve("geister").resolve(uuid.toString()).resolve(course + ".bin");
        float[] copy = path.clone();
        io.execute(() -> {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bytes))) {
                    out.writeInt(0x47534731); // „GSG1“
                    out.writeInt(copy.length);
                    for (float f : copy) {
                        out.writeFloat(f);
                    }
                }
                writeAtomic(file, bytes.toByteArray());
            } catch (IOException e) {
                log.log(Level.WARNING, "Geist ließ sich nicht speichern", e);
            }
        });
    }

    /** Weg der Bestzeit laden (null, wenn es keinen gibt). Klein genug für den Hauptthread (wenige KB). */
    public float[] loadGhost(UUID uuid, String course) {
        Path file = dir.resolve("geister").resolve(uuid.toString()).resolve(course + ".bin");
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try (InputStream raw = Files.newInputStream(file);
             DataInputStream in = new DataInputStream(new GZIPInputStream(raw))) {
            if (in.readInt() != 0x47534731) {
                return null;
            }
            int n = in.readInt();
            if (n < 0 || n > 3 * 20 * 60 * 30) {
                return null;
            }
            float[] path = new float[n];
            for (int i = 0; i < n; i++) {
                path[i] = in.readFloat();
            }
            return path;
        } catch (IOException e) {
            return null;
        }
    }

    public void deleteGhosts(UUID uuid, String course) {
        Path folder = dir.resolve("geister").resolve(uuid.toString());
        io.execute(() -> {
            try {
                if (course != null) {
                    Files.deleteIfExists(folder.resolve(course + ".bin"));
                } else if (Files.isDirectory(folder)) {
                    try (var files = Files.list(folder)) {
                        for (Path f : files.toList()) {
                            Files.deleteIfExists(f);
                        }
                    }
                }
            } catch (IOException e) {
                log.log(Level.WARNING, "Geister ließen sich nicht löschen", e);
            }
        });
    }

    /** Ausstehendes speichern (beim Abschalten). */
    public void close() {
        io.shutdown();
        try {
            if (!io.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warning("Speichern dauert zu lange – breche ab.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void writeAtomic(Path file, byte[] data) {
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, data);
            try {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.log(Level.WARNING, "Konnte " + file.getFileName() + " nicht speichern", e);
        }
    }

    /** YAML-Abschnitte rekursiv in Maps (für {@link PlayerRecord#fromMap}). */
    private static Map<String, Object> toMap(ConfigurationSection s) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String k : s.getKeys(false)) {
            Object v = s.get(k);
            m.put(k, v instanceof ConfigurationSection cs ? toMap(cs) : v);
        }
        return m;
    }

    public static long now() {
        return Instant.now().toEpochMilli();
    }

    public Collection<PlayerRecord> all() {
        return records.values();
    }

    public List<String> names() {
        return records.values().stream().map(r -> r.name).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }
}
