package ragone.io.quietmind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Launches the real activities and drives a session end to end on a device. */
@RunWith(AndroidJUnit4.class)
public class SmokeTest {

    private SharedPreferences prefs;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        prefs = context.getSharedPreferences("my_prefs", Context.MODE_PRIVATE);
        // Skip the tour and the one-time Do Not Disturb prompt so the play button starts a session.
        prefs.edit().clear()
                .putBoolean("first_time", false)
                .putBoolean("dnd_prompted", true)
                .commit();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .grantRuntimePermission(context.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Test
    public void startAndStopASession() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> {
                WheelView wheel = activity.findViewById(R.id.wheel);
                assertEquals(14, wheel.getSelectedPosition());
                assertEquals(90, wheel.getItems().size());
                assertTrue(wheel.isEnabled());
                activity.findViewById(R.id.play_pause_view).performClick();
                assertFalse("controls are locked during a session", wheel.isEnabled());
            });
            SystemClock.sleep(2500);
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.play_pause_view).performClick();
                assertTrue("controls are unlocked after stopping", activity.findViewById(R.id.wheel).isEnabled());
            });
        }
        assertEquals("stopping early doesn't count as a session", 0, prefs.getInt("streak", 0));
    }

    @Test
    public void completingAOneMinuteSessionStartsAStreak() {
        prefs.edit().putInt("time", 0).putInt("interval", 1).commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            scenario.onActivity(activity -> activity.findViewById(R.id.play_pause_view).performClick());
            AtomicBoolean finished = new AtomicBoolean(false);
            long deadline = SystemClock.elapsedRealtime() + 90_000;
            while (!finished.get() && SystemClock.elapsedRealtime() < deadline) {
                SystemClock.sleep(1000);
                scenario.onActivity(activity -> finished.set(activity.findViewById(R.id.wheel).isEnabled()));
            }
            assertTrue("session finished within 90 s", finished.get());
        }
        assertEquals(1, prefs.getInt("streak", 0));
        assertEquals(1, prefs.getInt("longeststreak", 0));
        assertEquals(1, prefs.getInt("totaltime", 0));
        assertNotEquals("", prefs.getString("lastday", ""));
    }

    @Test
    public void statsShowRecordedTime() {
        prefs.edit().putInt("streak", 3).putInt("longeststreak", 8).putInt("totaltime", 90)
                .putInt("averagetime", 15).commit();
        try (ActivityScenario<StatsActivity> scenario = ActivityScenario.launch(StatsActivity.class)) {
            AtomicReference<String> total = new AtomicReference<>();
            AtomicReference<String> streak = new AtomicReference<>();
            scenario.onActivity(activity -> {
                total.set(((TextView) activity.findViewById(R.id.totaltime)).getText().toString());
                streak.set(((TextView) activity.findViewById(R.id.currentstreak)).getText().toString());
            });
            assertTrue(total.get(), total.get().endsWith(" hours") && total.get().startsWith("1"));
            assertEquals("3 days", streak.get());
        }
    }
}
