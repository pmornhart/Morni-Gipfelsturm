package net.morni.gipfelsturm.game;

import java.util.Arrays;
import net.morni.gipfelsturm.course.Course;
import net.morni.gipfelsturm.time.RunClock;

/** Ein Lauf eines Spielers auf einer Strecke. */
final class Run {

    enum State {
        /** Auf dem Start: Zeit startet beim Verlassen des Smaragds. */
        READY,
        /** Party-Rennen: wartet auf den Startschuss, darf den Start nicht verlassen. */
        FROZEN,
        /** Zeit läuft. */
        RUNNING
    }

    /** Größte Aufzeichnung für den Geist: 30 Minuten. */
    private static final int MAX_PATH = 3 * 20 * 60 * 30;

    final Course course;
    final boolean practice;
    Race race;
    State state = State.READY;
    RunClock clock;
    /** Erreichte Checkpoints (0 = noch keiner). */
    int reached;
    final long[] splits;
    int falls;
    boolean hasPrev;
    double px;
    double py;
    double pz;
    /** Übungs-Punkt: x, y, z, Blick (null = keiner). */
    double[] practicePoint;
    /** Bis zu diesem Tick zeigt die Aktionsleiste die letzte Zwischenzeit statt der Uhr. */
    long splitShownUntil;
    net.kyori.adventure.text.Component splitText;
    long skipHintTick;
    /** So viele Ticks steht der Spieler schon neben der Strecke (auf Felsen o. Ä.). */
    int offCourse;
    /** Weg dieses Laufs (für den Geist, falls er Bestzeit wird). */
    private float[] path = new float[600];
    private int pathLen;
    /** Geist der Bestzeit (x/y/z pro Tick) oder null. */
    float[] ghost;

    Run(Course course, boolean practice) {
        this.course = course;
        this.practice = practice;
        this.splits = new long[course.checkpointCount()];
    }

    void resetProgress() {
        reached = 0;
        Arrays.fill(splits, 0);
        clock = null;
        pathLen = 0;
        hasPrev = false;
        splitShownUntil = 0;
    }

    void record(double x, double y, double z) {
        if (pathLen + 3 > MAX_PATH) {
            return;
        }
        if (pathLen + 3 > path.length) {
            path = Arrays.copyOf(path, Math.min(MAX_PATH, path.length * 2));
        }
        path[pathLen++] = (float) x;
        path[pathLen++] = (float) y;
        path[pathLen++] = (float) z;
    }

    float[] path() {
        return Arrays.copyOf(path, pathLen);
    }
}
