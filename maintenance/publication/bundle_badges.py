"""Generate and check the Manager changelog entries for a Hiro bundle release."""
import argparse
import re
from pathlib import Path

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

def read_changes(path):
    """TSV columns: added/updated/removed, original package, exact Manager patch name."""
    changes = []
    for number, line in enumerate(path.read_text().splitlines(), 1):
        if not line or line.startswith("#"):
            continue
        fields = line.split("\t")
        if (len(fields) != 3 or fields[0] not in {"added", "updated", "removed"}
                or not all(fields) or "`" in fields[2]):
            raise ValueError(f"Invalid change at {path}:{number}")
        changes.append(tuple(fields))
    return changes


def badge_lines(rows, changes):
    lines = set()
    pairs = {(pkg, patch) for pkg, _, patch in rows}
    for kind, pkg, patch in changes:
        if kind != "removed" and (pkg, patch) not in pairs:
            raise ValueError(f"Unknown package/patch: {pkg} / {patch}")
        if kind == "added":
            # No saved selection can contain a patch that did not exist yet.
            labels = {label for package, label, _ in rows if pkg == package and label}
            labels.update(ALIASES.get(pkg, ()))
            if not labels:
                raise ValueError(f"Missing app label for added patch: {pkg}")
            lines.update(f"* **{name}:** 「{patch}」を追加しました。" for name in labels)
        else:
            action = "更新" if kind == "updated" else "削除"
            lines.add(f"* `{patch}` を{action}しました。")
    return sorted(lines)


def check(rows, changes, version, changelog):
    sections = re.split(r"(?=^#{1,3} .*\(\d{4}-\d{2}-\d{2}\))", changelog, flags=re.M)
    if len(sections) < 2 or sections[0].strip() or not sections[1].startswith(f"# [{version}]("):
        raise ValueError("Unexpected latest changelog heading")
    actual = [line.strip() for line in sections[1].splitlines() if re.match(r"^[*+-]\s", line.strip())]
    if sorted(actual) != badge_lines(rows, changes):
        raise ValueError(f"{version}: changelog bullets must match the declared patch changes")


if __name__ == "__main__":
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument("catalog", type=Path)
    cli.add_argument("changes", type=Path)
    args = cli.parse_args()
    print("\n".join(badge_lines(catalog(args.catalog.read_text()), read_changes(args.changes))))
