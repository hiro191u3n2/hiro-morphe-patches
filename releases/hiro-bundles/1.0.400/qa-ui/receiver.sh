#!/usr/bin/env bash
set -euo pipefail
bt=$(ls -dv "$ANDROID_HOME"/build-tools/* | tail -1)
sdkjar=$(find "$ANDROID_HOME/platforms" -name android.jar | sort -V | tail -1)
mkdir -p receiver/classes receiver/dex
cat > receiver/AndroidManifest.xml <<'MANIFEST'
<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.instagram.android" android:versionCode="1" android:versionName="test-fixture">
<uses-sdk android:minSdkVersion="31" android:targetSdkVersion="35"/>
<application android:label="QA link receiver"><activity android:name=".Receiver" android:exported="true">
<intent-filter android:autoVerify="true"><action android:name="android.intent.action.VIEW"/><category android:name="android.intent.category.DEFAULT"/><category android:name="android.intent.category.BROWSABLE"/><data android:scheme="https"/><data android:host="www.instagram.com"/><data android:host="instagram.com"/></intent-filter>
</activity></application></manifest>
MANIFEST
cat > receiver/Receiver.java <<'JAVA'
package com.instagram.android;
public class Receiver extends android.app.Activity {
    public void onCreate(android.os.Bundle state) {
        super.onCreate(state);
        android.util.Log.i("BerryTestReceiver", String.valueOf(getIntent().getData()));
        android.widget.TextView text = new android.widget.TextView(this);
        text.setText(String.valueOf(getIntent().getData()));
        setContentView(text);
    }
}
JAVA
javac -source 8 -target 8 -classpath "$sdkjar" -d receiver/classes receiver/Receiver.java
"$bt/d8" --lib "$sdkjar" --min-api 31 --output receiver/dex receiver/classes/com/instagram/android/Receiver.class
"$bt/aapt2" link -I "$sdkjar" --manifest receiver/AndroidManifest.xml -o receiver/unsigned.apk
(cd receiver/dex && zip -q ../unsigned.apk classes.dex)
"$bt/zipalign" -f 4 receiver/unsigned.apk receiver/aligned.apk
mkdir -p ig
"$bt/apksigner" sign --ks debug.jks --ks-pass pass:android --out ig/qa-instagram-link-receiver.apk receiver/aligned.apk
