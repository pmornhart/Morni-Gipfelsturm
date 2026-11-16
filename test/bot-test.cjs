/**
 * Echter Test von Gipfelsturm mit einem Test-Spieler (mineflayer) auf einem lokalen Paper-Server (26.1.2, offline):
 * Basislager → Wanderkarte → leichteste Strecke wählen → echte Sprünge bis zum ersten Checkpoint → Checkpoints per
 * Teleport → Sturz → zurück zum Checkpoint → Ziel → Bestzeit gespeichert → Bestenliste (Befehl und Menü) → Übungsmodus →
 * zu schnelle Zeit wird nicht gewertet → Statistik → Strecke der Woche → Lobby-Gegenstand (ohne Netzwerk: freundlicher
 * Kick).
 *
 *   NODE_PATH=<mineflayer-Ordner>/node_modules node test/bot-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> <server-ordner>
 *
 * Mit Fake-Netzwerk (test/fake-network) zusätzlich: --network (Sprache Englisch, Statistik/Zustand an den „Hub“,
 * Party-Rennen mit zwei Test-Spielern).
 */
const fs = require('node:fs');
const net = require('node:net');
const path = require('node:path');
const crypto = require('node:crypto');
const mineflayer = require('mineflayer');
const nbt = require('prismarine-nbt');
const { Vec3 } = require('vec3');

const args = process.argv.slice(2).filter((a) => !a.startsWith('--'));
const NETWORK = process.argv.includes('--network');
const [HOST = '127.0.0.1', PORT = '25730', RCON = '127.0.0.1:25731', PASS_FILE = '.rcon-pass', SERVER_DIR = '.'] = args;
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const NAME = NETWORK ? 'Gipfelnetz' : 'Gipfeltest';
const VERSION = process.env.MC_VERSION || '26.1';
let weeklyId = null;

const wait = (ms) => new Promise((r) => setTimeout(r, ms));
const results = [];
const check = (name, ok, detail = '') => {
  results.push({ name, ok: !!ok });
  console.log(`${ok ? '✓' : '✗'} ${name}${detail ? ` – ${detail}` : ''}`);
};

// ---------- RCON (Minecraft-Protokoll: Länge, Id, Typ, Text, zwei Nullbytes) ----------
// Lange Antworten kommen in mehreren Paketen (je 4096 Bytes): Nach jedem Befehl schicken wir „list“ hinterher – kommt
// dessen Antwort, ist die des Befehls vollständig.
function rcon(...cmds) {
  const [host, port] = RCON.split(':');
  return new Promise((resolve, reject) => {
    const sock = net.createConnection({ host, port: Number(port) });
    let buf = Buffer.alloc(0);
    const out = [];
    let current = '';
    let i = 0;
    let listSent = false;
    const send = (id, type, body) => {
      const b = Buffer.from(body, 'utf8');
      const p = Buffer.alloc(14 + b.length);
      p.writeInt32LE(10 + b.length, 0);
      p.writeInt32LE(id, 4);
      p.writeInt32LE(type, 8);
      b.copy(p, 12);
      sock.write(p);
    };
    const next = () => {
      if (i >= cmds.length) {
        sock.end();
        resolve(out);
        return;
      }
      current = '';
      listSent = false;
      send(100 + i, 2, cmds[i]);
    };
    sock.on('data', (d) => {
      buf = Buffer.concat([buf, d]);
      while (buf.length >= 4 && buf.length >= buf.readInt32LE(0) + 4) {
        const len = buf.readInt32LE(0);
        const rid = buf.readInt32LE(4);
        const body = buf.subarray(12, 4 + len - 2).toString('utf8');
        buf = buf.subarray(4 + len);
        if (rid === -1) return reject(new Error('RCON: falsches Passwort'));
        if (rid === 1) {
          next();
        } else if (rid === 9999) {
          out.push(current);
          i++;
          next();
        } else {
          current += body;
          // „list“ erst jetzt: Der Server liest jedes Paket einzeln und beendet die Verbindung, wenn zwei auf einmal
          // ankommen (Vanilla-RCON erwartet genau ein Paket pro read()).
          if (!listSent) {
            listSent = true;
            send(9999, 2, 'list');
          }
        }
      }
    });
    sock.on('error', reject);
    sock.on('close', () => reject(new Error('RCON: Verbindung ohne Antwort beendet')));
    sock.on('connect', () => send(1, 3, PASS));
  });
}

/**
 * Text aus Chat-Bausteinen: JSON-Text, mineflayer-ChatMessage oder NBT (ab 1.20.3 schickt der Server Texte als NBT).
 */
function plain(m) {
  if (m == null) return '';
  if (typeof m === 'string') {
    try {
      return plain(JSON.parse(m));
    } catch {
      return m;
    }
  }
  if (typeof m === 'object' && typeof m.type === 'string' && 'value' in m) {
    try {
      return text(nbt.simplify(m));
    } catch {
      /* kein NBT */
    }
  }
  if (typeof m.toString === 'function' && m.toString !== Object.prototype.toString) return m.toString();
  return text(m);
}

function text(c) {
  if (c == null) return '';
  if (typeof c === 'string' || typeof c === 'number') return String(c);
  if (Array.isArray(c)) return c.map(text).join('');
  let s = '';
  if (typeof c.text === 'string') s += c.text;
  if (typeof c[''] === 'string') s += c[''];
  if (typeof c.translate === 'string') s += c.translate + (c.with ? ` ${text(c.with)}` : '');
  if (c.extra) s += text(c.extra);
  return s;
}

/** Teleport in der Gipfelsturm-Welt (RCON läuft sonst in der Hauptwelt). */
const tp = (name, x, y, z) => rcon(`execute in minecraft:gipfelsturm run tp ${name} ${x} ${y} ${z}`);

function offlineUuid(name) {
  const md5 = crypto.createHash('md5').update(`OfflinePlayer:${name}`).digest();
  md5[6] = (md5[6] & 0x0f) | 0x30;
  md5[8] = (md5[8] & 0x3f) | 0x80;
  const h = md5.toString('hex');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

const center = (p) => ({ x: (p.x0 + p.x1 + 1) / 2, y: p.s, z: (p.z0 + p.z1 + 1) / 2 });
const onPad = (pos, p, tol = 0.35) =>
  pos.x > p.x0 - tol && pos.x < p.x1 + 1 + tol && pos.z > p.z0 - tol && pos.z < p.z1 + 1 + tol && Math.abs(pos.y - p.s) < 0.6;

function makeBot(name) {
  const bot = mineflayer.createBot({ host: HOST, port: Number(PORT), username: name, version: VERSION, auth: 'offline' });
  bot.chats = [];
  bot.bars = [];
  bot.titles = [];
  bot.kickReason = null;
  bot.raw = [];
  bot.on('messagestr', (m, pos) => (pos === 'game_info' ? bot.bars : bot.chats).push(m));
  bot.on('message', (m, pos) => pos !== 'game_info' && bot.raw.push(JSON.stringify(m.json)));
  bot.on('actionBar', (m) => bot.bars.push(plain(m)));
  bot.on('title', (t) => bot.titles.push(plain(t)));
  bot._client.on('system_chat', (p) => {
    if (p.isActionBar || p.overlay) bot.bars.push(plain(p.content ?? p));
  });
  bot._client.on('action_bar', (p) => bot.bars.push(plain(p.text ?? p)));
  bot._client.on('set_title_text', (p) => bot.titles.push(plain(p.text ?? p)));
  bot._client.on('set_title_subtitle', (p) => bot.titles.push(plain(p.text ?? p)));
  bot.on('kicked', (r) => {
    bot.kickReason = plain(r);
  });
  bot.on('error', (e) => console.log(`Fehler (${name}):`, e.message));
  return bot;
}

async function spawned(bot) {
  await new Promise((r) => bot.once('spawn', r));
  await wait(2500);
}

const said = (bot, re) => bot.chats.some((c) => re.test(c));
const barSaid = (bot, re) => bot.bars.some((c) => re.test(c));

/** Rechtsklick mit einem Gegenstand der Leiste, Fenster abwarten. */
async function openWith(bot, slot) {
  bot.setQuickBarSlot(slot);
  await wait(300);
  const opened = new Promise((r) => {
    bot.once('windowOpen', r);
    setTimeout(() => r(null), 4000);
  });
  bot.activateItem();
  return opened;
}

/** Echter Sprung zum nächsten Pad: hinschauen, laufen (sprinten), an der Kante springen, auf dem Ziel anhalten. */
async function jumpTo(bot, from, to, sprint) {
  const target = center(to);
  for (let t = 0; t < 80; t++) {
    const p = bot.entity.position;
    if (bot.entity.onGround && onPad(p, to, 0.25)) {
      bot.clearControlStates();
      await bot.waitForTicks(8);
      return onPad(bot.entity.position, to, 0.3);
    }
    await bot.lookAt(new Vec3(target.x, p.y + 1.62, target.z), true);
    bot.setControlState('forward', true);
    bot.setControlState('sprint', sprint);
    // Springen, sobald vor den Füßen Luft ist (Kante des aktuellen Pads)
    const yaw = bot.entity.yaw;
    const ahead = p.offset(-Math.sin(yaw) * 0.55, -0.4, -Math.cos(yaw) * 0.55);
    const below = bot.blockAt(ahead);
    const edge = bot.entity.onGround && (!below || below.boundingBox === 'empty');
    bot.setControlState('jump', edge);
    await bot.waitForTicks(1);
  }
  bot.clearControlStates();
  return false;
}

/** Netzwerk-Teil (nur mit Fake-Netzwerk): Statistik, Zustand, Sprache wechseln, Party-Rennen. */
async function network(bot, calls) {
  await wait(1000);
  const log = fs.readFileSync(calls, 'utf8');
  const lines = log.split('\n').filter((l) => l.includes(NAME) || l.startsWith('state'));
  check('Statistik: Versuch gemeldet (add attempts=1, ohne Bestwert)',
    lines.some((l) => l.includes('add={attempts=1} best={} lower=true')), lines.find((l) => l.includes('attempts')));
  const counters = lines.find((l) => l.includes('finishes_almwiese=1'));
  check('Statistik: Zähler im Ziel (finishes, finishes_almwiese, falls) ohne Bestwert',
    counters && counters.includes('finishes=1') && counters.includes('falls=1') && counters.includes('best={}'), counters);
  const time = lines.find((l) => l.includes('time_almwiese_ms='));
  check('Statistik: Bestzeit getrennt gemeldet (time_almwiese_ms, kleiner ist besser)',
    time && time.includes('add={}') && time.includes('lower=true'), time);
  const timed = 3 + (weeklyId === 'almwiese' ? 1 : 0);
  check(`Statistik: jede gewertete Zeit gemeldet (${timed}), Übungs- und zu schnelle Läufe ohne Zeit`, lines.filter((l) => l.includes('time_almwiese_ms=')).length === timed);
  check('Zustand gemeldet: state gipfelsturm open <spieler>', log.split('\n').some((l) => /^state gipfelsturm open [12]$/.test(l)),
    log.split('\n').filter((l) => l.startsWith('state')).slice(-1)[0]);

  // Sprache wechseln (wie /sprache im Netzwerk): Leiste kommt in der neuen Sprache
  await rcon(`fakenet lang ${NAME} de`);
  await wait(2000);
  const de = plain(bot.inventory.slots[36]?.customName);
  await rcon(`fakenet lang ${NAME} en`);
  await wait(2000);
  const en = plain(bot.inventory.slots[36]?.customName);
  check('Sprache wechselt live (Leiste Deutsch ↔ Englisch)', /Wanderkarte/.test(de) && /Trail map/.test(en), `${de} / ${en}`);

  // Party-Rennen: zweiter Test-Spieler, der Anführer startet, der Freund macht mit
  const FRIEND = 'Gipfelfreund';
  await rcon(`fakenet lang ${FRIEND} de`, `fakenet party ${NAME} ${FRIEND}`);
  const friend = makeBot(FRIEND);
  await spawned(friend);
  bot.chats.length = 0;
  bot.titles.length = 0;
  friend.chats.length = 0;
  friend.titles.length = 0;
  bot.chat('/gipfel start almwiese');
  await wait(2500);
  check('Rennen: Freund bekommt die Einladung', said(friend, /startet ein Rennen/) && friend.inventory.slots[36 + 4]?.name === 'firework_rocket',
    friend.chats.filter((c) => /Rennen/.test(c))[0]);
  check('Rennen: Anführer wartet auf die Party', said(bot, /Waiting for your party/), bot.chats.filter((c) => /party/i.test(c))[0]);
  friend.chat('/gipfel mitmachen');
  await wait(1500);
  check('Rennen: Freund ist dabei (am Start)', said(bot, /joined/) && Math.abs(friend.entity.position.x - 501.5) < 1.2,
    `${friend.entity.position}`);
  // Countdown 3-2-1 und Startschuss
  await wait(4500);
  check('Rennen: Countdown und „Los!“', bot.titles.some((t) => /^Go!$/.test(t.trim())) && friend.titles.some((t) => /Los!/.test(t)),
    `${bot.titles.join(',')} | ${friend.titles.join(',')}`);
  // Die Uhr läuft für beide, auch ohne Bewegung
  friend.bars.length = 0;
  await wait(1200);
  const raceBar = friend.bars.filter((b) => b.includes('⏱')).pop() || '';
  const raceMs = (raceBar.match(/⏱ (\d+):(\d\d)[,.](\d{3})/) || []).slice(1).map(Number);
  check('Rennen: Zeit läuft für alle ab dem Startschuss (auch ohne Bewegung)', raceMs.length === 3 && raceMs[1] >= 1, raceBar);
  const padsJson = (await rcon('gipfel pads almwiese'))[0];
  const course = JSON.parse(padsJson.slice(padsJson.indexOf('{')));
  for (const cp of course.pads.filter((p) => p.role === 'CHECKPOINT')) {
    await wait(2600);
    await tp(NAME, center(cp).x, cp.s, center(cp).z);
  }
  await wait(2600);
  const fin = course.pads[course.pads.length - 1];
  await tp(NAME, center(fin).x, fin.s + 0.5, center(fin).z);
  await wait(1500);
  check('Rennen: Platz 1 wird allen gemeldet', said(friend, new RegExp(`Rennen: ${NAME} ist im Ziel .*Platz 1`)),
    friend.chats.filter((c) => /Rennen/.test(c)).pop());
  friend.chat('/gipfel aufgeben');
  await wait(1500);
  check('Rennen vorbei: Sieger steht fest', said(bot, new RegExp(`Race over! Winner: ${NAME}`)), bot.chats.filter((c) => /Race/.test(c)).pop());
  friend.quit();
  await wait(1000);
}

async function main() {
  const uuid = offlineUuid(NAME);
  // Saubere Ausgangslage: alte Zeiten dieses Test-Spielers löschen
  // alle Test-Spieler (auch aus dem anderen Durchlauf), sonst stehen sie auf der Wochen-Bestenliste vor uns
  await rcon('gipfel reset Gipfeltest', 'gipfel reset Gipfelnetz', 'gipfel reset Gipfelfreund').catch(() => {});
  const padsJson = (await rcon('gipfel pads almwiese'))[0];
  const course = JSON.parse(padsJson.slice(padsJson.indexOf('{')));
  const pads = course.pads;
  const cps = pads.filter((p) => p.role === 'CHECKPOINT');
  const start = pads[0];
  const finish = pads[pads.length - 1];

  const calls = path.join(SERVER_DIR, 'plugins', 'MoinMorni', 'calls.log');
  if (NETWORK) {
    // Fake-Netzwerk: Englisch für den Test-Spieler, Aufruf-Protokoll leeren
    if (fs.existsSync(calls)) fs.writeFileSync(calls, '');
    await rcon('fakenet clear', `fakenet lang ${NAME} en`);
  }
  const bot = makeBot(NAME);
  await spawned(bot);

  // ---------- Basislager ----------
  const pos = bot.entity.position;
  check('Ankunft im Basislager', Math.abs(pos.x - 0.5) < 1.5 && Math.abs(pos.y - 100) < 1 && Math.abs(pos.z - 10.5) < 1.5,
    `${pos.x.toFixed(1)} ${pos.y.toFixed(1)} ${pos.z.toFixed(1)}`);
  check('Abenteuermodus', bot.game.gameMode === 'adventure', bot.game.gameMode);
  const hot = [0, 1, 4, 7, 8].map((i) => bot.inventory.slots[36 + i]?.name);
  check('Leiste im Basislager: Karte, Gipfelbuch, Woche, Mitspieler, Lobby',
    hot[0] === 'map' && hot[1] === 'book' && hot[2] === 'nether_star' && hot[3] === 'lime_dye' && hot[4] === 'dark_oak_door',
    hot.join(','));
  await wait(1500);
  const welcome = NETWORK ? /base camp/ : /Basislager/;
  check('Begrüßung im Chat' + (NETWORK ? ' (Englisch)' : ''), said(bot, welcome), bot.chats.slice(-2).join(' | '));

  // ---------- Menü: Strecke wählen ----------
  const win = await openWith(bot, 0);
  const items = win ? win.slots.slice(0, 54).filter(Boolean) : [];
  const courseItems = items.filter((i) => [11, 13, 20, 22, 29, 31, 38, 40].includes(i.slot));
  check('Wanderkarte öffnet „Strecke wählen“ mit 8 Strecken in 4 Stufen', courseItems.length === 8,
    `${courseItems.length} Strecken, ${items.length} Felder`);
  const label = (it) => (it ? plain(it.customName) || it.displayName : '');
  const rows = [9, 18, 27, 36].map((s) => label(items.find((i) => i.slot === s)));
  check('Stufen-Zeilen Leicht/Mittel/Schwer/Extrem', /Leicht|Easy/.test(rows[0]) && /Extrem/.test(rows[3]), rows.join(' / '));
  const easy = items.find((i) => i.slot === 11);
  check('Erste Strecke ist der Almwiesen-Lauf', /Almwiesen|Meadow/.test(label(easy)), label(easy));
  bot.chats.length = 0;
  await bot.clickWindow(11, 0, 0);
  await wait(2500);
  const s = center(start);
  const p0 = bot.entity.position;
  check('Klick → am Start (Smaragd)', Math.abs(p0.x - s.x) < 0.8 && Math.abs(p0.z - s.z) < 0.8 && Math.abs(p0.y - s.y) < 0.6,
    `${p0.x.toFixed(2)} ${p0.y.toFixed(2)} ${p0.z.toFixed(2)} (Start ${s.x} ${s.y} ${s.z})`);
  const runHot = [0, 1, 8].map((i) => bot.inventory.slots[36 + i]?.name);
  check('Leiste unterwegs: Checkpoint, Neustart, Aufgeben',
    runHot[0] === 'light_weighted_pressure_plate' && runHot[1] === 'emerald' && runHot[2] === 'barrier', runHot.join(','));
  check('Hinweis „Lauf los …“', barSaid(bot, /Lauf los|Go when/), bot.bars.slice(-1)[0]);

  // ---------- Echte Sprünge bis zum ersten Checkpoint ----------
  // Die Welt entsteht beim ersten Besuch: warten, bis die Blöcke der Strecke beim Test-Spieler angekommen sind.
  const cp1Index = pads.indexOf(cps[0]);
  for (let t = 0; t < 40; t++) {
    const missing = pads.slice(0, cp1Index + 1).filter((p) => {
      const b = bot.blockAt(new Vec3(p.x0, p.y, p.z0));
      return !b || b.name === 'air';
    });
    if (!missing.length) break;
    await wait(250);
  }
  await wait(500);
  bot.bars.length = 0;
  let real = 0;
  const cp1 = cp1Index;
  for (let i = 1; i <= cp1; i++) {
    const ok = await jumpTo(bot, pads[i - 1], pads[i], true);
    if (!ok) {
      console.log(`  Sprung ${i - 1}→${i} nicht geschafft bei ${bot.entity.position}`);
      break;
    }
    real++;
  }
  check('Zeit läuft nach dem Verlassen des Starts (⏱ mit Millisekunden)', barSaid(bot, /⏱ \d+:\d\d[,.]\d{3}/),
    bot.bars.find((b) => b.includes('⏱')));
  check(`Echte Sprünge Start → Checkpoint 1 (${cp1} Sprünge)`, real === cp1, `${real}/${cp1}`);
  if (real < cp1) {
    await tp(NAME, center(cps[0]).x, cps[0].s, center(cps[0]).z);
    await wait(1500);
  }
  check('Checkpoint 1 erreicht (Zwischenzeit im Chat)', said(bot, /Checkpoint 1\/3/), bot.chats.filter((c) => /Checkpoint/.test(c)).pop());

  // ---------- Sturz → zurück zum Checkpoint ----------
  bot.bars.length = 0;
  const c1 = center(cps[0]);
  await tp(NAME, c1.x + 3, course.fall[1] - 4, c1.z);
  await wait(2000);
  const back = bot.entity.position;
  check('Sturz unter die Fallgrenze → zurück zu Checkpoint 1',
    Math.abs(back.x - c1.x) < 0.8 && Math.abs(back.z - c1.z) < 0.8 && Math.abs(back.y - c1.y) < 0.6,
    `${back.x.toFixed(2)} ${back.y.toFixed(2)} ${back.z.toFixed(2)}; ${bot.bars.filter((b) => /Zurück|Back/.test(b))[0]}`);

  // ---------- Checkpoint auslassen geht nicht ----------
  bot.bars.length = 0;
  await tp(NAME, center(cps[2]).x, cps[2].s, center(cps[2]).z);
  await wait(1500);
  check('Checkpoint 3 vor 2 zählt nicht (Hinweis)', barSaid(bot, /erst Checkpoint 2|checkpoint 2 first/i) && !said(bot, /Checkpoint 3\/3/),
    bot.bars.filter((b) => /Checkpoint/.test(b))[0]);

  // ---------- Restliche Checkpoints und Ziel (mit Pausen, damit die Zeit glaubhaft ist) ----------
  for (const cp of cps.slice(1)) {
    await wait(2500);
    const c = center(cp);
    await tp(NAME, c.x, cp.s, c.z);
    await wait(1200);
  }
  check('Checkpoints 2 und 3', said(bot, /Checkpoint 2\/3/) && said(bot, /Checkpoint 3\/3/));
  await wait(2500);
  bot.titles.length = 0;
  bot.chats.length = 0;
  const f = center(finish);
  await tp(NAME, f.x, finish.s + 0.5, f.z);
  await wait(2500);
  check('Ziel: Titel „Geschafft!“', bot.titles.some((t) => /Geschafft|made it/.test(t)), bot.titles.join(' | '));
  const firstLine = bot.chats.find((c) => /erste Zeit|first time/.test(c));
  check('Ziel: erste Zeit im Chat', !!firstLine, firstLine);
  const time = (firstLine || '').match(/(\d+):(\d\d)[,.](\d{3})/);
  const ms = time ? (Number(time[1]) * 60 + Number(time[2])) * 1000 + Number(time[3]) : 0;
  check('Zeit glaubhaft (über der Mindestzeit)', ms > course.plausible, `${ms} ms, Mindestzeit ${course.plausible} ms`);

  // ---------- Gespeichert? ----------
  await wait(1500);
  const file = path.join(SERVER_DIR, 'plugins', 'Gipfelsturm', 'spieler', `${uuid}.yml`);
  const yml = fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '';
  const best = Number((yml.match(/almwiese:\s*\n\s*best: (\d+)/) || [])[1] || 0);
  check('Bestzeit in der Spielerdatei gespeichert', best === ms && best > 0, `${file.split('/').slice(-2).join('/')}: best ${best}`);
  check('Zwischenzeiten gespeichert (3)', /splits:\n(\s*- \d+\n){3}/.test(yml));
  const ghost = path.join(SERVER_DIR, 'plugins', 'Gipfelsturm', 'geister', uuid, 'almwiese.bin');
  check('Geist der Bestzeit gespeichert', fs.existsSync(ghost) && fs.statSync(ghost).size > 20, ghost.split('/').slice(-2).join('/'));

  // ---------- Bestenliste: Befehl und Menü ----------
  bot.chats.length = 0;
  bot.chat('/gipfel top almwiese');
  await wait(1500);
  const topLine = bot.chats.find((c) => new RegExp(`\\d+\\. ${NAME} · \\d+:\\d\\d[,.]\\d{3}`).test(c));
  check('/gipfel top almwiese zeigt den Test-Spieler mit Platz und Zeit', !!topLine, topLine || bot.chats.slice(0, 3).join(' | '));
  const book = await openWith(bot, 2);
  if (book) {
    await bot.clickWindow(10, 0, 0);
    await wait(1200);
    const topWin = bot.currentWindow;
    const entries = [4, 12, 13, 14, 20, 21, 22, 23, 24, 31].map((i) => label(topWin?.slots?.[i])).filter(Boolean);
    const mine = entries.find((e) => new RegExp(`#\\d+ ${NAME}`).test(e));
    check('Gipfelbuch → Bestenliste der Strecke enthält den Test-Spieler', !!mine, mine || entries.join(', '));
    bot.closeWindow(topWin || book);
  } else {
    check('Gipfelbuch öffnet sich', false);
  }
  await wait(500);

  // ---------- Übungsmodus: Zeit zählt nicht ----------
  bot.chats.length = 0;
  bot.chat('/gipfel uebung');
  await wait(1200);
  check('/gipfel uebung schaltet den Übungsmodus an', said(bot, /Übungsmodus an|Practice mode on/));
  bot.chat('/gipfel start almwiese');
  await wait(2000);
  const hasPoint = bot.inventory.slots[36 + 4]?.name === 'lodestone';
  check('Übung: Leitstein „Übungs-Punkt setzen“ in der Leiste', hasPoint, bot.inventory.slots[36 + 4]?.name);
  await tp(NAME, center(pads[1]).x, pads[1].s, center(pads[1]).z);
  await wait(1200);
  bot.setQuickBarSlot(4);
  await wait(300);
  bot.activateItem();
  await wait(800);
  check('Übungs-Punkt gesetzt', barSaid(bot, /Übungs-Punkt gesetzt|Practice point set/));
  await tp(NAME, center(pads[1]).x, course.fall[0] - 4, center(pads[1]).z);
  await wait(1500);
  const pp = bot.entity.position;
  check('Übung: Sturz → zurück zum eigenen Punkt', onPad(pp, pads[1], 0.6), `${pp.x.toFixed(2)} ${pp.y.toFixed(2)} ${pp.z.toFixed(2)}`);
  for (const cp of cps) {
    await tp(NAME, center(cp).x, cp.s, center(cp).z);
    await wait(700);
  }
  await tp(NAME, f.x, finish.s + 0.5, f.z);
  await wait(1500);
  check('Übungs-Lauf zählt nicht', said(bot, /Übungs-Lauf|Practice run/), bot.chats.filter((c) => /Übung|Practice/.test(c)).pop());
  bot.chat('/gipfel uebung');
  await wait(800);

  // ---------- Zu schnell (Teleport ohne Pause) zählt nicht ----------
  bot.chats.length = 0;
  bot.chat('/gipfel start almwiese');
  await wait(1500);
  await tp(NAME, center(pads[1]).x, pads[1].s, center(pads[1]).z);
  await wait(300);
  for (const cp of cps) {
    await tp(NAME, center(cp).x, cp.s, center(cp).z);
    await wait(300);
  }
  await tp(NAME, f.x, finish.s + 0.5, f.z);
  await wait(1500);
  check('Unmöglich schnelle Zeit wird nicht gewertet', said(bot, /schneller als möglich|faster than possible/),
    bot.chats.filter((c) => /schnell|fast/.test(c)).pop());
  const yml2 = fs.readFileSync(file, 'utf8');
  check('Bestzeit bleibt unverändert', Number((yml2.match(/almwiese:\s*\n\s*best: (\d+)/) || [])[1] || 0) === best);

  // ---------- Statistik, Woche, Mitspieler ----------
  bot.chats.length = 0;
  bot.chat('/gipfel stats');
  await wait(1200);
  check('/gipfel stats (1× im Ziel, 2 Versuche, 1 Sturz)', said(bot, /Im Ziel: 1 · Versuche: 2 · Stürze: 1|Finishes: 1 · Attempts: 2 · Falls: 1/),
    bot.chats.slice(0, 3).join(' | '));
  bot.chat('/gipfel woche');
  await wait(1200);
  check('/gipfel woche nennt die Strecke der Woche', said(bot, /Strecke der Woche|Course of the week/),
    bot.chats.filter((c) => /Woche|week/.test(c)).pop());
  bot.chats.length = 0;
  bot.chat('/gipfel basislager');
  await wait(1500);
  bot.setQuickBarSlot(7);
  await wait(300);
  bot.activateItem();
  await wait(1000);
  check('Mitspieler ausblenden (Gegenstand)', said(bot, /ausgeblendet|hidden/) && bot.inventory.slots[36 + 7]?.name === 'gray_dye',
    bot.inventory.slots[36 + 7]?.name);
  bot.activateItem();
  await wait(800);

  // ---------- Zwischenzeiten gegen die Bestzeit: schneller (grün, neue Bestzeit), dann langsamer (rot) ----------
  /** Lauf per Teleport über alle Checkpoints ins Ziel, mit Pause pro Abschnitt (Zeit soll glaubhaft sein). */
  async function teleportRun(c, pause) {
    bot.chats.length = 0;
    bot.raw.length = 0;
    bot.titles.length = 0;
    bot.chat(`/gipfel start ${c.id}`);
    await wait(1500);
    const ps = c.pads;
    await tp(NAME, center(ps[1]).x, ps[1].s, center(ps[1]).z);
    for (const cp of ps.filter((p) => p.role === 'CHECKPOINT')) {
      await wait(pause);
      await tp(NAME, center(cp).x, cp.s, center(cp).z);
    }
    await wait(pause);
    const fin = ps[ps.length - 1];
    await tp(NAME, center(fin).x, fin.s + 0.5, center(fin).z);
    await wait(2000);
  }
  const deltaColours = () => bot.raw.filter((r) => /Checkpoint \d\/\d/.test(r)).map((r) => ((r.match(/"color":"(#5fd068|#ff6b5b)"/i) || [])[1] || '').toLowerCase());
  await teleportRun(course, 2600);
  const pbLine = bot.chats.find((c) => /Neue Bestzeit|New personal best/.test(c));
  const greens = deltaColours();
  check('Schnellerer Lauf: Zwischenzeiten grün mit „−“, „Neue Bestzeit!“ mit Abstand', !!pbLine && greens.length === 3 &&
    greens.every((g) => g === '#5fd068') && bot.chats.filter((c) => /Checkpoint \d\/3 .*\(−/.test(c)).length === 3,
    `${pbLine} · ${greens.join(',')} · ${bot.raw.find((r) => /Checkpoint/.test(r))?.slice(0, 400)}`);
  const pbMs = (() => {
    const m = (pbLine || '').match(/(\d+):(\d\d)[,.](\d{3})/);
    return m ? (Number(m[1]) * 60 + Number(m[2])) * 1000 + Number(m[3]) : 0;
  })();
  check('Medaille passt zur Zeit (Titel)', bot.titles.some((t) => {
    const want = pbMs <= course.medals.gipfel ? /Gipfel|Summit/ : pbMs <= course.medals.gold ? /Gold/ : pbMs <= course.medals.silber ? /Silber|Silver/ : /Bronze/;
    return want.test(t);
  }), `${pbMs} ms · ${bot.titles.join(' | ')} · Grenzen ${JSON.stringify(course.medals)}`);
  await teleportRun(course, 3600);
  const reds = deltaColours();
  const nopb = bot.chats.find((c) => /deine Bestzeit bleibt|your best stays/.test(c));
  check('Langsamerer Lauf: Zwischenzeiten rot mit „+“, Bestzeit bleibt', !!nopb && reds.length === 3 && reds.every((g) => g === '#ff6b5b'),
    `${nopb} · ${reds.join(',')}`);
  const yml3 = fs.readFileSync(file, 'utf8');
  check('Neue Bestzeit gespeichert', Number((yml3.match(/almwiese:\s*\n\s*best: (\d+)/) || [])[1] || 0) === pbMs, String(pbMs));

  // ---------- Tafel im Basislager zeigt die Bestzeit ----------
  bot.chat('/gipfel basislager');
  await wait(3000);
  const boards = Object.values(bot.entities).filter((e) => e.name === 'text_display')
    .map((e) => (e.metadata || []).map((m) => (m && typeof m === 'object' ? plain(m) : '')).join(' '));
  const board = boards.find((b) => /Almwiesen|Meadow/.test(b) && b.includes(NAME));
  check('Bestenliste-Tafel im Basislager zeigt den Test-Spieler', !!board, (board || boards.find((b) => /Almwiesen|Meadow/.test(b)) || `${boards.length} Tafeln`).replace(/\n/g, ' ').slice(0, 160));

  // ---------- Strecke der Woche: Stern → Start, Ziel → Wochen-Gipfel ----------
  bot.chats.length = 0;
  bot.setQuickBarSlot(4);
  await wait(300);
  bot.activateItem();
  await wait(2500);
  const wjson = (await rcon('gipfel woche'))[0];
  const wid = ['almwiese', 'gletscher', 'kletterwand', 'felsgrat', 'seilbahn', 'eishoehle', 'gipfelkreuz', 'wetterturm'];
  const wpads = {};
  for (const id of wid) {
    const j = (await rcon(`gipfel pads ${id}`))[0];
    wpads[id] = JSON.parse(j.slice(j.indexOf('{')));
  }
  const weekly = Object.values(wpads).find((c) => onPad(bot.entity.position, c.pads[0], 0.6));
  weeklyId = weekly?.id;
  check('Stern „Strecke der Woche“ startet die Strecke der Woche', !!weekly && wjson.includes(weekly.name) , `${weekly?.id} · ${wjson.split('\n')[0]}`);
  if (weekly) {
    // Wochen-Lauf: Pausen nach Länge der Strecke, damit die Zeit glaubhaft ist
    await teleportRun(weekly, Math.max(2600, Math.ceil(weekly.plausible / (weekly.pads.filter((p) => p.role === 'CHECKPOINT').length + 1)) + 800));
    check('Strecke der Woche geschafft → „▲ Wochen-Gipfel!“ und Wochen-Platz', said(bot, /Wochen-Gipfel!|Weekly Summit!/) && said(bot, /Wochenbeste .*Platz 1|Weekly best .*rank 1/),
      bot.chats.filter((c) => /Woche|week/i.test(c)).join(' | '));
    const tab = bot.players[NAME]?.displayName ? plain(bot.players[NAME].displayName) : '';
    await wait(6000);
    const tab2 = bot.players[NAME]?.displayName ? plain(bot.players[NAME].displayName) : tab;
    check('Abzeichen ▲ in der Tab-Liste', /▲/.test(tab2), tab2);
    bot.chats.length = 0;
    bot.chat('/gipfel woche');
    await wait(1200);
    check('/gipfel woche zeigt die Wochen-Bestenliste mit dem Test-Spieler', said(bot, new RegExp(`1\\. ${NAME}`)), bot.chats.join(' | ').slice(0, 200));
  }
  bot.chat('/gipfel basislager');
  await wait(1500);

  // ---------- Kein Schaden, kein Hunger ----------
  await rcon(`damage ${NAME} 6`);
  await wait(800);
  check('Kein Schaden', bot.health >= 20, String(bot.health));

  if (NETWORK) {
    await network(bot, calls);
  }

  // ---------- Lobby ----------
  bot.chat('/gipfel basislager');
  await wait(1500);
  bot.setQuickBarSlot(8);
  await wait(300);
  bot.activateItem();
  await wait(2500);
  if (NETWORK) {
    const log = fs.readFileSync(calls, 'utf8');
    check('Lobby-Gegenstand → Netzwerk „toLobby“', log.includes(`toLobby ${NAME}`) && /in die Lobby/.test(bot.kickReason || ''),
      bot.kickReason);
  } else {
    check('Lobby ohne Netzwerk: freundlicher Kick', /Danke fürs Klettern|Thanks for climbing/.test(bot.kickReason || ''),
      bot.kickReason);
  }

  const ok = results.filter((r) => r.ok).length;
  console.log(`\n${ok}/${results.length} bestanden`);
  try {
    bot.quit();
  } catch {
    /* schon weg */
  }
  await wait(500);
  process.exit(ok === results.length ? 0 : 1);
}

// Hängt etwas, ohne dass noch eine Verbindung offen ist, würde node still mit 0 enden – das wäre ein falsches Grün.
process.on('beforeExit', () => {
  console.error('Test endet unerwartet (nichts mehr offen) – gilt als Fehlschlag.');
  process.exit(1);
});

main().catch((err) => {
  console.error(err);
  process.exit(1);
});
