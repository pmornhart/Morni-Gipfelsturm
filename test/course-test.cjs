/**
 * Echter Lauf über alle Strecken mit einem Test-Spieler (mineflayer, nachgebaute Vanilla-Bewegung): Jede Strecke wird
 * gestartet und Pad für Pad mit echten Sprüngen, Leitern und Schleim-Sprüngen gelaufen. Schafft der Test-Spieler einen
 * Übergang nicht (oder ist es ein Neo, den die einfache Steuerung nicht kann), wird er auf den nächsten Pad gesetzt und
 * es geht weiter. Am Ende: pro Strecke, wie viele Übergänge echt geschafft wurden, ob alle Checkpoints und das Ziel
 * gezählt haben.
 *
 *   NODE_PATH=<mineflayer-Ordner>/node_modules node test/course-test.cjs <host> <port> <rcon-host:port> <rcon-passwort-datei> [strecke…]
 *
 * Achtung: Die Steuerung ist absichtlich einfach (anlaufen, an der Kante springen, in der Luft zum Ziel lenken). Ein
 * nicht geschaffter Sprung heißt nicht, dass er unmöglich ist – dafür gibt es die Sprung-Regeln und JumpPhysicsTest.
 * Der Test zeigt, dass die Strecken in der echten Welt so stehen, wie der Bauplan sagt, und echt laufbar sind.
 */
const fs = require('node:fs');
const net = require('node:net');
const mineflayer = require('mineflayer');
const { Vec3 } = require('vec3');

const args = process.argv.slice(2);
const [HOST = '127.0.0.1', PORT = '25730', RCON = '127.0.0.1:25731', PASS_FILE = '.rcon-pass', ...ONLY] = args;
const PASS = fs.readFileSync(PASS_FILE, 'utf8').trim();
const NAME = 'Gipfellaeufer';
const VERSION = process.env.MC_VERSION || '26.1';
const COURSES = ONLY.length
  ? ONLY
  : ['almwiese', 'gletscher', 'kletterwand', 'felsgrat', 'seilbahn', 'eishoehle', 'gipfelkreuz', 'wetterturm'];

const wait = (ms) => new Promise((r) => setTimeout(r, ms));

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
        if (rid === 1) next();
        else if (rid === 9999) {
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

const tp = (x, y, z, yaw = 0) => rcon(`execute in minecraft:gipfelsturm run tp ${NAME} ${x} ${y} ${z} ${yaw} 0`);
const center = (p) => ({ x: (p.x0 + p.x1 + 1) / 2, y: p.s, z: (p.z0 + p.z1 + 1) / 2 });
const inRect = (pos, p, tol) => pos.x > p.x0 - tol && pos.x < p.x1 + 1 + tol && pos.z > p.z0 - tol && pos.z < p.z1 + 1 + tol;
const onPad = (pos, p, tol = 0.3) => inRect(pos, p, tol) && Math.abs(pos.y - p.s) < 0.6;
const DIRS = { EAST: [1, 0], SOUTH: [0, 1], WEST: [-1, 0], NORTH: [0, -1] };

/** Wie weit ist der Punkt (Füße) in Richtung d über den Rand des Pads hinaus? (negativ = noch drauf) */
function pastEdge(pos, pad, dx, dz) {
  const lim = [];
  if (dx > 0) lim.push(pos.x - (pad.x1 + 1));
  if (dx < 0) lim.push(pad.x0 - pos.x);
  if (dz > 0) lim.push(pos.z - (pad.z1 + 1));
  if (dz < 0) lim.push(pad.z0 - pos.z);
  return Math.max(...lim);
}

async function main() {
  const bot = mineflayer.createBot({ host: HOST, port: Number(PORT), username: NAME, version: VERSION, auth: 'offline' });
  bot.chats = [];
  bot.bars = [];
  bot.on('messagestr', (m, pos) => (pos === 'game_info' ? bot.bars : bot.chats).push(m));
  bot.on('error', (e) => console.log('Fehler:', e.message));
  await new Promise((r) => bot.once('spawn', r));
  await wait(2500);
  await rcon(`gipfel reset ${NAME}`).catch(() => {});

  /** Auf der Stelle stehen bleiben, bis man ruhig steht. */
  async function settle() {
    bot.clearControlStates();
    for (let t = 0; t < 30; t++) {
      await bot.waitForTicks(1);
      const v = bot.entity.velocity;
      if (bot.entity.onGround && Math.hypot(v.x, v.z) < 0.003) break;
    }
  }

  /** Auf dem aktuellen Pad zum Punkt (x, z) laufen (für Anlauf). */
  async function walkTo(x, z, pad) {
    for (let t = 0; t < 60; t++) {
      const p = bot.entity.position;
      if (Math.hypot(p.x - x, p.z - z) < 0.2 || !onPad(p, pad, 0.1)) break;
      await bot.lookAt(new Vec3(x, p.y + 1.62, z), true);
      bot.setControlState('forward', true);
      bot.setControlState('sprint', false);
      await bot.waitForTicks(1);
    }
    await settle();
  }

  /**
   * Ein Übergang from → to. Rückgabe true, wenn der Test-Spieler danach ruhig auf {@code to} steht.
   */
  async function transition(from, to) {
    const [dx, dz] = DIRS[to.dir] || [0, 0];
    const tc = center(to);
    const fc = center(from);
    // Richtung zum Ziel (für schräge Sprünge) – Kante in Laufrichtung des Abschnitts
    const ex = tc.x - fc.x;
    const ez = tc.z - fc.z;
    const len = Math.hypot(ex, ez) || 1;
    const ux = ex / len;
    const uz = ez / len;
    // Anlauf: an den hinteren Rand des aktuellen Pads (längs der Sprungrichtung)
    const back = {
      x: Math.min(from.x1 + 0.7, Math.max(from.x0 + 0.3, fc.x - ux * 10)),
      z: Math.min(from.z1 + 0.7, Math.max(from.z0 + 0.3, fc.z - uz * 10)),
    };
    // Niedrige Decke über dem Ziel („Kopfstoß“): sprinten und dauernd springen – der Sprung-Schwung trägt über die Lücke
    const roof = (pad) => [3, 4].some((h) => {
      const blk = bot.blockAt(new Vec3(pad.x0, pad.y + h, pad.z0));
      return blk && blk.boundingBox !== 'empty';
    });
    const headHitter = to.kind === 'JUMP' && (roof(to) || roof(from));
    if ((to.kind === 'JUMP' && !headHitter) || to.kind === 'BOUNCE') await walkTo(back.x, back.z, from);
    if (to.kind === 'LADDER' && to.ladder.length) {
      // Zur Leiter laufen und hinaufklettern
      const l = to.ladder[0];
      const track = [];
      for (let t = 0; t < 200; t++) {
        const p = bot.entity.position;
        if (t % 5 === 0) track.push(`${p.x.toFixed(1)},${p.y.toFixed(1)},${p.z.toFixed(1)}`);
        if (bot.entity.onGround && onPad(p, to, 0.2)) break;
        const top = p.y > to.s - 0.2;
        const aim = top ? tc : { x: l[0] + 0.5, z: l[2] + 0.5 };
        // Leiter: geradeaus in die Leiter hinein (gegen die Wand drücken = klettern), oben auf den Pad
        const wall = { x: aim.x + (DIRS[to.dir]?.[0] ?? 0), z: aim.z + (DIRS[to.dir]?.[1] ?? 0) };
        const look = top ? tc : wall;
        await bot.lookAt(new Vec3(look.x, p.y + 1.62, look.z), true);
        bot.setControlState('forward', true);
        bot.setControlState('jump', !top);
        await bot.waitForTicks(1);
      }
      await settle();
      const q = bot.entity.position;
      if (process.env.DEBUG && !onPad(q, to)) console.log(`   ${to.i} Leiter: bei ${q.x.toFixed(2)} ${q.y.toFixed(2)} ${q.z.toFixed(2)}, Leiter ${JSON.stringify(to.ladder)} ${bot.blockAt(new Vec3(l[0], l[1], l[2]))?.name}/${JSON.stringify(bot.blockAt(new Vec3(l[0], l[1], l[2]))?.getProperties?.())} daneben ${bot.blockAt(new Vec3(l[0] + 1, l[1], l[2]))?.name} ${bot.blockAt(new Vec3(l[0] - 1, l[1], l[2]))?.name} ${to.dir} · ${bot.bars.slice(-2).join(' | ')} · ${track.join(' ')}`);
      return onPad(q, to, 0.31);
    }
    if (to.kind === 'NEO') return false;
    // Wie im Spiel: zum Ziel schauen, (sprintend) laufen, an der Kante springen, auf dem Ziel anhalten
    // echte Lücke zwischen den Rändern (nicht von Mitte zu Mitte)
    const gx = Math.max(0, to.x0 - (from.x1 + 1), from.x0 - (to.x1 + 1));
    const gz = Math.max(0, to.z0 - (from.z1 + 1), from.z0 - (to.z1 + 1));
    const gap = Math.hypot(gx, gz);
    const up = to.s > from.s + 0.4;
    const sprint = gap >= 1 || up;
    if (process.env.DEBUG === '2') console.log(`   ${to.i} Lücke ${gap.toFixed(1)} sprint=${sprint}`);
    let left = false;
    for (let t = 0; t < 120; t++) {
      const p = bot.entity.position;
      if (bot.entity.onGround && onPad(p, to, 0.25)) {
        bot.clearControlStates();
        // auf Eis: gegenlenken, bis man steht
        for (let k = 0; k < 30 && Math.hypot(bot.entity.velocity.x, bot.entity.velocity.z) > 0.03; k++) {
          await bot.lookAt(new Vec3(tc.x, bot.entity.position.y + 1.62, tc.z), true);
          const v = bot.entity.velocity;
          const yaw = bot.entity.yaw;
          const fwd = -Math.sin(yaw) * v.x - Math.cos(yaw) * v.z;
          bot.setControlState('back', fwd > 0.03 && Math.hypot(bot.entity.position.x - tc.x, bot.entity.position.z - tc.z) < 0.6);
          bot.setControlState('forward', fwd < -0.03);
          await bot.waitForTicks(1);
        }
        await settle();
        return onPad(bot.entity.position, to, 0.31);
      }
      if (!onPad(p, from, 0.31)) left = true;
      if (left && bot.entity.onGround && !onPad(p, to, 0.31) && !onPad(p, from, 0.31)) break;
      if (p.y < Math.min(from.s, to.s) - 3) break;
      await bot.lookAt(new Vec3(tc.x, p.y + 1.62, tc.z), true);
      const dist = Math.hypot(p.x - tc.x, p.z - tc.z);
      const small = to.x1 - to.x0 < 2 && to.z1 - to.z0 < 2;
      const brake = left && !bot.entity.onGround && small && dist < 0.9 && p.y > to.s - 0.2;
      bot.setControlState('forward', !brake);
      bot.setControlState('back', brake && Math.hypot(bot.entity.velocity.x, bot.entity.velocity.z) > 0.12);
      bot.setControlState('sprint', sprint && !brake);
      const yaw = bot.entity.yaw;
      const reach = up ? 0.25 : 0.3;
      const ahead = p.offset(-Math.sin(yaw) * reach, -0.4, -Math.cos(yaw) * reach);
      const below = bot.blockAt(ahead);
      const edge = bot.entity.onGround && to.kind !== 'BOUNCE' && (!below || below.boundingBox === 'empty');
      bot.setControlState('jump', (edge || (headHitter && !left)) && onPad(p, from, 0.31));
      await bot.waitForTicks(1);
    }
    bot.clearControlStates();
    const q = bot.entity.position;
    if (process.env.DEBUG) console.log(`   ${to.i}: bei ${q.x.toFixed(2)} ${q.y.toFixed(2)} ${q.z.toFixed(2)}`);
    return false;
  }

  const summary = [];
  for (const id of COURSES) {
    const json = (await rcon(`gipfel pads ${id}`))[0];
    const course = JSON.parse(json.slice(json.indexOf('{')));
    const pads = course.pads;
    const cps = pads.filter((p) => p.role === 'CHECKPOINT').length;
    bot.chats.length = 0;
    bot.chat(`/gipfel start ${id}`);
    await wait(2500);
    // Strecke laden lassen
    for (let t = 0; t < 60; t++) {
      const missing = pads.filter((p) => {
        const b = bot.blockAt(new Vec3(p.x0, p.y, p.z0));
        return !b || b.name === 'air';
      });
      if (!missing.length) break;
      await wait(250);
    }
    await wait(500);
    const fails = [];
    let real = 0;
    for (let i = 1; i < pads.length; i++) {
      const from = pads[i - 1];
      const to = pads[i];
      // stehen wir wirklich auf from? (sonst hinsetzen)
      if (!onPad(bot.entity.position, from, 0.3)) {
        const c = center(from);
        await tp(c.x, from.s, c.z);
        await wait(400);
        await settle();
      }
      const ok = await transition(from, to);
      if (ok) {
        real++;
      } else {
        fails.push(`${i}:${to.kind}${to.kind === 'JUMP' ? `(${(Math.hypot(center(to).x - center(from).x, center(to).z - center(from).z)).toFixed(1)}/${(to.s - from.s).toFixed(1)})` : ''}`);
        // Sturz → Plugin setzt zurück; wir setzen auf den Ziel-Pad und machen weiter (mit Pause, sonst ist die Zeit „zu schnell“)
        await wait(1500);
        const c = center(to);
        await tp(c.x, to.s, c.z);
        await wait(600);
        await settle();
      }
    }
    await wait(1500);
    const cpOk = bot.chats.filter((c) => new RegExp(`Checkpoint \\d+/${cps}`).test(c)).length;
    const finish = bot.chats.find((c) => /Im Ziel|made it|finish|zählt nicht|schneller als/.test(c)) || '';
    const line = `${id.padEnd(12)} ${course.difficulty.padEnd(7)} ${real}/${pads.length - 1} echt · Checkpoints ${cpOk}/${cps} · Ziel: ${finish ? 'ja' : 'NEIN'}${fails.length ? ` · gesetzt: ${fails.join(' ')}` : ''}`;
    console.log(line);
    if (finish) console.log(`   ${finish}`);
    summary.push({ id, real, total: pads.length - 1, cpOk: cpOk === cps, finish: !!finish });
    bot.chat('/gipfel basislager');
    await wait(1500);
  }
  const all = summary.reduce((a, s) => a + s.total, 0);
  const real = summary.reduce((a, s) => a + s.real, 0);
  console.log(`\nEcht gelaufen: ${real}/${all} Übergänge; Ziel erreicht: ${summary.filter((s) => s.finish).length}/${summary.length}; alle Checkpoints: ${summary.filter((s) => s.cpOk).length}/${summary.length}`);
  bot.quit();
  await wait(500);
  process.exit(summary.every((s) => s.finish && s.cpOk) ? 0 : 1);
}

// Hängt etwas, ohne dass noch eine Verbindung offen ist, würde node still mit 0 enden – das wäre ein falsches Grün.
process.on('beforeExit', () => {
  console.error('Test endet unerwartet (nichts mehr offen) – gilt als Fehlschlag.');
  process.exit(1);
});

main().catch((e) => {
  console.error(e);
  process.exit(1);
});
