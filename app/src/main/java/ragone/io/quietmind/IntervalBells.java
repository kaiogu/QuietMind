package ragone.io.quietmind;

/** Decides when interval bells ring: every {@code intervalMinutes} of elapsed session time. */
public final class IntervalBells {

    private static final long MINUTE_MS = 60_000L;
    /** No interval bell this close to the end; the closing bell is about to ring. */
    private static final long END_GUARD_MS = 2_000L;

    private IntervalBells() {
    }

    /**
     * Returns true if an interval boundary was crossed between two countdown ticks.
     * Comparing interval counts, rather than testing for an exact remainder, means a late or
     * early tick can neither skip a bell nor ring it twice.
     */
    public static boolean shouldRing(long sessionMs, long previousRemainingMs, long remainingMs,
                                     int intervalMinutes) {
        if (intervalMinutes <= 0 || remainingMs < END_GUARD_MS) {
            return false;
        }
        long intervalMs = intervalMinutes * MINUTE_MS;
        long previousElapsed = sessionMs - previousRemainingMs;
        long elapsed = sessionMs - remainingMs;
        return elapsed / intervalMs > previousElapsed / intervalMs;
    }
}
