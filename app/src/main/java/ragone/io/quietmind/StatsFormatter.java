package ragone.io.quietmind;

/**
 * Pure Java helper for formatting stats values into display strings.
 * No Android framework dependencies — testable with plain JUnit.
 */
public class StatsFormatter {

    /**
     * Formats a streak count into a display string.
     * e.g. 0 → "-", 1 → "1 day", 7 → "7 days"
     */
    public static String formatStreakCount(int streak) {
        if (streak == 0) return "-";
        return streak > 1 ? streak + " days" : streak + " day";
    }

    /**
     * Formats total meditation minutes into hours.
     * e.g. 0 → "-", 60 → "1.0 hour", 120 → "2.0 hours"
     *
     * Note: uses integer division (matches original app behaviour).
     */
    public static String formatTotalTime(int totalMinutes) {
        if (totalMinutes == 0) return "-";
        float hours = totalMinutes / 60;
        return totalMinutes > 1 ? hours + " hours" : hours + " hour";
    }

    /**
     * Formats an average session length in minutes into a display string.
     * e.g. 0 → "-", 20 → "20 min."
     */
    public static String formatAverageTime(int minutes) {
        if (minutes == 0) return "-";
        return minutes + " min.";
    }
}
