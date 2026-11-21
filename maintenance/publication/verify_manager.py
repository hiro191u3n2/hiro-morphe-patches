"""Check the real MPP catalog and Manager release's badge rules on the release runner."""
import hashlib
import re
import subprocess
import tempfile
from pathlib import Path
from zipfile import ZipFile

from bundle_badges import ALIASES, CATALOG, catalog as parse_catalog, check, read_changes

HERE = Path(__file__).resolve().parent
MANAGER = "66428e5cb214b9c7490720c65953331ff40b8e7f"
SOURCE = f"https://raw.githubusercontent.com/MorpheApp/morphe-manager/{MANAGER}/app/src/main/java/app/morphe/manager/util/"
DESKTOP = "https://github.com/MorpheApp/morphe-desktop/releases/download/v2.3.0-dev.3/morphe-desktop-2.3.0-dev.3-all.jar"

def fetch(url, expected, to):
    subprocess.run(["curl", "--fail", "--silent", "--show-error", "--location", "--retry", "3", url, "-o", to], check=True)
    with to.open("rb") as handle:
        if hashlib.file_digest(handle, "sha256").hexdigest() != expected:
            raise ValueError("Unexpected upstream bytes for " + to.name)

def verify(mpp, previous, version, markdown):
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
        release_dir = Path("releases/hiro-bundles")
        android_catalog = release_dir / version / "manager-android-catalog.tsv"
        rows = parse_catalog(catalog.read_text())
        if android_catalog.is_file():
            android = parse_catalog(android_catalog.read_text())
            if not set(map(tuple, rows)) <= set(map(tuple, android)):
                raise ValueError("Android catalog is missing a JVM-selectable patch")
            rows = android

        changes = read_changes(release_dir / version / "manager-changes.tsv")
        check(rows, changes, version, markdown)
        # Keep every release since the over-broad notifications began in the regression matrix.
        baseline = "1.0.391"
        def version_key(tag):
            return tuple(map(int, tag.split(".")))

        sections = {section.split("]", 1)[0][3:]: section.strip() for section in
                    re.split(r"(?=^# \[)", markdown, flags=re.M) if section.startswith("# [")}
        change_rows = []
        for release in sorted(release_dir.iterdir()):
            if not re.fullmatch(r"\d+\.\d+\.\d+", release.name):
                continue
            tag = release.name
            if not version_key(baseline) < version_key(tag) <= version_key(version):
                continue
            if sections.get(tag) != (release / "manager-entry.md").read_text().strip():
                raise ValueError(f"Manager history differs from reviewed entry for {tag}")
            for kind, pkg, patch in read_changes(release / "manager-changes.tsv"):
                change_rows.append((tag, kind, pkg, patch))
        if previous not in sections:
            raise ValueError(f"Missing previous release {previous}")
        history = tmp / "changes.tsv"
        history.write_text("".join("\t".join(change) + "\n" for change in change_rows))
        # Include labels from other sources and legacy patch names still saved in old selections.
        labels = {(pkg, name, patch) for pkg, name, patch in rows}
        for _, _, pkg, patch in change_rows:
            labels.add((pkg, "", patch))
        for pkg, _, patch in list(labels):
            labels.update((pkg, alias, patch) for alias in ALIASES.get(pkg, ()))
        catalog.write_text("".join("\t".join(row) + "\n" for row in sorted(labels)))
        subprocess.run(["java", "-jar", out, catalog, notes, history, baseline, version], check=True)
