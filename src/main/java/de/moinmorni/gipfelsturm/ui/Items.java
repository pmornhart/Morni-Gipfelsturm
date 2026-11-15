package de.moinmorni.gipfelsturm.ui;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/** Gegenstände in der Schnellleiste (Basislager und unterwegs) – erkannt an einer unsichtbaren Markierung. */
public final class Items {

    public static final String MENU = "menu";
    public static final String TOP = "top";
    public static final String WEEKLY = "weekly";
    public static final String HIDE = "hide";
    public static final String LOBBY = "lobby";
    public static final String CHECKPOINT = "checkpoint";
    public static final String RESTART = "restart";
    public static final String GIVEUP = "giveup";
    public static final String SETPOINT = "setpoint";
    public static final String AGAIN = "again";
    public static final String BASE = "base";
    public static final String RACE = "race";

    private static NamespacedKey key;

    private Items() {
    }

    public static void init(JavaPlugin plugin) {
        key = new NamespacedKey(plugin, "item");
    }

    /** Markierung eines Gegenstands (null = keiner von uns). */
    public static String id(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
    }

    public static ItemStack make(String id, Material m, String lang, String nameKey, String loreKey,
                                 TagResolver... args) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(Texts.get(lang, nameKey, args)));
        if (loreKey != null) {
            meta.lore(List.of(plain(Texts.get(lang, loreKey, args))));
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, id);
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    /** Für Menüs: Name und mehrere Zeilen, ohne Markierung. */
    public static ItemStack icon(Material m, Component name, List<Component> lore, boolean glint) {
        ItemStack item = new ItemStack(m);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plain(name));
        List<Component> lines = new ArrayList<>();
        for (Component c : lore) {
            lines.add(plain(c));
        }
        meta.lore(lines);
        meta.addItemFlags(ItemFlag.values());
        if (glint) {
            meta.setEnchantmentGlintOverride(true);
        }
        item.setItemMeta(meta);
        return item;
    }

    /** Ohne die kursive Standard-Schrift von Gegenstandsnamen. */
    public static Component plain(Component c) {
        return c.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
}
