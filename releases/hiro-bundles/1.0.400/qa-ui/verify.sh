set -eu
adb shell input keyevent KEYCODE_WAKEUP
adb shell wm dismiss-keyguard
adb shell wm size 480x800
adb shell wm density 160
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0
adb shell pm set-app-links-user-selection --user 0 --package com.instagram.android true www.instagram.com instagram.com
adb shell pm get-app-links --user 0 com.instagram.android | tail -30
adb reverse tcp:8787 tcp:8787
adb logcat -c
