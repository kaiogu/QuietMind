package ragone.io.quietmind;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Daily streak rules. Days are stored as "dd/MM/yyyy" strings; this class has no Android
 * dependencies so the rules can be unit tested.
 */
public final class StreakCalculator {

    public static final int DAYS_PER_ROW = 7;
    private static final String DATE_FORMAT = "dd/MM/yyyy";

    private StreakCalculator() {
    }

    /** Formats the calendar's date the way it is persisted. */
    public static String formatDay(Calendar calendar) {
        SimpleDateFormat format = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        format.setTimeZone(calendar.getTimeZone());
        return format.format(calendar.getTime());
    }

    /** The persisted form of the day before {@code today}. */
    public static String formatPreviousDay(Calendar today) {
        Calendar yesterday = (Calendar) today.clone();
        yesterday.add(Calendar.DATE, -1);
        return formatDay(yesterday);
    }

    /** A streak is broken when the last session was neither today nor yesterday. */
    public static boolean isStreakBroken(String lastDay, String today, String yesterday, int streak) {
        return streak != 0 && !lastDay.equals(today) && !lastDay.equals(yesterday);
    }

    /**
     * Returns the streak after finishing a session today. Only the first session of a day
     * counts, and a session after a gap starts a new streak of one.
     */
    public static int streakAfterSession(String lastDay, String today, String yesterday, int streak) {
        if (streak == 0 || isStreakBroken(lastDay, today, yesterday, streak)) {
            return 1;
        }
        return lastDay.equals(yesterday) ? streak + 1 : streak;
    }

    /** Zero-based day number shown in the first circle of the current row of seven. */
    public static int rowStart(int streak) {
        return streak - streak % DAYS_PER_ROW;
    }
}
