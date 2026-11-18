"""Reissue the v384 payloads and compare every ZIP entry with both prior releases."""
import argparse
from hashlib import sha256
import importlib.util
import json
from pathlib import Path
from tempfile import TemporaryDirectory
from zipfile import ZipFile

MANIFEST = "META-INF/MANIFEST.MF"
EXTENSION = "extensions/berry_bfcache_restore.mpe"
TIMESTAMP = "2026-11-18T16:44:00"
DESCRIPTION = "Berry Browser 1.4.9 restores the Berry Browser 1.4.7 patch payload."
RESTORED_RUNTIME = "50fa3b5879ebf02c463f80cfba84adfe77080192c28ae8e29752486f2c7b82a4"
WITHDRAWN_RUNTIME = "116f21b4cc8579480a0dfef1b2c7565007ab14055e4abcd6e398f35847365715"
PACKAGES = (
    ("Hiro_Morphe_Patches", "1.0.384", "12d01d30b4cd602e6b4f071661aad0613ef558245ba4084284464ca31e1bd498",
     "1.0.385", "5e0e7a9fa993838b9821ea655ec74006e4fa73a5d5ff0d94e16198aec2486ae9", "1.0.386"),
    ("Berry_Browser", "1.4.7", "d4b255d538bc3bf744700132c50f81ab883657ba0073cbb5cb1110ca9923ac83",
     "1.4.8", "9d063d65f05da260ba40f2c6bf235692dad890a505538ee320fb4e9c9f993be0", "1.4.9"),
)


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(data):
    return sha256(data).hexdigest()


def compare(writer, original, output, differences):
    source = writer.archive(original)
    target = writer.archive(output)
    with ZipFile(original) as before, ZipFile(output) as after:
        require(before.namelist() == after.namelist(), "Entry order or names differ")
        require(before.comment == after.comment, "ZIP comment differs")
    changed = sorted(name for name in source if source[name] != target[name])
    require(changed == sorted(differences), f"Unexpected ZIP entry changes: {changed}")
    old_local, old_central = writer.zip_records(original)
    new_local, new_central = writer.zip_records(output)
    unchanged = sorted(source.keys() - set(changed))
    entries = sha256()
    for name in unchanged:
        require(old_local[name] == new_local[name], f"Compressed entry differs: {name}")
        old = old_central[name]
        new = new_central[name]
        require(old[:42] == new[:42] and old[46:] == new[46:], f"Directory metadata differs: {name}")
        entries.update(name.encode("utf-8") + b"\0" + sha256(target[name]).digest())
    return {
        "source": original.name,
        "entry_count": len(source),
        "changed_entries": changed,
        "identical_local_records": len(unchanged),
        "identical_entries_name_and_content_sha256": entries.hexdigest(),
    }


def build(args):
    spec = importlib.util.spec_from_file_location("mpp_zip", args.zip_helper)
    writer = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(writer)
    args.output.mkdir(parents=True, exist_ok=True)
    report = {
        "result": "PASS", "timestamp_utc": TIMESTAMP,
        "changed_entries": [MANIFEST, EXTENSION], "changed_entries_vs_restored_release": [MANIFEST],
        "previous_extension_sha256": WITHDRAWN_RUNTIME, "extension_sha256": RESTORED_RUNTIME, "artifacts": {},
    }
    audit = {"result": "PASS", "artifacts": {}}
    with TemporaryDirectory() as temp:
        for name, restored_v, restored_sha, withdrawn_v, withdrawn_sha, version in PACKAGES:
            restored = args.restored / f"{name}_v{restored_v}.mpp"
            withdrawn = args.withdrawn / f"{name}_v{withdrawn_v}.mpp"
            require(digest(restored.read_bytes()) == restored_sha, f"Restore input differs: {restored.name}")
            require(digest(withdrawn.read_bytes()) == withdrawn_sha, f"Withdrawn input differs: {withdrawn.name}")
            source = writer.archive(restored)
            with ZipFile(withdrawn) as last:
                require(digest(last.read(EXTENSION)) == WITHDRAWN_RUNTIME, "Unexpected withdrawn runtime")
            require(digest(source[EXTENSION]) == RESTORED_RUNTIME, "Unexpected restored runtime")
            fields = writer.headers(source[MANIFEST])
            require(fields["Version"] == restored_v, "Restore input version differs")
            current = dict(fields, Version=version, Timestamp=TIMESTAMP, Description=DESCRIPTION)
            contents = dict(source)
            contents[MANIFEST] = writer.manifest(current)
            require(writer.headers(contents[MANIFEST]) == current, "Manifest round-trip differs")
            output = args.output / f"{name}_v{version}.mpp"
            if not args.verify_only:
                writer.write_combined_zip(output, contents, restored, source, Path(temp) / "changed.zip")
            with ZipFile(output) as actual:
                require(actual.read(MANIFEST) == contents[MANIFEST], "Manifest output differs")
            audit["artifacts"][output.name] = {
                "restored": compare(writer, restored, output, [MANIFEST]),
                "withdrawn": compare(writer, withdrawn, output, [MANIFEST, EXTENSION]),
                "manifest_changed_fields_vs_restored": sorted(k for k in fields if fields[k] != current[k]),
            }
            report["artifacts"][output.name] = {"bytes": output.stat().st_size, "sha256": digest(output.read_bytes())}
    for path, value in [(args.output / "build-evidence.json", report), (args.audit, audit)]:
        data = json.dumps(value, indent=2) + "\n"
        if args.verify_only:
            require(path.read_text(encoding="utf-8") == data, f"Recorded evidence differs: {path}")
        else:
            path.write_text(data, encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for arg in ["zip-helper", "restored", "withdrawn", "output", "audit"]:
        parser.add_argument("--" + arg, type=Path, required=True)
    parser.add_argument("--verify-only", action="store_true")
    build(parser.parse_args())
