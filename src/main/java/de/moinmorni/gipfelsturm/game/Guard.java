package de.moinmorni.gipfelsturm.game;

import de.moinmorni.gipfelsturm.GipfelsturmPlugin;
import de.moinmorni.gipfelsturm.ui.Items;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

/**
 * Schutz in der Gipfelsturm-Welt: nichts abbauen oder bauen (außer Admins im Kreativmodus), kein Schaden (auch kein
 * Fallschaden, kein PvP), kein Hunger, Gegenstände der Leiste bleiben, wo sie sind. Fällt jemand ins Nichts, holt ihn
 * {@link Runs} bzw. das Plugin zurück.
 */
public final class Guard implements Listener {

    private final GipfelsturmPlugin pl;

    public Guard(GipfelsturmPlugin pl) {
        this.pl = pl;
    }

    private boolean here(Entity e) {
        return e.getWorld().equals(pl.world());
    }

    private static boolean builder(Player p) {
        return p.getGameMode() == GameMode.CREATIVE && p.hasPermission("gipfelsturm.admin");
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        if (here(e.getPlayer()) && !builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent e) {
        if (here(e.getPlayer()) && !builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p) || !here(p)) {
            return;
        }
        e.setCancelled(true);
        if (e.getCause() == EntityDamageEvent.DamageCause.VOID) {
            pl.rescue(p);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHunger(FoodLevelChangeEvent e) {
        if (here(e.getEntity()) && e.getFoodLevel() < e.getEntity().getFoodLevel()) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (here(e.getPlayer()) && !builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent e) {
        if (e.getEntity() instanceof Player p && here(p) && !builder(p)) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (here(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onInventory(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p && here(p) && !builder(p)
                && e.getClickedInventory() == p.getInventory()) {
            e.setCancelled(true);
        }
        if (e.getWhoClicked() instanceof Player p && here(p) && Items.id(e.getCurrentItem()) != null && !builder(p)) {
            e.setCancelled(true);
        }
    }

    /** Türen der Hütte dürfen auf, sonst nichts (Falltüren, Knöpfe, Töpfe …). */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onUse(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!here(p) || builder(p)) {
            return;
        }
        Block b = e.getClickedBlock();
        if (b == null) {
            return;
        }
        if (e.getAction() == Action.PHYSICAL) {
            if (b.getType() == Material.FARMLAND || b.getType() == Material.TURTLE_EGG) {
                e.setCancelled(true);
            }
            return;
        }
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK && b.getType().isInteractable()
                && !b.getType().name().endsWith("_DOOR") && !b.getType().name().endsWith("_SIGN")) {
            e.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntity(PlayerInteractEntityEvent e) {
        if (here(e.getPlayer()) && !builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorStand(PlayerArmorStandManipulateEvent e) {
        if (here(e.getPlayer()) && !builder(e.getPlayer())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHanging(HangingBreakByEntityEvent e) {
        if (here(e.getEntity())) {
            e.setCancelled(true);
        }
    }
}
