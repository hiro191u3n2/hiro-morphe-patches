#!/usr/bin/env bash
set -euo pipefail
HERE=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
mkdir -p "$HERE/build"
g++ -std=c++17 -Wall -Wextra -Werror -pedantic -O2 "$HERE/style_color.cpp" "$HERE/test_style_color.cpp" -o "$HERE/build/test_style_color"
"$HERE/build/test_style_color"
g++ -std=c++17 -Wall -Wextra -Werror -pedantic -O2 "$HERE/style_color.cpp" "$HERE/probe.cpp" -o "$HERE/build/probe"
python3 "$HERE/test_actual_assets.py" "$@"
