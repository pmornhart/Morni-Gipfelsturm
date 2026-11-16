package net.morni.gipfelsturm.game;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.morni.gipfelsturm.GipfelsturmPlugin;
import net.morni.gipfelsturm.course.Course;
import net.morni.gipfelsturm.course.Pad;
import net.morni.gipfelsturm.courses.Basecamp;
import net.morni.gipfelsturm.stats.Leaderboard;
import net.morni.gipfelsturm.time.TimeFormat;
import net.morni.gipfelsturm.ui.Texts;
import net.morni.gipfelsturm.world.Holograms;

/** Tafeln im Basislager (Bestenliste je Strecke, Strecke der Woche, Hütte) und Marker auf den Strecken. */
public final class Boards {

    private static final int TOP = 10;

    private final GipfelsturmPlugin pl;

    public Boards(GipfelsturmPlugin pl) {
        this.pl = pl;
    }

    /** Alles anmelden (erscheint, sobald der Chunk geladen ist). */
    public void register(Holograms h) {
        Basecamp camp = pl.model().basecamp();
        for (Basecamp.Station s : camp.stations()) {
            pl.model().course(s.course()).ifPresent(c ->
                    h.text("board:" + c.id, s.boardX(), s.boardY(), s.boardZ(), l -> station(c, l), true, 0.75f));
        }
        double[] w = camp.weeklyBoard();
        h.text("weekly", w[0], w[1], w[2], this::weekly, true, 0.75f);
        double[] hut = camp.hutSign();
        h.text("hut", hut[0], hut[1], hut[2], l -> Texts.get(l, "board.hut"), true, 1.1f);
        for (Course c : pl.model().courses()) {
            Pad start = c.start();
            h.text("start:" + c.id, start.centerX(), start.surface() + 2.4, start.centerZ(), l -> startLabel(c, l), true, 0.7f);
            int n = 1;
            for (Pad cp : c.checkpoints()) {
                h.block("cp:" + c.id + ":" + n, cp.centerX(), cp.surface() + 1.9, cp.centerZ(), "minecraft:gold_block", 0.35f);
                int number = n;
                h.text("cpt:" + c.id + ":" + n, cp.centerX(), cp.surface() + 2.35, cp.centerZ(),
                        l -> Component.text("Checkpoint " + number + "/" + c.checkpointCount(), NamedTextColor.GOLD)
                                .decorate(TextDecoration.BOLD), false, 0.6f);
                n++;
            }
            Pad fin = c.finish();
            h.block("fin:" + c.id, fin.centerX(), fin.surface() + 1.9, fin.centerZ(), "minecraft:diamond_block", 0.45f);
            h.text("fint:" + c.id, fin.centerX(), fin.surface() + 2.5, fin.centerZ(), l -> Texts.get(l, "board.finish"),
                    true, 0.9f);
        }
    }

    private Component station(Course c, String l) {
        Leaderboard board = pl.store().board(c.id);
        Component t = Runs.courseName(c, l).decorate(TextDecoration.BOLD);
        if (c.id.equals(pl.store().weekCourse())) {
            t = Component.text("★ ", TextColor.fromHexString("#7fdcff")).append(t);
        }
        t = t.appendNewline().append(Runs.difficulty(c, l)).append(Component.text(" · ", NamedTextColor.DARK_GRAY))
                .append(Texts.get(l, "menu.cps", Texts.arg("n", c.checkpointCount())));
        t = t.appendNewline().append(Component.text("───────────────", NamedTextColor.DARK_GRAY));
        return t.appendNewline().append(lines(board, c, l)).appendNewline()
                .append(Texts.get(l, "board.click"));
    }

    private Component weekly(String l) {
        Course w = pl.weeklyCourse();
        if (w == null) {
            return Component.empty();
        }
        Component t = Texts.get(l, "board.weekly").appendNewline()
                .append(Runs.courseName(w, l).decorate(TextDecoration.BOLD)).append(Component.text(" · "))
                .append(Runs.difficulty(w, l)).appendNewline()
                .append(Texts.get(l, "board.weekly.left", Texts.arg("left", pl.weekLeft(l)),
                        Texts.comp("badge", Texts.get(l, "badge")))).appendNewline()
                .append(Component.text("───────────────", NamedTextColor.DARK_GRAY)).appendNewline()
                .append(lines(pl.store().weekly(), w, l)).appendNewline()
                .append(Texts.get(l, "board.weekly.summits", Texts.arg("n", pl.store().weeklySummits())));
        return t;
    }

    private Component lines(Leaderboard board, Course c, String l) {
        List<Leaderboard.Entry> top = board.top(TOP);
        if (top.isEmpty()) {
            return Texts.get(l, "board.empty");
        }
        Component out = Component.empty();
        for (int i = 0; i < top.size(); i++) {
            Leaderboard.Entry e = top.get(i);
            TextColor rank = i == 0 ? TextColor.fromHexString("#ffd23f") : i == 1 ? TextColor.fromHexString("#c9d3dc")
                    : i == 2 ? TextColor.fromHexString("#d08a52") : NamedTextColor.GRAY;
            if (i > 0) {
                out = out.appendNewline();
            }
            out = out.append(Component.text((i + 1) + ". ", rank)).append(Component.text(e.name(), NamedTextColor.WHITE))
                    .append(Component.text("  " + TimeFormat.clock(e.ms(), l) + " ", NamedTextColor.AQUA))
                    .append(symbol(c, e.ms()));
        }
        return out;
    }

    private static Component symbol(Course c, long ms) {
        var m = c.medal(ms);
        return m == null ? Component.empty() : Component.text(m.symbol, TextColor.fromHexString(m.colour));
    }

    private Component startLabel(Course c, String l) {
        Component t = Texts.get(l, "board.start", Texts.comp("course", Runs.courseName(c, l).decorate(TextDecoration.BOLD)),
                Texts.comp("diff", Runs.difficulty(c, l)));
        if (c.id.equals(pl.store().weekCourse())) {
            t = Texts.get(l, "menu.weeklytag").appendNewline().append(t);
        }
        return t;
    }
}
