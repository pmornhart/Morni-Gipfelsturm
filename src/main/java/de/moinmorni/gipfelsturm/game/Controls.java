package de.moinmorni.gipfelsturm.game;

import de.moinmorni.gipfelsturm.GipfelsturmPlugin;
import de.moinmorni.gipfelsturm.course.Course;
import de.moinmorni.gipfelsturm.courses.Basecamp;
import de.moinmorni.gipfelsturm.ui.Items;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

/** Klicks mit den Gegenständen der Schnellleiste und auf die Schilder der Stationen. */
public final class Controls implements Listener {

    /** Doppelklicks (Hand + Neben-Hand, Block + Luft) nicht doppelt auswerten. */
    private static final long COOLDOWN_MS = 250;

    private final GipfelsturmPlugin pl;
    private final Map<UUID, Long> last = new HashMap<>();

    public Controls(GipfelsturmPlugin pl) {
        this.pl = pl;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        Player p = event.getPlayer();
        if (!p.getWorld().equals(pl.world()) || event.getAction() == Action.PHYSICAL) {
            return;
        }
        if (event.getHand() == EquipmentSlot.OFF_HAND) {
            event.setCancelled(true);
            return;
        }
        Block block = event.getClickedBlock();
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null) {
            Course c = stationAt(block);
            if (c != null) {
                event.setCancelled(true);
                if (debounce(p)) {
                    pl.runs().select(p, c);
                }
                return;
            }
        }
        String id = Items.id(event.getItem());
        if (id == null) {
            return;
        }
        event.setCancelled(true);
        if (!debounce(p)) {
            return;
        }
        switch (id.contains(":") ? id.substring(0, id.indexOf(':')) : id) {
            case Items.MENU -> pl.menus().courses(p);
            case Items.TOP -> pl.menus().book(p);
            case Items.WEEKLY -> {
                Course w = pl.weeklyCourse();
                if (w != null) {
                    pl.runs().select(p, w);
                }
            }
            case Items.HIDE -> pl.toggleHide(p);
            case Items.LOBBY -> pl.net().toLobby(p);
            case Items.CHECKPOINT -> pl.runs().toCheckpoint(p);
            case Items.RESTART -> pl.runs().restart(p);
            case Items.GIVEUP -> pl.runs().giveUp(p);
            case Items.SETPOINT -> pl.runs().setPracticePoint(p);
            case Items.AGAIN -> pl.model().course(id.substring(id.indexOf(':') + 1)).ifPresent(c -> pl.runs().select(p, c));
            case Items.BASE -> pl.toBasecamp(p);
            case Items.RACE -> pl.runs().joinRace(p);
            default -> {
            }
        }
    }

    private boolean debounce(Player p) {
        long now = System.currentTimeMillis();
        Long before = last.put(p.getUniqueId(), now);
        return before == null || now - before >= COOLDOWN_MS;
    }

    private Course stationAt(Block b) {
        for (Basecamp.Station s : pl.model().basecamp().stations()) {
            if (s.signX() == b.getX() && s.signY() == b.getY() && s.signZ() == b.getZ()) {
                return pl.model().course(s.course()).orElse(null);
            }
        }
        return null;
    }

    public void forget(UUID id) {
        last.remove(id);
    }
}
