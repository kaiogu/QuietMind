package ragone.io.quietmind;

import java.util.concurrent.TimeUnit;

/**
 * Pure Java helper for formatting countdown timer values.
 * No Android framework dependencies — testable with plain JUnit.
 */
public class TimerFormatter {

    /**
     * Formats milliseconds into a "MM:SS" countdown string.
     * e.g. 90000ms → "01:30", 3600000ms → "60:00"
     */
    public static String formatMillis(long millis) {
        return String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(millis),
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                        TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis))
        );
    }
}
