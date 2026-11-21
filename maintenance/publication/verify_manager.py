"""Check the real MPP catalog and Manager release's badge rules on the release runner."""
import hashlib
import subprocess
import tempfile
from pathlib import Path
from zipfile import ZipFile

from bundle_badges import CATALOG, check

HERE = Path(__file__).resolve().parent
MANAGER = "c783f7683580d9c61b3f6de4411794a162ecb476"
SOURCE = f"https://raw.githubusercontent.com/MorpheApp/morphe-manager/{MANAGER}/app/src/main/java/app/morphe/manager/util/"
DESKTOP = "https://github.com/MorpheApp/morphe-desktop/releases/download/v2.3.0-dev.3/morphe-desktop-2.3.0-dev.3-all.jar"

def fetch(url, expected, to):
    subprocess.run(["curl", "--fail", "--silent", "--show-error", "--location", "--retry", "3", url, "-o", to], check=True)
    with to.open("rb") as handle:
        if hashlib.file_digest(handle, "sha256").hexdigest() != expected:
            raise ValueError("Unexpected upstream bytes for " + to.name)

def verify(mpp, previous, version, markdown):
    check(mpp, version, markdown)
    with tempfile.TemporaryDirectory() as work:
        tmp = Path(work)
        jar = tmp / "desktop.jar"
        fetch(DESKTOP, "2f95d8784a21139abd574b9ca3f17c255e465fce74e0c1e30632671efeb749ce", jar)
        subprocess.run(["javac", "-encoding", "UTF-8", "-cp", jar, "-d", tmp, HERE/"BundleCatalog.java"], check=True)
        catalog = tmp/"catalog.tsv"
        with catalog.open("wb") as out:
            subprocess.run(["java", "-cp", f"{tmp}:{jar}", "BundleCatalog", mpp], stdout=out, check=True)
        with ZipFile(mpp) as archive:
            if catalog.read_bytes() != archive.read(CATALOG):
                raise ValueError("Badge catalog does not match the patches loaded from the MPP")
        for name, digest in {
            "ChangelogParser.kt":"dcc92d623e0cf9bb2acabfafdf5bfde1f459f7a8e8a8e363699fafe58c6adc11",
            "VersionUtils.kt":"5e4f363ec1285e57d131ded2f3e2ebcac938af5a7ef3e9816f267cc00c6dc3b7",
        }.items():
            fetch(SOURCE + name, digest, tmp/name)
        out = tmp/"check.jar"
        subprocess.run(["kotlinc", tmp/"ChangelogParser.kt", tmp/"VersionUtils.kt", HERE/"ManagerBadges.kt",
                        HERE/"HostConfig.kt", HERE/"BuildConfig.kt", "-include-runtime", "-d", out], check=True)
        notes = tmp/"CHANGELOG.md"
        notes.write_text(markdown)
        subprocess.run(["java", "-jar", out, catalog, notes, previous, version], check=True)
