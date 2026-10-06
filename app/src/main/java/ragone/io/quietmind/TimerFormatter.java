package ragone.io.quietmind;

import java.util.Locale;

/** Countdown text for the session notification. */
public final class TimerFormatter {

    private TimerFormatter() {
    }

    /** Milliseconds as "MM:SS", rounded up so the display never shows 00:00 while time remains. */
    public static String formatMillis(long millis) {
        long totalSeconds = (Math.max(0, millis) + 999) / 1000;
        return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60);
    }
}
