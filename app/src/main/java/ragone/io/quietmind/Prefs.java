package ragone.io.quietmind;

import android.content.Context;
import android.content.SharedPreferences;

/** Typed access to the app's single SharedPreferences file. Keys are unchanged from earlier releases. */
public final class Prefs {

    private static final String FILE = "my_prefs";
    private static final String INTERVAL = "interval";
    private static final String VIPASSANA = "vipassana";
    private static final String FIRST_TIME = "first_time";
    private static final String SESSION_NUM = "session_num";
    private static final String LONGEST_STREAK = "longeststreak";
    private static final String TOTAL_TIME = "totaltime";
    private static final String AVERAGE_TIME = "averagetime";
    private static final String STREAK = "streak";
    private static final String TIME = "time";
    private static final String LAST_DAY = "lastday";
    private static final String LAST_VIEWED_STAGE = "last_viewed_stage";
    private static final String DND_PROMPTED = "dnd_prompted";
    private static final String STAGE_PREFIX = "stage";
    private static final int DEFAULT_TIME_INDEX = 14;

    private final SharedPreferences prefs;

    public Prefs(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public int getIntervalMinutes() {
        return prefs.getInt(INTERVAL, 0);
    }

    public void setIntervalMinutes(int minutes) {
        prefs.edit().putInt(INTERVAL, minutes).apply();
    }

    public boolean isVipassana() {
        return prefs.getBoolean(VIPASSANA, false);
    }

    public boolean isFirstTime() {
        return prefs.getBoolean(FIRST_TIME, true);
    }

    /** Wheel index of the chosen session length (index + 1 minutes). */
    public int getTimeIndex() {
        return prefs.getInt(TIME, DEFAULT_TIME_INDEX);
    }

    public void saveSettings(int timeIndex, boolean vipassana, boolean firstTime) {
        prefs.edit()
                .putInt(TIME, timeIndex)
                .putBoolean(VIPASSANA, vipassana)
                .putBoolean(FIRST_TIME, firstTime)
                .apply();
    }

    public int getStreak() {
        return prefs.getInt(STREAK, 0);
    }

    public void setStreak(int streak) {
        prefs.edit().putInt(STREAK, streak).apply();
    }

    public String getLastDay() {
        return prefs.getString(LAST_DAY, "");
    }

    public int getLongestStreak() {
        return prefs.getInt(LONGEST_STREAK, 0);
    }

    public int getTotalMinutes() {
        return prefs.getInt(TOTAL_TIME, 0);
    }

    public int getAverageMinutes() {
        return prefs.getInt(AVERAGE_TIME, 0);
    }

    /** Records a completed session of {@code minutes} that left the streak at {@code streak}. */
    public void recordSession(int minutes, int streak, String day) {
        int total = getTotalMinutes() + minutes;
        // Stored as "number of the next session", so it starts at 1.
        int sessionNumber = prefs.getInt(SESSION_NUM, 1);
        prefs.edit()
                .putInt(STREAK, streak)
                .putString(LAST_DAY, day)
                .putInt(LONGEST_STREAK, Math.max(streak, getLongestStreak()))
                .putInt(TOTAL_TIME, total)
                .putInt(AVERAGE_TIME, total / sessionNumber)
                .putInt(SESSION_NUM, sessionNumber + 1)
                .apply();
    }

    public int getLastViewedStage() {
        return prefs.getInt(LAST_VIEWED_STAGE, 0);
    }

    public void setLastViewedStage(int position) {
        prefs.edit().putInt(LAST_VIEWED_STAGE, position).apply();
    }

    /** Whether the user ticked stage {@code stage} (1-10) as done. */
    public boolean isStageDone(int stage) {
        return prefs.getBoolean(STAGE_PREFIX + stage, false);
    }

    public void setStageDone(int stage, boolean done) {
        prefs.edit().putBoolean(STAGE_PREFIX + stage, done).apply();
    }

    public boolean wasDndPrompted() {
        return prefs.getBoolean(DND_PROMPTED, false);
    }

    public void setDndPrompted() {
        prefs.edit().putBoolean(DND_PROMPTED, true).apply();
    }
}
