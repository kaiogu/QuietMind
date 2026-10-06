#!/bin/sh
# Installs the debug app on the running emulator, seeds a 3-day streak, and prints a
# base64 screenshot of the main screen into the log (artifacts can't always be fetched).
set -e
PKG=ragone.io.quietmind
./gradlew -q installDebug
TODAY=$(date -u +%d/%m/%Y)
adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/my_prefs.xml'" <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <boolean name="first_time" value="false" />
    <int name="streak" value="3" />
    <int name="longeststreak" value="8" />
    <string name="lastday">$TODAY</string>
</map>
XML
adb shell am start -W -n $PKG/.MainActivity
sleep 4
adb exec-out screencap -p > main.png
python3 -c "import base64,zlib;d=open('main.png','rb').read();print('SCREENSHOT_BEGIN');print(base64.b64encode(zlib.compress(d,9)).decode());print('SCREENSHOT_END')" | fold -w 4000
