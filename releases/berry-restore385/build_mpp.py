"""Build both Berry release bundles from the published v384 archives and a new runtime."""
import argparse
from hashlib import sha256
import importlib.util
import json
from pathlib import Path
from tempfile import TemporaryDirectory

MANIFEST = "META-INF/MANIFEST.MF"
EXTENSION = "extensions/berry_bfcache_restore.mpe"
PREVIOUS_EXTENSION = "50fa3b5879ebf02c463f80cfba84adfe77080192c28ae8e29752486f2c7b82a4"
BASES = {
    "Hiro_Morphe_Patches_v1.0.384.mpp": (
        "1.0.385", "12d01d30b4cd602e6b4f071661aad0613ef558245ba4084284464ca31e1bd498",
    ),
    "Berry_Browser_v1.4.7.mpp": (
        "1.4.8", "d4b255d538bc3bf744700132c50f81ab883657ba0073cbb5cb1110ca9923ac83",
    ),
}


def build(args):
    spec = importlib.util.spec_from_file_location("zip_helpers", args.zip_helper)
    writer = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(writer)
    patch = args.extension.read_bytes()
    patch_sha = sha256(patch).hexdigest()
    if not patch.startswith(b"dex\n") or patch_sha == PREVIOUS_EXTENSION:
        raise ValueError("Expected the updated DEX runtime")
    args.output.mkdir(parents=True, exist_ok=True)
    report = {
        "result": "PASS",
        "changed_entries": [MANIFEST, EXTENSION],
        "previous_extension_sha256": PREVIOUS_EXTENSION,
        "extension_sha256": patch_sha,
        "artifacts": {},
    }
    with TemporaryDirectory() as temp:
        for name, (version, expected) in BASES.items():
            base = args.previous / name
            if sha256(base.read_bytes()).hexdigest() != expected:
                raise ValueError(f"Published baseline hash mismatch: {name}")
            entries = writer.archive(base)
            if sha256(entries[EXTENSION]).hexdigest() != PREVIOUS_EXTENSION:
                raise ValueError(f"Unexpected baseline runtime: {name}")
            after = entries.copy()
            fields = writer.headers(entries[MANIFEST])
            previous_version = fields["Version"]
            fields.update(
                Version=version,
                Timestamp="2026-11-18T15:52:00",
                Description="Berry Browser 1.4.8: update WebView history restoration during rapid navigation.",
            )
            after[MANIFEST] = writer.manifest(fields)
            after[EXTENSION] = patch
            path = args.output / name.replace(previous_version, version)
            retained = writer.write_combined_zip(
                path, after, base, entries, Path(temp) / "changed.zip",
            )
            if retained != len(entries) - 2 or writer.archive(path) != after:
                raise RuntimeError(f"Rebuilt MPP differs from its intended entries: {name}")
            report["artifacts"][path.name] = {
                "bytes": path.stat().st_size, "sha256": sha256(path.read_bytes()).hexdigest(),
            }
    (args.output / "build-evidence.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    for argument in ("zip-helper", "previous", "extension", "output"):
        parser.add_argument("--" + argument, type=Path, required=True)
    build(parser.parse_args())
