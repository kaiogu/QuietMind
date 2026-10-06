package ragone.io.quietmind;

import java.util.Locale;

/** Display strings for the stats screen. */
public final class StatsFormatter {

    private static final String NONE = "-";

    private StatsFormatter() {
    }

    /** 0 → "-", 1 → "1 day", 7 → "7 days". */
    public static String formatDays(int days) {
        if (days <= 0) {
            return NONE;
        }
        return days == 1 ? "1 day" : days + " days";
    }

    /** Total minutes as hours with one decimal: 0 → "-", 60 → "1.0 hour", 90 → "1.5 hours". */
    public static String formatTotalTime(int totalMinutes, Locale locale) {
        if (totalMinutes <= 0) {
            return NONE;
        }
        long tenthsOfHour = Math.round(totalMinutes / 6.0);
        String hours = String.format(locale, "%.1f", tenthsOfHour / 10.0);
        return tenthsOfHour == 10 ? hours + " hour" : hours + " hours";
    }

    /** 0 → "-", 20 → "20 min.". */
    public static String formatAverageTime(int minutes) {
        if (minutes <= 0) {
            return NONE;
        }
        return minutes + " min.";
    }
}
