package de.moinmorni.gipfelsturm.course;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Bauplan der ganzen Gipfelsturm-Welt: jeder Block mit Position. Basislager, Strecken und Landschaft schreiben hier
 * hinein; das Plugin baut daraus die Welt (in Portionen), die Prüfung liest daraus, was im Weg ist.
 */
public final class Plan {

    /** Schild mit vier Zeilen (Vorderseite). */
    public record Sign(int x, int y, int z, List<String> lines) {
    }

    private final Map<Long, Blk> blocks = new LinkedHashMap<>();
    private final List<Sign> signs = new ArrayList<>();
    /** Zellen, in die Landschaft nicht hinein darf (Sprungwege und Stehplatz auf den Strecken). */
    private final Set<Long> reserved = new HashSet<>();

    public static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | (y & 0xFFF);
    }

    public static int x(long key) {
        return (int) (key >> 38);
    }

    public static int y(long key) {
        int y = (int) (key & 0xFFF);
        return y >= 0x800 ? y - 0x1000 : y;
    }

    public static int z(long key) {
        return (int) (key << 26 >> 38);
    }

    public void set(int x, int y, int z, Blk blk) {
        blocks.put(key(x, y, z), blk);
    }

    /** Nur setzen, wo noch nichts ist und kein Sprungweg verläuft (für Landschaft). */
    public void decorate(int x, int y, int z, Blk blk) {
        long k = key(x, y, z);
        if (!blocks.containsKey(k) && !reserved.contains(k)) {
            blocks.put(k, blk);
        }
    }

    public Blk get(int x, int y, int z) {
        return blocks.getOrDefault(key(x, y, z), Blk.AIR);
    }

    public boolean has(int x, int y, int z) {
        return blocks.containsKey(key(x, y, z));
    }

    public void remove(int x, int y, int z) {
        blocks.remove(key(x, y, z));
    }

    public void fill(int x0, int y0, int z0, int x1, int y1, int z1, Blk blk) {
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                    set(x, y, z, blk);
                }
            }
        }
    }

    public void sign(int x, int y, int z, List<String> lines) {
        signs.add(new Sign(x, y, z, List.copyOf(lines)));
    }

    public void reserve(int x, int y, int z) {
        reserved.add(key(x, y, z));
    }

    public boolean isReserved(int x, int y, int z) {
        return reserved.contains(key(x, y, z));
    }

    public Map<Long, Blk> blocks() {
        return Collections.unmodifiableMap(blocks);
    }

    public List<Sign> signs() {
        return Collections.unmodifiableList(signs);
    }

    public int size() {
        return blocks.size();
    }

    /** Alle festen Blöcke in einem Quader, außer denen, die {@code skip} ausnimmt. */
    public List<long[]> solidIn(Box box, Predicate<Long> skip) {
        List<long[]> out = new ArrayList<>();
        for (int x = (int) Math.floor(box.minX()); x < Math.ceil(box.maxX()); x++) {
            for (int y = (int) Math.floor(box.minY()) - 1; y < Math.ceil(box.maxY()); y++) {
                for (int z = (int) Math.floor(box.minZ()); z < Math.ceil(box.maxZ()); z++) {
                    long k = key(x, y, z);
                    Blk b = blocks.get(k);
                    if (b == null || !b.solid() || skip.test(k)) {
                        continue;
                    }
                    Box cb = b.shape().box(x, y, z);
                    if (cb != null && cb.intersects(box)) {
                        out.add(new long[] {x, y, z});
                    }
                }
            }
        }
        return out;
    }

    /**
     * Fingerabdruck des Plans: ändert sich ein Block, baut das Plugin die Welt neu. Reihenfolge-unabhängig.
     */
    public String fingerprint() {
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            List<Long> keys = new ArrayList<>(blocks.keySet());
            Collections.sort(keys);
            byte[] buf = new byte[8];
            for (long k : keys) {
                for (int i = 0; i < 8; i++) {
                    buf[i] = (byte) (k >>> (56 - 8 * i));
                }
                sha.update(buf);
                sha.update(blocks.get(k).data().getBytes(StandardCharsets.UTF_8));
            }
            for (Sign s : signs) {
                sha.update((s.x() + "," + s.y() + "," + s.z() + String.join("|", s.lines())).getBytes(StandardCharsets.UTF_8));
            }
            StringBuilder hex = new StringBuilder();
            for (byte b : sha.digest()) {
                hex.append(String.format("%02x", b));
            }
            return hex.substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
