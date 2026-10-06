package ragone.io.quietmind;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Pure Java helper for streak logic — no Android framework dependencies,
 * so it can be tested with plain JUnit without an emulator.
 */
public class StreakCalculator {

    private static final String DATE_FORMAT = "dd/MM/yyyy";

    /** Returns today's date as a string, e.g. "07/03/2026". */
    public static String getCurrentDay() {
        SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        return fmt.format(Calendar.getInstance().getTime());
    }

    /** Returns yesterday's date as a string. */
    public static String getYesterday() {
        SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT, Locale.US);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        return fmt.format(cal.getTime());
    }

    /**
     * Returns true if the streak should increment after a completed session.
     *
     * Increment when:
     * - streak is 0 (very first session ever), OR
     * - lastDay was yesterday and we haven't already counted today
     */
    public static boolean shouldIncrementStreak(String lastDay, String currentDay,
                                                String yesterday, int streak) {
        return (!lastDay.equals(currentDay) && lastDay.equals(yesterday)) || streak == 0;
    }

    /**
     * Returns true if the streak should be reset to 0.
     *
     * Reset when the user missed at least one day:
     * lastDay is neither yesterday nor today, and the streak was > 0.
     */
    public static boolean shouldResetStreak(String lastDay, String currentDay,
                                            String yesterday, int streak) {
        return !lastDay.equals(yesterday) && !lastDay.equals(currentDay) && streak != 0;
    }

    /**
     * Given a streak count, returns the index of the first day shown
     * in the 7-day rolling streak display.
     *
     * e.g. streak=8 → dayStart=7, showing days 8-14 (indices 7-13)
     */
    public static int getStreakDayStart(int streak) {
        int streakRemain = streak % 7;
        return streak - streakRemain;
    }
}
