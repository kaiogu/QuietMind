package ragone.io.quietmind;

import android.app.NotificationManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

/**
 * Quiets the phone for a session and restores it afterwards.
 *
 * Since Android 7, switching to or from silent mode toggles Do Not Disturb and throws a
 * SecurityException unless the user granted the app Do Not Disturb access. Without that
 * access the phone is set to vibrate, which needs no special permission.
 */
final class RingerController {

    private static final String TAG = "RingerController";
    private static final int NOT_CHANGED = -1;

    private final AudioManager audio;
    private final NotificationManager notifications;
    private int savedMode = NOT_CHANGED;

    RingerController(Context context) {
        audio = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        notifications = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /** True when the user would have to grant Do Not Disturb access for full silence. */
    boolean needsDndAccess() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !notifications.isNotificationPolicyAccessGranted();
    }

    void silence() {
        if (savedMode != NOT_CHANGED) {
            return;
        }
        int current = audio.getRingerMode();
        if (current != AudioManager.RINGER_MODE_NORMAL) {
            // Already quiet: leave the user's choice alone and don't "restore" it later.
            return;
        }
        int target = needsDndAccess() ? AudioManager.RINGER_MODE_VIBRATE : AudioManager.RINGER_MODE_SILENT;
        try {
            audio.setRingerMode(target);
            savedMode = current;
        } catch (SecurityException e) {
            Log.w(TAG, "Not allowed to change the ringer mode", e);
        }
    }

    void restore() {
        if (savedMode == NOT_CHANGED) {
            return;
        }
        try {
            audio.setRingerMode(savedMode);
        } catch (SecurityException e) {
            Log.w(TAG, "Not allowed to restore the ringer mode", e);
        }
        savedMode = NOT_CHANGED;
    }
}
