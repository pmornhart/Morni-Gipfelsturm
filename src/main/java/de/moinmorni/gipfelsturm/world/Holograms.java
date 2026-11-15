package de.moinmorni.gipfelsturm.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Schwebende Tafeln und Marker (Text- und Block-Anzeigen): Bestenlisten im Basislager, Start, Checkpoints und Ziel auf
 * den Strecken. Sie werden nie gespeichert (nicht dauerhaft), sondern beim Laden eines Chunks neu erzeugt – so gibt es
 * nie doppelte. Texte in zwei Sprachen: zwei Anzeigen, jeder Spieler sieht die seiner Sprache.
 */
public final class Holograms implements Listener {

    public static final String TAG = "gipfelsturm";
    private static final List<String> LANGS = List.of("de", "en");

    /** Was angezeigt wird. */
    public static final class Spec {
        final String id;
        final double x;
        final double y;
        final double z;
        final Function<String, Component> text;
        final boolean perLang;
        final String block;
        final float scale;
        final boolean spin;
        final Map<String, Entity> spawned = new HashMap<>();

        private Spec(String id, double x, double y, double z, Function<String, Component> text, boolean perLang,
                     String block, float scale, boolean spin) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
            this.text = text;
            this.perLang = perLang;
            this.block = block;
            this.scale = scale;
            this.spin = spin;
        }

        long chunk() {
            return ((long) ((int) Math.floor(x) >> 4) << 32) | (((int) Math.floor(z) >> 4) & 0xFFFFFFFFL);
        }
    }

    private final JavaPlugin plugin;
    private final org.bukkit.World world;
    private final Function<UUID, String> langOf;
    private final Map<Long, List<Spec>> byChunk = new HashMap<>();
    private final Map<String, Spec> byId = new HashMap<>();
    private final Map<UUID, String> seen = new HashMap<>();
    private float angle;

    public Holograms(JavaPlugin plugin, org.bukkit.World world, Function<UUID, String> langOf) {
        this.plugin = plugin;
        this.world = world;
        this.langOf = langOf;
    }

    /** Text-Tafel; {@code perLang} = eigene Anzeige je Sprache. */
    public void text(String id, double x, double y, double z, Function<String, Component> text, boolean perLang,
                     float scale) {
        add(new Spec(id, x, y, z, text, perLang, null, scale, false));
    }

    /** Sich drehender Block (Marker für Checkpoint und Ziel). */
    public void block(String id, double x, double y, double z, String blockData, float scale) {
        add(new Spec(id, x, y, z, null, false, blockData, scale, true));
    }

    private void add(Spec s) {
        byChunk.computeIfAbsent(s.chunk(), k -> new ArrayList<>()).add(s);
        byId.put(s.id, s);
    }

    /** Alte Reste entfernen und alles in geladenen Chunks erzeugen. */
    public void start() {
        for (Entity e : world.getEntities()) {
            if (e.getScoreboardTags().contains(TAG)) {
                e.remove();
            }
        }
        for (List<Spec> list : byChunk.values()) {
            for (Spec s : list) {
                int cx = (int) Math.floor(s.x) >> 4;
                int cz = (int) Math.floor(s.z) >> 4;
                if (world.isChunkLoaded(cx, cz)) {
                    spawn(s);
                }
            }
        }
        Bukkit.getPluginManager().registerEvents(this, plugin);
        Bukkit.getScheduler().runTaskTimer(plugin, this::spin, 40L, 40L);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.getWorld().equals(world)) {
            return;
        }
        long key = ((long) event.getChunk().getX() << 32) | (event.getChunk().getZ() & 0xFFFFFFFFL);
        List<Spec> list = byChunk.get(key);
        if (list == null) {
            return;
        }
        // Erst im nächsten Tick: beim Laden selbst keine neuen Wesen erzeugen.
        Bukkit.getScheduler().runTask(plugin, () -> list.forEach(this::spawn));
    }

    private void spawn(Spec s) {
        for (Entity e : s.spawned.values()) {
            if (e.isValid()) {
                return; // schon da
            }
        }
        s.spawned.clear();
        Location at = new Location(world, s.x, s.y, s.z);
        if (s.block != null) {
            BlockData data;
            try {
                data = Bukkit.createBlockData(s.block);
            } catch (IllegalArgumentException e) {
                return;
            }
            BlockDisplay d = world.spawn(at, BlockDisplay.class, b -> {
                prepare(b);
                b.setBlock(data);
                float h = s.scale / 2;
                b.setTransformation(new Transformation(new Vector3f(-h, -h, -h), new AxisAngle4f(),
                        new Vector3f(s.scale, s.scale, s.scale), new AxisAngle4f()));
                b.setBrightness(new Display.Brightness(15, 15));
            });
            s.spawned.put("all", d);
            return;
        }
        for (String lang : s.perLang ? LANGS : List.of("de")) {
            TextDisplay d = world.spawn(at, TextDisplay.class, t -> {
                prepare(t);
                t.text(s.text.apply(lang));
                t.setBillboard(Display.Billboard.VERTICAL);
                t.setAlignment(TextDisplay.TextAlignment.CENTER);
                t.setLineWidth(260);
                t.setShadowed(true);
                t.setDefaultBackground(false);
                t.setBackgroundColor(Color.fromARGB(110, 10, 20, 35));
                t.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                        new Vector3f(s.scale, s.scale, s.scale), new AxisAngle4f()));
                t.setBrightness(new Display.Brightness(15, 15));
                if (s.perLang) {
                    t.setVisibleByDefault(false);
                }
            });
            s.spawned.put(s.perLang ? lang : "all", d);
            if (s.perLang) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (lang.equals(langOf.apply(p.getUniqueId()))) {
                        p.showEntity(plugin, d);
                    }
                }
            }
        }
    }

    private static void prepare(Display d) {
        d.setPersistent(false);
        d.addScoreboardTag(TAG);
        d.setViewRange(1.5f);
    }

    /** Text einer Tafel neu setzen (z. B. neue Bestzeit). */
    public void refresh(String id) {
        Spec s = byId.get(id);
        if (s == null || s.text == null) {
            return;
        }
        s.spawned.forEach((lang, e) -> {
            if (e instanceof TextDisplay t && t.isValid()) {
                t.text(s.text.apply("all".equals(lang) ? "de" : lang));
            }
        });
    }

    public void refreshAll() {
        byId.keySet().forEach(this::refresh);
    }

    /** Sprache eines Spielers (beim Betreten und wenn sie sich ändert): passende Tafeln zeigen, andere verstecken. */
    public void language(Player p) {
        String lang = langOf.apply(p.getUniqueId());
        if (lang.equals(seen.get(p.getUniqueId()))) {
            return;
        }
        seen.put(p.getUniqueId(), lang);
        for (Spec s : byId.values()) {
            if (!s.perLang) {
                continue;
            }
            s.spawned.forEach((l, e) -> {
                if (!e.isValid()) {
                    return;
                }
                if (l.equals(lang)) {
                    p.showEntity(plugin, e);
                } else {
                    p.hideEntity(plugin, e);
                }
            });
        }
    }

    public void forget(UUID uuid) {
        seen.remove(uuid);
    }

    /** Marker drehen sich langsam (sanft über die Interpolation des Spiels). */
    private void spin() {
        angle += (float) (Math.PI / 2);
        for (Spec s : byId.values()) {
            if (!s.spin) {
                continue;
            }
            Entity e = s.spawned.get("all");
            if (e instanceof BlockDisplay b && b.isValid()) {
                float h = s.scale / 2;
                b.setInterpolationDelay(0);
                b.setInterpolationDuration(40);
                // Drehen um die Mitte: erst verschieben, dann drehen (Transformation: Verschiebung + linke Drehung).
                Vector3f offset = new Vector3f(-h, -h, -h).rotateY(angle);
                b.setTransformation(new Transformation(offset, new AxisAngle4f(angle, 0, 1, 0),
                        new Vector3f(s.scale, s.scale, s.scale), new AxisAngle4f()));
            }
        }
    }
}
