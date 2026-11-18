"""Compile the Berry 1.4.8 runtime using the existing v339 toolchain and tests."""
import argparse
from hashlib import sha256
from pathlib import Path
import shutil
import subprocess
import sys
from tempfile import TemporaryDirectory

HERE = Path(__file__).resolve().parent
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--java", default="java")
parser.add_argument("--tools", required=True, type=Path, help="ECJ and D8 tool directory")
parser.add_argument("--output", required=True, type=Path)
args = parser.parse_args()
tools = args.tools.resolve()
for name, pin in {
    "ecj.jar": "74c93172475fee0cd4e6eefb44233516dd40525642da35d009146d97ea598229",
    "d8.jar": "38a9ffaaebbd17d1514e3ecbff5a85b989bb13c551b9e224c7409514eea84ff5",
}.items():
    if sha256((tools / name).read_bytes()).hexdigest() != pin:
        raise ValueError(f"Tool digest differs from the release toolchain: {name}")
with TemporaryDirectory() as temp:
    runtime = Path(temp) / "runtime"
    shutil.copytree(HERE.parent / "berry-repair339" / "runtime", runtime)
    for directory in ("runtime_src", "runtime_stubs", "runtime_tests"):
        source = HERE / directory
        if source.is_dir():
            shutil.copytree(source, runtime / directory, dirs_exist_ok=True)
    subprocess.run([
        sys.executable, str(runtime / "build_runtime.py"),
        "--java", args.java, "--ecj", str(tools / "ecj.jar"),
        "--d8", str(tools / "d8.jar"), "--output-dir", str(args.output.resolve()),
    ], check=True)
