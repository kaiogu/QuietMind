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

No audio is distributed with this repository. The original app's recordings were never published
here, and their licensing is unknown. The app looks for these optional files in
`app/src/main/res/raw/` (any format Android plays, for example `.ogg` or `.mp3`):

| File | Played |
|---|---|
| `bell2` | when a session starts |
| `bell1` | at each interval, and three times when a session ends |
| `vipassanastart` | when a Vipassanā session starts, instead of `bell2` |
| `vipassanaend` | starting 13 min 29.4 s before a Vipassanā session ends, instead of the closing bells |

Only add recordings you have the right to distribute. S. N. Goenka's chanting is copyrighted;
don't bundle it without written permission from the Vipassana Research Institute. When a bell
is missing, the app plays the phone's default notification sound instead.
