#!/usr/bin/env bash
set -euo pipefail
if [[ $# != 2 ]]; then
  echo 'Usage: JAVA_HOME=/path/jdk bash test.sh /path/Natural.zip /path/Purity.zip' >&2
  exit 2
fi
style_root=$(cd "$(dirname "$0")" && pwd)
style_build=$(mktemp -d)
trap 'rm -rf "$style_build"' EXIT
style_java=${JAVA_HOME:+"$JAVA_HOME/bin/"}java
style_javac=${JAVA_HOME:+"$JAVA_HOME/bin/"}javac
mapfile -t style_sources < <(rg --files "$style_root/src" -g '*.java' | sort)
"$style_javac" --release 8 -Xlint:-options -d "$style_build" "${style_sources[@]}"
"$style_java" -cp "$style_build" com.hiro.ulike.style.PipelineTest
python "$style_root/verify_actual_assets.py" "$1" "$2" --java "$style_java" --classes "$style_build" --output "$style_root/QA_ACTUAL_ASSETS.json"
