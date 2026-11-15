package de.moinmorni.gipfelsturm;

import de.moinmorni.gipfelsturm.cmd.GipfelCommand;
import de.moinmorni.gipfelsturm.course.Course;
import de.moinmorni.gipfelsturm.course.CourseValidator;
import de.moinmorni.gipfelsturm.course.Medal;
import de.moinmorni.gipfelsturm.course.Pad;
import de.moinmorni.gipfelsturm.courses.Basecamp;
import de.moinmorni.gipfelsturm.courses.World;
import de.moinmorni.gipfelsturm.game.Boards;
import de.moinmorni.gipfelsturm.game.Controls;
import de.moinmorni.gipfelsturm.game.GameMenus;
import de.moinmorni.gipfelsturm.game.Guard;
import de.moinmorni.gipfelsturm.game.Hotbar;
import de.moinmorni.gipfelsturm.game.Runs;
import de.moinmorni.gipfelsturm.net.Net;
import de.moinmorni.gipfelsturm.stats.PlayerRecord;
import de.moinmorni.gipfelsturm.stats.Store;
import de.moinmorni.gipfelsturm.stats.WeeklyRotation;
import de.moinmorni.gipfelsturm.ui.Items;
import de.moinmorni.gipfelsturm.ui.Menu;
import de.moinmorni.gipfelsturm.ui.Texts;
import de.moinmorni.gipfelsturm.world.Holograms;
import de.moinmorni.gipfelsturm.world.WorldManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

/**
 * Gipfelsturm – Parkour-Modus des MoinMorni-Netzwerks. Baut beim Start seine eigene Welt (Basislager + acht Strecken),
 * misst Zeiten, führt Bestenlisten und meldet Statistik und Zustand ans Netzwerk. Läuft auch ohne Netzwerk.
 */
public final class GipfelsturmPlugin extends JavaPlugin implements Listener {

    /** Einstellungen aus der config.yml. */
    public static final class Settings {
        public String world = "gipfelsturm";
        public boolean plausible = true;
        public int raceWait = 15;
        public boolean ghostDefault = true;
    }

    private static final String NO_COLLISION = "gipfel_nocoll";
    private static final Title.Times WELCOME = Title.Times.times(Duration.ofMillis(300), Duration.ofMillis(3500),
            Duration.ofMillis(900));

    private final Settings settings = new Settings();
    private World model;
    private WorldManager worlds;
    private Net net;
    private Store store;
    private Holograms holos;
    private Runs runs;
    private Hotbar hotbar;
    private GameMenus menus;
    private Controls controls;
    private final Map<UUID, String> langSeen = new HashMap<>();
    private final Map<UUID, Component> tabBase = new HashMap<>();
    private Course weekly;
    private String lastState = "";
    private long lastStateAt;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings.world = getConfig().getString("welt", "gipfelsturm");
        settings.plausible = getConfig().getBoolean("plausibel-pruefen", true);
        settings.raceWait = Math.max(5, getConfig().getInt("rennen-wartezeit", 15));
        settings.ghostDefault = getConfig().getBoolean("geist-standard", true);
        Items.init(this);

        model = World.create();
        List<String> problems = validate();
        if (!problems.isEmpty()) {
            getLogger().warning("Strecken-Prüfung: " + problems.size() + " Hinweise:");
            problems.forEach(p -> getLogger().warning("  " + p));
        }
        worlds = new WorldManager(this, model, settings.world);
        try {
            worlds.setup();
        } catch (IOException | RuntimeException e) {
            getLogger().severe("Die Gipfelsturm-Welt ließ sich nicht bauen: " + e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        net = Net.detect(this);
        store = new Store(getDataFolder().toPath(), getLogger(), model.courses());
        store.ghostDefault(settings.ghostDefault);
        try {
            store.load();
        } catch (IOException e) {
            getLogger().severe("Spielerdaten ließen sich nicht laden: " + e);
        }
        rollWeek();

        hotbar = new Hotbar(this);
        menus = new GameMenus(this);
        runs = new Runs(this);
        controls = new Controls(this);
        holos = new Holograms(this, worlds.world(), this::langOf);
        new Boards(this).register(holos);
        keepBasecampLoaded();
        holos.start();
        runs.start();

        var pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
        pm.registerEvents(controls, this);
        pm.registerEvents(new Guard(this), this);
        pm.registerEvents(new Menu.Clicks(), this);
        PluginCommand cmd = getCommand("gipfel");
        if (cmd != null) {
            GipfelCommand gc = new GipfelCommand(this);
            cmd.setExecutor(gc);
            cmd.setTabCompleter(gc);
        }
        Bukkit.getScheduler().runTaskTimer(this, this::everySecond, 20L, 20L);
        Bukkit.getScheduler().runTaskTimer(this, this::everyFiveSeconds, 100L, 100L);
        for (Player p : Bukkit.getOnlinePlayers()) {
            welcome(p, false);
        }
        state();
        getLogger().info("Gipfelsturm bereit: " + model.courses().size() + " Strecken, Strecke der Woche: "
                + (weekly == null ? "-" : weekly.nameDe) + ".");
    }

    @Override
    public void onDisable() {
        if (store != null) {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (runs != null) {
                    runs.abandon(p);
                }
            }
            store.close();
        }
    }

    // =========================================================================================
    // Für die anderen Teile
    // =========================================================================================

    public Settings settings() {
        return settings;
    }

    public World model() {
        return model;
    }

    public WorldManager worlds() {
        return worlds;
    }

    public org.bukkit.World world() {
        return worlds.world();
    }

    public Net net() {
        return net;
    }

    public Store store() {
        return store;
    }

    public Runs runs() {
        return runs;
    }

    public Hotbar hotbar() {
        return hotbar;
    }

    public GameMenus menus() {
        return menus;
    }

    public String lang(Player p) {
        return langOf(p.getUniqueId());
    }

    private String langOf(UUID id) {
        return net == null ? "de" : net.lang(id);
    }

    public List<String> validate() {
        List<String> out = new ArrayList<>();
        for (Course c : model.courses()) {
            CourseValidator.validate(c, model.plan()).forEach(p -> out.add(c.id + ": " + p));
        }
        out.addAll(CourseValidator.separate(model.courses(), List.of(model.basecamp().bounds())));
        return out;
    }

    /** Mitte des Starts, Blick in Laufrichtung. */
    public Location startLocation(Course c) {
        Pad s = c.start();
        return new Location(world(), s.centerX(), s.surface(), s.centerZ(), c.startDir.yaw(), 0);
    }

    /** Mitte eines Pads, Blick zum nächsten Pad. */
    public Location padLocation(Course c, Pad p) {
        float yaw = c.startDir.yaw();
        if (p.index + 1 < c.pads.size()) {
            Pad n = c.pads.get(p.index + 1);
            double dx = n.centerX() - p.centerX();
            double dz = n.centerZ() - p.centerZ();
            yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        } else if (p.arrival != null) {
            yaw = p.arrival.dir().yaw();
        }
        return new Location(world(), p.centerX(), p.surface(), p.centerZ(), yaw, 0);
    }

    public Location spawn() {
        Basecamp b = model.basecamp();
        return new Location(world(), b.spawnX(), b.spawnY(), b.spawnZ(), b.spawnYaw(), 0);
    }

    public void toBasecamp(Player p) {
        p.setFallDistance(0);
        p.teleport(spawn());
        hotbar.base(p);
    }

    /** Ins Nichts gefallen (ohne laufenden Versuch): zurück ins Basislager. */
    public void rescue(Player p) {
        if (!runs.running(p.getUniqueId())) {
            toBasecamp(p);
        }
    }

    public Course weeklyCourse() {
        return weekly;
    }

    /** Restzeit der Woche als Text („3 Tagen 4 Std.“). */
    public String weekLeft(String l) {
        Duration left = Duration.between(Instant.now(), WeeklyRotation.nextChange(Instant.now()));
        long days = left.toDays();
        long hours = left.toHoursPart();
        if (days > 0) {
            return Texts.raw(l, "time.days").replace("<d>", String.valueOf(days)).replace("<h>", String.valueOf(hours));
        }
        return Texts.raw(l, "time.hours").replace("<h>", String.valueOf(hours))
                .replace("<m>", String.valueOf(left.toMinutesPart()));
    }

    public void toggleHide(Player p) {
        PlayerRecord r = store.record(p.getUniqueId(), p.getName());
        r.hideOthers = !r.hideOthers;
        store.save(r);
        visibility(p);
        hotbar.refreshHide(p);
        p.sendMessage(Texts.chat(lang(p), r.hideOthers ? "hide.hidden" : "hide.shown"));
    }

    public void toggleGhost(Player p) {
        PlayerRecord r = store.record(p.getUniqueId(), p.getName());
        r.ghost = !r.ghost;
        store.save(r);
        p.sendMessage(Texts.chat(lang(p), r.ghost ? "ghost.on" : "ghost.off"));
    }

    /** Medaillen als „▲ 1 · ● 2 · ● 0 · ● 3“. */
    public Component medalCount(PlayerRecord r, String l) {
        int[] n = new int[Medal.values().length];
        for (Course c : model.courses()) {
            Medal m = c.medal(r.best(c.id));
            if (m != null) {
                n[m.ordinal()]++;
            }
        }
        Component out = Component.empty();
        for (int i = n.length - 1; i >= 0; i--) {
            Medal m = Medal.values()[i];
            out = out.append(Component.text(m.symbol + " " + m.name(l) + " " + n[i], TextColor.fromHexString(m.colour)));
            if (i > 0) {
                out = out.append(Component.text(" · ", NamedTextColor.DARK_GRAY));
            }
        }
        return out;
    }

    public void boards(String courseId) {
        holos.refresh("board:" + courseId);
        holos.refresh("weekly");
    }

    public void boardsAll() {
        holos.refreshAll();
    }

    /** Zustand ans Netzwerk (Lobby-Figur, Webseite): „open“ und Spielerzahl – nur bei Änderung, sonst jede Minute. */
    public void state() {
        Bukkit.getScheduler().runTask(this, () -> {
            int players = Bukkit.getOnlinePlayers().size();
            String now = "open:" + players;
            long t = System.currentTimeMillis();
            if (!now.equals(lastState) || t - lastStateAt > 60_000) {
                lastState = now;
                lastStateAt = t;
                net.state("open", players);
            }
        });
    }

    // =========================================================================================
    // Woche
    // =========================================================================================

    private void rollWeek() {
        Instant now = Instant.now();
        List<Course> cs = model.courses();
        Course w = cs.get(WeeklyRotation.index(now, cs.size()));
        String id = WeeklyRotation.weekId(now);
        boolean changed = weekly != null && (!id.equals(store.weekId()) || w != weekly);
        weekly = w;
        store.setWeek(id, w.id);
        if (changed) {
            getLogger().info("Neue Woche " + id + ": Strecke der Woche ist " + w.nameDe + ".");
            holos.refreshAll();
            for (Player p : Bukkit.getOnlinePlayers()) {
                store.record(p.getUniqueId(), p.getName());
                if (!runs.running(p.getUniqueId())) {
                    hotbar.base(p);
                }
            }
        }
    }

    // =========================================================================================
    // Beitreten und Verlassen
    // =========================================================================================

    @EventHandler
    public void onSpawnLocation(AsyncPlayerSpawnLocationEvent event) {
        if (worlds != null && worlds.ready()) {
            event.setSpawnLocation(spawn());
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        welcome(event.getPlayer(), true);
    }

    private void welcome(Player p, boolean greet) {
        store.record(p.getUniqueId(), p.getName());
        if (!p.getWorld().equals(world())) {
            p.teleport(spawn());
        }
        hotbar.base(p);
        p.setCollidable(false);
        noCollision(p);
        visibility(p);
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (!o.equals(p)) {
                visibility(o);
            }
        }
        holos.language(p);
        langSeen.put(p.getUniqueId(), lang(p));
        state();
        if (!greet) {
            return;
        }
        // Kurz warten: Die Sprache kommt vom Netzwerk erst kurz nach dem Betreten.
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (!p.isOnline()) {
                return;
            }
            String l = lang(p);
            p.showTitle(Title.title(Texts.get(l, "welcome.title"), Texts.get(l, "welcome.subtitle"), WELCOME));
            p.sendMessage(Texts.chat(l, "welcome.chat"));
            if (weekly != null) {
                p.sendMessage(Texts.chat(l, "welcome.weekly", Texts.comp("course", Runs.courseName(weekly, l)),
                        Texts.comp("badge", Texts.get(l, "badge"))));
            }
        }, 30L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        runs.quit(p);
        controls.forget(p.getUniqueId());
        holos.forget(p.getUniqueId());
        langSeen.remove(p.getUniqueId());
        tabBase.remove(p.getUniqueId());
        runs.forgetGhosts(p.getUniqueId());
        store.save(store.record(p.getUniqueId(), p.getName()));
        state();
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        event.setRespawnLocation(spawn());
        Bukkit.getScheduler().runTask(this, () -> hotbar.base(event.getPlayer()));
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if (event.getFrom().equals(world())) {
            runs.abandon(event.getPlayer());
        }
    }

    /** Abzeichen „Wochen-Gipfel“ im Chat – gilt, solange der Chat auf diesem Server bleibt (ohne Netzwerk). */
    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onChat(AsyncChatEvent event) {
        Player p = event.getPlayer();
        PlayerRecord r = store.find(p.getUniqueId());
        if (r == null || !r.weeklySummit(store.weekId())) {
            return;
        }
        event.renderer((source, name, message, viewer) -> {
            String l = viewer instanceof Player v ? lang(v) : "de";
            return Texts.get(l, "badge").append(Component.text(" ")).append(name)
                    .append(Component.text(" » ", NamedTextColor.DARK_GRAY)).append(message);
        });
    }

    // =========================================================================================
    // Wiederkehrend
    // =========================================================================================

    private void everySecond() {
        if (!WeeklyRotation.weekId(Instant.now()).equals(store.weekId())) {
            rollWeek();
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            String l = lang(p);
            if (!l.equals(langSeen.get(p.getUniqueId()))) {
                // Sprache kam vom Netzwerk oder wurde mit /sprache geändert: Tafeln und Leiste neu.
                langSeen.put(p.getUniqueId(), l);
                holos.language(p);
                if (runs.running(p.getUniqueId())) {
                    hotbar.run(p, runs.practice(p.getUniqueId()));
                } else {
                    hotbar.base(p);
                }
            }
            if (!runs.running(p.getUniqueId()) && p.getWorld().equals(world())) {
                Course near = model.near(p.getX(), p.getY(), p.getZ());
                double floor = near == null ? Basecamp.Y - 45 : near.lowestFallY() - 6;
                if (p.getY() < floor) {
                    toBasecamp(p);
                }
            }
        }
    }

    private void everyFiveSeconds() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            noCollision(p);
            badge(p);
        }
        state();
    }

    /** Spieler schieben sich nicht gegenseitig (auch nicht in Teams des Netzwerks). */
    private void noCollision(Player p) {
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getEntryTeam(p.getName());
        if (team == null) {
            team = board.getTeam(NO_COLLISION);
            if (team == null) {
                team = board.registerNewTeam(NO_COLLISION);
            }
            team.addEntry(p.getName());
        }
        if (team.getOption(Team.Option.COLLISION_RULE) != Team.OptionStatus.NEVER) {
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
    }

    /** ▲ hinter dem Namen in der Tab-Liste für alle mit Wochen-Gipfel. */
    private void badge(Player p) {
        PlayerRecord r = store.find(p.getUniqueId());
        boolean has = r != null && r.weeklySummit(store.weekId());
        Component current = p.playerListName();
        String plain = PlainTextComponentSerializer.plainText().serialize(current);
        if (has && !plain.endsWith(" ▲")) {
            tabBase.put(p.getUniqueId(), current);
            p.playerListName(current.append(Component.text(" ▲", TextColor.fromHexString("#7fdcff"))));
        } else if (!has && plain.endsWith(" ▲")) {
            Component base = tabBase.remove(p.getUniqueId());
            p.playerListName(base);
        }
    }

    /** Sichtbarkeit: Wer „Mitspieler ausblenden“ an hat, sieht niemanden. */
    private void visibility(Player viewer) {
        PlayerRecord r = store.record(viewer.getUniqueId(), viewer.getName());
        for (Player o : Bukkit.getOnlinePlayers()) {
            if (o.equals(viewer)) {
                continue;
            }
            if (r.hideOthers) {
                viewer.hidePlayer(this, o);
            } else {
                viewer.showPlayer(this, o);
            }
        }
    }

    /** Chunks des Basislagers bleiben geladen (Tafeln, schneller Einstieg). */
    private void keepBasecampLoaded() {
        org.bukkit.World w = world();
        for (int cx = -3; cx <= 2; cx++) {
            for (int cz = -3; cz <= 2; cz++) {
                w.setChunkForceLoaded(cx, cz, true);
            }
        }
    }
}
