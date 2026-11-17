# Stand – Gipfelsturm (Morni-Gipfelsturm)

**Was es ist:** Der Parkour-Modus des MoinMorni-Netzwerks (Paper-Plugin `Gipfelsturm`, Java 25, Paper 26.x). Baut seine
eigene Welt aus dem Code – Basislager und acht eigene Alpen-Strecken von leicht bis extrem – mit Bestzeiten auf die
Millisekunde, Zwischenzeiten, Medaillen, Bestenlisten, Strecke der Woche, Übungsmodus, Geist und Party-Rennen. Läuft im
Netzwerk (Schnittstelle `de.moinmorni.api.MoinMorni`) und ganz allein.

## Aufbau

- `GipfelsturmPlugin` (Start, Woche, Beitreten, wiederkehrende Aufgaben), `cmd/GipfelCommand` (`/gipfel …`).
- `course/` – Strecken-Modell ohne Bukkit: `CourseBuilder`, `JumpRules`, `CourseValidator`, `Plan`, `Course`, `Pad`,
  `Medal`, `Difficulty`, `Blk`/`Shape`/`Surface`.
- `courses/` – `CourseCatalog` (8 Strecken), `Basecamp`, `Scenery`, `World`.
- `world/` – `VoidGenerator` (Chunks direkt aus dem Bauplan), `WorldManager` (neu bei neuem Bauplan), `Holograms`.
- `game/` – `Runs`/`Run` (Läufe, Zeit, Checkpoints, Stürze, Ziel, Geist), `Race`, `Hotbar`, `Controls`, `GameMenus`,
  `Boards`, `Guard`.
- `stats/` – `Store`, `Leaderboard`, `PlayerRecord`, `WeeklyRotation`. `time/` – `RunClock`, `TimeFormat`, `Splits`.
- `net/` – `Net`, `MoinMorniNet` (einzige Klasse mit der Schnittstelle), `Standalone`. `ui/` – `Texts` (de/en), `Items`, `Menu`.
- `src/api/java/de/moinmorni/api/MoinMorni.java` – unveränderte Kopie aus `pmornhart/moinmorni.net` (nicht in der Jar).
- `test/` – `bot-test.cjs`, `course-test.cjs`, `fake-network/` (Test-Netzwerk), siehe `test/README.md`.

## Strecken

| Strecke | Stufe | Pads | Checkpoints | Besonderes |
|---|---|---|---|---|
| Almwiesen-Lauf (`almwiese`) | Leicht | 33 | 3 | Trittsteine, Leiter auf den Heustadel |
| Gletscherspalten (`gletscher`) | Leicht | 30 | 3 | Eistürme, Packeis |
| Kletterwand (`kletterwand`) | Mittel | 33 | 4 | 3 Leitern, 1 Neo |
| Felsgrat (`felsgrat`) | Mittel | 32 | 3 | Mauer-Pfosten, halbe Stufen, schräge Sprünge |
| Seilbahn-Masten (`seilbahn`) | Schwer | 35 | 4 | Leiter, Schleim-Sprung, Zaun-Pfosten |
| Eishöhle (`eishoehle`) | Schwer | 38 | 4 | Kopfstoß-Gänge, Blaueis, Seelensand, Wasser, Schleim |
| Gipfelkreuz-Sprint (`gipfelkreuz`) | Extrem | 42 | 5 | „3+1“, Vierer, 2 Neos, Schleim |
| Wetterturm (`wetterturm`) | Extrem | 47 | 5 | 3 Leitern, 2 Schleim-Sprünge, Neo, Blaueis |

## Stand (16.11.2026)

- Version 0.1.3 (Branch `claude/zurueck-ins-spiel`, wartet auf Philips Go): **Zurück ins Spiel** (Philip: „mitten im
  Spiel nochmal fragen, ob man wirklich zur Lobby will; nach einem Absturz zurück ins Spiel“). Netzwerk-Schnittstelle 2
  (byte-gleiche Kopie; `askToLobby`, `playing`, `holdSlot`; älteres Netzwerk-Plugin → `LinkageError` einmal im Log,
  sonst wie bisher). `playing`, solange die Uhr eines Laufs läuft oder ein Party-Rennen ansteht; „Zurück ins Dorf“ und
  `/gipfel lobby` rufen `askToLobby`. Verbindung mitten im Lauf weg → Lauf `rueckkehr-sekunden` frei (neu oben in der
  config.yml, Vorgabe 90, 0 = aus; Fristen in `game/Comeback`), `holdSlot`; im Party-Rennen wartet das Rennen. Fair:
  Rechtzeitig zurück geht es am letzten Checkpoint weiter wie nach einem Sturz (ohne ihn zu zählen), die Uhr ist
  weitergelaufen, der Geist bekommt die Lücke aufgefüllt (`Run.pad`); noch ohne Checkpoint zurück an den Start (im
  Rennen läuft die Uhr weiter, allein beginnt der Lauf neu). Zu spät: Lauf vorbei wie bisher (Stürze gemeldet, im Rennen
  ausgestiegen). Neu `test/run.sh` (baut, richtet den Test-Server ein, frische Spielerdaten, `rueckkehr-sekunden: 20`,
  `MEMORY`, erst allein, dann mit Fake-Netzwerk). JUnit 6.1.3. Geprüft: 53 Unit-Tests (neu `ComebackTest`), Paper 26.1
  übersetzt, echt mit Paper 26.1.2: allein **49/49** (zweimal, vor und nach dem Neustart des Rechners), mit
  Fake-Netzwerk **64/67** – alle neuen Prüfungen grün (spielt im Lauf, holdSlot 20, „Welcome back!“ am Checkpoint 1,
  Uhr 5,9 s → 15,0 s weitergelaufen, holdSlot 0 und wieder „spielt“, im Ziel nicht mehr, zu spät zurück → Basislager,
  holdSlot 20 → 0, Lobby-Gegenstand → `askToLobby` ohne Nachfrage im Basislager); die drei roten sind Zeit-Prüfungen
  der echten Sprünge und Klicks auf dem stark ausgelasteten Rechner (Last ~20 auf 4 Kernen; allein liefen dieselben
  Prüfungen grün), Log ohne Fehler.

- Version 0.1.2 (Branch `claude/moinmorni`, wartet auf Philips Go): **Name wieder MoinMorni** – 0.1.1 (16.11.
  veröffentlicht) hatte Gipfelsturm kurz an einen neuen Netzwerk-Namen angepasst; der war nicht verfügbar, darum ist
  der Code wieder wie in 0.1.0 (Umbenennung per `git revert` zurückgenommen): Paket `de.moinmorni.gipfelsturm`,
  Schnittstelle `de.moinmorni.api.MoinMorni`, Netzwerk-Plugin „MoinMorni“, Brücke `net/MoinMorniNet`. Plugin-Name,
  Datenordner `plugins/Gipfelsturm`, Welt und Strecken waren nie anders (kein Umzug, kein Neubau). Geprüft: 49
  Unit-Tests, Paper 26.1 übersetzt, echt mit Paper 26.1.2: `bot-test.cjs` **45/45**, `--network` **58/58**
  (Gipfelsturm meldet sich beim Test-Netzwerk „MoinMorni“), Server-Log ohne Fehler.
- Version 0.1.1 (16.11. veröffentlicht): `Runs.onTick` warf eine ConcurrentModificationException, wenn im
  Party-Rennen jemand ins Ziel kam, während noch andere liefen (Lauf fiel mitten im Durchgang aus der Liste) – jetzt
  über eine Kopie. Test-Werkzeug: Die RCON-Hilfe in `bot-test.cjs`/`course-test.cjs` schickte Befehl und „list“ auf
  einmal; Vanilla-RCON erwartet genau ein Paket pro Lesen und trennte dann – das Versprechen hing und node endete
  still mit 0 (falsches Grün). Jetzt „list“ erst nach der ersten Antwort, Verbindungsende ohne Antwort = Fehler,
  `beforeExit` = Fehlschlag. Beides bleibt in 0.1.2.
- Version 0.1.0 (15.11. veröffentlicht, Release v0.1.0): alles aus der README.
- 49 Unit-Tests grün (Sprung-Physik Tick für Tick, alle Strecken gegen die Sprung-Regeln, Medaillen, Bestenliste,
  Strecke der Woche inkl. Sommer-/Winterzeit, Zeitformat, Texte de/en).
- Echt mit Paper 26.1.2 (+ ViaVersion/ViaBackwards 5.12.2) und Test-Spielern (mineflayer):
  - `bot-test.cjs` allein: **45/45** – Basislager, Menü 8 Strecken in 4 Stufen, echte Sprünge bis Checkpoint 1, Sturz,
    Checkpoint-Reihenfolge, Ziel, Bestzeit/Zwischenzeiten/Geist gespeichert, `/gipfel top`, Gipfelbuch, Tafel im
    Basislager, Übungsmodus, zu schnelle Zeit, Zwischenzeiten grün/rot gegen die Bestzeit, Medaille, `/gipfel stats`,
    Strecke der Woche (Stern, Wochen-Gipfel, ▲ in der Tab-Liste, Wochen-Bestenliste), Mitspieler aus, kein Schaden,
    Lobby ohne Netzwerk (freundlicher Kick).
  - `bot-test.cjs --network` mit Fake-Netzwerk: **58/58** – zusätzlich Englisch, `stats("gipfelsturm", …)` (Zähler
    getrennt von `time_<strecke>_ms`, kleiner ist besser), `state("gipfelsturm", "open", n)`, Sprache wechselt live,
    Party-Rennen mit zwei Test-Spielern (Einladung, Mitmachen, Countdown, Zeit ab Startschuss, Platz 1, Sieger),
    `toLobby`.
  - `course-test.cjs`: alle 8 Strecken gestartet und Pad für Pad gelaufen – alle Checkpoints und das Ziel zählen auf
    allen 8. Echt geschafft: **230/282** Übergänge – Almwiese 32/32, Gletscher 24/29, Kletterwand 31/32, Felsgrat 29/31,
    Seilbahn 27/34, Eishöhle 24/37, Gipfelkreuz 28/41, Wetterturm 35/46; alle 7 Leitern echt hinauf. Den Rest setzt
    der Test auf den nächsten Pad (siehe Offen).
  - Wechsel der Strecke der Woche live: Server mit `-Dgipfelsturm.uhr-versatz` kurz vor Montag 0:00 (Berlin) gestartet
    → „Neue Woche 2026-W47: Strecke der Woche ist Gletscherspalten“, Wochen-Bestenliste leer, `/gipfel woche` neu.
- Behoben beim echten Lauf: Wer eine Leiter hinaufkletterte, wurde nach einem Moment als „Abkürzung“ zum Checkpoint
  zurückgesetzt (Leitern zählen jetzt zum Weg).

## Offen

- Philip: Strecken im echten Client ansehen und laufen; Go für `main` mit 0.1.3 (Zurück ins Spiel; das Netzwerk holt
  sich die Jar von dort).
- Netzwerk-Repo: Einstellungs-Katalog um `rueckkehr-sekunden` ergänzen (0–600, Vorgabe 90).
- Nicht echt geprüft: Rückkehr über das echte Netzwerk (Lobby-Angebot „Zurück ins Spiel“), Rückkehr im Party-Rennen
  (nur Code; das Rennen wartet, bis die Frist abläuft).
- Nicht mit Test-Spielern geschafft (nur über Sprung-Regeln und `JumpPhysicsTest` geprüft): Neos, Schleim-Sprünge,
  Kopfstoß-Gänge der Eishöhle, die weitesten Sprint-Sprünge (Vierer, „3+1“) – die einfache Steuerung hält auf jedem Pad
  an und nimmt keinen Schwung mit.
- Nicht echt geprüft: Aussehen von Basislager, Strecken und Tafeln im echten Client, Geist (Partikel), Bedrock-Spieler,
  Paper 26.3 mit Test-Spieler (mineflayer kann nur bis 26.1), echter Hub statt Fake-Netzwerk.
- Medaillen-Grenzen sind aus einer Schätzung der Laufzeit berechnet – nach den ersten echten Läufen nachjustieren.
- Einstellungen (`config.yml`) im Panel des Netzwerks (Einstellungs-Katalog) – gehört ins Netzwerk-Repo.
