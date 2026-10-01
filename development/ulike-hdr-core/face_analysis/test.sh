#!/usr/bin/env bash
set -euo pipefail
task_root=$(cd "$(dirname "$0")" && pwd)
task_build=$(mktemp -d)
trap 'rm -rf "$task_build"' EXIT
task_javac=${JAVA_HOME:+$JAVA_HOME/bin/}javac
task_java=${JAVA_HOME:+$JAVA_HOME/bin/}java
mapfile -t task_sources < <(find "$task_root/src/main/java" "$task_root/src/test/java" "$task_root/../hdr_input/src/main/java" -name '*.java' -type f)
"$task_javac" -d "$task_build" "${task_sources[@]}"
"$task_java" -cp "$task_build" com.hiro.ulike.hdr.input.StillFaceAnalysisTest
