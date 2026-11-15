package de.moinmorni.gipfelsturm.game;

import de.moinmorni.gipfelsturm.GipfelsturmPlugin;
import de.moinmorni.gipfelsturm.course.Box;
import de.moinmorni.gipfelsturm.course.Course;
import de.moinmorni.gipfelsturm.course.Medal;
import de.moinmorni.gipfelsturm.course.Pad;
import de.moinmorni.gipfelsturm.course.Rect;
import de.moinmorni.gipfelsturm.stats.Leaderboard;
import de.moinmorni.gipfelsturm.stats.PlayerRecord;
import de.moinmorni.gipfelsturm.stats.Store;
import de.moinmorni.gipfelsturm.time.RunClock;
import de.moinmorni.gipfelsturm.time.Splits;
import de.moinmorni.gipfelsturm.time.TimeFormat;
import de.moinmorni.gipfelsturm.ui.Texts;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Herz von Gipfelsturm: die Läufe. Jeden Tick wird für jeden Läufer geprüft, ob er losläuft, einen Checkpoint oder das
 * Ziel erreicht, gestürzt oder in eine Gefahr gefallen ist. Zeiten in Ticks mit Bruchteilen ({@link RunClock}).
 */
public final class Runs {

    private static final Title.Times SHORT = Title.Times.times(Duration.ofMillis(150), Duration.ofMillis(1600),
            Duration.ofMillis(400));
    private static final Title.Times LONG = Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(3000),
            Duration.ofMillis(800));
    private static final Particle.DustOptions GHOST = new Particle.DustOptions(Color.fromRGB(127, 220, 255), 1.1f);

    private final GipfelsturmPlugin pl;
    private final Map<UUID, Run> runs = new LinkedHashMap<>();
    private final Map<UUID, Race> invites = new HashMap<>();
    private final Set<Race> races = new HashSet<>();
    private final Map<String, float[]> ghosts = new HashMap<>();
    private final Set<UUID> practice = new HashSet<>();
    private long tick;

    public Runs(GipfelsturmPlugin pl) {
        this.pl = pl;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(pl, this::onTick, 1L, 1L);
    }

    public long tick() {
        return tick;
    }

    public boolean running(UUID id) {
        return runs.containsKey(id);
    }

    public Course courseOf(UUID id) {
        Run r = runs.get(id);
        return r == null ? null : r.course;
    }

    public boolean practice(UUID id) {
        return practice.contains(id);
    }

    public int count() {
        return runs.size();
    }

    // =========================================================================================
    // Wählen und Starten
    // =========================================================================================

    /** Strecke wählen: Party-Anführer mit Mitgliedern hier → Rennen, sonst allein. */
    public void select(Player p, Course c) {
        UUID id = p.getUniqueId();
        abandon(p);
        pl.net().party(id).completeOnTimeout(Optional.empty(), 2, TimeUnit.SECONDS)
                .whenComplete((party, err) -> Bukkit.getScheduler().runTask(pl, () -> {
                    if (!p.isOnline()) {
                        return;
                    }
                    List<Player> mates = new ArrayList<>();
                    if (err == null && party != null && party.isPresent() && party.get().leader().equals(id)) {
                        for (UUID m : party.get().members()) {
                            Player o = Bukkit.getPlayer(m);
                            if (!m.equals(id) && o != null && o.getWorld().equals(pl.world()) && !runs.containsKey(m)
                                    && !invites.containsKey(m)) {
                                mates.add(o);
                            }
                        }
                    }
                    if (mates.isEmpty()) {
                        begin(p, c, null);
                        return;
                    }
                    Race race = new Race(c, id, p.getName());
                    race.until = tick + pl.settings().raceWait * 20L;
                    races.add(race);
                    begin(p, c, race);
                    for (Player m : mates) {
                        race.invited.add(m.getUniqueId());
                        invites.put(m.getUniqueId(), race);
                        String l = pl.lang(m);
                        m.sendMessage(Texts.chat(l, "race.invite", Texts.arg("leader", p.getName()),
                                Texts.comp("course", courseName(c, l)), Texts.arg("sec", pl.settings().raceWait)));
                        m.playSound(m.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);
                        pl.hotbar().raceInvite(m, race);
                    }
                }));
    }

    /** Auf den Start stellen. */
    private void begin(Player p, Course c, Race race) {
        UUID id = p.getUniqueId();
        Run r = new Run(c, practice.contains(id));
        r.race = race;
        r.state = race != null ? Run.State.FROZEN : Run.State.READY;
        PlayerRecord rec = pl.store().record(id, p.getName());
        if (rec.ghost && !r.practice) {
            r.ghost = ghost(id, c.id);
        }
        runs.put(id, r);
        invites.remove(id);
        teleport(p, r, pl.startLocation(c));
        pl.hotbar().run(p, r.practice);
        String l = pl.lang(p);
        p.showTitle(Title.title(courseName(c, l), Texts.get(l, "run.welcome", Texts.comp("course", Component.empty()),
                Texts.comp("diff", difficulty(c, l)), Texts.arg("n", c.checkpointCount())), LONG));
        p.sendMessage(Texts.chat(l, "run.welcome", Texts.comp("course", courseName(c, l)),
                Texts.comp("diff", difficulty(c, l)), Texts.arg("n", c.checkpointCount())));
        if (race != null) {
            p.sendMessage(Texts.chat(l, "race.wait", Texts.arg("sec", pl.settings().raceWait)));
        }
        p.playSound(p.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1f);
        pl.state();
    }

    /** Einladung zum Rennen annehmen ({@code /gipfel mitmachen} oder Gegenstand). */
    public void joinRace(Player p) {
        Race race = invites.get(p.getUniqueId());
        String l = pl.lang(p);
        if (race == null || race.state != Race.State.GATHER || !races.contains(race)) {
            p.sendMessage(Texts.chat(l, "race.none"));
            return;
        }
        if (runs.containsKey(p.getUniqueId())) {
            abandon(p);
        }
        race.joined.add(p.getUniqueId());
        begin(p, race.course, race);
        for (UUID u : race.joined) {
            Player o = Bukkit.getPlayer(u);
            if (o != null) {
                o.sendMessage(Texts.chat(pl.lang(o), "race.joined", Texts.arg("player", p.getName())));
            }
        }
    }

    // =========================================================================================
    // Unterwegs: Gegenstände und Befehle
    // =========================================================================================

    /** Zurück zum letzten Checkpoint (im Übungsmodus zum Übungs-Punkt). */
    public void toCheckpoint(Player p) {
        Run r = runs.get(p.getUniqueId());
        if (r == null) {
            p.sendMessage(Texts.chat(pl.lang(p), "cmd.notrunning"));
            return;
        }
        respawn(p, r, false, null);
    }

    public void restart(Player p) {
        Run r = runs.get(p.getUniqueId());
        if (r == null) {
            p.sendMessage(Texts.chat(pl.lang(p), "cmd.notrunning"));
            return;
        }
        reportFalls(p, r);
        if (r.race != null && r.state == Run.State.RUNNING) {
            // Im Rennen läuft die Uhr weiter – nur zurück auf den Start.
            r.reached = 0;
            teleport(p, r, pl.startLocation(r.course));
        } else if (r.state != Run.State.FROZEN) {
            r.resetProgress();
            r.state = Run.State.READY;
            teleport(p, r, pl.startLocation(r.course));
        }
        p.sendActionBar(Texts.get(pl.lang(p), "run.restart"));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1.4f);
    }

    public void giveUp(Player p) {
        if (!runs.containsKey(p.getUniqueId())) {
            pl.toBasecamp(p);
            return;
        }
        abandon(p);
        pl.toBasecamp(p);
        p.sendMessage(Texts.chat(pl.lang(p), "run.giveup"));
    }

    /** Lauf beenden (Aufgeben, Verlassen, neue Strecke) – Stürze melden, speichern. */
    public void abandon(Player p) {
        UUID id = p.getUniqueId();
        Run r = runs.remove(id);
        if (r == null) {
            return;
        }
        reportFalls(p, r);
        if (r.race != null) {
            r.race.out.add(id);
            checkRaceOver(r.race);
        }
        pl.store().save(pl.store().record(id, p.getName()));
        pl.state();
    }

    public void quit(Player p) {
        abandon(p);
        Race inv = invites.remove(p.getUniqueId());
        if (inv != null) {
            inv.invited.remove(p.getUniqueId());
        }
    }

    /** Übungsmodus an/aus (setzt einen laufenden Versuch zurück). */
    public void togglePractice(Player p) {
        UUID id = p.getUniqueId();
        boolean on = practice.add(id);
        if (!on) {
            practice.remove(id);
        }
        String l = pl.lang(p);
        p.sendMessage(Texts.chat(l, on ? "practice.on" : "practice.off"));
        Run r = runs.get(id);
        if (r != null && r.race == null) {
            Course c = r.course;
            abandon(p);
            begin(p, c, null);
        }
    }

    public void setPracticePoint(Player p) {
        Run r = runs.get(p.getUniqueId());
        String l = pl.lang(p);
        if (r == null || !r.practice) {
            p.sendMessage(Texts.chat(l, "practice.only"));
            return;
        }
        if (!p.isOnGround()) {
            p.sendActionBar(Texts.get(l, "practice.air"));
            return;
        }
        Location at = p.getLocation();
        r.practicePoint = new double[] {at.getX(), at.getY(), at.getZ(), at.getYaw(), at.getPitch()};
        p.sendActionBar(Texts.get(l, "practice.set"));
        p.playSound(at, Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1f, 1.6f);
        p.spawnParticle(Particle.WAX_ON, at.getX(), at.getY() + 0.2, at.getZ(), 12, 0.3, 0.1, 0.3, 0);
    }

    // =========================================================================================
    // Jeder Tick
    // =========================================================================================

    private void onTick() {
        tick++;
        for (Iterator<Map.Entry<UUID, Run>> it = runs.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Run> e = it.next();
            Player p = Bukkit.getPlayer(e.getKey());
            if (p == null) {
                it.remove();
                continue;
            }
            if (!p.getWorld().equals(pl.world())) {
                it.remove();
                reportFalls(p, e.getValue());
                continue;
            }
            update(p, e.getValue());
        }
        for (Race race : new ArrayList<>(races)) {
            race(race);
        }
    }

    private void update(Player p, Run r) {
        Location l = p.getLocation();
        double x = l.getX();
        double y = l.getY();
        double z = l.getZ();
        double ax = r.hasPrev ? r.px : x;
        double ay = r.hasPrev ? r.py : y;
        double az = r.hasPrev ? r.pz : z;
        Course c = r.course;
        Pad start = c.start();
        Rect startArea = start.stand();
        switch (r.state) {
            case FROZEN -> {
                if (!startArea.contains(x, z) || y < start.surface() - 1) {
                    Location back = pl.startLocation(c);
                    back.setYaw(l.getYaw());
                    back.setPitch(l.getPitch());
                    teleport(p, r, back);
                    return;
                }
            }
            case READY -> {
                if (!startArea.contains(x, z)) {
                    Box area = new Box(startArea.minX(), -1000, startArea.minZ(), startArea.maxX(), 1000,
                            startArea.maxZ());
                    double back = area.entry(x, y, z, ax, ay, az);
                    startRun(p, r, back < 0 ? 0 : 1 - back);
                } else if (y < c.fallY(0)) {
                    respawn(p, r, true, null);
                    return;
                }
            }
            case RUNNING -> {
                if (!r.practice) {
                    r.record(x, y, z);
                }
                for (Course.Hazard h : c.hazards) {
                    if (h.box().contains(x, y, z)) {
                        respawn(p, r, true, h.what());
                        return;
                    }
                }
                if (y < c.fallY(r.reached)) {
                    respawn(p, r, true, null);
                    return;
                }
                progress(p, r, ax, ay, az, x, y, z);
                if (!runs.containsKey(p.getUniqueId())) {
                    return; // im Ziel
                }
            }
        }
        r.px = x;
        r.py = y;
        r.pz = z;
        r.hasPrev = true;
        hud(p, r);
    }

    /** Checkpoints der Reihe nach, dann das Ziel. */
    private void progress(Player p, Run r, double ax, double ay, double az, double x, double y, double z) {
        Course c = r.course;
        List<Pad> cps = c.checkpoints();
        if (r.reached < cps.size()) {
            double f = cps.get(r.reached).zone().entry(ax, ay, az, x, y, z);
            if (f >= 0) {
                checkpoint(p, r, f);
                return;
            }
            for (int k = r.reached + 1; k < cps.size(); k++) {
                if (cps.get(k).zone().contains(x, y, z) && tick - r.skipHintTick > 60) {
                    r.skipHintTick = tick;
                    p.sendActionBar(Texts.get(pl.lang(p), "run.skip", Texts.arg("n", r.reached + 1)));
                }
            }
            if (c.finish().zone().contains(x, y, z) && tick - r.skipHintTick > 60) {
                r.skipHintTick = tick;
                p.sendActionBar(Texts.get(pl.lang(p), "run.missing", Texts.arg("n", r.reached + 1)));
            }
            return;
        }
        double f = c.finish().zone().entry(ax, ay, az, x, y, z);
        if (f >= 0) {
            finish(p, r, f);
        }
    }

    private void startRun(Player p, Run r, double fraction) {
        r.state = Run.State.RUNNING;
        r.clock = new RunClock(tick, fraction);
        countAttempt(p, r);
        p.sendActionBar(Texts.get(pl.lang(p), "run.go"));
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 0.8f, 1.6f);
    }

    private void countAttempt(Player p, Run r) {
        if (r.practice) {
            return;
        }
        PlayerRecord rec = pl.store().record(p.getUniqueId(), p.getName());
        rec.attempts++;
        rec.course(r.course.id).attempts++;
        pl.net().stats(p.getUniqueId(), Map.of("attempts", 1L), Map.of());
    }

    private void checkpoint(Player p, Run r, double f) {
        long ms = r.clock.at(tick, f);
        int index = r.reached;
        r.splits[index] = ms;
        r.reached++;
        String l = pl.lang(p);
        PlayerRecord rec = pl.store().record(p.getUniqueId(), p.getName());
        Long delta = Splits.delta(rec.course(r.course.id).splits, index, ms);
        Component d = delta == null ? Texts.get(l, "run.checkpoint.first")
                : Component.text("(" + TimeFormat.diff(delta, l) + ")", TextColor.fromHexString(Splits.colour(delta)));
        Component msg = Texts.get(l, "run.checkpoint", Texts.arg("n", r.reached), Texts.arg("total",
                r.course.checkpointCount()), Texts.arg("time", TimeFormat.clock(ms, l)), Texts.comp("delta", d));
        p.sendMessage(Texts.chat(l, "run.checkpoint", Texts.arg("n", r.reached), Texts.arg("total",
                r.course.checkpointCount()), Texts.arg("time", TimeFormat.clock(ms, l)), Texts.comp("delta", d)));
        r.splitText = msg;
        r.splitShownUntil = tick + 40;
        p.sendActionBar(msg);
        p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1f, delta != null && delta < 0 ? 1.6f : 1.2f);
        Location at = p.getLocation();
        p.spawnParticle(Particle.HAPPY_VILLAGER, at.getX(), at.getY() + 1, at.getZ(), 14, 0.4, 0.5, 0.4, 0);
    }

    /** Sturz/Gefahr oder Gegenstand „Zurück zum Checkpoint“. */
    private void respawn(Player p, Run r, boolean fell, String hazard) {
        String l = pl.lang(p);
        Course c = r.course;
        if (fell && r.state == Run.State.RUNNING) {
            r.falls++;
            if (!r.practice) {
                PlayerRecord rec = pl.store().record(p.getUniqueId(), p.getName());
                rec.falls++;
                rec.course(c.id).falls++;
            }
        }
        Component msg;
        Location to;
        if (r.practice && r.practicePoint != null) {
            double[] pp = r.practicePoint;
            to = new Location(pl.world(), pp[0], pp[1], pp[2], (float) pp[3], (float) pp[4]);
            msg = Texts.get(l, "run.fall.practice");
        } else if (r.reached > 0) {
            to = pl.padLocation(c, c.checkpoints().get(r.reached - 1));
            msg = Texts.get(l, "run.fall.cp", Texts.arg("n", r.reached));
        } else {
            to = pl.startLocation(c);
            msg = Texts.get(l, "run.fall.start");
            if (r.race == null || r.state != Run.State.RUNNING) {
                r.resetProgress();
                if (r.state != Run.State.FROZEN) {
                    r.state = Run.State.READY;
                }
            }
        }
        teleport(p, r, to);
        if (hazard != null) {
            String what = Texts.has("hazard." + hazard) ? Texts.raw(l, "hazard." + hazard) : hazard;
            p.sendMessage(Texts.chat(l, "run.hazard", Texts.arg("what", what)));
        }
        p.sendActionBar(msg);
        r.splitShownUntil = tick + 30;
        r.splitText = msg;
        if (fell) {
            p.playSound(p.getLocation(), Sound.ITEM_CHORUS_FRUIT_TELEPORT, 0.6f, 1.2f);
        }
    }

    private void finish(Player p, Run r, double f) {
        UUID id = p.getUniqueId();
        long ms = r.clock.at(tick, f);
        Course c = r.course;
        runs.remove(id);
        String l = pl.lang(p);
        Store store = pl.store();
        PlayerRecord rec = store.record(id, p.getName());
        pl.hotbar().after(p, c);
        Location at = p.getLocation();
        p.spawnParticle(Particle.FIREWORK, at.getX(), at.getY() + 1, at.getZ(), 40, 0.5, 0.8, 0.5, 0.08);
        p.playSound(at, Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1f);
        if (r.practice) {
            p.showTitle(Title.title(Texts.get(l, "finish.title"), Component.text(TimeFormat.clock(ms, l),
                    NamedTextColor.LIGHT_PURPLE), LONG));
            p.sendMessage(Texts.chat(l, "finish.practice", Texts.arg("time", TimeFormat.clock(ms, l))));
            raceResult(p, r, ms);
            pl.state();
            return;
        }
        if (pl.settings().plausible && ms < c.plausibleMs()) {
            pl.getLogger().warning(p.getName() + " war auf " + c.id + " unmöglich schnell (" + ms + " ms) – nicht gewertet.");
            p.sendMessage(Texts.chat(l, "finish.toofast", Texts.arg("time", TimeFormat.clock(ms, l))));
            reportFalls(p, r);
            store.save(rec);
            pl.state();
            return;
        }
        PlayerRecord.CourseStats cs = rec.course(c.id);
        long before = cs.best;
        Leaderboard board = store.board(c.id);
        Leaderboard.Entry oldRecord = board.record();
        rec.finishes++;
        cs.finishes++;
        boolean pb = before == 0 || ms < before;
        Medal oldMedal = before > 0 ? c.medal(before) : null;
        Medal medal = c.medal(pb ? ms : before);
        long now = Store.now();
        if (pb) {
            cs.best = ms;
            cs.bestWhen = now;
            cs.splits = new ArrayList<>();
            for (long s : r.splits) {
                cs.splits.add(s);
            }
            float[] path = r.path();
            if (path.length > 0) {
                store.saveGhost(id, c.id, path);
                ghosts.put(id + ":" + c.id, path);
            }
        }
        boolean weekly = c.id.equals(store.weekCourse());
        boolean firstWeekly = false;
        if (weekly) {
            firstWeekly = !rec.weekDone;
            rec.weekDone = true;
            if (rec.weekBest == 0 || ms < rec.weekBest) {
                rec.weekBest = ms;
                rec.weekBestWhen = now;
            }
        }
        store.submit(rec, c.id, cs.best, cs.bestWhen);
        // Titel und Nachrichten
        Medal shown = c.medal(ms);
        p.showTitle(Title.title(Texts.get(l, "finish.title"), Texts.get(l, "finish.sub", Texts.arg("time",
                TimeFormat.clock(ms, l)), Texts.comp("medal", medal(shown, l))), LONG));
        if (before == 0) {
            p.sendMessage(Texts.chat(l, "finish.first", Texts.comp("course", courseName(c, l)),
                    Texts.arg("time", TimeFormat.clock(ms, l))));
        } else if (pb) {
            p.sendMessage(Texts.chat(l, "finish.pb", Texts.arg("time", TimeFormat.clock(ms, l)),
                    Texts.comp("delta", Component.text("(" + TimeFormat.diff(ms - before, l) + ")", NamedTextColor.GREEN)),
                    Texts.comp("course", courseName(c, l))));
        } else {
            p.sendMessage(Texts.chat(l, "finish.nopb", Texts.arg("time", TimeFormat.clock(ms, l)),
                    Texts.comp("delta", Component.text("(" + TimeFormat.diff(ms - before, l) + ")", NamedTextColor.RED)),
                    Texts.arg("best", TimeFormat.clock(before, l))));
        }
        if (medal != null && (oldMedal == null || medal.ordinal() > oldMedal.ordinal())) {
            p.sendMessage(Texts.chat(l, "finish.medal", Texts.comp("medal", medal(medal, l))));
        }
        Medal next = Medal.next(medal);
        if (next != null) {
            p.sendMessage(Texts.chat(l, "finish.next", Texts.comp("medal", medal(next, l)),
                    Texts.arg("diff", TimeFormat.diff(cs.best - next.limit(c.medals), l).substring(1))));
        }
        p.sendMessage(Texts.chat(l, "finish.rank", Texts.arg("rank", board.rank(id)), Texts.arg("total", board.size()),
                Texts.comp("course", courseName(c, l))));
        if (pb) {
            p.playSound(at, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.2f);
        }
        Leaderboard.Entry newRecord = board.record();
        if (pb && newRecord != null && newRecord.uuid().equals(id)
                && (oldRecord == null || oldRecord.ms() > ms)) {
            for (Player o : Bukkit.getOnlinePlayers()) {
                String ol = pl.lang(o);
                o.sendMessage(Texts.chat(ol, "finish.record", Texts.arg("player", p.getName()),
                        Texts.comp("course", courseName(c, ol)), Texts.arg("time", TimeFormat.clock(ms, ol))));
                o.playSound(o.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.7f, 1f);
            }
            p.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, at.getX(), at.getY() + 1, at.getZ(), 60, 0.6, 1, 0.6, 0.3);
        }
        if (weekly) {
            Leaderboard wk = store.weekly();
            p.sendMessage(Texts.chat(l, "finish.weekly", Texts.arg("rank", wk.rank(id))));
            if (firstWeekly) {
                p.sendMessage(Texts.chat(l, "weekly.badge"));
                p.playSound(at, Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1.4f);
                for (Player o : Bukkit.getOnlinePlayers()) {
                    if (!o.equals(p)) {
                        o.sendMessage(Texts.chat(pl.lang(o), "weekly.broadcast", Texts.arg("player", p.getName())));
                    }
                }
            }
        }
        // Netzwerk: Zähler (ohne Bestwerte) und Bestzeit getrennt.
        Map<String, Long> add = new LinkedHashMap<>();
        add.put("finishes", 1L);
        add.put("finishes_" + c.id, 1L);
        if (r.falls > 0) {
            add.put("falls", (long) r.falls);
        }
        pl.net().stats(id, add, Map.of());
        pl.net().stats(id, Map.of(), Map.of("time_" + c.id + "_ms", ms));
        store.save(rec);
        pl.boards(c.id);
        raceResult(p, r, ms);
        pl.state();
    }

    private void reportFalls(Player p, Run r) {
        if (r.falls > 0 && !r.practice) {
            pl.net().stats(p.getUniqueId(), Map.of("falls", (long) r.falls), Map.of());
        }
        r.falls = 0;
    }

    // =========================================================================================
    // Anzeige unterwegs
    // =========================================================================================

    private void hud(Player p, Run r) {
        String l = pl.lang(p);
        if (tick % 2 == 0) {
            if (tick < r.splitShownUntil && r.splitText != null) {
                p.sendActionBar(r.splitText);
            } else if (r.state == Run.State.RUNNING) {
                long ms = r.clock.now(tick);
                Component bar = Component.text("⏱ ", NamedTextColor.AQUA)
                        .append(Component.text(TimeFormat.clock(ms, l), NamedTextColor.WHITE))
                        .append(Component.text("  ·  ", NamedTextColor.DARK_GRAY))
                        .append(Component.text("Checkpoint " + r.reached + "/" + r.course.checkpointCount(),
                                NamedTextColor.GOLD));
                if (r.practice) {
                    bar = bar.append(Component.text("  ·  ", NamedTextColor.DARK_GRAY)).append(Texts.get(l, "run.practice"));
                }
                p.sendActionBar(bar);
            } else if (r.state == Run.State.READY && tick % 20 == 0) {
                p.sendActionBar(Texts.get(l, "run.ready"));
            }
        }
        // Wegweiser: Funken über dem nächsten Ziel (nur für diesen Spieler)
        if (tick % 10 == 0 && r.state == Run.State.RUNNING) {
            Pad next = r.reached < r.course.checkpointCount() ? r.course.checkpoints().get(r.reached) : r.course.finish();
            p.spawnParticle(Particle.END_ROD, next.centerX(), next.surface() + 1.2, next.centerZ(), 4, 0.15, 0.6, 0.15, 0.005);
        }
        // Geist der Bestzeit: blaue Funken laufen den besten Weg mit
        if (r.ghost != null && r.state == Run.State.RUNNING) {
            long i = (tick - r.clock.startTick()) * 3;
            if (i >= 0 && i + 2 < r.ghost.length) {
                double gx = r.ghost[(int) i];
                double gy = r.ghost[(int) i + 1];
                double gz = r.ghost[(int) i + 2];
                p.spawnParticle(Particle.DUST, gx, gy + 0.3, gz, 2, 0.08, 0.1, 0.08, 0, GHOST);
                p.spawnParticle(Particle.DUST, gx, gy + 1.0, gz, 2, 0.12, 0.25, 0.12, 0, GHOST);
                p.spawnParticle(Particle.DUST, gx, gy + 1.65, gz, 1, 0.05, 0.05, 0.05, 0, GHOST);
            }
        }
    }

    private float[] ghost(UUID id, String course) {
        String key = id + ":" + course;
        if (!ghosts.containsKey(key)) {
            ghosts.put(key, pl.store().loadGhost(id, course));
        }
        return ghosts.get(key);
    }

    public void forgetGhosts(UUID id) {
        ghosts.keySet().removeIf(k -> k.startsWith(id + ":"));
    }

    // =========================================================================================
    // Party-Rennen
    // =========================================================================================

    private void race(Race race) {
        switch (race.state) {
            case GATHER -> {
                boolean all = race.joined.containsAll(race.invited);
                if (tick < race.until && !all) {
                    if ((race.until - tick) % 100 == 0) {
                        for (UUID u : race.joined) {
                            Player o = Bukkit.getPlayer(u);
                            if (o != null) {
                                o.sendActionBar(Texts.get(pl.lang(o), "race.wait", Texts.arg("sec", (race.until - tick) / 20)));
                            }
                        }
                    }
                    return;
                }
                for (UUID u : race.invited) {
                    if (invites.get(u) == race) {
                        invites.remove(u);
                        Player o = Bukkit.getPlayer(u);
                        if (o != null && !runs.containsKey(u)) {
                            pl.hotbar().base(o);
                        }
                    }
                }
                if (race.joined.size() <= 1) {
                    races.remove(race);
                    Run r = runs.get(race.leader);
                    Player p = Bukkit.getPlayer(race.leader);
                    if (r != null && p != null) {
                        r.race = null;
                        r.state = Run.State.READY;
                        p.sendMessage(Texts.chat(pl.lang(p), "race.solo"));
                    }
                    return;
                }
                race.state = Race.State.COUNTDOWN;
                race.count = 3;
                race.until = tick;
            }
            case COUNTDOWN -> {
                if (tick < race.until) {
                    return;
                }
                if (race.count > 0) {
                    for (Player o : members(race)) {
                        o.showTitle(Title.title(Component.text(race.count, NamedTextColor.YELLOW), Component.empty(), SHORT));
                        o.playSound(o.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 1f, 1f);
                    }
                    race.count--;
                    race.until = tick + 20;
                    return;
                }
                race.state = Race.State.RUNNING;
                for (Player o : members(race)) {
                    Run r = runs.get(o.getUniqueId());
                    if (r == null) {
                        continue;
                    }
                    r.state = Run.State.RUNNING;
                    r.clock = new RunClock(tick, 0);
                    countAttempt(o, r);
                    o.showTitle(Title.title(Texts.get(pl.lang(o), "race.go"), Component.empty(), SHORT));
                    o.playSound(o.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2f);
                }
            }
            case RUNNING -> checkRaceOver(race);
        }
    }

    private List<Player> members(Race race) {
        List<Player> out = new ArrayList<>();
        for (UUID u : race.joined) {
            Player o = Bukkit.getPlayer(u);
            Run r = runs.get(u);
            if (o != null && r != null && r.race == race) {
                out.add(o);
            }
        }
        return out;
    }

    private void raceResult(Player p, Run r, long ms) {
        Race race = r.race;
        if (race == null || race.state != Race.State.RUNNING) {
            return;
        }
        race.results.add(new Race.Result(p.getUniqueId(), p.getName(), ms));
        int place = race.results.size();
        for (UUID u : race.joined) {
            Player o = Bukkit.getPlayer(u);
            if (o != null) {
                String ol = pl.lang(o);
                o.sendMessage(Texts.chat(ol, "race.finish", Texts.arg("player", p.getName()),
                        Texts.arg("time", TimeFormat.clock(ms, ol)), Texts.arg("place", place)));
            }
        }
        checkRaceOver(race);
    }

    private void checkRaceOver(Race race) {
        if (!races.contains(race) || race.state != Race.State.RUNNING || !race.done()) {
            return;
        }
        races.remove(race);
        if (race.results.isEmpty()) {
            return;
        }
        String winner = race.results.get(0).name();
        for (UUID u : race.joined) {
            Player o = Bukkit.getPlayer(u);
            if (o != null) {
                o.sendMessage(Texts.chat(pl.lang(o), "race.over", Texts.arg("player", winner)));
            }
        }
    }

    // =========================================================================================
    // Hilfen
    // =========================================================================================

    private void teleport(Player p, Run r, Location to) {
        p.setVelocity(new Vector());
        p.setFallDistance(0);
        p.teleport(to);
        r.hasPrev = false;
    }

    public static Component courseName(Course c, String lang) {
        return Component.text(c.name(lang), TextColor.fromHexString(c.difficulty.colour));
    }

    public static Component difficulty(Course c, String lang) {
        return Component.text(c.difficulty.stars() + " " + c.difficulty.name(lang), TextColor.fromHexString(c.difficulty.colour));
    }

    public static Component medal(Medal m, String lang) {
        if (m == null) {
            return Component.empty();
        }
        return Component.text(m.symbol + " " + m.name(lang), TextColor.fromHexString(m.colour));
    }
}
