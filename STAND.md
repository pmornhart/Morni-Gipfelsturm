# Stand – Gipfelsturm (Morni-Gipfelsturm)

**Was es ist:** Der Parkour-Modus des Netzwerks morni.net (Paper-Plugin `Gipfelsturm`, Java 25, Paper 26.x). Baut seine
eigene Welt aus dem Code – Basislager und acht eigene Alpen-Strecken von leicht bis extrem – mit Bestzeiten auf die
Millisekunde, Zwischenzeiten, Medaillen, Bestenlisten, Strecke der Woche, Übungsmodus, Geist und Party-Rennen. Läuft im
Netzwerk (Schnittstelle `net.morni.api.MorniNet`) und ganz allein.

## Aufbau

- `GipfelsturmPlugin` (Start, Woche, Beitreten, wiederkehrende Aufgaben), `cmd/GipfelCommand` (`/gipfel …`).
- `course/` – Strecken-Modell ohne Bukkit: `CourseBuilder`, `JumpRules`, `CourseValidator`, `Plan`, `Course`, `Pad`,
  `Medal`, `Difficulty`, `Blk`/`Shape`/`Surface`.
- `courses/` – `CourseCatalog` (8 Strecken), `Basecamp`, `Scenery`, `World`.
- `world/` – `VoidGenerator` (Chunks direkt aus dem Bauplan), `WorldManager` (neu bei neuem Bauplan), `Holograms`.
- `game/` – `Runs`/`Run` (Läufe, Zeit, Checkpoints, Stürze, Ziel, Geist), `Race`, `Hotbar`, `Controls`, `GameMenus`,
  `Boards`, `Guard`.
- `stats/` – `Store`, `Leaderboard`, `PlayerRecord`, `WeeklyRotation`. `time/` – `RunClock`, `TimeFormat`, `Splits`.
- `net/` – `Net`, `Connected` (einzige Klasse mit der Schnittstelle), `Standalone`. `ui/` – `Texts` (de/en), `Items`, `Menu`.
- `src/api/java/net/morni/api/MorniNet.java` – unveränderte Kopie aus `pmornhart/morni.net` (nicht in der Jar).
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

- Version 0.1.1 (16.11., Branch `claude/morni-net`, wartet auf Philips Go): **Umbenannt in morni.net** (vorher
  MoinMorni) – Paket `net.morni.gipfelsturm`, Schnittstelle `net.morni.api.MorniNet` vom Netzwerk-Plugin „MorniNet“
  (`softdepend: [MorniNet]`), Brücke `net/Connected` (vorher nach dem alten Namen benannt), Plugin-Angaben und Texte
  mit morni.net. Plugin-Name, Datenordner `plugins/Gipfelsturm`, Welt und Strecken bleiben (kein Neubau).
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

- Philip: Strecken im echten Client ansehen und laufen; Go für `main` (dann Release v0.1.1, das Netzwerk holt sich die
  Jar von dort).
- Nicht mit Test-Spielern geschafft (nur über Sprung-Regeln und `JumpPhysicsTest` geprüft): Neos, Schleim-Sprünge,
  Kopfstoß-Gänge der Eishöhle, die weitesten Sprint-Sprünge (Vierer, „3+1“) – die einfache Steuerung hält auf jedem Pad
  an und nimmt keinen Schwung mit.
- Nicht echt geprüft: Aussehen von Basislager, Strecken und Tafeln im echten Client, Geist (Partikel), Bedrock-Spieler,
  Paper 26.3 mit Test-Spieler (mineflayer kann nur bis 26.1), echter Hub statt Fake-Netzwerk.
- Medaillen-Grenzen sind aus einer Schätzung der Laufzeit berechnet – nach den ersten echten Läufen nachjustieren.
- Einstellungen (`config.yml`) im Panel des Netzwerks (Einstellungs-Katalog) – gehört ins Netzwerk-Repo.
