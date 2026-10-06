package ragone.io.quietmind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class IntervalBellsTest {

    private static final long MINUTE = 60_000L;

    /** Simulates a countdown with the given tick times and returns the elapsed minute of each bell. */
    private static List<Long> bells(long sessionMs, int intervalMinutes, long[] remainingTicks) {
        List<Long> rang = new ArrayList<>();
        long previous = sessionMs;
        for (long remaining : remainingTicks) {
            if (IntervalBells.shouldRing(sessionMs, previous, remaining, intervalMinutes)) {
                rang.add((sessionMs - remaining) / MINUTE);
            }
            previous = remaining;
        }
        return rang;
    }

    private static long[] ticks(long sessionMs, long tickMs, long jitterMs) {
        int count = (int) (sessionMs / tickMs);
        long[] result = new long[count];
        for (int i = 0; i < count; i++) {
            long jitter = (i % 3 == 0) ? jitterMs : (i % 3 == 1 ? -jitterMs : 0);
            result[i] = Math.max(0, sessionMs - (i + 1) * tickMs + jitter);
        }
        return result;
    }

    @Test
    public void ringsEveryIntervalOfElapsedTime() {
        long session = 20 * MINUTE;
        assertEquals(Arrays.asList(5L, 10L, 15L), bells(session, 5, ticks(session, 1000, 0)));
    }

    @Test
    public void countsFromTheStartWhenTheLengthIsNotAMultiple() {
        long session = 22 * MINUTE;
        assertEquals(Arrays.asList(5L, 10L, 15L, 20L), bells(session, 5, ticks(session, 1000, 0)));
    }

    @Test
    public void jitteryTicksNeitherSkipNorRepeatBells() {
        long session = 30 * MINUTE;
        assertEquals(Arrays.asList(10L, 20L), bells(session, 10, ticks(session, 1000, 400)));
        // Ticks that arrive late enough to skip whole seconds still ring once per interval.
        assertEquals(Arrays.asList(10L, 20L), bells(session, 10, ticks(session, 1700, 0)));
    }

    @Test
    public void noBellAtTheVeryEnd() {
        long session = 10 * MINUTE;
        // The closing bell covers the end; an interval that divides the session must not double it.
        assertEquals(Arrays.asList(5L), bells(session, 5, ticks(session, 1000, 0)));
        assertFalse(IntervalBells.shouldRing(session, 1500, 500, 5));
    }

    @Test
    public void disabledIntervalNeverRings() {
        long session = 20 * MINUTE;
        assertEquals(Arrays.asList(), bells(session, 0, ticks(session, 1000, 0)));
    }

    @Test
    public void intervalLongerThanTheSessionNeverRings() {
        long session = 5 * MINUTE;
        assertEquals(Arrays.asList(), bells(session, 10, ticks(session, 1000, 0)));
    }
}
