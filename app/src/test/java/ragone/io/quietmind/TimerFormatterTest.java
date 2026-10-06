package ragone.io.quietmind;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimerFormatterTest {

    @Test
    public void formatsMinutesAndSeconds() {
        assertEquals("01:30", TimerFormatter.formatMillis(90_000));
        assertEquals("15:00", TimerFormatter.formatMillis(900_000));
        assertEquals("90:00", TimerFormatter.formatMillis(5_400_000));
    }

    @Test
    public void roundsUpPartialSeconds() {
        assertEquals("00:01", TimerFormatter.formatMillis(1));
        assertEquals("00:59", TimerFormatter.formatMillis(58_001));
        assertEquals("14:59", TimerFormatter.formatMillis(898_500));
    }

    @Test
    public void zeroAndNegativeShowZero() {
        assertEquals("00:00", TimerFormatter.formatMillis(0));
        assertEquals("00:00", TimerFormatter.formatMillis(-500));
    }
}
