package net.morni.gipfelsturm.net;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.morni.gipfelsturm.ui.Texts;
import org.bukkit.entity.Player;

/** Ohne Netzwerk: Deutsch, keine Party, Statistik nur lokal, „Lobby“ = freundlich verabschieden. */
public final class Standalone implements Net {

    @Override
    public String lang(UUID player) {
        return "de";
    }

    @Override
    public boolean network() {
        return false;
    }

    @Override
    public boolean online() {
        return false;
    }

    @Override
    public CompletableFuture<Optional<PartyInfo>> party(UUID player) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    public void stats(UUID player, Map<String, Long> add, Map<String, Long> best) {
        // nur lokal (siehe Store)
    }

    @Override
    public void state(String state, int players) {
        // niemand fragt
    }

    @Override
    public void toLobby(Player player) {
        player.kick(Texts.get("de", "lobby.kick"));
    }
}
