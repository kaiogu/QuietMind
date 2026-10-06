package ragone.io.quietmind;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.Target;
import com.github.amlcurran.showcaseview.targets.ViewTarget;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "MainActivity";
    private static final String NOTIFICATION_CHANNEL = "meditation";
    private static final int NOTIFICATION_ID = 1;
    private static final int MAX_MINUTES = 90;
    private static final int MAX_INTERVAL_MINUTES = 20;
    /** Vipassanā sessions are a fixed 60 minutes (wheel index 59). */
    private static final int VIPASSANA_INDEX = 59;
    /** The closing chant is this long, so it starts this far before the end of the session. */
    private static final long VIPASSANA_END_LEAD_MS = 809_400L;
    private static final long MINUTE_MS = 60_000L;
    private static final int END_BELL_STRIKES = 3;
    private static final float SESSION_BRIGHTNESS = 0.2f;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private Prefs prefs;
    private SoundPlayer sounds;
    private RingerController ringer;
    private NotificationManager notificationManager;
    private NotificationCompat.Builder notificationBuilder;
    private ActivityResultLauncher<String> notificationPermissionLauncher;

    private WheelView wheelView;
    private PlayPauseView playPauseView;
    private LinearLayout dayLayout;
    private SwitchCompat vipassanaMode;
    private TextView bigText;
    private ImageView statsBtn;
    private ImageView helpBtn;
    private Button intervalBtn;
    private ShowcaseView scv;
    private final List<SmoothCheckBox> days = new ArrayList<>();

    private CountDownTimer timer;
    private boolean sessionRunning;
    private long sessionMs;
    private long lastRemainingMs;
    private boolean vipassanaEndStarted;
    /** Optional raw resources for the Vipassanā chants; 0 when they aren't bundled. */
    private int vipassanaStartSound;
    private int vipassanaEndSound;

    /** Wheel index of the chosen session length (index + 1 minutes). */
    private int selectedIndex;
    private int streak;
    private int counter = 0;
    private boolean firstTime;
    private float brightness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = new Prefs(this);
        sounds = new SoundPlayer(this);
        ringer = new RingerController(this);
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> startSession());
        createNotificationChannel();
        vipassanaStartSound = findRawResource("vipassanastart");
        vipassanaEndSound = findRawResource("vipassanaend");
        firstTime = prefs.isFirstTime();

        bigText = findViewById(R.id.bigText);
        bigText.setVisibility(View.INVISIBLE);
        wheelView = findViewById(R.id.wheel);
        playPauseView = findViewById(R.id.play_pause_view);
        vipassanaMode = findViewById(R.id.vipassanaMode);
        vipassanaMode.setChecked(prefs.isVipassana());
        vipassanaMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedIndex = VIPASSANA_INDEX;
                wheelView.smoothSelectIndex(VIPASSANA_INDEX);
            }
            saveData();
        });

        setupDays();
        setupPlayPauseButton();
        setupWheel();

        if (firstTime) {
            showShowcase();
        }

        statsBtn = findViewById(R.id.stats_button);
        statsBtn.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, StatsActivity.class)));

        helpBtn = findViewById(R.id.help_button);
        helpBtn.setOnClickListener(v -> showShowcase());

        intervalBtn = findViewById(R.id.interval);
        intervalBtn.setOnClickListener(v -> showIntervalPicker());
    }

    private void showIntervalPicker() {
        final NumberPicker picker = new NumberPicker(this);
        String[] labels = new String[MAX_INTERVAL_MINUTES + 1];
        labels[0] = getString(R.string.interval_disabled);
        for (int i = 1; i <= MAX_INTERVAL_MINUTES; i++) {
            labels[i] = getString(R.string.interval_minutes, i);
        }
        picker.setMinValue(0);
        picker.setMaxValue(MAX_INTERVAL_MINUTES);
        picker.setDisplayedValues(labels);
        picker.setValue(prefs.getIntervalMinutes());
        picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);

        new AlertDialog.Builder(this)
                .setTitle(R.string.interval_title)
                .setView(picker)
                .setPositiveButton(R.string.set, (dialog, which) -> prefs.setIntervalMinutes(picker.getValue()))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showShowcase() {
        // Demonstrate a streak by ticking the day circles one by one (first run only).
        for (int i = 0; i < days.size(); i++) {
            final SmoothCheckBox day = days.get(i);
            mainHandler.postDelayed(() -> {
                if (counter == 0 && firstTime && !isFinishing()) {
                    day.setChecked(true, true);
                }
            }, 1000L * (i + 1));
        }

        ViewTarget target = new ViewTarget(R.id.dayLayout, this);
        MyDrawer myDrawer = new MyDrawer(getResources(), this, days);

        scv = new ShowcaseView.Builder(this)
                .setTarget(target)
                .setStyle(R.style.MyTheme)
                .setContentTitle(getString(R.string.showcase_streaks_title))
                .setContentText(getString(R.string.showcase_streaks_text))
                .setOnClickListener(this)
                .blockAllTouches()
                .setShowcaseDrawer(myDrawer)
                .build();
    }

    /** Advances the showcase tour. */
    @Override
    public void onClick(View v) {
        switch (counter) {
            case 0:
                scv.setTarget(new ViewTarget(R.id.vipassanaMode, this));
                scv.setContentTitle(getString(R.string.showcase_vipassana_title));
                scv.setContentText(getString(R.string.showcase_vipassana_text));

                // Undo the streak demonstration.
                for (int i = 0; i < days.size(); i++) {
                    final SmoothCheckBox day = days.get(i);
                    mainHandler.postDelayed(() -> {
                        if (day.isChecked() && firstTime) {
                            day.setChecked(false, true);
                        }
                    }, 100L * (i + 1));
                }
                updateDays();
                break;
            case 1:
                scv.setTarget(new ViewTarget(R.id.stats_button, this));
                scv.setContentTitle(getString(R.string.showcase_stages_title));
                scv.setContentText(getString(R.string.showcase_stages_text));
                break;
            case 2:
                scv.setTarget(new ViewTarget(R.id.interval, this));
                scv.setContentTitle(getString(R.string.showcase_interval_title));
                scv.setContentText(getString(R.string.showcase_interval_text));
                break;
            case 3:
                scv.setTarget(Target.NONE);
                scv.setContentTitle(getString(R.string.showcase_howto_title));
                scv.setStyle(R.style.MyTheme2);
                scv.setShouldCentreText(true);
                scv.setContentText(getString(R.string.showcase_howto_text));
                break;
            case 4:
                scv.hide();
                firstTime = false;
                saveData();
                break;
            default:
                break;
        }
        counter = (counter + 1) % 5;
    }

    private void setScreenDim(float value) {
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.screenBrightness = value;
        getWindow().setAttributes(params);
    }

    private float getScreenDim() {
        return getWindow().getAttributes().screenBrightness;
    }

    private void setupPlayPauseButton() {
        // The drawable starts as "pause"; flip it to "play" for the idle state.
        playPauseView.toggle();
        playPauseView.setOnClickListener(v -> {
            if (sessionRunning) {
                stopSession();
            } else {
                requestStartSession();
            }
        });
    }

    /** Asks once for Do Not Disturb access and for notification permission, then starts. */
    private void requestStartSession() {
        if (!prefs.wasDndPrompted() && ringer.needsDndAccess()) {
            prefs.setDndPrompted();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.dnd_title)
                    .setMessage(R.string.dnd_message)
                    .setPositiveButton(R.string.dnd_open_settings, (dialog, which) -> openDndSettings())
                    .setNegativeButton(R.string.dnd_not_now, (dialog, which) -> requestStartSession())
                    .show();
            return;
        }
        if (needsNotificationPermission()) {
            // The session starts from the permission callback, whatever the answer.
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        startSession();
    }

    private void openDndSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
        } catch (ActivityNotFoundException e) {
            Log.w(TAG, "No Do Not Disturb settings screen", e);
            requestStartSession();
        }
    }

    private boolean needsNotificationPermission() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;
    }

    private void startSession() {
        if (sessionRunning || isFinishing()) {
            return;
        }
        sessionRunning = true;
        playPauseView.toggle();
        brightness = getScreenDim();
        setScreenDim(SESSION_BRIGHTNESS);
        ringer.silence();
        setInputFieldEnabled(false);
        showSnackBar();

        sessionMs = (selectedIndex + 1) * MINUTE_MS;
        lastRemainingMs = sessionMs;
        vipassanaEndStarted = false;
        boolean chantPlayed = vipassanaMode.isChecked() && vipassanaStartSound != 0
                && sounds.play(vipassanaStartSound, 1);
        if (!chantPlayed) {
            sounds.play(R.raw.bell2, 1);
        }
        setupNotification();
        timer = new SessionTimer(sessionMs).start();
    }

    /** The user stopped the session early. */
    private void stopSession() {
        sessionRunning = false;
        playPauseView.toggle();
        timer.cancel();
        sounds.stopAll();
        restoreAfterSession();
    }

    private void restoreAfterSession() {
        setScreenDim(brightness);
        ringer.restore();
        setInputFieldEnabled(true);
        wheelView.smoothSelectIndex(selectedIndex);
        removeNotification();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL,
                    getString(R.string.notification_channel_name), NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(getString(R.string.notification_channel_description));
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void setupNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 1, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_time_left, TimerFormatter.formatMillis(sessionMs)))
                .setColor(ContextCompat.getColor(this, R.color.float_color))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(contentIntent);
        postNotification();
    }

    private void postNotification() {
        if (notificationBuilder == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
                || ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build());
        }
    }

    private void removeNotification() {
        notificationBuilder = null;
        notificationManager.cancel(NOTIFICATION_ID);
    }

    @Override
    protected void onDestroy() {
        mainHandler.removeCallbacksAndMessages(null);
        if (timer != null) {
            timer.cancel();
        }
        if (sessionRunning) {
            sessionRunning = false;
            ringer.restore();
        }
        sounds.stopAll();
        removeNotification();
        saveData();
        super.onDestroy();
    }

    private void setupWheel() {
        List<String> data = new ArrayList<>();
        for (int i = 1; i <= MAX_MINUTES; i++) {
            data.add(String.valueOf(i));
        }
        wheelView.setItems(data);
        selectedIndex = Math.min(prefs.getTimeIndex(), MAX_MINUTES - 1);
        wheelView.selectIndex(selectedIndex);
        wheelView.setOnWheelItemSelectedListener(new WheelView.OnWheelItemSelectedListener() {
            @Override
            public void onWheelItemChanged(WheelView wheelView, int position) {
            }

            @Override
            public void onWheelItemSelected(WheelView wheelView, int position) {
                selectedIndex = position;
                if (vipassanaMode.isChecked() && position != VIPASSANA_INDEX) {
                    vipassanaMode.setChecked(false);
                }
                saveData();
            }
        });
    }

    private void setupDays() {
        dayLayout = findViewById(R.id.dayLayout);
        streak = prefs.getStreak();

        int dayStart = StreakCalculator.rowStart(streak);
        int size = CompatUtils.dp2px(this, 30);
        int margin = CompatUtils.dp2px(this, 6);
        for (int i = dayStart; i < dayStart + StreakCalculator.DAYS_PER_ROW; i++) {
            SmoothCheckBox checkBox = new SmoothCheckBox(this);
            checkBox.setText(String.valueOf(i + 1));
            checkBox.setEnabled(false);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, margin, margin, margin);
            checkBox.setLayoutParams(params);
            checkBox.setChecked(i < streak);
            dayLayout.addView(checkBox);
            days.add(checkBox);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Calendar now = Calendar.getInstance();
        if (StreakCalculator.isStreakBroken(prefs.getLastDay(), StreakCalculator.formatDay(now),
                StreakCalculator.formatPreviousDay(now), streak)) {
            breakStreak();
        }
    }

    /** Clears a streak that lapsed because a day was missed. */
    private void breakStreak() {
        if (streak > 1) {
            SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.ERROR_TYPE);
            dialog.setTitleText(getString(R.string.streak_over_title));
            dialog.setContentText(getString(R.string.streak_over_text, streak));
            dialog.setConfirmText(getString(R.string.ok));
            dialog.setCancelable(false);
            dialog.show();
        }
        streak = 0;
        prefs.setStreak(0);
        for (SmoothCheckBox day : days) {
            if (day.isChecked()) {
                day.setChecked(false, true);
            }
        }
        updateDays();
    }

    private void setInputFieldEnabled(boolean isEnabled) {
        intervalBtn.setEnabled(isEnabled);
        wheelView.setEnabled(isEnabled);
        vipassanaMode.setEnabled(isEnabled);
        statsBtn.setEnabled(isEnabled);
        helpBtn.setEnabled(isEnabled);
    }

    private void saveData() {
        prefs.saveSettings(selectedIndex, vipassanaMode.isChecked(), firstTime);
    }

    /** Fades "Take a deep breath" in and out at the start of a session. */
    private void showSnackBar() {
        bigText.setVisibility(View.VISIBLE);
        AnimationSet fadeIn = new AnimationSet(true);
        final AnimationSet fadeOut = new AnimationSet(true);
        fadeIn.addAnimation(new AlphaAnimation(0.0f, 1.0f));
        fadeIn.addAnimation(new ScaleAnimation(0.5f, 1.0f, 0.5f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
        fadeOut.addAnimation(new AlphaAnimation(1.0f, 0.0f));
        fadeOut.addAnimation(new ScaleAnimation(1.0f, 0.5f, 1.0f, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f));
        fadeIn.setDuration(5000);
        fadeOut.setDuration(5000);

        fadeIn.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                bigText.startAnimation(fadeOut);
                bigText.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        bigText.startAnimation(fadeIn);
    }

    /** Relabels the day circles for the current row of seven. */
    private void updateDays() {
        int dayStart = StreakCalculator.rowStart(streak);
        for (int i = dayStart; i < dayStart + StreakCalculator.DAYS_PER_ROW; i++) {
            days.get(i % StreakCalculator.DAYS_PER_ROW).setText(String.valueOf(i + 1));
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        saveData();
    }

    /** Looks up an optional raw resource by name, returning 0 when it isn't bundled. */
    @SuppressWarnings("DiscouragedApi")
    private int findRawResource(String name) {
        return getResources().getIdentifier(name, "raw", getPackageName());
    }

    private void finishSession() {
        sessionRunning = false;
        if (!vipassanaEndStarted) {
            sounds.play(R.raw.bell1, END_BELL_STRIKES);
        }
        playPauseView.toggle();
        restoreAfterSession();

        Calendar now = Calendar.getInstance();
        String today = StreakCalculator.formatDay(now);
        String yesterday = StreakCalculator.formatPreviousDay(now);
        String lastDay = prefs.getLastDay();
        if (StreakCalculator.isStreakBroken(lastDay, today, yesterday, streak)) {
            // The app stayed open across a missed day, so onStart never caught it.
            breakStreak();
        }
        int newStreak = StreakCalculator.streakAfterSession(lastDay, today, yesterday, streak);
        if (newStreak != streak) {
            streak = newStreak;
            onStreakExtended();
        }
        prefs.recordSession(selectedIndex + 1, streak, today);
        saveData();
    }

    private void onStreakExtended() {
        int dayInRow = streak % StreakCalculator.DAYS_PER_ROW;
        if (dayInRow != 0) {
            days.get(dayInRow - 1).setChecked(true, true);
            return;
        }
        // A full row of seven: celebrate, then clear the row for the next week.
        days.get(StreakCalculator.DAYS_PER_ROW - 1).setChecked(true, true);
        final SweetAlertDialog dialog = new SweetAlertDialog(this, SweetAlertDialog.SUCCESS_TYPE);
        dialog.setTitleText(getString(R.string.streak_milestone_title, streak));
        dialog.setConfirmText(getString(R.string.streak_milestone_confirm));
        dialog.setCancelable(false);
        dialog.setConfirmClickListener(sweetAlertDialog -> {
            dialog.dismissWithAnimation();
            for (int i = 0; i < days.size(); i++) {
                final SmoothCheckBox day = days.get(i);
                mainHandler.postDelayed(() -> day.setChecked(false, true), 100L * (i + 1));
            }
        });
        dialog.show();
        updateDays();
    }

    private class SessionTimer extends CountDownTimer {

        SessionTimer(long millisInFuture) {
            super(millisInFuture, 1000);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            wheelView.smoothSelectIndex((int) (millisUntilFinished / MINUTE_MS));

            if (vipassanaMode.isChecked() && vipassanaEndSound != 0 && !vipassanaEndStarted
                    && millisUntilFinished < VIPASSANA_END_LEAD_MS) {
                vipassanaEndStarted = sounds.play(vipassanaEndSound, 1);
            }

            if (IntervalBells.shouldRing(sessionMs, lastRemainingMs, millisUntilFinished,
                    prefs.getIntervalMinutes())) {
                sounds.play(R.raw.bell1, 1);
            }
            lastRemainingMs = millisUntilFinished;

            if (notificationBuilder != null) {
                notificationBuilder.setContentText(getString(R.string.notification_time_left,
                        TimerFormatter.formatMillis(millisUntilFinished)));
                int total = (int) sessionMs;
                notificationBuilder.setProgress(total, total - (int) millisUntilFinished, false);
                postNotification();
            }
        }

        @Override
        public void onFinish() {
            finishSession();
        }
    }
}
