package ragone.io.quietmind;

import org.junit.Test;
import static org.junit.Assert.*;

public class TimerFormatterTest {

    @Test
    public void zero_returnsZeroZero() {
        assertEquals("00:00", TimerFormatter.formatMillis(0));
    }

    @Test
    public void thirtySeconds() {
        assertEquals("00:30", TimerFormatter.formatMillis(30_000));
    }

    @Test
    public void oneMinute() {
        assertEquals("01:00", TimerFormatter.formatMillis(60_000));
    }

    @Test
    public void oneMinuteThirty() {
        assertEquals("01:30", TimerFormatter.formatMillis(90_000));
    }

    @Test
    public void sixtyMinutes() {
        assertEquals("60:00", TimerFormatter.formatMillis(3_600_000));
    }

    @Test
    public void ninetyMinutes() {
        assertEquals("90:00", TimerFormatter.formatMillis(5_400_000));
    }

    @Test
    public void fiftyNineMinutesFiftyNineSeconds() {
        assertEquals("59:59", TimerFormatter.formatMillis(3_599_000));
    }
}
