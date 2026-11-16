package net.morni.gipfelsturm.game;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.morni.gipfelsturm.course.Course;

/** Party-Rennen: Der Anführer startet, Mitglieder auf diesem Server machen mit, alle laufen beim Startschuss los. */
final class Race {

    enum State {
        GATHER, COUNTDOWN, RUNNING
    }

    record Result(UUID uuid, String name, long ms) {
    }

    final Course course;
    final UUID leader;
    final String leaderName;
    final Set<UUID> invited = new HashSet<>();
    final Set<UUID> joined = new LinkedHashSet<>();
    final List<Result> results = new ArrayList<>();
    final Set<UUID> out = new HashSet<>();
    State state = State.GATHER;
    long until;
    int count;

    Race(Course course, UUID leader, String leaderName) {
        this.course = course;
        this.leader = leader;
        this.leaderName = leaderName;
        joined.add(leader);
    }

    /** Alle, die noch laufen (nicht im Ziel, nicht ausgestiegen). */
    boolean done() {
        for (UUID u : joined) {
            if (!out.contains(u) && results.stream().noneMatch(r -> r.uuid().equals(u))) {
                return false;
            }
        }
        return true;
    }
}
