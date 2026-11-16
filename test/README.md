# Echte Tests mit Test-Spielern

Alle Tests laufen gegen einen lokalen Paper-Server **26.1.2** (mineflayer kann bis 26.1) mit Java 25, `online-mode=false`,
`enable-rcon=true`, `white-list=false` und `target/Morni-Gipfelsturm.jar` in `plugins/` (ViaVersion/ViaBackwards
dürfen dabei sein). Server-Ordner, RCON-Passwort und Welten gehören nie ins Repo (`.gitignore`). Für kleine Rechner:
`java -Xmx2G -jar paper-26.1.2.jar --nogui`.

mineflayer einmal irgendwo installieren (`npm install mineflayer`) und `NODE_PATH=<ordner>/node_modules` setzen.

## `bot-test.cjs` – alles rund um einen Lauf

```
node test/bot-test.cjs 127.0.0.1 <port> 127.0.0.1:<rcon-port> <datei-mit-rcon-passwort> <server-ordner>
```

Basislager, Wanderkarte, echte Sprünge bis Checkpoint 1, Sturz, Checkpoint-Reihenfolge, Ziel, Bestzeit und Geist auf der
Platte, Bestenliste (Befehl, Gipfelbuch, Tafel im Basislager), Übungsmodus, zu schnelle Zeit, Zwischenzeiten grün/rot
gegen die Bestzeit, Medaille, Statistik, Strecke der Woche (Stern, Wochen-Gipfel, Abzeichen in der Tab-Liste,
Wochen-Bestenliste), Mitspieler ausblenden, kein Schaden, Lobby ohne Netzwerk.

### Mit Fake-Netzwerk (`--network`)

`fake-network/` ist ein kleines Plugin namens `MoinMorni`, das die Schnittstelle `de.moinmorni.api.MoinMorni`
bereitstellt und jeden Aufruf (`stats`, `state`, `toLobby`) in `plugins/MoinMorni/calls.log` schreibt. Sprache und
Party werden per `/fakenet lang <spieler> de|en` bzw. `/fakenet party <anführer> <mitglied…>` gesetzt.

```
(cd test/fake-network && mvn package)   # → target/Fake-MoinMorni.jar nach plugins/
node test/bot-test.cjs … --network
```

Zusätzlich: Englisch, Statistik und Zustand ans „Netzwerk“, Sprache wechselt live, Party-Rennen mit zwei
Test-Spielern (Einladung, Mitmachen, Countdown, Platz 1, Sieger), Lobby über das Netzwerk.

## `course-test.cjs` – alle Strecken echt laufen

```
node test/course-test.cjs 127.0.0.1 <port> 127.0.0.1:<rcon-port> <datei-mit-rcon-passwort> [strecke…]
```

Startet jede Strecke und läuft sie Pad für Pad mit echten Sprüngen, Leitern und Schleim (nachgebaute Vanilla-Bewegung
von mineflayer). Schafft der Test-Spieler einen Übergang nicht, wird er auf den nächsten Pad gesetzt. Ausgabe pro
Strecke: echt geschaffte Übergänge, Checkpoints, Ziel. `DEBUG=1` zeigt, wo ein Sprung endete.

Die Steuerung ist einfach (anlaufen, an der Kante springen, in der Luft zum Ziel lenken, kein Neo). Ein nicht
geschaffter Übergang heißt nicht, dass er unmöglich ist – dafür prüfen `JumpRules`/`CourseValidator` jede Strecke und
`JumpPhysicsTest` jede Grenze Tick für Tick.

## Wechsel der Strecke der Woche

Die Uhr der Wochen-Strecke lässt sich nur für Tests verstellen: `java -Dgipfelsturm.uhr-versatz=<ms> -jar …`. Mit einem
Versatz, der kurz vor Montag 0:00 (Berlin) landet, sieht man den Wechsel im Server-Log („Neue Woche …“) und mit
`/gipfel woche`.
