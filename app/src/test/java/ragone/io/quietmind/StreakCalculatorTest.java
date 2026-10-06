package ragone.io.quietmind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.Test;

public class StreakCalculatorTest {

    private static final String TODAY = "15/03/2026";
    private static final String YESTERDAY = "14/03/2026";
    private static final String LAST_WEEK = "08/03/2026";

    private static Calendar date(int year, int month, int day) {
        Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.clear();
        calendar.set(year, month, day, 12, 0);
        return calendar;
    }

    @Test
    public void formatDay_usesDayMonthYear() {
        assertEquals("05/01/2026", StreakCalculator.formatDay(date(2026, Calendar.JANUARY, 5)));
    }

    @Test
    public void formatPreviousDay_crossesMonthAndYearBoundaries() {
        assertEquals("31/12/2025", StreakCalculator.formatPreviousDay(date(2026, Calendar.JANUARY, 1)));
        assertEquals("29/02/2024", StreakCalculator.formatPreviousDay(date(2024, Calendar.MARCH, 1)));
    }

    @Test
    public void formatPreviousDay_doesNotModifyItsArgument() {
        Calendar today = date(2026, Calendar.MARCH, 15);
        StreakCalculator.formatPreviousDay(today);
        assertEquals(TODAY, StreakCalculator.formatDay(today));
    }

    @Test
    public void isStreakBroken_whenADayWasMissed() {
        assertTrue(StreakCalculator.isStreakBroken(LAST_WEEK, TODAY, YESTERDAY, 5));
    }

    @Test
    public void isStreakBroken_notWhenLastSessionWasTodayOrYesterday() {
        assertFalse(StreakCalculator.isStreakBroken(TODAY, TODAY, YESTERDAY, 5));
        assertFalse(StreakCalculator.isStreakBroken(YESTERDAY, TODAY, YESTERDAY, 5));
    }

    @Test
    public void isStreakBroken_notWithoutAStreak() {
        assertFalse(StreakCalculator.isStreakBroken(LAST_WEEK, TODAY, YESTERDAY, 0));
        assertFalse(StreakCalculator.isStreakBroken("", TODAY, YESTERDAY, 0));
    }

    @Test
    public void streakAfterSession_firstEverSessionStartsAStreak() {
        assertEquals(1, StreakCalculator.streakAfterSession("", TODAY, YESTERDAY, 0));
    }

    @Test
    public void streakAfterSession_consecutiveDayExtendsTheStreak() {
        assertEquals(6, StreakCalculator.streakAfterSession(YESTERDAY, TODAY, YESTERDAY, 5));
    }

    @Test
    public void streakAfterSession_secondSessionTheSameDayDoesNotCount() {
        assertEquals(5, StreakCalculator.streakAfterSession(TODAY, TODAY, YESTERDAY, 5));
    }

    @Test
    public void streakAfterSession_afterAGapStartsOver() {
        assertEquals(1, StreakCalculator.streakAfterSession(LAST_WEEK, TODAY, YESTERDAY, 5));
    }

    @Test
    public void streakAfterSession_afterAResetStartsAtOne() {
        assertEquals(1, StreakCalculator.streakAfterSession(LAST_WEEK, TODAY, YESTERDAY, 0));
    }

    @Test
    public void rowStart_groupsDaysInSevens() {
        assertEquals(0, StreakCalculator.rowStart(0));
        assertEquals(0, StreakCalculator.rowStart(6));
        assertEquals(7, StreakCalculator.rowStart(7));
        assertEquals(7, StreakCalculator.rowStart(13));
        assertEquals(14, StreakCalculator.rowStart(14));
    }
}
