package de.moinmorni.fakenet;

import de.moinmorni.api.MoinMorni;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Fake des MoinMorni-Netzwerks für Tests: Sprache und Partys per Befehl (/fakenet), jeder Aufruf von stats, state,
 * toLobby, askToLobby, playing und holdSlot landet als Zeile in plugins/MoinMorni/calls.log. Wie das echte Netzwerk
 * fragt es bei askToLobby nur nach, solange der Modus „spielt gerade“ gemeldet hat, und vergisst den Stand, wenn jemand
 * den Server verlässt.
 */
public final class FakeNetwork extends JavaPlugin implements MoinMorni, Listener {

    private final Set<UUID> playing = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> langs = new ConcurrentHashMap<>();
    private final Map<UUID, Party> parties = new ConcurrentHashMap<>();
    private Path log;

    @Override
    public void onEnable() {
        log = getDataFolder().toPath().resolve("calls.log");
        try {
            Files.createDirectories(log.getParent());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        getServer().getServicesManager().register(MoinMorni.class, this, this, ServicePriority.Normal);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        playing.remove(e.getPlayer().getUniqueId());
    }

    private void write(String line) {
        try {
            Files.writeString(log, line + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            getLogger().warning(e.toString());
        }
    }

    @Override
    public String serverName() {
        return "gipfelsturm";
    }

    @Override
    public boolean online() {
        return true;
    }

    @Override
    public String language(UUID player) {
        return langs.getOrDefault(player, "de");
    }

    @Override
    public Rank rank(UUID player) {
        return new Rank("player", "", "#aaaaaa", 99);
    }

    @Override
    public boolean hasRight(UUID player, String right) {
        return false;
    }

    @Override
    public CompletableFuture<Optional<Party>> party(UUID player) {
        // wie der echte Hub: Antwort kommt auf einem anderen Thread
        return CompletableFuture.supplyAsync(() -> Optional.ofNullable(parties.get(player)));
    }

    @Override
    public void stats(String mode, UUID player, Map<String, Long> add, Map<String, Long> best, boolean lowerIsBetter) {
        write("stats " + mode + " " + name(player) + " add=" + new java.util.TreeMap<>(add) + " best="
                + new java.util.TreeMap<>(best) + " lower=" + lowerIsBetter);
    }

    @Override
    public void state(String mode, String state, int players) {
        write("state " + mode + " " + state + " " + players);
    }

    @Override
    public void toLobby(Player player) {
        write("toLobby " + player.getName());
        player.kick(Component.text("Fake-Netzwerk: in die Lobby"));
    }

    @Override
    public void askToLobby(Player player) {
        boolean asks = playing.contains(player.getUniqueId());
        write("askToLobby " + player.getName() + " asked=" + asks);
        if (asks) {
            player.sendMessage(Component.text("Fake-Netzwerk: Willst du wirklich zur Lobby? (Nachfrage)"));
        } else {
            player.kick(Component.text("Fake-Netzwerk: in die Lobby"));
        }
    }

    @Override
    public void playing(Player player, boolean on) {
        if (on) {
            playing.add(player.getUniqueId());
        } else {
            playing.remove(player.getUniqueId());
        }
        write("playing " + player.getName() + " " + on);
    }

    @Override
    public void holdSlot(UUID player, int seconds) {
        write("holdSlot " + name(player) + " " + seconds);
    }

    /** UUID zu einem Namen – auch wenn der Spieler noch nicht da ist (Offline-Modus: aus dem Namen). */
    private static UUID uuid(String name) {
        Player p = Bukkit.getPlayerExact(name);
        if (p != null) {
            return p.getUniqueId();
        }
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private static String name(UUID id) {
        OfflinePlayer p = Bukkit.getOfflinePlayer(id);
        return p.getName() == null ? id.toString() : p.getName();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length >= 3 && args[0].equals("lang")) {
            UUID id = uuid(args[1]);
            langs.put(id, args[2]);
            sender.sendMessage("Sprache von " + args[1] + ": " + args[2]);
            return true;
        }
        if (args.length >= 3 && args[0].equals("party")) {
            List<Party.Member> list = new ArrayList<>();
            for (int i = 1; i < args.length; i++) {
                list.add(new Party.Member(uuid(args[i]), args[i], "gipfelsturm"));
            }
            Party party = new Party(list.get(0).uuid(), List.copyOf(list));
            list.forEach(m -> parties.put(m.uuid(), party));
            sender.sendMessage("Party: " + list.stream().map(Party.Member::name).toList());
            return true;
        }
        if (args.length >= 1 && args[0].equals("clear")) {
            langs.clear();
            parties.clear();
            sender.sendMessage("Fake-Netzwerk zurückgesetzt");
            return true;
        }
        sender.sendMessage("/fakenet lang <spieler> de|en · party <anführer> <mitglied…> · clear");
        return true;
    }
}
