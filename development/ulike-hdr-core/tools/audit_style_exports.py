#!/usr/bin/env python3
"""Verify user-exported style ZIPs without executing their Lua or shaders.

Reports observed resources and unresolved named model dependencies. Resource
availability is not proof that the native SDK executed the same graph.
"""
import argparse
import collections
import hashlib
import json
from pathlib import Path, PurePosixPath
import stat
import zipfile


def audit(path):
    path = Path(path)
    with zipfile.ZipFile(path) as archive:
        infos = archive.infolist()
        names = [info.filename for info in infos]
        if len(set(names)) != len(names):
            raise ValueError("duplicate archive entry")
        if len(names) > 10000 or sum(info.file_size for info in infos) > 256 * 1024 * 1024:
            raise ValueError("archive exceeds analysis budget")
        for info in infos:
            part = PurePosixPath(info.filename)
            if part.is_absolute() or ".." in part.parts or "\\" in info.filename:
                raise ValueError("unsafe archive member")
            if stat.S_ISLNK(info.external_attr >> 16):
                raise ValueError("symlink archive member")
        if archive.testzip() is not None:
            raise ValueError("CRC failure")
        manifest = json.loads(archive.read("manifest.json"))
        if manifest["schema"] != "ulike-style-materials-164":
            raise ValueError("unsupported export schema")
        copy = manifest["material_copy"]
        entries = [entry for entry in copy["files"] if entry["status"] == "copied"]
        copied_names = [entry["archive_path"] for entry in entries]
        actual_names = [info.filename for info in infos if not info.is_dir() and info.filename != "manifest.json"]
        if len(copied_names) != len(set(copied_names)) or set(copied_names) != set(actual_names):
            raise ValueError("manifest/ZIP file inventory mismatch")
        if len(entries) != copy["copied_file_count"]:
            raise ValueError("copied count mismatch")
        verified_bytes = 0
        for entry in entries:
            data = archive.read(entry["archive_path"])
            if len(data) != entry["bytes"] or hashlib.sha256(data).hexdigest() != entry["sha256"]:
                raise ValueError("material digest or length mismatch")
            if entry["source_changed_while_copying"]:
                raise ValueError("material changed during export")
            verified_bytes += len(data)
        if verified_bytes != copy["copied_bytes"]:
            raise ValueError("copied byte count mismatch")
        selected = manifest["selected_style_material"]
        roots = [root for root in copy["roots"] if root["requested_path"] == selected["unzip_path"]]
        if len(roots) != 1 or roots[0]["status"] != "copied_observed_resource":
            raise ValueError("selected style resource is unresolved")
        root = roots[0]["archive_root"] + "/"
        config = json.loads(archive.read(root + "config.json"))
        algorithm = json.loads(archive.read(root + "algorithmConfig.json"))
        creator = json.loads(archive.read(root + "creator_metaInfos.json"))
        models = sorted({model for group in creator.get("modelNames", {}).values() for model in group})
        model_keys = []
        for node in algorithm.get("nodes", []):
            parameters = node.get("config", {}).get("keyMaps", {}).get("stringParam", {})
            for key in ("model_name", "idream_model_key"):
                if key in parameters:
                    model_keys.append({"node": node["name"], "type": node["type"], "key": key, "value": parameters[key]})
        observed_updates = []
        for event in manifest["api_events"]:
            if event.get("operation") == "update" and selected["unzip_path"] in (event.get("paths") or []):
                observed_updates.append({
                    "sequence": event["sequence"],
                    "keys": event.get("keys"), "values_float": event.get("values_float"),
                    "return_code": event.get("return_code"),
                    "completion_sequence": event.get("completion_sequence"),
                })
        graph = [{"path": entry["path"], "type": entry["type"], "zorder": entry.get("zorder")}
                 for entry in config["effect"]["Link"]]
        # The names identify missing dependencies, not their internal format or
        # device location. No credential, license or unrelated file is inspected.
        candidates = {model: [name for name in actual_names if model.lower() in name.lower()] for model in models}
        return {
            "archive": path.name,
            "archive_sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
            "schema": manifest["schema"],
            "style_id": manifest["style_id"],
            "style_label": manifest["style_label"],
            "verified_material_files": len(entries),
            "verified_material_bytes": verified_bytes,
            "selected_style_files": roots[0]["copied_files"],
            "resource_roots": len(copy["roots"]),
            "limits_reached": copy["limits_reached"],
            "observer_unchanged_during_copy": copy["observer_unchanged_during_copy"],
            "all_dependencies_resolved_by_exporter": copy["all_dependencies_resolved"],
            "selected_native_use_confirmed": selected["active_native_use_confirmed"],
            "selected_style_parameter_api_events": observed_updates,
            "parameter_success_is_native_execution_proof": False,
            "selected_graph_declared": graph,
            "algorithm_nodes_declared": [{"name": n["name"], "type": n["type"]} for n in algorithm["nodes"]],
            "algorithm_branch_size_declared": algorithm.get("size"),
            "branch_size_is_saved_image_size_proof": False,
            "named_model_calls": model_keys,
            "declared_model_names": models,
            "model_filename_matches": candidates,
            "model_dependency_resolution": "unresolved; names/configuration are not model weights",
            "file_suffix_counts": dict(sorted(collections.Counter(PurePosixPath(n).suffix for n in actual_names).items())),
            "observer_completeness": manifest["completeness"],
            "full_style_reproduction_verified": False,
            "hdr_runtime_integration_verified": False,
        }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("archives", nargs="+", type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()
    report = {"schema": "ulike-style-audit-1", "styles": [audit(path) for path in args.archives]}
    rendered = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    else:
        print(rendered, end="")


if __name__ == "__main__":
    main()
