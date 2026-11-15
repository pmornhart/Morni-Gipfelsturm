package de.moinmorni.gipfelsturm.net;

import de.moinmorni.api.MoinMorni;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

/** Brücke zur Netzwerk-Schnittstelle {@link MoinMorni} – die einzige Klasse, die sie kennt. */
final class MoinMorniNet implements Net {

    private final MoinMorni api;

    private MoinMorniNet(MoinMorni api) {
        this.api = api;
    }

    static Net create() {
        return MoinMorni.get().map(MoinMorniNet::new).orElse(null);
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
}
