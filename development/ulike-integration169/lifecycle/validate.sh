#!/usr/bin/env bash
set -euo pipefail
HERE=$(cd "$(dirname "$0")" && pwd)
WORK=$(cd "$HERE/../.." && pwd)
TOOLS="$WORK/models168/tools"
JDK="$TOOLS/jdk21/jdk-21.0.12.1+1/bin"
PRIVATE="$HERE/private_build"
mkdir -p "$PRIVATE/tools"
CP="$TOOLS/morphe-1.16.jar:$HERE/../tool_baseline:$HERE/../tools:$PRIVATE/tools"
"$JDK/javac" -cp "$CP" -d "$PRIVATE/tools" "$HERE/Inventory169.java" "$HERE/PrepareNativeLifetime169.java" "$HERE/VerifyNativeLifetime169.java"
"$JDK/java" -Xmx2g -cp "$CP" Inventory169 "$WORK/models168/inputs/base.apk" "$PRIVATE/stock-references.tsv"
python3 "$HERE/verify_inventory.py" --stock-apk "$WORK/models168/inputs/base.apk" --references "$PRIVATE/stock-references.tsv" --output "$HERE/INVENTORY.json"
"$JDK/java" -Xmx2g -cp "$CP" PrepareNativeLifetime169 "$WORK/models168/inputs/base.apk" "$PRIVATE"
"$JDK/java" -Xmx2g -cp "$CP" VerifyNativeLifetime169 "$WORK/models168/inputs/base.apk" "$PRIVATE/native-lifetime-partial.dex"
JAVA_HOME="$TOOLS/jdk21/jdk-21.0.12.1+1" ANDROID_JAR="$TOOLS/android.jar" bash "$WORK/hdr_rebuild167/core/android_still_analysis/test.sh"
