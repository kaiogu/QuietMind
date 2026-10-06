# QuietMind
Beautiful timer for silent meditation. Simple.

![Screenshot](screenshot.png)

Download on Google Play Store: https://play.google.com/store/apps/details?id=ragone.io.quietmind

## Building

Requires JDK 17 and the Android SDK (platform 35).

```sh
./gradlew testDebugUnitTest   # JVM unit tests for the streak, stats and timer logic
./gradlew lintDebug
./gradlew assembleDebug       # app/build/outputs/apk/debug/
```

CI runs all three on every push (`.github/workflows/android.yml`).

## Sounds

`app/src/main/res/raw/bell1.ogg` (interval and closing bell) and `bell2.ogg` (opening bell) are
synthesized by `tools/generate_bells.py`.

Vipassanā mode can play S. N. Goenka's opening and closing chants. The recordings are not
distributed with this repository. To include them, add `vipassanastart` and `vipassanaend` audio
files (for example `.mp3` or `.ogg`) to `app/src/main/res/raw/`. The closing chant starts 13 min
29.4 s before the end of the session. Without the recordings, Vipassanā mode uses the bells.
