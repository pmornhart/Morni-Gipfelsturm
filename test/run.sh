#!/usr/bin/env bash
# Echter Test von Gipfelsturm: baut das Plugin und das Fake-Netzwerk, richtet einen Paper-Server ein (offline, eigener
# Port, RCON an), startet ihn, schickt den Test-Spieler durch alle Abläufe – erst allein (ohne Netzwerk), dann mit
# Fake-Netzwerk (--network) – und hält ihn wieder an. Zurück ins Spiel nach 20 Sekunden (der Test kommt einmal
# rechtzeitig und einmal zu spät zurück).
#
#   test/run.sh                    # beides
#   ONLY=network test/run.sh       # nur mit Fake-Netzwerk (ONLY=alone: nur allein)
#   KEEP=1 SKIP_TEST=1 test/run.sh # nur Server starten (mit Fake-Netzwerk)
#
# Erwartet (Vorgaben für die MoinMorni-Sandbox, alles per Umgebungsvariable änderbar):
#   GIPFEL_JAVA   Java 25 (Ordner; sonst die JDK 25 aus dem Cache, sonst JAVA_HOME)
#   PAPER_JAR     Paper 26.1.2 (mineflayer kann bis 26.1)
#   TEMPLATE      ein schon einmal gestarteter Paper-26.1.2-Ordner (libraries/, cache/, versions/ werden kopiert) – optional
#   GIPFEL_NODE_MODULES  node_modules mit mineflayer
#
# Verfasst mit Claude 🤖
set -euo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(dirname "$HERE")"
CACHE="${MM_CACHE:-/home/user/mm-cache}"
CACHED_JDK="$(ls -d "$CACHE"/jdk/jdk-25* 2>/dev/null | head -1 || true)"
JAVA_HOME="${GIPFEL_JAVA:-${CACHED_JDK:-${JAVA_HOME:-}}}"
export JAVA_HOME
JAVA="$JAVA_HOME/bin/java"
PAPER_JAR="${PAPER_JAR:-$CACHE/jars/paper-26.1.2.jar}"
TEMPLATE="${TEMPLATE:-$CACHE/servers/t1}"
SERVER="${SERVER_DIR:-$CACHE/servers/gipfelsturm-test}"
PORT="${PORT:-25730}"
RCON_PORT="${RCON_PORT:-25731}"
COMEBACK="${COMEBACK:-20}"
MEMORY="${MEMORY:-2G}"
export NODE_PATH="${GIPFEL_NODE_MODULES:-$CACHE/node/node_modules}${NODE_PATH:+:$NODE_PATH}"

echo "== Bauen"
(cd "$REPO" && nice -n 15 mvn -q -B -DskipTests package)
(cd "$REPO/test/fake-network" && nice -n 15 mvn -q -B package)
FAKE_JAR="$(ls "$REPO"/test/fake-network/target/*.jar | grep -v original | head -1)"

echo "== Server einrichten: $SERVER"
mkdir -p "$SERVER/plugins/Gipfelsturm"
if [ -d "$TEMPLATE" ] && [ ! -d "$SERVER/libraries" ]; then
  cp -r "$TEMPLATE/libraries" "$TEMPLATE/cache" "$TEMPLATE/versions" "$SERVER/" 2>/dev/null || true
fi
cp "$PAPER_JAR" "$SERVER/paper.jar"
echo "eula=true" > "$SERVER/eula.txt"
RCON_PASS="$(head -c 18 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 20)"
printf '%s' "$RCON_PASS" > "$SERVER/.rcon-pass"
chmod 600 "$SERVER/.rcon-pass"
cat > "$SERVER/server.properties" <<EOF
online-mode=false
enforce-secure-profile=false
server-port=$PORT
enable-rcon=true
rcon.port=$RCON_PORT
rcon.password=$RCON_PASS
level-name=world
level-type=minecraft\:flat
generate-structures=false
spawn-protection=0
view-distance=6
simulation-distance=4
max-players=20
white-list=false
enforce-whitelist=false
motd=Gipfelsturm Test
allow-flight=true
EOF
# Testwert: Zurück ins Spiel nach $COMEBACK Sekunden (sonst die mitgelieferten Einstellungen).
CFG="$SERVER/plugins/Gipfelsturm/config.yml"
if [ -f "$CFG" ]; then
  if grep -q '^rueckkehr-sekunden:' "$CFG"; then
    sed -i "s/^rueckkehr-sekunden:.*/rueckkehr-sekunden: $COMEBACK/" "$CFG"
  else
    printf 'rueckkehr-sekunden: %s\n' "$COMEBACK" | cat - "$CFG" > "$CFG.tmp" && mv "$CFG.tmp" "$CFG"
  fi
else
  printf 'rueckkehr-sekunden: %s\n' "$COMEBACK" > "$CFG"
fi
# nie zwei Fassungen im Server: alte Jars (auch unter früheren Namen) weg
rm -f "$SERVER"/plugins/Morni-Gipfelsturm*.jar "$SERVER"/plugins/*Fake*.jar "$SERVER"/plugins/*fake*.jar
cp "$REPO/target/Morni-Gipfelsturm.jar" "$SERVER/plugins/"

cd "$SERVER"
PID=""
start_server() {
  echo "== Server starten (Port $PORT, RCON $RCON_PORT)"
  : > console.log
  nice -n 15 "$JAVA" -Xms256M -Xmx"$MEMORY" -jar paper.jar --nogui < /dev/null > console.log 2>&1 &
  PID=$!
  for _ in $(seq 1 600); do
    if grep -q "RCON running on" console.log && grep -q "Gipfelsturm bereit:" console.log; then break; fi
    if ! kill -0 "$PID" 2>/dev/null; then echo "Server ist abgestürzt:"; tail -40 console.log; exit 1; fi
    sleep 1
  done
  grep -q "RCON running on" console.log || { echo "RCON lauscht nicht:"; tail -40 console.log; exit 1; }
  sleep 3
}
stop_server() {
  if [ -n "$PID" ] && kill -0 "$PID" 2>/dev/null; then
    node -e "
      const net = require('net'); const [h, p] = process.argv[1].split(':'); const pass = require('fs').readFileSync(process.argv[2], 'utf8').trim();
      const pkt = (id, type, body) => { const b = Buffer.from(body, 'utf8'); const out = Buffer.alloc(14 + b.length);
        out.writeInt32LE(10 + b.length, 0); out.writeInt32LE(id, 4); out.writeInt32LE(type, 8); b.copy(out, 12); return out; };
      const s = net.connect(Number(p), h, () => { s.write(pkt(1, 3, pass)); setTimeout(() => s.write(pkt(2, 2, 'stop')), 300);
        setTimeout(() => s.end(), 1500); });
      s.on('error', () => process.exit(0));
    " "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" >/dev/null 2>&1 || kill "$PID" || true
    for _ in $(seq 1 60); do kill -0 "$PID" 2>/dev/null || break; sleep 1; done
    kill -9 "$PID" 2>/dev/null || true
  fi
  PID=""
}
if [ "${KEEP:-0}" != "1" ]; then
  trap stop_server EXIT
fi
errors() {
  if grep -qE "ERROR|Exception" console.log; then
    grep -E "ERROR|Exception" console.log | grep -v "Mojang\|429\|authserver\|sessionserver" | head -20 || echo "keine"
  else
    echo "keine"
  fi
}

STATUS=0
ONLY="${ONLY:-}"
if [ "$ONLY" != "network" ] && [ "${SKIP_TEST:-0}" != "1" ]; then
  echo "== Allein (ohne Netzwerk)"
  start_server
  node "$HERE/bot-test.cjs" 127.0.0.1 "$PORT" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" "$SERVER" || STATUS=$?
  echo "== Fehler im Server-Log:"
  errors
  stop_server
fi
if [ "$ONLY" != "alone" ]; then
  echo "== Mit Fake-Netzwerk"
  cp "$FAKE_JAR" "$SERVER/plugins/"
  start_server
  if [ "${SKIP_TEST:-0}" = "1" ]; then
    echo "Server läuft (PID $PID)."
    exit 0
  fi
  node "$HERE/bot-test.cjs" 127.0.0.1 "$PORT" "127.0.0.1:$RCON_PORT" "$SERVER/.rcon-pass" "$SERVER" --network || STATUS=$?
  echo "== Fehler im Server-Log:"
  errors
  stop_server
  rm -f "$SERVER"/plugins/*Fake*.jar
fi
exit $STATUS
