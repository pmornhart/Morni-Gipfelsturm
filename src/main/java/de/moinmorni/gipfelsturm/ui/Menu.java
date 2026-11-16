package de.moinmorni.gipfelsturm.ui;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/**
 * Truhen-Menü mit einer Aktion je Feld. Nur Truhen-Menüs (keine Formulare), damit es auch über Geyser auf Bedrock geht.
 */
public final class Menu implements InventoryHolder {

    /** Was ein Klick auslöst ({@code right} = Rechtsklick). */
    public interface Action {
        void run(Player player, boolean right);
    }

    private final Inventory inventory;
    private final Map<Integer, Action> actions = new HashMap<>();

    public Menu(int rows, Component title) {
        this.inventory = Bukkit.createInventory(this, rows * 9, title);
    }

    public Menu set(int slot, ItemStack item, Action action) {
        inventory.setItem(slot, item);
        if (action != null) {
            actions.put(slot, action);
        }
        return this;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void open(Player p) {
        p.openInventory(inventory);
    }

    /** Wertet Klicks in allen Gipfelsturm-Menüs aus; nichts lässt sich herausnehmen. */
    public static final class Clicks implements Listener {
        @EventHandler
        public void onClick(InventoryClickEvent event) {
            if (!(event.getView().getTopInventory().getHolder(false) instanceof Menu menu)) {
                return;
            }
            event.setCancelled(true);
            if (event.getClickedInventory() != event.getView().getTopInventory()) {
                return;
            }
            Action a = menu.actions.get(event.getSlot());
            if (a != null && event.getWhoClicked() instanceof Player p) {
                ClickType t = event.getClick();
                a.run(p, t == ClickType.RIGHT || t == ClickType.SHIFT_RIGHT);
            }
        }

        @EventHandler
        public void onDrag(InventoryDragEvent event) {
            if (event.getView().getTopInventory().getHolder(false) instanceof Menu) {
                event.setCancelled(true);
            }
        }
    }
}
