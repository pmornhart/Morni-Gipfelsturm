package de.moinmorni.gipfelsturm.net;

import de.moinmorni.api.MoinMorni;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import org.bukkit.entity.Player;

/**
 * Brücke zur Netzwerk-Schnittstelle {@link MoinMorni} – die einzige Klasse, die sie kennt.
 *
 * <p>Nachfrage vor der Lobby, „spielt gerade“ und „Platz freihalten“ gibt es erst ab Version 2 der Schnittstelle. Ein
 * älteres Netzwerk-Plugin kennt sie nicht ({@link LinkageError}): dann geht „zur Lobby“ sofort und der Rest entfällt –
 * alles andere läuft wie bisher.
 */
final class MoinMorniNet implements Net {

    private final MoinMorni api;
    private final Logger log;
    private volatile boolean olderNetwork;

    private MoinMorniNet(MoinMorni api, Logger log) {
        this.api = api;
        this.log = log;
    }

    static Net create(Logger log) {
        return MoinMorni.get().map(a -> (Net) new MoinMorniNet(a, log)).orElse(null);
    }

    @Override
    public String lang(UUID player) {
        String l = api.language(player);
        return "en".equals(l) ? "en" : "de";
    }

    @Override
    public boolean network() {
        return true;
    }

    @Override
    public boolean online() {
        return api.online();
    }

    @Override
    public CompletableFuture<Optional<PartyInfo>> party(UUID player) {
        return api.party(player).thenApply(p -> p.map(party -> new PartyInfo(party.leader(),
                party.members().stream().map(MoinMorni.Party.Member::uuid).toList())));
    }

    @Override
    public void stats(UUID player, Map<String, Long> add, Map<String, Long> best) {
        api.stats(MODE, player, add, best, true);
    }

    @Override
    public void state(String state, int players) {
        api.state(MODE, state, players);
    }

    @Override
    public void toLobby(Player player) {
        api.toLobby(player);
    }

    @Override
    public void askToLobby(Player player) {
        if (!olderNetwork) {
            try {
                api.askToLobby(player);
                return;
            } catch (LinkageError e) {
                older(e);
            }
        }
        api.toLobby(player);
    }

    @Override
    public void playing(Player player, boolean playing) {
        if (olderNetwork) {
            return;
        }
        try {
            api.playing(player, playing);
        } catch (LinkageError e) {
            older(e);
        }
    }

    @Override
    public void holdSlot(UUID player, int seconds) {
        if (olderNetwork) {
            return;
        }
        try {
            api.holdSlot(player, seconds);
        } catch (LinkageError e) {
            older(e);
        }
    }

    /** Älteres Netzwerk-Plugin (Schnittstelle 1): einmal melden, danach die neuen Methoden nicht mehr versuchen. */
    private void older(LinkageError e) {
        if (!olderNetwork) {
            olderNetwork = true;
            log.warning("Das Netzwerk-Plugin ist älter (" + e + "): keine Nachfrage vor der Lobby und kein „Zurück ins "
                    + "Spiel“ – alles andere läuft wie bisher.");
        }
    }
}
