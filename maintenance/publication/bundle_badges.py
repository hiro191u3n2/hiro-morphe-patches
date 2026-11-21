"""Generate and check the Manager changelog entries for a Hiro bundle release."""
import argparse
import re
from pathlib import Path
from zipfile import ZipFile

CATALOG = "META-INF/hiro-publication/manager-catalog.tsv"
# Some packages have localized system labels or another name in a co-selected source.
ALIASES = {
    "com.amazon.mShop.android.shopping": ("Amazon Shopping", "Amazon ショッピング", "Amazon"),
    "com.aurora.store": ("Aurora Store",),
    "com.discord": ("Discord",), "com.discord.venus": ("Venus", "Discord"),
    "com.google.android.apps.maps": ("Google Maps", "マップ"),
    "org.ungoogled.android.apps.maps.microg": ("microG Maps", "Google Maps", "マップ"),
    "org.fossify.calendar": ("Fossify Calendar", "カレンダー"),
    "org.fossify.gallery": ("Fossify Gallery", "Gallery", "ギャラリー"),
    "com.instagram.android": ("Instagram",),
    "com.mxtech.videoplayer.pro": ("MX Player Pro", "MX プレーヤー Pro"),
    "com.ss.android.ugc.trill": ("TikTok",), "com.zhiliaoapp.musically": ("TikTok",),
    "com.twitter.android": ("Twitter", "X", "NewX"),
    "com.touchtype.swiftkey.beta": ("SwiftKey Beta", "Microsoft SwiftKey Beta Keyboard"),
    "jp.ddo.sugihiro.quicksearch": ("簡単検索くん",),
    "jp.co.yahoo.android.apps.transit": ("Yahoo!乗換案内", "Y!乗換案内"),
}
def catalog(data):
    rows = [line.split("\t") for line in data.splitlines()]
    if not rows or any(len(row) != 3 or not row[0] or not row[2] or "`" in row[2] for row in rows):
        raise ValueError("Invalid catalog. Generate it from the actual MPP with BundleCatalog.")
    return rows

def badge_lines(data):
    rows = catalog(data)
    names = {name for pkg, name, _ in rows if name and pkg != "*"}
    for pkg, name, _ in rows:
        names.update(ALIASES.get(pkg, ()))
        if pkg != "*" and not name and pkg not in ALIASES and not any(p == pkg and n for p,n,_ in rows):
            raise ValueError("Add at least one known Manager label for "+pkg)
    scoped = [f"* **{name}:** Hiro総合の配布版を更新しました。" for name in sorted(names)]
    # Manager also matches an unscoped bullet quoting any patch in the saved selection.
    # This covers translated labels, clone names and packages targeted by universal patches.
    selected = [f"* `{patch}` を含むHiro総合の新版を配布しました。" for patch in sorted({r[2] for r in rows})]
    return scoped + selected

def check(bundle, version, changelog):
    with ZipFile(bundle) as archive:
        data = archive.read(CATALOG).decode()
    first, *remaining = changelog.splitlines()
    if not first.startswith(f"# [{version}]("):
        raise ValueError("Unexpected latest changelog heading")
    section = []
    for line in remaining:
        if re.match(r"^#{1,3} .*\(\d{4}-\d{2}-\d{2}\)", line):
            break
        section.append(line)
    missing = set(badge_lines(data)) - set(section)
    if missing:
        raise ValueError(f"{len(missing)} Manager badge entries missing: {min(missing)}")

if __name__ == "__main__":
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("catalog", type=Path)
    print("\n".join(badge_lines(cli.parse_args().catalog.read_text())))
