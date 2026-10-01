#!/usr/bin/env bash
# Desktop build checks only. No Android device installation/execution is implied.
set -euo pipefail
SOURCE=$(cd "$(dirname "$0")" && pwd)
if [[ $# != 6 ]]; then echo 'Usage: validate.sh ORIGINAL_APKS STANDALONE_MPP INTEGRATED_MPP BUILD TOOLS QA_OUTPUT' >&2; exit 2; fi
INPUT=$(realpath "$1"); STANDALONE=$(realpath "$2"); INTEGRATED=$(realpath "$3"); BUILD=$(realpath "$4"); TOOLS=$(realpath "$5")
mkdir -p "$6"; QA=$(realpath "$6"); JDK_DIR=${ULIKE_JDK_DIR:-"$TOOLS/jdk21-clean"}; JAVA="$JDK_DIR/bin/java"; CP="$BUILD/java:$TOOLS/morphe-1.16.jar:$BUILD/old"
# Keep live resource extraction outside the synchronized project tree. A temporary
# sibling created by an external copier must never become a resource APK input.
VALIDATION_ROOT=$(mktemp -d /tmp/ulike169-validate.XXXXXX)
trap 'rm -rf "$VALIDATION_ROOT"' EXIT
INTEGRATED_STATUS=not-run
for RUN in '1.16 standalone' '1.16 integrated'; do
 read -r VERSION KIND <<< "$RUN"
 if [[ "$KIND" == standalone ]]; then PATCH="$STANDALONE"; else PATCH="$INTEGRATED"; fi
 LABEL="morphe-${VERSION}-${KIND}"
 mkdir -p "$VALIDATION_ROOT/$LABEL"
 LIVE="$VALIDATION_ROOT/$LABEL"
 # Remove only this script's generated outputs so no previous run can satisfy a gate.
 rm -f "$QA/${LABEL}-patched.apk" "$QA/${LABEL}-result.json" \
  "$QA/${LABEL}-dex-verification.txt" "$QA/${LABEL}-resource-verification.json" "$QA/${LABEL}-resource-verification.txt"
 if "$JAVA" -Xmx3g -jar "$TOOLS/morphe-${VERSION}.jar" patch "$INPUT" \
  -p "$PATCH" --exclusive -e '高画質撮影・質感美肌・素材通信を復旧' \
  --unsigned --bytecode-mode FULL -o "$LIVE/patched.apk" \
  -t "$LIVE/temp" -r "$LIVE/result.json" > "$QA/${LABEL}.log" 2>&1; then
  :
 else
  STATUS=$?
  if [[ -f "$LIVE/result.json" ]]; then cp "$LIVE/result.json" "$QA/${LABEL}-result.json"; fi
  if [[ "$KIND" == standalone ]]; then echo "FAIL $LABEL (exit $STATUS); see $QA/${LABEL}.log" >&2; exit "$STATUS"; fi
  INTEGRATED_STATUS="failed-cli-exit-${STATUS}"
  echo "Integrated loader/application check failed (exit $STATUS); retained full log. No unrelated app patch was changed." | tee "$QA/integrated-application-status.txt"
  exit "$STATUS"
 fi
 cp "$LIVE/result.json" "$QA/${LABEL}-result.json"
 cp "$LIVE/patched.apk" "$QA/${LABEL}-patched.apk"
 cmp "$LIVE/patched.apk" "$QA/${LABEL}-patched.apk"
 "$JAVA" -Xmx2g -cp "$CP" VerifyApplied "$BUILD/runtime.dex" "$BUILD/methods.tsv" "$QA/${LABEL}-patched.apk" | tee "$QA/${LABEL}-dex-verification.txt"
  python3 "$SOURCE/verify_applied_resources.py" --apks "$INPUT" --mpp "$PATCH" \
  --apk "$QA/${LABEL}-patched.apk" --result "$QA/${LABEL}-result.json" --report "$QA/${LABEL}-resource-verification.json" --native-contract nv21-effect-flag | tee "$QA/${LABEL}-resource-verification.txt"
 if [[ "$KIND" == integrated ]]; then INTEGRATED_STATUS=passed; fi
done
echo "$INTEGRATED_STATUS" > "$QA/integrated-application-status.txt"
test "$INTEGRATED_STATUS" = passed
echo 'PASS private standalone and integrated on Morphe1.16: all payload contracts, exact four-byte native change, two exact ORT native additions, four notices, other native libraries and unrelated assets.'
echo "Integrated application status: $INTEGRATED_STATUS"
echo 'Desktop checks only; Android launch, camera behavior, capture quality and device HDR behavior remain untested.'
