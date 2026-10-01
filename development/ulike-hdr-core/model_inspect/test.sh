#!/usr/bin/env bash
set -euo pipefail
HERE="$(cd -- "$(dirname -- "$0")" && pwd)"
python3 "$HERE/test_model_inspector.py" "$@"
python3 "$HERE/test_bm_graph_metadata.py"
python3 "$HERE/test_legacy_graph_metadata.py"
python3 "$HERE/test_legacy_face_template.py"
