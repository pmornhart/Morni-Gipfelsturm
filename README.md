# Gipfelsturm

**Parkour-Strecken von leicht bis extrem, mit Bestzeiten und Strecke der Woche.**

Gipfelsturm ist ein Spielmodus des Netzwerks [morni.net](https://morni.net): ein Paper-Plugin, das sich seine
eigene Welt aus dem Code baut – ein Basislager auf einer Bergschulter und acht Parkour-Strecken quer durch die Alpen.
Es läuft im Netzwerk (Sprache, Party, Statistik) und genauso ganz allein auf einem einzelnen Server.

## So spielt man

- Wer den Server betritt, steht im **Basislager**: Berghütte, weiß-blaues Rauten-Mosaik, und für jede Strecke ein
  Wegweiser mit Schild (anklicken = Start) und der Bestenliste darüber. Über dem Platz schwebt die Tafel der
  **Strecke der Woche**.
- In der Leiste: **Wanderkarte** (Strecke wählen), **Gipfelbuch** (Bestenlisten), **Strecke der Woche** (direkt
  starten), **Mitspieler ein/aus** und **Zurück ins Dorf** (Lobby).
- Die Farben sind wie im Parkour der Lobby: **Smaragd = Start, Gold = Checkpoint, Diamant = Ziel**, weiß/hellblau =
  Sprungblöcke.
- Die Zeit startet, sobald man den Smaragd verlässt, und läuft in der Aktionsleiste mit – auf die Millisekunde (gemessen
  in Spiel-Ticks, dazwischen genau nachgerechnet, damit Server-Ruckler nichts verfälschen).
- An jedem **Checkpoint** gibt es die Zwischenzeit, grün oder rot im Vergleich zur eigenen Bestzeit. Wer fällt (unter die
  Fallgrenze des Abschnitts oder ins eiskalte Wasser), landet wieder am letzten Checkpoint – vor dem ersten Checkpoint
  zurück am Start, dann beginnt die Zeit neu.
- Unterwegs in der Leiste: **Zurück zum Checkpoint**, **Neustart**, **Mitspieler ein/aus**, **Aufgeben**.
- Kein Fallschaden, kein Hunger, kein PvP, Spieler schieben sich nicht gegenseitig.

### Bestzeiten und Medaillen

Jede Strecke hat vier Medaillen: **Bronze, Silber, Gold** und – für richtig gute Läufe – **Gipfel ▲**. Bestzeiten
stehen auf den Tafeln im Basislager (Top 10), im Gipfelbuch und mit `/gipfel top <strecke>`.

### Strecke der Woche

Jeden **Montag um 0:00 Uhr** (deutsche Zeit) wird eine andere Strecke zur Strecke der Woche – fest aus der Nummer der
Woche, abwechselnd leicht und schwer, in acht Wochen kommt jede dran. Sie hat ihre eigene Wochen-Bestenliste. Wer sie in
der Woche einmal schafft, trägt bis Montag das Abzeichen **▲ Wochen-Gipfel** (im Chat und in der Tab-Liste).

### Üben, Geist, Rennen

- `/gipfel uebung` – **Übungsmodus**: mit dem Leitstein eigene Punkte setzen, beim Sturz geht es dorthin zurück; Zeiten
  zählen nicht.
- **Geist**: Blaue Funken laufen die eigene Bestzeit mit (an/aus mit `/gipfel geist` oder im Menü).
- **Party-Rennen**: Startet der Anführer einer Party eine Strecke, können Party-Mitglieder auf dem Server mitmachen
  (Feuerwerk in der Leiste oder `/gipfel mitmachen`). Nach dem Countdown laufen alle gleichzeitig los.

## Die Strecken

Alle Strecken sind eigene Entwürfe und werden aus dem Code gebaut. Jeder Sprung ist mit normaler Bewegung schaffbar –
das prüft ein Test für jede Strecke (siehe „Fair“ unten).

| Strecke | Stufe | Checkpoints | Das gibt es dort |
|---|---|---|---|
| Almwiesen-Lauf | Leicht | 3 | große Felder, Trittsteine über den Bach, Leiter auf den Heustadel – schaffbar ohne Sprinten |
| Gletscherspalten | Leicht | 3 | von Eisturm zu Eisturm über Spalten, rutschige Packeis-Felder |
| Kletterwand | Mittel | 4 | Leitern, schmale Bänder, Sprünge mit Anlauf, der erste Neo |
| Felsgrat | Mittel | 3 | Mauer-Pfosten, halbe Stufen, schräge Sprünge über den Grat |
| Seilbahn-Masten | Schwer | 4 | Gondel-Dächer, Zaun-Pfosten, Vierer abwärts, Schleim-Sprung |
| Eishöhle | Schwer | 4 | Kopfstoß-Gänge unter niedriger Decke, Blaueis, zäher Seelensand, eiskaltes Wasser |
| Gipfelkreuz-Sprint | Extrem | 5 | „3+1“ mit Anlauf, Vierer über den Abgrund, Neo um eine dicke Wand, Gitter-Pfosten, Gipfelkreuz am Ziel |
| Wetterturm | Extrem | 5 | rund um den Turm hinauf: lange Leitern, zwei Schleim-Sprünge, Blaueis, Wetterstation am Ziel |

## Befehle

| Befehl | Was er macht |
|---|---|
| `/gipfel` | Menü „Strecke wählen“ |
| `/gipfel start <strecke>` | Strecke starten (z. B. `almwiese`, `gipfelkreuz`) |
| `/gipfel top <strecke>` | Top 10 einer Strecke |
| `/gipfel stats [spieler]` | Statistik: im Ziel, Versuche, Stürze, Medaillen, Bestzeiten |
| `/gipfel woche` | Strecke der Woche und Wochen-Bestenliste |
| `/gipfel uebung` | Übungsmodus an/aus |
| `/gipfel geist` | Geist der eigenen Bestzeit an/aus |
| `/gipfel verstecken` | Mitspieler aus-/einblenden |
| `/gipfel checkpoint` · `neustart` · `aufgeben` · `basislager` | wie die Gegenstände in der Leiste |
| `/gipfel mitmachen` | beim Party-Rennen mitmachen |
| `/gipfel lobby` | zurück in die Lobby |

Abkürzungen: `/gs`, `/gipfelsturm`, `/summit`.

Für Admins (`gipfelsturm.admin`, Standard: OP): `/gipfel reset <spieler> [strecke]` (Zeiten löschen),
`/gipfel neubau` (Welt beim nächsten Start neu erzeugen), `/gipfel pruefen` (alle Strecken gegen die Sprung-Regeln
prüfen), `/gipfel pads <strecke>` (Lage aller Pads als JSON). Admins im Kreativmodus dürfen bauen.

## Einstellungen (`plugins/Gipfelsturm/config.yml`)

| Schlüssel | Standard | Bedeutung |
|---|---|---|
| `welt` | `gipfelsturm` | Name der eigenen Welt |
| `plausibel-pruefen` | `true` | Zeiten, die schneller sind als möglich (Teleport, Fliegen), nicht werten |
| `rennen-wartezeit` | `15` | Sekunden zum Mitmachen beim Party-Rennen |
| `geist-standard` | `true` | Geist für neue Spieler an |

Nur für Tests: `-Dgipfelsturm.uhr-versatz=<ms>` beim Start verstellt die Uhr der Strecke der Woche (um den Wechsel am
Montag zu prüfen).

Die Welt entsteht direkt aus dem Bauplan, Chunk für Chunk, sobald jemand hinkommt. Ändert eine neue Version die
Strecken, wird die Welt beim Start gelöscht und neu erzeugt (Spieler bauen dort nichts). Spielerdaten liegen unter
`plugins/Gipfelsturm/spieler/<uuid>.yml`, die Wege der Bestzeiten unter `geister/`.

## Im Netzwerk morni.net

Läuft das Netzwerk-Plugin `MorniNet`, nutzt Gipfelsturm dessen Schnittstelle (`net.morni.api.MorniNet`):

- **Sprache** jedes Spielers (Deutsch/Englisch, auch nach `/sprache`), sonst Deutsch.
- **Statistik** (`stats("gipfelsturm", …)`): Zähler `attempts`, `falls`, `finishes`, `finishes_<strecke>` und getrennt die
  Bestzeit `time_<strecke>_ms` (kleiner ist besser). Lokal wird alles zusätzlich gespeichert.
- **Zustand** (`state("gipfelsturm", "open", <spieler>)`) bei jeder Änderung und jede Minute.
- **Party** für Rennen, **Lobby** für „Zurück ins Dorf“.

Ohne Netzwerk: Deutsch, Statistik nur lokal, „Zurück ins Dorf“ verabschiedet freundlich (Neu verbinden).

## Fair: die Sprung-Regeln

Jede Strecke wird beim Bauen (Test) und beim Start des Plugins geprüft (`course/JumpRules.java`,
`course/CourseValidator.java`). Die Grenzen kommen aus der Bewegungsphysik des Spiels und lassen Luft:

| Höhe (Ziel − Absprung) | Leicht (ohne Sprinten) | Mittel/Schwer | Extrem |
|---|---|---|---|
| über +1 | geht nicht (höchster Sprung 1,25) | geht nicht | geht nicht |
| +0,5 … +1 | Lücke 1 | Lücke 2 | Lücke 2, mit 3 Blöcken Anlauf Lücke 3 („3+1“) |
| 0 … +0,5 | Lücke 1 | Lücke 3 | Lücke 3 |
| −1 … 0 | Lücke 2 | Lücke 3 | Lücke 3 |
| −1 und tiefer | Lücke 2 | Lücke 4 | Lücke 4 |

Dazu: Eine flache Vierer-Lücke ist verboten (ohne Schwung reicht es knapp nicht). Unter einer Decke („Kopfstoß“) nur
geradeaus oder abwärts, bei 2 Blöcken Platz Lücke 1, bei 2,5 Lücke 2 – und in einen Kopfstoß-Gang läuft man hinein,
statt hineinzuspringen. Seelensand bremst (Lücke 2 bzw. 1 nach oben), auf Eis muss die Landefläche zum Bremsen reichen,
Schleim federt nur so hoch, wie man vorher gefallen ist. Neos (um eine Wand herum) erst ab Mittel. Schräge Sprünge zählen
mit ihrer echten Entfernung, Zaun- und Gitter-Pfosten mit ihrer schmalen Oberseite. Nichts darf im Sprungweg stehen.
`JumpPhysicsTest` rechnet jede Grenze mit einer Tick-für-Tick-Nachbildung der Bewegung nach.

## Bauen und testen

Voraussetzung: Java 25, Maven.

```
mvn package          # baut und testet: target/Morni-Gipfelsturm.jar
mvn test             # nur die Unit-Tests
```

### Test mit Test-Spieler

`test/bot-test.cjs` spielt Gipfelsturm mit einem Test-Spieler ([mineflayer](https://github.com/PrismarineJS/mineflayer))
auf einem lokalen Paper-Server: Basislager, Wanderkarte, Almwiesen-Lauf mit echten Sprüngen bis zum ersten Checkpoint,
weitere Checkpoints per Teleport, Sturz, Ziel, gespeicherte Bestzeit, Bestenliste (Befehl und Menü), Übungsmodus,
Schutz vor unmöglichen Zeiten, Zwischenzeiten gegen die Bestzeit, Medaillen, Tafeln im Basislager, Statistik, Strecke
der Woche mit Abzeichen, Mitspieler ausblenden, kein Schaden, Lobby.

1. Paper **26.1.2** (mineflayer kann bis 26.1) mit Java 25, `online-mode=false`, `enable-rcon=true`,
   `white-list=false`; `target/Morni-Gipfelsturm.jar` nach `plugins/`.
2. `npm install mineflayer` (irgendwo) und dann:

```
NODE_PATH=<ordner>/node_modules node test/bot-test.cjs 127.0.0.1 <port> 127.0.0.1:<rcon-port> <datei-mit-rcon-passwort> <server-ordner>
```

Mit dem Fake-Netzwerk aus `test/fake-network/` zusätzlich Englisch, Statistik/Zustand ans „Netzwerk“ und ein
Party-Rennen mit zwei Test-Spielern (`--network`). `test/course-test.cjs` läuft alle acht Strecken Pad für Pad mit
echten Sprüngen, Leitern und Schleim ab. Mehr in `test/README.md`.

---

Verfasst mit Claude 🤖
