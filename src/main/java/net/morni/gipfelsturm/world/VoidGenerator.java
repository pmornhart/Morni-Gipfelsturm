package net.morni.gipfelsturm.world;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.morni.gipfelsturm.course.Course;
import net.morni.gipfelsturm.courses.World;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.generator.BiomeProvider;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.generator.WorldInfo;

/**
 * Die Welt entsteht direkt aus dem Bauplan: Beim Erzeugen eines Chunks setzt der Generator genau die Blöcke des Plans
 * in diesem Chunk, sonst bleibt alles Himmel. So gibt es keinen langen Bau beim Start – Paper erzeugt die Chunks
 * parallel, wenn jemand hinkommt. Biome je Gegend für Gras-, Laub- und Himmelsfarben.
 */
public final class VoidGenerator extends ChunkGenerator {

    /** Blöcke eines Chunks: Position (x 4 Bit, z 4 Bit, y Rest) und Nummer in der Palette. */
    record ChunkBlocks(int[] pos, short[] palette) {
    }

    private final World model;
    private final Map<Long, ChunkBlocks> chunks;
    private final BlockData[] palette;

    public VoidGenerator(World model, Map<Long, ChunkBlocks> chunks, BlockData[] palette) {
        this.model = model;
        this.chunks = chunks;
        this.palette = palette;
    }

    public static long chunkKey(int cx, int cz) {
        return ((long) cx << 32) | (cz & 0xFFFFFFFFL);
    }

    /** Läuft auf den Erzeugungs-Threads: liest nur aus fertigen, unveränderlichen Tabellen. */
    @Override
    public void generateNoise(WorldInfo worldInfo, Random random, int chunkX, int chunkZ, ChunkData data) {
        ChunkBlocks b = chunks.get(chunkKey(chunkX, chunkZ));
        if (b == null) {
            return;
        }
        for (int i = 0; i < b.pos().length; i++) {
            int p = b.pos()[i];
            BlockData d = palette[b.palette()[i]];
            if (d != null) {
                data.setBlock(p & 15, p >> 8, (p >> 4) & 15, d);
            }
        }
    }

    @Override
    public boolean shouldGenerateNoise() {
        return false;
    }

    @Override
    public boolean shouldGenerateSurface() {
        return false;
    }

    @Override
    public boolean shouldGenerateCaves() {
        return false;
    }

    @Override
    public boolean shouldGenerateDecorations() {
        return false;
    }

    @Override
    public boolean shouldGenerateMobs() {
        return false;
    }

    @Override
    public boolean shouldGenerateStructures() {
        return false;
    }

    @Override
    public Location getFixedSpawnLocation(org.bukkit.World world, Random random) {
        return new Location(world, model.basecamp().spawnX(), model.basecamp().spawnY(), model.basecamp().spawnZ());
    }

    @Override
    public BiomeProvider getDefaultBiomeProvider(WorldInfo worldInfo) {
        return new Biomes(model);
    }

    /** Biom: um jede Strecke ihr eigenes, sonst Almwiese. */
    static final class Biomes extends BiomeProvider {
        private final List<double[]> areas = new ArrayList<>();
        private final List<Biome> biomes = new ArrayList<>();
        private final Biome fallback;
        private final List<Biome> all;

        Biomes(World model) {
            fallback = biome("minecraft:meadow", null);
            Set<Biome> used = new LinkedHashSet<>();
            used.add(fallback);
            for (Course c : model.courses()) {
                Biome b = biome(c.biome, fallback);
                used.add(b);
                biomes.add(b);
                // weiter Bereich um die Strecke (auch die Berge ringsum)
                areas.add(new double[] {c.bounds.minX() - 140, c.bounds.minZ() - 140, c.bounds.maxX() + 140,
                    c.bounds.maxZ() + 140});
            }
            all = List.copyOf(used);
        }

        private static Biome biome(String key, Biome fallback) {
            NamespacedKey k = NamespacedKey.fromString(key);
            Biome b = k == null ? null : Registry.BIOME.get(k);
            return b == null ? fallback : b;
        }

        @Override
        public Biome getBiome(WorldInfo worldInfo, int x, int y, int z) {
            for (int i = 0; i < areas.size(); i++) {
                double[] a = areas.get(i);
                if (x >= a[0] && x <= a[2] && z >= a[1] && z <= a[3]) {
                    return biomes.get(i);
                }
            }
            return fallback;
        }

        @Override
        public List<Biome> getBiomes(WorldInfo worldInfo) {
            return all;
        }
    }
}
