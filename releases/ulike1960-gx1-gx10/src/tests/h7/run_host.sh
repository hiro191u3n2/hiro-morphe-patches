#!/usr/bin/env bash
set -euo pipefail
source_dir="$(cd "$(dirname "$0")/../.." && pwd)"
build_dir="${1:-/tmp/ulike-h7-host}"
mkdir -p "$build_dir"
"${CC:-cc}" -std=c11 -O3 -Wall -Wextra -Werror -I "$source_dir/tests/h7" \
  "$source_dir/tests/h7/baseline_core1950.c" "$source_dir/native1950/core1950.c" \
  "$source_dir/tests/h7/test_h7.c" -lm -o "$build_dir/test-h7"
"$build_dir/test-h7"
"${CC:-cc}" -std=c11 -O3 -Wall -Wextra -Werror -I "$source_dir/tests/h7" \
  "$source_dir/native1950/core1950.c" "$source_dir/native1950/test_copy1950.c" \
  -o "$build_dir/test-h7-copy"
"$build_dir/test-h7-copy"
