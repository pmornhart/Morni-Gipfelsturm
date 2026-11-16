package net.morni.gipfelsturm.time;

/** Zeiten zum Anzeigen: „1:05,250“ (Deutsch) bzw. „1:05.250“ (Englisch), Abstände „+0,452“ / „−1,203“. */
public final class TimeFormat {

    private TimeFormat() {
    }

    /** m:ss,mmm (ab einer Stunde h:mm:ss,mmm). */
    public static String clock(long ms, String lang) {
        long total = Math.max(0, ms);
        long millis = total % 1000;
        long sec = total / 1000;
        long h = sec / 3600;
        long m = (sec / 60) % 60;
        long s = sec % 60;
        char sep = "en".equals(lang) ? '.' : ',';
        if (h > 0) {
            return String.format("%d:%02d:%02d%c%03d", h, m, s, sep, millis);
        }
        return String.format("%d:%02d%c%03d", m, s, sep, millis);
    }

    /** Abstand zu einer Vergleichszeit mit Vorzeichen: „+0,452“, „−1,203“ (Minuten nur wenn nötig). */
    public static String diff(long ms, String lang) {
        String sign = ms > 0 ? "+" : ms < 0 ? "−" : "±";
        long a = Math.abs(ms);
        char sep = "en".equals(lang) ? '.' : ',';
        long sec = a / 1000;
        if (sec >= 60) {
            return sign + String.format("%d:%02d%c%03d", sec / 60, sec % 60, sep, a % 1000);
        }
        return sign + String.format("%d%c%03d", sec, sep, a % 1000);
    }

    /** Kurz für Grenzen wie Medaillen: „0:45“ oder „1:05,5“. */
    public static String limit(long ms, String lang) {
        long sec = ms / 1000;
        long rest = ms % 1000;
        String base = String.format("%d:%02d", sec / 60, sec % 60);
        if (rest == 0) {
            return base;
        }
        char sep = "en".equals(lang) ? '.' : ',';
        String frac = String.format("%03d", rest).replaceAll("0+$", "");
        return base + sep + frac;
    }
}
