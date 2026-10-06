package ragone.io.quietmind;

import static org.junit.Assert.assertEquals;

import java.util.Locale;

import org.junit.Test;

public class StatsFormatterTest {

    @Test
    public void formatDays() {
        assertEquals("-", StatsFormatter.formatDays(0));
        assertEquals("1 day", StatsFormatter.formatDays(1));
        assertEquals("2 days", StatsFormatter.formatDays(2));
        assertEquals("38 days", StatsFormatter.formatDays(38));
    }

    @Test
    public void formatTotalTime_showsFractionalHours() {
        assertEquals("-", StatsFormatter.formatTotalTime(0, Locale.US));
        assertEquals("0.5 hours", StatsFormatter.formatTotalTime(30, Locale.US));
        assertEquals("1.0 hour", StatsFormatter.formatTotalTime(60, Locale.US));
        assertEquals("1.5 hours", StatsFormatter.formatTotalTime(90, Locale.US));
        assertEquals("145.2 hours", StatsFormatter.formatTotalTime(8712, Locale.US));
    }

    @Test
    public void formatTotalTime_singularOnlyWhenItRoundsToOneHour() {
        assertEquals("1.0 hour", StatsFormatter.formatTotalTime(62, Locale.US));
        assertEquals("1.1 hours", StatsFormatter.formatTotalTime(63, Locale.US));
        assertEquals("0.0 hours", StatsFormatter.formatTotalTime(1, Locale.US));
    }

    @Test
    public void formatTotalTime_usesTheLocaleDecimalSeparator() {
        assertEquals("1,5 hours", StatsFormatter.formatTotalTime(90, Locale.GERMANY));
    }

    @Test
    public void formatAverageTime() {
        assertEquals("-", StatsFormatter.formatAverageTime(0));
        assertEquals("20 min.", StatsFormatter.formatAverageTime(20));
    }
}
