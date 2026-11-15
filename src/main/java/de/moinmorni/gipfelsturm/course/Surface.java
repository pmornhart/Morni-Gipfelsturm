package de.moinmorni.gipfelsturm.course;

/** Wie sich ein Block unter den Füßen verhält. */
public enum Surface {
    NORMAL,
    /** Rutschig (Packeis, Blaueis): weite Sprünge, aber man bremst schlecht. */
    ICE,
    /** Federt zurück (Schleim). */
    SLIME,
    /** Bremst (Seelensand): kurze Sprünge. */
    SOUL_SAND,
    /** Man klettert (Leiter). */
    CLIMB,
    /** Wasser: fängt den Sturz ab – auf Strecken eine Gefahr-Zone. */
    WATER
}
