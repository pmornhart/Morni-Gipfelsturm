package net.morni.gipfelsturm.course;

/** Schwierigkeit einer Strecke – bestimmt auch, welche Sprünge erlaubt sind ({@link JumpRules.Tier}). */
public enum Difficulty {
    LEICHT("Leicht", "Easy", "#5fd068", "lime_concrete", JumpRules.Tier.WALK),
    MITTEL("Mittel", "Medium", "#f5c542", "yellow_concrete", JumpRules.Tier.SPRINT),
    SCHWER("Schwer", "Hard", "#ff6b5b", "red_concrete", JumpRules.Tier.SPRINT),
    EXTREM("Extrem", "Extreme", "#c77dff", "purple_concrete", JumpRules.Tier.EXPERT);

    public final String de;
    public final String en;
    /** Farbe in Texten (#rrggbb). */
    public final String colour;
    /** Block/Item für Menüs und Stationen. */
    public final String block;
    public final JumpRules.Tier tier;

    Difficulty(String de, String en, String colour, String block, JumpRules.Tier tier) {
        this.de = de;
        this.en = en;
        this.colour = colour;
        this.block = block;
        this.tier = tier;
    }

    public String name(String lang) {
        return "en".equals(lang) ? en : de;
    }

    /** ★ bis ★★★★. */
    public String stars() {
        return "★".repeat(ordinal() + 1) + "☆".repeat(values().length - ordinal() - 1);
    }
}
