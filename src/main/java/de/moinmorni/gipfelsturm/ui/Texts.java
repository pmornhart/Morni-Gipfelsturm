package de.moinmorni.gipfelsturm.ui;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Alle Texte von Gipfelsturm auf Deutsch (Standard) und Englisch, im MiniMessage-Format. Platzhalter wie
 * {@code <time>} werden beim Senden gefüllt ({@link #arg(String, Object)} für Text, {@link #comp(String, Component)}
 * für fertige Bausteine).
 */
public final class Texts {

    public static final MiniMessage MM = MiniMessage.miniMessage();
    public static final String PREFIX = "<gradient:#7fdcff:#ffffff><bold>Gipfelsturm</bold></gradient> <dark_gray>»</dark_gray> ";

    private static final Map<String, String[]> T = new HashMap<>();

    private Texts() {
    }

    private static void t(String key, String de, String en) {
        T.put(key, new String[] {de, en});
    }

    static {
        // ---------- Begrüßung ----------
        t("welcome.title", "<gradient:#7fdcff:#ffffff><bold>Gipfelsturm</bold></gradient>",
                "<gradient:#7fdcff:#ffffff><bold>Summit Rush</bold></gradient>");
        t("welcome.subtitle", "<gray>Wähle eine Strecke mit der <aqua>Wanderkarte</aqua></gray>",
                "<gray>Pick a course with the <aqua>trail map</aqua></gray>");
        t("welcome.chat", "Servus im <white>Basislager</white>! Mit der <aqua>Wanderkarte</aqua> wählst du eine "
                + "Strecke, im <gold>Gipfelbuch</gold> stehen die Bestzeiten. Viel Spaß beim Klettern!",
                "Welcome to <white>base camp</white>! Use the <aqua>trail map</aqua> to pick a course – the best times "
                        + "are in the <gold>summit book</gold>. Have fun climbing!");
        t("welcome.weekly", "<gray>Strecke der Woche:</gray> <course> <gray>– schaff sie bis Montag für das Abzeichen</gray> "
                + "<badge><gray>!</gray>", "<gray>Course of the week:</gray> <course> <gray>– finish it by Monday to earn "
                + "the</gray> <badge> <gray>badge!</gray>");
        t("building", "Die Berge werden gerade aufgebaut – gleich geht's los!",
                "The mountains are being built – just a moment!");

        // ---------- Gegenstände ----------
        t("item.menu", "<aqua><bold>Wanderkarte</bold></aqua> <gray>– Strecke wählen</gray>",
                "<aqua><bold>Trail map</bold></aqua> <gray>– pick a course</gray>");
        t("item.menu.lore", "<gray>Rechtsklick: alle Strecken</gray>", "<gray>Right-click: all courses</gray>");
        t("item.top", "<gold><bold>Gipfelbuch</bold></gold> <gray>– Bestenlisten</gray>",
                "<gold><bold>Summit book</bold></gold> <gray>– leaderboards</gray>");
        t("item.top.lore", "<gray>Rechtsklick: Bestzeiten aller Strecken</gray>",
                "<gray>Right-click: best times of every course</gray>");
        t("item.weekly", "<#7fdcff><bold>Strecke der Woche</bold></#7fdcff>", "<#7fdcff><bold>Course of the week</bold></#7fdcff>");
        t("item.weekly.lore", "<gray>Rechtsklick: <white><course></white> starten</gray>",
                "<gray>Right-click: start <white><course></white></gray>");
        t("item.hide.shown", "<green>Mitspieler: sichtbar</green>", "<green>Other players: visible</green>");
        t("item.hide.hidden", "<gray>Mitspieler: ausgeblendet</gray>", "<gray>Other players: hidden</gray>");
        t("item.hide.lore", "<gray>Rechtsklick: umschalten</gray>", "<gray>Right-click: toggle</gray>");
        t("item.lobby", "<red><bold>Zurück ins Dorf</bold></red> <gray>(Lobby)</gray>",
                "<red><bold>Back to the village</bold></red> <gray>(lobby)</gray>");
        t("item.checkpoint", "<gold><bold>Zurück zum Checkpoint</bold></gold>", "<gold><bold>Back to checkpoint</bold></gold>");
        t("item.checkpoint.practice", "<light_purple><bold>Zurück zum Übungs-Punkt</bold></light_purple>",
                "<light_purple><bold>Back to practice point</bold></light_purple>");
        t("item.restart", "<green><bold>Neustart</bold></green>", "<green><bold>Restart</bold></green>");
        t("item.giveup", "<red><bold>Aufgeben</bold></red>", "<red><bold>Give up</bold></red>");
        t("item.setpoint", "<light_purple><bold>Übungs-Punkt setzen</bold></light_purple>",
                "<light_purple><bold>Set practice point</bold></light_purple>");
        t("item.setpoint.lore", "<gray>Rechtsklick auf festem Boden</gray>", "<gray>Right-click while standing</gray>");
        t("item.again", "<green><bold>Nochmal!</bold></green>", "<green><bold>Again!</bold></green>");
        t("item.base", "<aqua><bold>Zum Basislager</bold></aqua>", "<aqua><bold>To base camp</bold></aqua>");
        t("item.race", "<yellow><bold>Rennen beitreten</bold></yellow>", "<yellow><bold>Join the race</bold></yellow>");
        t("item.race.lore", "<gray><leader> startet <course></gray>", "<gray><leader> starts <course></gray>");

        // ---------- Lauf ----------
        t("run.ready", "<green>Lauf los, sobald du bereit bist</green> <gray>– die Zeit startet, wenn du den Smaragd verlässt</gray>",
                "<green>Go when you're ready</green> <gray>– the timer starts when you leave the emerald</gray>");
        t("run.go", "<green><bold>Los geht's!</bold></green>", "<green><bold>Go!</bold></green>");
        t("run.welcome", "<course> <gray>· <diff> · <n> Checkpoints</gray>", "<course> <gray>· <diff> · <n> checkpoints</gray>");
        t("run.sub", "<diff> <gray>· <n> Checkpoints</gray>", "<diff> <gray>· <n> checkpoints</gray>");
        t("run.practice", "<light_purple>Übung</light_purple>", "<light_purple>Practice</light_purple>");
        t("run.checkpoint", "<gold>Checkpoint <n>/<total></gold> <white><time></white> <delta>",
                "<gold>Checkpoint <n>/<total></gold> <white><time></white> <delta>");
        t("run.checkpoint.first", "<gray>(erste Zeit hier)</gray>", "<gray>(first time here)</gray>");
        t("run.skip", "<yellow>Halt – erst Checkpoint <n>!</yellow>", "<yellow>Hold on – checkpoint <n> first!</yellow>");
        t("run.missing", "<yellow>Fast! Dir fehlt noch Checkpoint <n>.</yellow>",
                "<yellow>Almost! You still need checkpoint <n>.</yellow>");
        t("run.fall.cp", "<yellow>Zurück zum Checkpoint <n></yellow>", "<yellow>Back to checkpoint <n></yellow>");
        t("run.fall.start", "<yellow>Zurück zum Start</yellow>", "<yellow>Back to the start</yellow>");
        t("run.fall.practice", "<light_purple>Zurück zu deinem Übungs-Punkt</light_purple>",
                "<light_purple>Back to your practice point</light_purple>");
        t("run.hazard", "<aqua>Platsch! <what> – zurück zum Checkpoint.</aqua>", "<aqua>Splash! <what> – back to the checkpoint.</aqua>");
        t("hazard.Wasser", "Eiskaltes Wasser", "Ice-cold water");
        t("run.offcourse", "<yellow>Abkürzung? Auf der Strecke zählen nur die Sprungblöcke – zurück zum Checkpoint.</yellow>",
                "<yellow>Shortcut? Only the jump blocks count on the course – back to the checkpoint.</yellow>");
        t("run.restart", "<green>Neustart!</green>", "<green>Restart!</green>");
        t("run.giveup", "Aufgegeben – zurück im <white>Basislager</white>. Nächstes Mal klappt's!",
                "Gave up – back at <white>base camp</white>. You'll get it next time!");
        t("run.left", "<gray>Lauf beendet.</gray>", "<gray>Run ended.</gray>");
        // Zurück ins Spiel (Verbindung verloren, Lauf bleibt eine Weile frei)
        t("back.title", "<green><bold>Willkommen zurück!</bold></green>", "<green><bold>Welcome back!</bold></green>");
        t("back.sub", "<gray>Dein Lauf geht weiter</gray>", "<gray>Your run continues</gray>");
        t("back.chat", "Willkommen zurück! Dein Lauf geht am letzten Checkpoint weiter <gray>(Checkpoint <n>) – die Uhr "
                + "ist weitergelaufen.</gray>", "Welcome back! Your run continues at the last checkpoint <gray>(checkpoint "
                + "<n>) – the clock kept running.</gray>");
        t("back.chat.start", "Willkommen zurück! Du stehst wieder am Start <gray>– die Uhr ist weitergelaufen.</gray>",
                "Welcome back! You're back at the start <gray>– the clock kept running.</gray>");
        t("back.chat.fresh", "Willkommen zurück! Du stehst wieder am Start <gray>– die Uhr läuft los, sobald du startest.</gray>",
                "Welcome back! You're back at the start <gray>– the clock starts as soon as you set off.</gray>");
        t("race.away", "<yellow><player> hat die Verbindung verloren</yellow> <gray>– das Rennen wartet bis zu <sec> s.</gray>",
                "<yellow><player> lost the connection</yellow> <gray>– the race waits up to <sec> s.</gray>");
        t("race.back", "<green><player> ist zurück im Rennen!</green>", "<green><player> is back in the race!</green>");

        // ---------- Ziel ----------
        t("finish.title", "<gradient:#ffd23f:#ffffff><bold>Geschafft!</bold></gradient>",
                "<gradient:#ffd23f:#ffffff><bold>You made it!</bold></gradient>");
        t("finish.sub", "<white><time></white> <medal>", "<white><time></white> <medal>");
        t("finish.first", "Im Ziel! Deine erste Zeit auf <course>: <white><time></white>",
                "Finished! Your first time on <course>: <white><time></white>");
        t("finish.pb", "<green><bold>Neue Bestzeit!</bold></green> <white><time></white> <delta> <gray>auf</gray> <course>",
                "<green><bold>New personal best!</bold></green> <white><time></white> <delta> <gray>on</gray> <course>");
        t("finish.nopb", "Im Ziel: <white><time></white> <delta> <gray>– deine Bestzeit bleibt</gray> <white><best></white>",
                "Finished: <white><time></white> <delta> <gray>– your best stays</gray> <white><best></white>");
        t("finish.medal", "Neue Medaille: <medal><gray>!</gray>", "New medal: <medal><gray>!</gray>");
        t("finish.next", "<gray>Für</gray> <medal> <gray>fehlen dir noch</gray> <white><diff></white><gray>.</gray>",
                "<white><diff></white> <gray>more to go for</gray> <medal><gray>.</gray>");
        t("finish.rank", "<gray>Platz</gray> <white><rank></white> <gray>von</gray> <white><total></white> <gray>auf</gray> <course><gray>.</gray>",
                "<gray>Rank</gray> <white><rank></white> <gray>of</gray> <white><total></white> <gray>on</gray> <course><gray>.</gray>");
        t("finish.record", "<gold>★</gold> <white><player></white> <gold>hat den Rekord auf</gold> <course> <gold>geknackt:</gold> <white><time></white><gold>!</gold>",
                "<gold>★</gold> <white><player></white> <gold>set a new record on</gold> <course><gold>:</gold> <white><time></white><gold>!</gold>");
        t("finish.practice", "<light_purple>Übungs-Lauf:</light_purple> <white><time></white> <gray>– zählt nicht für die Bestenliste.</gray>",
                "<light_purple>Practice run:</light_purple> <white><time></white> <gray>– doesn't count for the leaderboard.</gray>");
        t("finish.toofast", "<red>Hoppla – <time> ist schneller als möglich. Diese Zeit zählt nicht.</red>",
                "<red>Whoops – <time> is faster than possible. This time doesn't count.</red>");
        t("finish.weekly", "<#7fdcff>Wochenbeste auf der Strecke der Woche: Platz <rank>.</#7fdcff>",
                "<#7fdcff>Weekly best on the course of the week: rank <rank>.</#7fdcff>");
        t("weekly.badge", "<#7fdcff><bold>▲ Wochen-Gipfel!</bold></#7fdcff> <gray>Du hast die Strecke der Woche geschafft – "
                + "das Abzeichen trägst du bis Montag.</gray>", "<#7fdcff><bold>▲ Weekly Summit!</bold></#7fdcff> <gray>You "
                + "finished the course of the week – you'll wear the badge until Monday.</gray>");
        t("weekly.broadcast", "<#7fdcff>▲</#7fdcff> <white><player></white> <gray>hat diese Woche den</gray> "
                + "<#7fdcff>Wochen-Gipfel</#7fdcff> <gray>erklommen!</gray>", "<#7fdcff>▲</#7fdcff> <white><player></white> "
                + "<gray>reached this week's</gray> <#7fdcff>Weekly Summit</#7fdcff><gray>!</gray>");
        t("badge", "<#7fdcff>▲ Wochen-Gipfel</#7fdcff>", "<#7fdcff>▲ Weekly Summit</#7fdcff>");

        // ---------- Übung, Geist, Ausblenden ----------
        t("practice.on", "<light_purple>Übungsmodus an:</light_purple> <gray>Setz dir mit dem Leitstein eigene Punkte. "
                + "Zeiten zählen nicht.</gray>", "<light_purple>Practice mode on:</light_purple> <gray>Set your own points "
                + "with the lodestone. Times don't count.</gray>");
        t("practice.off", "<green>Übungsmodus aus</green> <gray>– jetzt zählt's wieder!</gray>",
                "<green>Practice mode off</green> <gray>– times count again!</gray>");
        t("practice.set", "<light_purple>Übungs-Punkt gesetzt.</light_purple>", "<light_purple>Practice point set.</light_purple>");
        t("practice.air", "<yellow>Stell dich erst auf festen Boden.</yellow>", "<yellow>Stand on solid ground first.</yellow>");
        t("practice.only", "<yellow>Das geht nur im Übungsmodus</yellow> <gray>(/gipfel uebung)</gray>",
                "<yellow>That only works in practice mode</yellow> <gray>(/gipfel uebung)</gray>");
        t("ghost.on", "<aqua>Geist deiner Bestzeit: an</aqua> <gray>– die blauen Funken laufen deine beste Runde.</gray>",
                "<aqua>Ghost of your best time: on</aqua> <gray>– the blue sparks run your best lap.</gray>");
        t("ghost.off", "<gray>Geist deiner Bestzeit: aus</gray>", "<gray>Ghost of your best time: off</gray>");
        t("hide.hidden", "<gray>Mitspieler ausgeblendet.</gray>", "<gray>Other players hidden.</gray>");
        t("hide.shown", "<green>Mitspieler wieder sichtbar.</green>", "<green>Other players visible again.</green>");

        // ---------- Lobby ----------
        t("lobby.kick", "<gradient:#7fdcff:#ffffff><bold>Gipfelsturm</bold></gradient>\n\n<white>Danke fürs Klettern!</white>\n"
                + "<gray>Verbinde dich neu, um zurück in die Lobby zu kommen.</gray>",
                "<gradient:#7fdcff:#ffffff><bold>Summit Rush</bold></gradient>\n\n<white>Thanks for climbing!</white>\n"
                        + "<gray>Reconnect to get back to the lobby.</gray>");

        // ---------- Party-Rennen ----------
        t("race.invite", "<yellow><leader> startet ein Rennen auf</yellow> <course><yellow>!</yellow> "
                + "<click:run_command:'/gipfel mitmachen'><hover:show_text:'<green>Mitmachen</green>'><green><bold>[Mitmachen]</bold></green></hover></click> "
                + "<gray>oder /gipfel mitmachen (<sec> s)</gray>", "<yellow><leader> is starting a race on</yellow> <course><yellow>!</yellow> "
                + "<click:run_command:'/gipfel mitmachen'><hover:show_text:'<green>Join</green>'><green><bold>[Join]</bold></green></hover></click> "
                + "<gray>or /gipfel mitmachen (<sec> s)</gray>");
        t("race.wait", "<yellow>Warte auf deine Party …</yellow> <gray>(<sec> s)</gray>", "<yellow>Waiting for your party …</yellow> <gray>(<sec> s)</gray>");
        t("race.joined", "<green><player> ist dabei!</green>", "<green><player> joined!</green>");
        t("race.go", "<green><bold>Los!</bold></green>", "<green><bold>Go!</bold></green>");
        t("race.solo", "<gray>Niemand ist mitgekommen – dann eben solo!</gray>", "<gray>Nobody joined – going solo then!</gray>");
        t("race.finish", "<yellow>Rennen:</yellow> <white><player></white> <gray>ist im Ziel –</gray> <white><time></white> <gray>(Platz <place>)</gray>",
                "<yellow>Race:</yellow> <white><player></white> <gray>finished –</gray> <white><time></white> <gray>(place <place>)</gray>");
        t("race.over", "<yellow>Rennen vorbei!</yellow> <gray>Sieger:</gray> <white><player></white>",
                "<yellow>Race over!</yellow> <gray>Winner:</gray> <white><player></white>");
        t("race.none", "<gray>Gerade gibt es kein Rennen zum Mitmachen.</gray>", "<gray>There's no race to join right now.</gray>");
        t("race.busy", "<gray>Du bist schon in einem Lauf – erst aufgeben, dann mitmachen.</gray>",
                "<gray>You're already on a run – give up first, then join.</gray>");

        // ---------- Befehle ----------
        t("cmd.help", "<gold>/gipfel</gold> <gray>– Strecke wählen</gray>\n"
                + "<gold>/gipfel start ‹strecke›</gold> <gray>– Strecke starten</gray>\n"
                + "<gold>/gipfel top ‹strecke›</gold> <gray>– Bestenliste</gray>\n"
                + "<gold>/gipfel stats [spieler]</gold> <gray>– Statistik</gray>\n"
                + "<gold>/gipfel woche</gold> <gray>– Strecke der Woche</gray>\n"
                + "<gold>/gipfel uebung</gold> <gray>– Übungsmodus an/aus</gray>\n"
                + "<gold>/gipfel geist</gold> <gray>– Geist deiner Bestzeit an/aus</gray>\n"
                + "<gold>/gipfel verstecken</gold> <gray>– Mitspieler aus-/einblenden</gray>\n"
                + "<gold>/gipfel checkpoint · neustart · aufgeben · basislager · mitmachen · lobby</gold>",
                "<gold>/gipfel</gold> <gray>– pick a course</gray>\n"
                        + "<gold>/gipfel start ‹course›</gold> <gray>– start a course</gray>\n"
                        + "<gold>/gipfel top ‹course›</gold> <gray>– leaderboard</gray>\n"
                        + "<gold>/gipfel stats [player]</gold> <gray>– statistics</gray>\n"
                        + "<gold>/gipfel woche</gold> <gray>– course of the week</gray>\n"
                        + "<gold>/gipfel uebung</gold> <gray>– practice mode on/off</gray>\n"
                        + "<gold>/gipfel geist</gold> <gray>– ghost of your best time on/off</gray>\n"
                        + "<gold>/gipfel verstecken</gold> <gray>– hide/show other players</gray>\n"
                        + "<gold>/gipfel checkpoint · neustart · aufgeben · basislager · mitmachen · lobby</gold>");
        t("cmd.course", "<yellow>Diese Strecke kenne ich nicht.</yellow> <gray>Es gibt: <list></gray>",
                "<yellow>I don't know that course.</yellow> <gray>Available: <list></gray>");
        t("cmd.player", "<yellow>Das geht nur im Spiel.</yellow>", "<yellow>That only works in-game.</yellow>");
        t("cmd.perm", "<red>Dafür fehlt dir das Recht.</red>", "<red>You're not allowed to do that.</red>");
        t("cmd.notrunning", "<gray>Du bist gerade auf keiner Strecke.</gray>", "<gray>You're not on a course right now.</gray>");
        t("cmd.unknown", "<gray>Den Spieler kenne ich nicht.</gray>", "<gray>I don't know that player.</gray>");
        t("cmd.notready", "<yellow>Die Welt wird noch gebaut – gleich geht's los.</yellow>",
                "<yellow>The world is still being built – one moment.</yellow>");
        t("top.head", "<gold><bold>Bestenliste</bold></gold> <gray>–</gray> <course>", "<gold><bold>Leaderboard</bold></gold> <gray>–</gray> <course>");
        t("top.line", "<gray><rank>.</gray> <white><player></white> <gray>·</gray> <white><time></white> <medal>",
                "<gray><rank>.</gray> <white><player></white> <gray>·</gray> <white><time></white> <medal>");
        t("top.empty", "<gray>Noch keine Zeiten – sei der Erste!</gray>", "<gray>No times yet – be the first!</gray>");
        t("top.you", "<gray>Du: Platz <rank> ·</gray> <white><time></white>", "<gray>You: rank <rank> ·</gray> <white><time></white>");
        t("stats.head", "<gold><bold>Statistik</bold></gold> <gray>–</gray> <white><player></white>",
                "<gold><bold>Statistics</bold></gold> <gray>–</gray> <white><player></white>");
        t("stats.total", "<gray>Im Ziel:</gray> <white><finishes></white> <gray>· Versuche:</gray> <white><attempts></white> "
                + "<gray>· Stürze:</gray> <white><falls></white>", "<gray>Finishes:</gray> <white><finishes></white> "
                + "<gray>· Attempts:</gray> <white><attempts></white> <gray>· Falls:</gray> <white><falls></white>");
        t("stats.medals", "<gray>Medaillen:</gray> <medals>", "<gray>Medals:</gray> <medals>");
        t("stats.course", "<course> <gray>·</gray> <white><time></white> <medal> <gray>(Platz <rank>)</gray>",
                "<course> <gray>·</gray> <white><time></white> <medal> <gray>(rank <rank>)</gray>");
        t("stats.course.none", "<course> <gray>· noch keine Zeit</gray>", "<course> <gray>· no time yet</gray>");
        t("stats.badge", "<badge> <gray>diese Woche</gray>", "<badge> <gray>this week</gray>");
        t("week.head", "<#7fdcff><bold>Strecke der Woche</bold></#7fdcff> <gray>–</gray> <course> <gray>(wechselt in <left>)</gray>",
                "<#7fdcff><bold>Course of the week</bold></#7fdcff> <gray>–</gray> <course> <gray>(changes in <left>)</gray>");
        t("admin.reset", "<green>Zeiten von <player> gelöscht (<what>).</green>", "<green>Times of <player> removed (<what>).</green>");
        t("admin.rebuild", "<yellow>Die Welt wird beim nächsten Start neu gebaut.</yellow>",
                "<yellow>The world will be rebuilt on the next start.</yellow>");

        // ---------- Menüs ----------
        t("menu.courses", "Strecke wählen", "Pick a course");
        t("menu.book", "Gipfelbuch – Bestenlisten", "Summit book – leaderboards");
        t("menu.top", "Bestenliste – <course>", "Leaderboard – <course>");
        t("menu.weekly", "Strecke der Woche", "Course of the week");
        t("menu.best", "<gray>Deine Bestzeit:</gray> <white><time></white> <medal>", "<gray>Your best:</gray> <white><time></white> <medal>");
        t("menu.best.none", "<gray>Noch keine Zeit – los geht's!</gray>", "<gray>No time yet – let's go!</gray>");
        t("menu.medals", "<gray>Medaillen:</gray> <list>", "<gray>Medals:</gray> <list>");
        t("menu.record", "<gray>Rekord:</gray> <white><time></white> <gray>–</gray> <white><player></white>",
                "<gray>Record:</gray> <white><time></white> <gray>–</gray> <white><player></white>");
        t("menu.record.none", "<gray>Rekord: noch frei!</gray>", "<gray>Record: still up for grabs!</gray>");
        t("menu.rank", "<gray>Platz</gray> <white><rank></white> <gray>von</gray> <white><total></white>",
                "<gray>Rank</gray> <white><rank></white> <gray>of</gray> <white><total></white>");
        t("menu.cps", "<gray><n> Checkpoints</gray>", "<gray><n> checkpoints</gray>");
        t("menu.weeklytag", "<#7fdcff><bold>★ Strecke der Woche</bold></#7fdcff>", "<#7fdcff><bold>★ Course of the week</bold></#7fdcff>");
        t("menu.click.start", "<green>▶ Linksklick: Starten</green>", "<green>▶ Left-click: start</green>");
        t("menu.click.top", "<yellow>▶ Rechtsklick: Bestenliste</yellow>", "<yellow>▶ Right-click: leaderboard</yellow>");
        t("menu.click.open", "<yellow>▶ Klick: Bestenliste öffnen</yellow>", "<yellow>▶ Click: open leaderboard</yellow>");
        t("menu.practice", "<light_purple><bold>Übungsmodus</bold></light_purple> <gray>(<state>)</gray>",
                "<light_purple><bold>Practice mode</bold></light_purple> <gray>(<state>)</gray>");
        t("menu.practice.lore", "<gray>Eigene Punkte setzen, Zeiten zählen nicht.</gray>",
                "<gray>Set your own points, times don't count.</gray>");
        t("menu.ghost", "<aqua><bold>Geist deiner Bestzeit</bold></aqua> <gray>(<state>)</gray>",
                "<aqua><bold>Ghost of your best</bold></aqua> <gray>(<state>)</gray>");
        t("menu.ghost.lore", "<gray>Blaue Funken laufen deine beste Runde mit.</gray>",
                "<gray>Blue sparks run your best lap alongside you.</gray>");
        t("on", "an", "on");
        t("off", "aus", "off");
        t("menu.close", "<red>Schließen</red>", "<red>Close</red>");
        t("menu.back", "<gray>« Zurück</gray>", "<gray>« Back</gray>");
        t("menu.you", "<white><bold>Du</bold></white>", "<white><bold>You</bold></white>");
        t("menu.stats", "<gold><bold>Deine Statistik</bold></gold>", "<gold><bold>Your statistics</bold></gold>");
        t("menu.entry", "<white>#<rank> <player></white>", "<white>#<rank> <player></white>");
        t("menu.entry.time", "<gray>Zeit:</gray> <white><time></white> <medal>", "<gray>Time:</gray> <white><time></white> <medal>");
        t("menu.entry.when", "<gray>am <date></gray>", "<gray>on <date></gray>");
        t("menu.weekly.left", "<gray>Wechselt in <left></gray>", "<gray>Changes in <left></gray>");

        // ---------- Tafeln ----------
        t("board.record", "<gray>Rekord</gray> <white><time></white> <gray><player></gray>",
                "<gray>Record</gray> <white><time></white> <gray><player></gray>");
        t("board.empty", "<gray>Noch keine Zeiten – sei der Erste!</gray>", "<gray>No times yet – be the first!</gray>");
        t("board.click", "<green>▶ Schild anklicken zum Starten</green>", "<green>▶ Click the sign to start</green>");
        t("board.weekly", "<#7fdcff><bold>★ Strecke der Woche ★</bold></#7fdcff>", "<#7fdcff><bold>★ Course of the week ★</bold></#7fdcff>");
        t("board.weekly.left", "<gray>wechselt in <left> · schaff sie für</gray> <badge>",
                "<gray>changes in <left> · finish it for</gray> <badge>");
        t("board.weekly.summits", "<gray>Wochen-Gipfel bisher:</gray> <white><n></white>",
                "<gray>Weekly summits so far:</gray> <white><n></white>");
        t("board.hut", "<gradient:#7fdcff:#ffffff><bold>Basislager Gipfelsturm</bold></gradient>\n<gray>Wanderkarte = Strecke wählen · "
                + "Gipfelbuch = Bestzeiten</gray>", "<gradient:#7fdcff:#ffffff><bold>Summit Rush base camp</bold></gradient>\n"
                + "<gray>Trail map = pick a course · Summit book = best times</gray>");
        t("board.start", "<course>\n<diff>\n<gray>Zeit läuft ab dem Verlassen des Smaragds</gray>",
                "<course>\n<diff>\n<gray>Timer starts when you leave the emerald</gray>");
        t("board.finish", "<aqua><bold>▲ Ziel ▲</bold></aqua>", "<aqua><bold>▲ Finish ▲</bold></aqua>");
        t("time.days", "<d> Tagen <h> Std.", "<d> days <h> h");
        t("time.hours", "<h> Std. <m> Min.", "<h> h <m> min");
    }

    /** Text in der Sprache (de/en) mit Platzhaltern. Fehlt ein Schlüssel, steht er selbst da. */
    public static Component get(String lang, String key, TagResolver... args) {
        return MM.deserialize(raw(lang, key), args);
    }

    /** Mit Vorsilbe „Gipfelsturm »“ für den Chat. */
    public static Component chat(String lang, String key, TagResolver... args) {
        return MM.deserialize(PREFIX + raw(lang, key), args);
    }

    public static String raw(String lang, String key) {
        String[] v = T.get(key);
        if (v == null) {
            return key;
        }
        return "en".equals(lang) ? v[1] : v[0];
    }

    public static boolean has(String key) {
        return T.containsKey(key);
    }

    /** Platzhalter mit Text (ohne Formatierung, sicher für Spielernamen). */
    public static TagResolver arg(String name, Object value) {
        return Placeholder.unparsed(name, String.valueOf(value));
    }

    /** Platzhalter mit fertigem Baustein. */
    public static TagResolver comp(String name, Component value) {
        return Placeholder.component(name, value);
    }

    /** Platzhalter mit MiniMessage-Text (nur für eigene Texte, nie für Spielereingaben). */
    public static TagResolver parsed(String name, String mini) {
        return Placeholder.parsed(name, mini);
    }

    /** Alle Schlüssel (für den Test, dass jede Sprache jeden Text hat). */
    public static Map<String, String[]> all() {
        return Map.copyOf(T);
    }
}
