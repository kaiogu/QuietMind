package ragone.io.quietmind;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/** Plays short sounds and owns every MediaPlayer it creates until it completes or is stopped. */
final class SoundPlayer {

    private static final String TAG = "SoundPlayer";

    private final Context context;
    private final Set<MediaPlayer> active = new HashSet<>();

    SoundPlayer(Context context) {
        this.context = context.getApplicationContext();
    }

    /** Plays {@code resId} {@code times} times back to back. Returns false if it can't be played. */
    boolean play(int resId, int times) {
        MediaPlayer player = MediaPlayer.create(context, resId);
        if (player == null) {
            Log.w(TAG, "Could not create a player for resource " + resId);
            return false;
        }
        active.add(player);
        final int[] repeatsLeft = {times - 1};
        player.setOnCompletionListener(mp -> {
            if (repeatsLeft[0] > 0) {
                repeatsLeft[0]--;
                mp.seekTo(0);
                mp.start();
            } else {
                active.remove(mp);
                mp.release();
            }
        });
        player.start();
        return true;
    }

    /** Stops and releases everything still playing. */
    void stopAll() {
        for (MediaPlayer player : new ArrayList<>(active)) {
            try {
                player.stop();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Player was not in a stoppable state", e);
            }
            player.release();
        }
        active.clear();
    }
}
