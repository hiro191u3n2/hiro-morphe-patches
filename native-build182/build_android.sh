#!/usr/bin/env bash
set -euo pipefail
if [[ $# -ne 2 ]]; then
  echo "Usage: build_android.sh NDK_ROOT OUTPUT_SO" >&2
  exit 2
fi
module_root=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
ndk_root=$(cd -- "$1" && pwd)
output_so=$2
python3 - "$ndk_root/source.properties" <<'PY'
import pathlib,re,sys
s=pathlib.Path(sys.argv[1]).read_text()
if not re.search(r'^Pkg\.Revision\s*=\s*27\.2\.12479018\s*$',s,re.M):
    raise SystemExit('Pinned Android NDK 27.2.12479018 required')
PY
clang="$ndk_root/toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang++"
mkdir -p -- "$(dirname -- "$output_so")"
export SOURCE_DATE_EPOCH=1790990400
"$clang" -std=c++17 -O2 -fPIC -shared -fno-exceptions -fno-rtti \
  -fvisibility=hidden -fno-ident -Wall -Wextra -Werror -nostdlib++ \
  "-ffile-prefix-map=$module_root=/hiro-readback" "-fdebug-prefix-map=$module_root=/hiro-readback" \
  -Wl,--build-id=none -Wl,-soname,libhiro_draw_readback.so \
  -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=4096 -Wl,-z,relro,-z,now \
  "$module_root/native/readback_core.cpp" "$module_root/native/native_capture.cpp" \
  -lGLESv3 -lEGL -ldl -llog -pthread -o "$output_so"
"$ndk_root/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" --strip-unneeded "$output_so"
