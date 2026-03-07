package ragone.io.quietmind;

import org.junit.Test;
import static org.junit.Assert.*;

public class StreakCalculatorTest {

    // --- shouldIncrementStreak ---

    @Test
    public void firstEverSession_alwaysIncrements() {
        // streak == 0 means first session ever, always increment regardless of dates
        assertTrue(StreakCalculator.shouldIncrementStreak("", "07/03/2026", "06/03/2026", 0));
    }

    @Test
    public void sessionAfterYesterday_increments() {
        // Meditated yesterday, meditating again today — streak should grow
        assertTrue(StreakCalculator.shouldIncrementStreak(
                "06/03/2026", "07/03/2026", "06/03/2026", 5));
    }

    @Test
    public void sessionOnSameDay_doesNotIncrement() {
        // Already counted today — don't double-count
        assertFalse(StreakCalculator.shouldIncrementStreak(
                "07/03/2026", "07/03/2026", "06/03/2026", 5));
    }

    @Test
    public void sessionAfterMissedDay_doesNotIncrement() {
        // Missed a day — streak should have been reset already, not incremented here
        // (reset happens in onStart, increment only if lastDay was yesterday)
        assertFalse(StreakCalculator.shouldIncrementStreak(
                "05/03/2026", "07/03/2026", "06/03/2026", 5));
    }

    // --- shouldResetStreak ---

    @Test
    public void missedADay_resetsStreak() {
        // lastDay is not yesterday and not today — user missed at least one day
        assertTrue(StreakCalculator.shouldResetStreak(
                "05/03/2026", "07/03/2026", "06/03/2026", 3));
    }

    @Test
    public void lastDayIsYesterday_doesNotReset() {
        // User meditated yesterday, opens app today before meditating — no reset
        assertFalse(StreakCalculator.shouldResetStreak(
                "06/03/2026", "07/03/2026", "06/03/2026", 3));
    }

    @Test
    public void lastDayIsToday_doesNotReset() {
        // User already meditated today — no reset
        assertFalse(StreakCalculator.shouldResetStreak(
                "07/03/2026", "07/03/2026", "06/03/2026", 3));
    }

    @Test
    public void streakIsZero_doesNotReset() {
        // Nothing to reset if streak is already 0
        assertFalse(StreakCalculator.shouldResetStreak(
                "01/01/2020", "07/03/2026", "06/03/2026", 0));
    }

    // --- getStreakDayStart ---

    @Test
    public void streak0_dayStartIs0() {
        assertEquals(0, StreakCalculator.getStreakDayStart(0));
    }

    @Test
    public void streak7_dayStartIs7() {
        // Completed first full week, second row starts at day 8 (index 7)
        assertEquals(7, StreakCalculator.getStreakDayStart(7));
    }

    @Test
    public void streak8_dayStartIs7() {
        // Day 8 of 9 is in the second row — dayStart stays at 7
        assertEquals(7, StreakCalculator.getStreakDayStart(8));
    }

    @Test
    public void streak13_dayStartIs7() {
        assertEquals(7, StreakCalculator.getStreakDayStart(13));
    }

    @Test
    public void streak14_dayStartIs14() {
        // Third row starts
        assertEquals(14, StreakCalculator.getStreakDayStart(14));
    }

    @Test
    public void streak6_dayStartIs0() {
        // Still in first row
        assertEquals(0, StreakCalculator.getStreakDayStart(6));
    }
}
