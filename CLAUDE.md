# Gipfelsturm (Morni-Gipfelsturm)

Parkour-Modus des MoinMorni-Netzwerks (Paper-Plugin, Java 25, Paper 26.x). Baut seine eigene Welt aus dem Code
(Basislager + 8 Strecken), läuft im Netzwerk und auch ganz allein.

## Regeln für dieses Repo

- Repo ist **öffentlich**: keine Geheimnisse, Tokens, Passwörter oder privaten Daten committen (auch keine
  Server-Ordner, Welten, Spielerdaten oder RCON-Passwörter aus Tests).
- Texte im Spiel auf **Deutsch (Standard) und Englisch**, freundlich, keine Fachbegriffe ohne Erklärung. Jeder Text
  steht in `ui/Texts.java` in beiden Sprachen mit denselben Platzhaltern (`TextsTest` prüft das).
- **Keine fremden Marken, Figuren, Namen oder Grafiken** (nichts von Mojang, Nintendo, Hypixel o. Ä.) – nur eigene
  Entwürfe; Parkour-Mechaniken ja, fremde Strecken/Namen nein.
- Die Netzwerk-Schnittstelle `src/api/java/de/moinmorni/api/MoinMorni.java` ist eine **unveränderte Kopie** aus
  `pmornhart/moinmorni.net` (`plugins/moinmorni-network/src/main/java/de/moinmorni/api/MoinMorni.java`). Nie hier
  ändern – ändert sie sich dort, die Kopie 1:1 ersetzen. Sie wird nur zum Übersetzen gebraucht und landet nicht in der
  Jar (`maven-jar-plugin` schließt `de/moinmorni/api/**` aus). Nur `net/MoinMorniNet.java` darf sie benutzen.
- Neue oder geänderte Strecken müssen die Sprung-Regeln einhalten (`course/JumpRules.java`, geprüft von
  `CourseValidator` in `CourseGeometryTest`). Regeln nur mit Beleg aus der Bewegungsphysik ändern (`JumpPhysicsTest`).
- „Verfasst mit Claude 🤖“ als letzte Zeile in Commit-Messages (durch eine Leerzeile getrennt), in PRs, Issues und
  Release-Notes; einmal als Fußzeile im README.
- Commit-Messages auf Deutsch mit Stichpunkten `- Neu:` / `- Behoben:` / `- Verbessert:` / `- Entfernt:`.
- Neue Version = `<version>` in `pom.xml` erhöhen und `docs/releases/v<version>.md` anlegen; der Workflow baut bei
  jedem Push und legt auf `main` das Release `v<version>` mit `Morni-Gipfelsturm.jar` an (MoinMorni spielt es auf).
- Vor jeder Veröffentlichung (Merge nach `main`) Philips Go abwarten.
- Nach sichtbaren Änderungen im Spiel echt testen (`test/bot-test.cjs` gegen einen lokalen Paper-Server) und Bilder
  machen – Bilder mit echten Spiel-Texturen nur lokal, nie ins Repo.

## Aufbau

- `course/` – Strecken-Modell ohne Bukkit (testbar): `CourseBuilder` (Baukasten), `JumpRules` (Sprung-Regeln),
  `CourseValidator`, `Plan` (Bauplan der Welt), `Course`, `Pad`, `Medal`.
- `courses/` – die Entwürfe: `CourseCatalog` (8 Strecken), `Basecamp` (Basislager), `Scenery` (Landschaft), `World`.
- `world/` – `VoidGenerator` (erzeugt Chunks direkt aus dem Bauplan), `WorldManager` (Welt anlegen, bei neuem Bauplan
  neu), `Holograms` (Tafeln und Marker, nicht dauerhaft, je Sprache).
- `game/` – `Runs` (Läufe, Zeit, Checkpoints, Stürze, Ziel, Geist, Party-Rennen), `Hotbar`, `Controls`, `GameMenus`,
  `Boards`, `Guard` (Schutz).
- `stats/` – `Store` (YAML je Spieler, Bestenlisten, Geister), `Leaderboard`, `PlayerRecord`, `WeeklyRotation`.
- `net/` – `Net` (Netzwerk oder allein), `MoinMorniNet` (Brücke zur Schnittstelle), `Standalone`.
- `time/` – `RunClock` (Ticks mit Bruchteilen), `TimeFormat`, `Splits`.

## Bauen und testen

```
JAVA_HOME=<jdk-25> mvn -q -DskipTests package   # Jar: target/Morni-Gipfelsturm.jar
JAVA_HOME=<jdk-25> mvn test                     # Unit-Tests
```

Echter Test: siehe README („Test mit Test-Spieler“).
