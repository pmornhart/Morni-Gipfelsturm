package de.moinmorni.gipfelsturm.world;

import de.moinmorni.gipfelsturm.course.Blk;
import de.moinmorni.gipfelsturm.course.Plan;
import de.moinmorni.gipfelsturm.courses.World;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.GameRule;
import org.bukkit.GameRules;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Die Gipfelsturm-Welt: eine eigene Welt, deren Chunks der {@link VoidGenerator} direkt aus dem Bauplan erzeugt. Ändert
 * sich der Plan (neue Version mit anderen Strecken), wird die Welt gelöscht und entsteht neu – Spieler bauen hier
 * nichts, es geht nichts verloren. Schilder bekommen ihren Text, sobald ihr Chunk zum ersten Mal geladen wird.
 */
public final class WorldManager implements Listener {

    private static final String STAMP = "welt.yml";

    private final JavaPlugin plugin;
    private final World model;
    private final String name;
    private final Logger log;
    private final Map<Long, List<Plan.Sign>> signs = new HashMap<>();
    private org.bukkit.World world;
    private boolean ready;
    private VoidGenerator generator;

    public WorldManager(JavaPlugin plugin, World model, String name) {
        this.plugin = plugin;
        this.model = model;
        this.name = name;
        this.log = plugin.getLogger();
    }

    public org.bukkit.World world() {
        return world;
    }

    public boolean ready() {
        return ready;
    }

    /** Welt laden oder (bei neuem Bauplan) frisch anlegen. */
    public void setup() throws IOException {
        Plan plan = model.plan();
        long t = System.currentTimeMillis();
        generator = prepare(plan);
        String fingerprint = plan.fingerprint();
        Path stampFile = plugin.getDataFolder().toPath().resolve(STAMP);
        YamlConfiguration stamp = YamlConfiguration.loadConfiguration(stampFile.toFile());
        String built = stamp.getString("bauplan", "");
        world = create();
        if (!fingerprint.equals(built)) {
            if (!built.isEmpty()) {
                log.info("Neuer Bauplan (" + built + " → " + fingerprint + ") – die Gipfelsturm-Welt entsteht neu.");
                File folder = world.getWorldFolder();
                if (!Bukkit.unloadWorld(world, false)) {
                    throw new IOException("Welt " + name + " ließ sich nicht entladen");
                }
                deleteFolder(folder.toPath());
                world = create();
            }
            stamp.set("bauplan", fingerprint);
            stamp.set("bloecke", plan.size());
            stamp.set("angelegt", java.time.Instant.now().toString());
            Files.createDirectories(stampFile.getParent());
            stamp.save(stampFile.toFile());
        }
        rules();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        ready = true;
        log.info("Gipfelsturm-Welt bereit (" + plan.size() + " Blöcke im Bauplan, " + (System.currentTimeMillis() - t)
                + " ms).");
    }

    /** Bauplan nach Chunks sortieren und Block-Zustände einmal anlegen (Hauptthread). */
    private VoidGenerator prepare(Plan plan) {
        Map<String, Short> ids = new HashMap<>();
        List<BlockData> palette = new ArrayList<>();
        Map<Long, List<long[]>> byChunk = new HashMap<>();
        for (Map.Entry<Long, Blk> e : plan.blocks().entrySet()) {
            long k = e.getKey();
            String data = e.getValue().data();
            Short id = ids.get(data);
            if (id == null) {
                BlockData bd;
                try {
                    bd = Bukkit.createBlockData(data);
                } catch (IllegalArgumentException ex) {
                    log.warning("Unbekannter Block im Bauplan: " + data);
                    bd = null;
                }
                id = (short) palette.size();
                palette.add(bd);
                ids.put(data, id);
            }
            int x = Plan.x(k);
            int y = Plan.y(k);
            int z = Plan.z(k);
            byChunk.computeIfAbsent(VoidGenerator.chunkKey(x >> 4, z >> 4), c -> new ArrayList<>())
                    .add(new long[] {(y << 8) | ((z & 15) << 4) | (x & 15), id});
        }
        Map<Long, VoidGenerator.ChunkBlocks> chunks = new HashMap<>();
        byChunk.forEach((key, list) -> {
            int[] pos = new int[list.size()];
            short[] pal = new short[list.size()];
            for (int i = 0; i < list.size(); i++) {
                pos[i] = (int) list.get(i)[0];
                pal[i] = (short) list.get(i)[1];
            }
            chunks.put(key, new VoidGenerator.ChunkBlocks(pos, pal));
        });
        for (Plan.Sign s : plan.signs()) {
            signs.computeIfAbsent(VoidGenerator.chunkKey(s.x() >> 4, s.z() >> 4), c -> new ArrayList<>()).add(s);
        }
        return new VoidGenerator(model, Map.copyOf(chunks), palette.toArray(new BlockData[0]));
    }

    private org.bukkit.World create() {
        WorldCreator wc = new WorldCreator(name).generator(generator)
                .environment(org.bukkit.World.Environment.NORMAL).generateStructures(false);
        org.bukkit.World w = Bukkit.createWorld(wc);
        if (w == null) {
            throw new IllegalStateException("Welt " + name + " ließ sich nicht anlegen");
        }
        return w;
    }

    /** Schilder beschriften, wenn ihr Chunk geladen wird (beim ersten Mal entstehen sie leer). */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.getWorld().equals(world)) {
            return;
        }
        List<Plan.Sign> list = signs.get(VoidGenerator.chunkKey(event.getChunk().getX(), event.getChunk().getZ()));
        if (list == null) {
            return;
        }
        for (Plan.Sign s : list) {
            Block b = world.getBlockAt(s.x(), s.y(), s.z());
            if (b.getState() instanceof Sign sign) {
                SignSide side = sign.getSide(Side.FRONT);
                boolean same = true;
                for (int i = 0; i < Math.min(4, s.lines().size()); i++) {
                    Component want = Component.text(s.lines().get(i));
                    if (!want.equals(side.line(i))) {
                        side.line(i, want);
                        same = false;
                    }
                }
                if (!same || !sign.isWaxed()) {
                    sign.setWaxed(true);
                    sign.update(true, false);
                }
            }
        }
    }

    /** Ewiger Spätvormittag ohne Wetter, keine Monster, nichts geht kaputt. */
    private void rules() {
        world.setDifficulty(Difficulty.PEACEFUL);
        world.setTime(4500);
        world.setStorm(false);
        world.setThundering(false);
        world.setSpawnLocation(new Location(world, model.basecamp().spawnX(), model.basecamp().spawnY(),
                model.basecamp().spawnZ(), model.basecamp().spawnYaw(), 0));
        rule(GameRules.ADVANCE_TIME, false);
        rule(GameRules.ADVANCE_WEATHER, false);
        rule(GameRules.SPAWN_MOBS, false);
        rule(GameRules.SPAWN_MONSTERS, false);
        rule(GameRules.SPAWN_PHANTOMS, false);
        rule(GameRules.SPAWN_PATROLS, false);
        rule(GameRules.SPAWN_WANDERING_TRADERS, false);
        rule(GameRules.FALL_DAMAGE, false);
        rule(GameRules.FIRE_DAMAGE, false);
        rule(GameRules.DROWNING_DAMAGE, false);
        rule(GameRules.FREEZE_DAMAGE, false);
        rule(GameRules.PVP, false);
        rule(GameRules.RANDOM_TICK_SPEED, 0);
        rule(GameRules.KEEP_INVENTORY, true);
        rule(GameRules.IMMEDIATE_RESPAWN, true);
        rule(GameRules.SHOW_ADVANCEMENT_MESSAGES, false);
        rule(GameRules.SHOW_DEATH_MESSAGES, false);
        rule(GameRules.MOB_GRIEFING, false);
        rule(GameRules.BLOCK_DROPS, false);
        rule(GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER, 0);
        rule(GameRules.RESPAWN_RADIUS, 0);
        rule(GameRules.LOCATOR_BAR, false);
        rule(GameRules.WATER_SOURCE_CONVERSION, false);
    }

    private <T> void rule(GameRule<T> r, T value) {
        try {
            world.setGameRule(r, value);
        } catch (RuntimeException e) {
            log.fine("Spielregel " + r + " nicht gesetzt: " + e.getMessage());
        }
    }

    /** Ordner der alten Welt löschen – nur wenn es wirklich der eigene Welt-Ordner ist. */
    private void deleteFolder(Path folder) throws IOException {
        Path server = Bukkit.getWorldContainer().toPath().toAbsolutePath().normalize();
        Path target = folder.toAbsolutePath().normalize();
        Path main = Bukkit.getWorlds().get(0).getWorldFolder().toPath().toAbsolutePath().normalize();
        if (target.equals(server) || target.equals(main) || !target.startsWith(server)
                || !target.getFileName().toString().equals(name)) {
            throw new IOException("Welt-Ordner sieht nicht nach Gipfelsturm aus, lösche ihn nicht: " + target);
        }
        if (!Files.exists(target)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(target)) {
            for (Path p : walk.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    /** Nach einem Befehl: beim nächsten Start neu entstehen lassen. */
    public void scheduleRebuild() throws IOException {
        Path stampFile = plugin.getDataFolder().toPath().resolve(STAMP);
        YamlConfiguration stamp = YamlConfiguration.loadConfiguration(stampFile.toFile());
        stamp.set("bauplan", "neu");
        stamp.save(stampFile.toFile());
    }

    public static Material material(String id) {
        Material m = Material.matchMaterial(id);
        return m == null ? Material.PAPER : m;
    }
}
