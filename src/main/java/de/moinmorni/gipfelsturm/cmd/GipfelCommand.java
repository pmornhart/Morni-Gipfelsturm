package de.moinmorni.gipfelsturm.cmd;

import de.moinmorni.gipfelsturm.GipfelsturmPlugin;
import de.moinmorni.gipfelsturm.course.Course;
import de.moinmorni.gipfelsturm.game.Runs;
import de.moinmorni.gipfelsturm.stats.Leaderboard;
import de.moinmorni.gipfelsturm.stats.PlayerRecord;
import de.moinmorni.gipfelsturm.time.TimeFormat;
import de.moinmorni.gipfelsturm.ui.Texts;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

/** {@code /gipfel} – alles, was die Gegenstände können, auch als Befehl (z. B. für Bedrock oder Tastatur-Fans). */
public final class GipfelCommand implements TabExecutor {

    private static final List<String> SUBS = List.of("start", "top", "stats", "woche", "uebung", "geist", "verstecken",
            "checkpoint", "neustart", "aufgeben", "basislager", "mitmachen", "lobby", "hilfe");
    private static final List<String> ADMIN = List.of("reset", "neubau", "pruefen", "pads");

    private final GipfelsturmPlugin pl;

    public GipfelCommand(GipfelsturmPlugin pl) {
        this.pl = pl;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player p = sender instanceof Player pp ? pp : null;
        String l = p == null ? "de" : pl.lang(p);
        String sub = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
        if (!pl.worlds().ready() && !sub.equals("pruefen")) {
            sender.sendMessage(Texts.chat(l, "cmd.notready"));
            return true;
        }
        switch (sub) {
            case "" -> {
                if (p == null) {
                    sender.sendMessage(Texts.chat(l, "cmd.help"));
                } else {
                    pl.menus().courses(p);
                }
            }
            case "hilfe", "help", "?" -> sender.sendMessage(Texts.chat(l, "cmd.help"));
            case "start" -> {
                if (p == null) {
                    sender.sendMessage(Texts.chat(l, "cmd.player"));
                    return true;
                }
                course(args, 1, sender, l).ifPresent(c -> pl.runs().select(p, c));
            }
            case "top" -> course(args, 1, sender, l).ifPresent(c -> top(sender, l, c));
            case "stats" -> stats(sender, l, p, args.length > 1 ? args[1] : null);
            case "woche", "week" -> week(sender, l);
            case "uebung", "übung", "practice" -> {
                if (p != null) {
                    pl.runs().togglePractice(p);
                }
            }
            case "geist", "ghost" -> {
                if (p != null) {
                    pl.toggleGhost(p);
                }
            }
            case "verstecken", "hide" -> {
                if (p != null) {
                    pl.toggleHide(p);
                }
            }
            case "checkpoint", "cp" -> {
                if (p != null) {
                    pl.runs().toCheckpoint(p);
                }
            }
            case "neustart", "restart" -> {
                if (p != null) {
                    pl.runs().restart(p);
                }
            }
            case "aufgeben", "giveup" -> {
                if (p != null) {
                    pl.runs().giveUp(p);
                }
            }
            case "basislager", "base" -> {
                if (p != null) {
                    pl.runs().abandon(p);
                    pl.toBasecamp(p);
                }
            }
            case "mitmachen", "join" -> {
                if (p != null) {
                    pl.runs().joinRace(p);
                }
            }
            case "lobby" -> {
                if (p != null) {
                    pl.net().toLobby(p);
                }
            }
            case "reset" -> reset(sender, l, args);
            case "neubau" -> {
                if (!sender.hasPermission("gipfelsturm.admin")) {
                    sender.sendMessage(Texts.chat(l, "cmd.perm"));
                    return true;
                }
                try {
                    pl.worlds().scheduleRebuild();
                    sender.sendMessage(Texts.chat(l, "admin.rebuild"));
                } catch (IOException e) {
                    sender.sendMessage(Component.text(e.getMessage()));
                }
            }
            case "pads" -> {
                if (!sender.hasPermission("gipfelsturm.admin")) {
                    sender.sendMessage(Texts.chat(l, "cmd.perm"));
                    return true;
                }
                course(args, 1, sender, l).ifPresent(c -> sender.sendMessage(Component.text(padsJson(c))));
            }
            case "pruefen" -> {
                if (!sender.hasPermission("gipfelsturm.admin")) {
                    sender.sendMessage(Texts.chat(l, "cmd.perm"));
                    return true;
                }
                List<String> problems = pl.validate();
                sender.sendMessage(Component.text(problems.isEmpty() ? "Alle Strecken in Ordnung."
                        : String.join("\n", problems)));
            }
            default -> {
                Optional<Course> c = pl.model().course(sub);
                if (c.isPresent() && p != null) {
                    pl.runs().select(p, c.get());
                } else {
                    sender.sendMessage(Texts.chat(l, "cmd.help"));
                }
            }
        }
        return true;
    }

    private Optional<Course> course(String[] args, int i, CommandSender sender, String l) {
        Optional<Course> c = args.length > i ? pl.model().course(String.join(" ", List.of(args).subList(i, args.length)))
                : Optional.empty();
        if (c.isEmpty()) {
            sender.sendMessage(Texts.chat(l, "cmd.course", Texts.arg("list",
                    String.join(", ", pl.model().courses().stream().map(x -> x.id).toList()))));
        }
        return c;
    }

    /** Lage aller Pads als JSON (für Tests und zum Nachsehen). */
    private static String padsJson(Course c) {
        StringBuilder b = new StringBuilder("{\"id\":\"").append(c.id).append("\",\"name\":\"").append(c.nameDe)
                .append("\",\"fall\":[");
        for (int k = 0; k <= c.checkpointCount(); k++) {
            b.append(k > 0 ? "," : "").append(c.fallY(k));
        }
        b.append("],\"plausible\":").append(c.plausibleMs()).append(",\"difficulty\":\"").append(c.difficulty)
                .append("\",\"medals\":{\"gipfel\":").append(c.medals.gipfel()).append(",\"gold\":")
                .append(c.medals.gold()).append(",\"silber\":").append(c.medals.silber()).append(",\"bronze\":")
                .append(c.medals.bronze()).append("},\"pads\":[");
        for (var p : c.pads) {
            b.append(p.index > 0 ? "," : "").append(String.format(Locale.ROOT,
                    "{\"i\":%d,\"role\":\"%s\",\"kind\":\"%s\",\"x0\":%d,\"x1\":%d,\"z0\":%d,\"z1\":%d,\"y\":%d,\"s\":%.4f,\"b\":\"%s\"}",
                    p.index, p.role, p.arrival == null ? "START" : p.arrival.kind(), p.x0, p.x1, p.z0, p.z1, p.y,
                    p.surface(), p.blk.id()));
            if (p.arrival != null) {
                b.setLength(b.length() - 1);
                b.append(",\"dir\":\"").append(p.arrival.dir()).append("\",\"side\":").append(p.arrival.passSide())
                        .append(",\"ladder\":").append(cells(p.arrival.ladder())).append(",\"wall\":")
                        .append(cells(p.arrival.wall())).append('}');
            }
        }
        return b.append("]}").toString();
    }

    private static String cells(List<long[]> cells) {
        StringBuilder b = new StringBuilder("[");
        for (long[] c : cells) {
            b.append(b.length() > 1 ? "," : "").append('[').append(c[0]).append(',').append(c[1]).append(',')
                    .append(c[2]).append(']');
        }
        return b.append(']').toString();
    }

    private void top(CommandSender sender, String l, Course c) {
        Leaderboard board = pl.store().board(c.id);
        sender.sendMessage(Texts.chat(l, "top.head", Texts.comp("course", Runs.courseName(c, l))));
        List<Leaderboard.Entry> top = board.top(10);
        if (top.isEmpty()) {
            sender.sendMessage(Texts.get(l, "top.empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            Leaderboard.Entry e = top.get(i);
            sender.sendMessage(Texts.get(l, "top.line", Texts.arg("rank", i + 1), Texts.arg("player", e.name()),
                    Texts.arg("time", TimeFormat.clock(e.ms(), l)), Texts.comp("medal", Runs.medal(c.medal(e.ms()), l))));
        }
        if (sender instanceof Player p) {
            Leaderboard.Entry mine = board.of(p.getUniqueId());
            if (mine != null && board.rank(p.getUniqueId()) > 10) {
                sender.sendMessage(Texts.get(l, "top.you", Texts.arg("rank", board.rank(p.getUniqueId())),
                        Texts.arg("time", TimeFormat.clock(mine.ms(), l))));
            }
        }
    }

    private void stats(CommandSender sender, String l, Player self, String name) {
        PlayerRecord r;
        if (name == null) {
            if (self == null) {
                sender.sendMessage(Texts.chat(l, "cmd.player"));
                return;
            }
            r = pl.store().record(self.getUniqueId(), self.getName());
        } else {
            Player online = Bukkit.getPlayerExact(name);
            r = online != null ? pl.store().record(online.getUniqueId(), online.getName()) : pl.store().findByName(name);
        }
        if (r == null) {
            sender.sendMessage(Texts.chat(l, "cmd.unknown"));
            return;
        }
        sender.sendMessage(Texts.chat(l, "stats.head", Texts.arg("player", r.name)));
        sender.sendMessage(Texts.get(l, "stats.total", Texts.arg("finishes", r.finishes), Texts.arg("attempts", r.attempts),
                Texts.arg("falls", r.falls)));
        sender.sendMessage(Texts.get(l, "stats.medals", Texts.comp("medals", pl.medalCount(r, l))));
        if (r.weeklySummit(pl.store().weekId())) {
            sender.sendMessage(Texts.get(l, "stats.badge", Texts.comp("badge", Texts.get(l, "badge"))));
        }
        for (Course c : pl.model().courses()) {
            long best = r.best(c.id);
            if (best > 0) {
                sender.sendMessage(Texts.get(l, "stats.course", Texts.comp("course", Runs.courseName(c, l)),
                        Texts.arg("time", TimeFormat.clock(best, l)), Texts.comp("medal", Runs.medal(c.medal(best), l)),
                        Texts.arg("rank", pl.store().board(c.id).rank(r.uuid))));
            } else {
                sender.sendMessage(Texts.get(l, "stats.course.none", Texts.comp("course", Runs.courseName(c, l))));
            }
        }
    }

    private void week(CommandSender sender, String l) {
        Course w = pl.weeklyCourse();
        if (w == null) {
            return;
        }
        sender.sendMessage(Texts.chat(l, "week.head", Texts.comp("course", Runs.courseName(w, l)),
                Texts.arg("left", pl.weekLeft(l))));
        List<Leaderboard.Entry> top = pl.store().weekly().top(10);
        if (top.isEmpty()) {
            sender.sendMessage(Texts.get(l, "top.empty"));
        }
        for (int i = 0; i < top.size(); i++) {
            Leaderboard.Entry e = top.get(i);
            sender.sendMessage(Texts.get(l, "top.line", Texts.arg("rank", i + 1), Texts.arg("player", e.name()),
                    Texts.arg("time", TimeFormat.clock(e.ms(), l)), Texts.comp("medal", Runs.medal(w.medal(e.ms()), l))));
        }
    }

    private void reset(CommandSender sender, String l, String[] args) {
        if (!sender.hasPermission("gipfelsturm.admin")) {
            sender.sendMessage(Texts.chat(l, "cmd.perm"));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("/gipfel reset <spieler> [strecke]"));
            return;
        }
        PlayerRecord r = pl.store().findByName(args[1]);
        if (r == null) {
            sender.sendMessage(Texts.chat(l, "cmd.unknown"));
            return;
        }
        String course = null;
        if (args.length > 2) {
            Optional<Course> c = course(args, 2, sender, l);
            if (c.isEmpty()) {
                return;
            }
            course = c.get().id;
        }
        pl.store().reset(r, course);
        pl.store().deleteGhosts(r.uuid, course);
        pl.runs().forgetGhosts(r.uuid);
        pl.boardsAll();
        sender.sendMessage(Texts.chat(l, "admin.reset", Texts.arg("player", r.name), Texts.arg("what",
                course == null ? "alle" : course)));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1) {
            Stream.concat(SUBS.stream(), sender.hasPermission("gipfelsturm.admin") ? ADMIN.stream() : Stream.empty())
                    .filter(s -> s.startsWith(args[0].toLowerCase(Locale.ROOT))).forEach(out::add);
        } else if (args.length == 2 && List.of("start", "top").contains(args[0].toLowerCase(Locale.ROOT))) {
            pl.model().courses().stream().map(c -> c.id).filter(s -> s.startsWith(args[1].toLowerCase(Locale.ROOT)))
                    .forEach(out::add);
        } else if (args.length == 2 && List.of("stats", "reset").contains(args[0].toLowerCase(Locale.ROOT))) {
            Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(s -> s.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))).forEach(out::add);
        } else if (args.length == 3 && "reset".equalsIgnoreCase(args[0])) {
            pl.model().courses().stream().map(c -> c.id).forEach(out::add);
        }
        return out;
    }
}
