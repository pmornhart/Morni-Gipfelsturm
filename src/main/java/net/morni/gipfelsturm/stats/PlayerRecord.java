package net.morni.gipfelsturm.stats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Alles, was Gipfelsturm über einen Spieler speichert (lokal, damit es auch ohne Netzwerk geht): Bestzeiten mit
 * Zwischenzeiten je Strecke, Zähler, Woche, Einstellungen. Als einfache Map für YAML.
 */
public final class PlayerRecord {

    /** Werte je Strecke. */
    public static final class CourseStats {
        public long best;
        public long bestWhen;
        public List<Long> splits = new ArrayList<>();
        public int finishes;
        public int attempts;
        public int falls;
    }

    public final UUID uuid;
    public String name;
    public int attempts;
    public int falls;
    public int finishes;
    public final Map<String, CourseStats> courses = new LinkedHashMap<>();
    /** Woche, für die {@link #weekBest} und {@link #weekDone} gelten (z. B. „2026-W47“). */
    public String weekId = "";
    public String weekCourse = "";
    public long weekBest;
    public long weekBestWhen;
    public boolean weekDone;
    public boolean ghost = true;
    public boolean hideOthers;

    public PlayerRecord(UUID uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public CourseStats course(String id) {
        return courses.computeIfAbsent(id, k -> new CourseStats());
    }

    public long best(String course) {
        CourseStats s = courses.get(course);
        return s == null ? 0 : s.best;
    }

    /** Woche wechseln: alte Wochenwerte verfallen. */
    public void enterWeek(String id, String course) {
        if (!id.equals(weekId)) {
            weekId = id;
            weekCourse = course;
            weekBest = 0;
            weekBestWhen = 0;
            weekDone = false;
        }
    }

    /** Hat der Spieler in der Woche {@code id} die Strecke der Woche geschafft? (Abzeichen „Wochen-Gipfel“) */
    public boolean weeklySummit(String id) {
        return id.equals(weekId) && weekDone;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("attempts", attempts);
        m.put("falls", falls);
        m.put("finishes", finishes);
        Map<String, Object> cs = new LinkedHashMap<>();
        courses.forEach((id, s) -> {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("best", s.best);
            c.put("best-when", s.bestWhen);
            c.put("splits", new ArrayList<>(s.splits));
            c.put("finishes", s.finishes);
            c.put("attempts", s.attempts);
            c.put("falls", s.falls);
            cs.put(id, c);
        });
        m.put("courses", cs);
        Map<String, Object> w = new LinkedHashMap<>();
        w.put("id", weekId);
        w.put("course", weekCourse);
        w.put("best", weekBest);
        w.put("best-when", weekBestWhen);
        w.put("done", weekDone);
        m.put("week", w);
        m.put("ghost", ghost);
        m.put("hide-others", hideOthers);
        return m;
    }

    @SuppressWarnings("unchecked")
    public static PlayerRecord fromMap(UUID uuid, Map<String, Object> m) {
        PlayerRecord r = new PlayerRecord(uuid, str(m.get("name"), "?"));
        r.attempts = (int) num(m.get("attempts"));
        r.falls = (int) num(m.get("falls"));
        r.finishes = (int) num(m.get("finishes"));
        Object cs = m.get("courses");
        if (cs instanceof Map<?, ?> courses) {
            courses.forEach((k, v) -> {
                if (v instanceof Map<?, ?> c) {
                    CourseStats s = r.course(String.valueOf(k));
                    s.best = num(c.get("best"));
                    s.bestWhen = num(c.get("best-when"));
                    if (c.get("splits") instanceof List<?> list) {
                        for (Object o : list) {
                            s.splits.add(num(o));
                        }
                    }
                    s.finishes = (int) num(c.get("finishes"));
                    s.attempts = (int) num(c.get("attempts"));
                    s.falls = (int) num(c.get("falls"));
                }
            });
        }
        if (m.get("week") instanceof Map<?, ?> w) {
            r.weekId = str(w.get("id"), "");
            r.weekCourse = str(w.get("course"), "");
            r.weekBest = num(w.get("best"));
            r.weekBestWhen = num(w.get("best-when"));
            r.weekDone = Boolean.TRUE.equals(w.get("done"));
        }
        r.ghost = !Boolean.FALSE.equals(m.get("ghost"));
        r.hideOthers = Boolean.TRUE.equals(m.get("hide-others"));
        return r;
    }

    private static long num(Object o) {
        if (o instanceof Number n) {
            return n.longValue();
        }
        if (o instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    private static String str(Object o, String fallback) {
        return o == null ? fallback : String.valueOf(o);
    }
}
