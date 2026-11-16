package de.moinmorni.gipfelsturm.net;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Was Gipfelsturm vom MoinMorni-Netzwerk braucht: Sprache, Party, Statistik, Zustand, zurück in die Lobby. Ohne
 * Netzwerk ({@link Standalone}) läuft alles auf Deutsch und nur lokal.
 */
public interface Net {

    /** Name des Modus für Statistik und Zustand im Netzwerk. */
    String MODE = "gipfelsturm";

    /** Party: Anführer und Mitglieder (UUIDs, alle Server). */
    record PartyInfo(UUID leader, List<UUID> members) {
    }

    /** „de“ oder „en“. */
    String lang(UUID player);

    /** Läuft das Netzwerk-Plugin (auch wenn der Hub gerade weg ist)? */
    boolean network();

    /** Ist der Hub gerade verbunden? */
    boolean online();

    CompletableFuture<Optional<PartyInfo>> party(UUID player);

    /** Werte melden: {@code add} hochzählen, {@code best} als Bestwert (kleiner ist besser – Zeiten). */
    void stats(UUID player, Map<String, Long> add, Map<String, Long> best);

    void state(String state, int players);

    void toLobby(Player player);

    /**
     * Der Spieler will selbst in die Lobby (Gegenstand, {@code /gipfel lobby}): Läuft gerade sein Lauf
     * ({@link #playing}), fragt das Netzwerk erst nach. Ohne Netzwerk wie {@link #toLobby}.
     */
    void askToLobby(Player player);

    /** Läuft gerade ein Lauf (Uhr läuft oder Party-Rennen)? Solange fragt „/lobby“ erst nach. Ohne Netzwerk: nichts. */
    void playing(Player player, boolean playing);

    /**
     * Lauf nach einem Verbindungsabbruch so viele Sekunden freihalten – so lange bietet die Lobby „Zurück ins Spiel“
     * an; {@code 0} nimmt das Angebot zurück. Ohne Netzwerk: nichts (man verbindet sich direkt wieder).
     */
    void holdSlot(UUID player, int seconds);

    /** Netzwerk finden; ohne Netzwerk-Plugin (oder wenn dessen Schnittstelle fehlt) allein. */
    static Net detect(JavaPlugin plugin) {
        Logger log = plugin.getLogger();
        if (Bukkit.getPluginManager().getPlugin("MoinMorni") == null) {
            log.info("Kein MoinMorni-Netzwerk gefunden – Gipfelsturm läuft allein (Deutsch, Statistik nur lokal).");
            return new Standalone();
        }
        try {
            // Eigene Klasse: Die Netzwerk-Schnittstelle wird erst hier geladen, nie ohne das Netzwerk-Plugin.
            Net net = MoinMorniNet.create(log);
            if (net != null) {
                log.info("Mit dem MoinMorni-Netzwerk verbunden.");
                return net;
            }
        } catch (LinkageError | RuntimeException e) {
            log.warning("MoinMorni-Schnittstelle nicht nutzbar (" + e + ") – Gipfelsturm läuft allein.");
        }
        return new Standalone();
    }
}
