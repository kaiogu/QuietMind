package ragone.io.quietmind;

import org.junit.Test;
import static org.junit.Assert.*;

public class StatsFormatterTest {

    // --- formatStreakCount ---

    @Test
    public void streakZero_returnsDash() {
        assertEquals("-", StatsFormatter.formatStreakCount(0));
    }

    @Test
    public void streakOne_returnsSingular() {
        assertEquals("1 day", StatsFormatter.formatStreakCount(1));
    }

    @Test
    public void streakMany_returnsPlural() {
        assertEquals("7 days", StatsFormatter.formatStreakCount(7));
        assertEquals("30 days", StatsFormatter.formatStreakCount(30));
    }

    // --- formatTotalTime ---

    @Test
    public void totalTimeZero_returnsDash() {
        assertEquals("-", StatsFormatter.formatTotalTime(0));
    }

    @Test
    public void totalTimeSingular_returnsHour() {
        // 1 minute total — integer division gives 0 hours, but "hour" singular
        // because totalMinutes == 1 (not > 1... wait, 1 is not > 1)
        // Original code: if(result > 1) → "hours" else "hour"
        // So 1 minute → 0.0 hour (integer division 1/60 = 0)
        assertEquals("0.0 hour", StatsFormatter.formatTotalTime(1));
    }

    @Test
    public void totalTimeMany_returnsHours() {
        // 120 minutes = 2.0 hours (integer division: 120/60 = 2)
        assertEquals("2.0 hours", StatsFormatter.formatTotalTime(120));
    }

    @Test
    public void totalTimeIntegerDivision_truncates() {
        // 90 minutes: integer division 90/60 = 1 (not 1.5) — this is the original behaviour
        assertEquals("1.0 hours", StatsFormatter.formatTotalTime(90));
    }

    // --- formatAverageTime ---

    @Test
    public void averageZero_returnsDash() {
        assertEquals("-", StatsFormatter.formatAverageTime(0));
    }

    @Test
    public void averageNonZero_returnsMinutes() {
        assertEquals("20 min.", StatsFormatter.formatAverageTime(20));
        assertEquals("1 min.", StatsFormatter.formatAverageTime(1));
    }
}
