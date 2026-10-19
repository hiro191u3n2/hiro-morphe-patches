#!/bin/sh
set -eu
if [ "$#" -ne 3 ]; then
  echo "Usage: $0 PUBLISHED124_RUNTIME_CLASSES PUBLISHED124_HOST_CLASSES OUTPUT_DIR" >&2
  exit 2
fi
baseline_runtime=$1
baseline_hosts=$2
probe_out=$3
probe_source=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
mkdir -p "$probe_out/classes"
javac -source 8 -target 8 -cp "$baseline_runtime:$baseline_hosts" -d "$probe_out/classes"   "$probe_source/X124EpochRepro.java" "$probe_source/X124Api33FallbackRepro.java" "$probe_source/X124MissingObserverRepro.java"
probe_cp="$probe_out/classes:$baseline_runtime:$baseline_hosts"
java -cp "$probe_cp" app.hiro.oneback.runtime.X124EpochRepro > "$probe_out/key-baseline-repro.txt"
java -cp "$probe_cp" app.hiro.oneback.runtime.X124Api33FallbackRepro > "$probe_out/api33-baseline-repro.txt"
java -cp "$probe_cp" app.hiro.oneback.runtime.X124MissingObserverRepro > "$probe_out/observer-baseline-repro.txt"
for report in key-baseline-repro.txt api33-baseline-repro.txt observer-baseline-repro.txt; do
  diff -u "$probe_source/$report" "$probe_out/$report"
done
