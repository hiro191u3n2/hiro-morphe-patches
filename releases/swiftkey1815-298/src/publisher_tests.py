#!/usr/bin/env python3
"""Offline publication invariants; never contacts or writes to GitHub."""
from __future__ import annotations

import copy
import io
import json
from pathlib import Path
import tempfile
import unittest
from unittest import mock
import zipfile

import publish1815 as p


def zipped(files):
    stream = io.BytesIO()
    with zipfile.ZipFile(stream, "w") as z:
        for name, raw in files.items():
            z.writestr(name, raw)
    return stream.getvalue()


def expected():
    return {
        "schema": "swiftkey1815-publication-v1",
        "swiftkey_version": p.VERSION, "bundle_version": p.BUNDLE,
        "baseline_bundle_version": p.PREVIOUS, "baseline_bundle_sha256": p.BASE_SHA256,
        "baseline_swiftkey_version": "1.8.14", "baseline_swiftkey_sha256": p.SINGLE_BASE_SHA256,
        "change_plan_reviewed": True, "qa_contract_reviewed": True,
        "android_device_tested": False, "original_apk_apply_tested": False,
        "qa_required_values": copy.deepcopy(p.REQUIRED_QA),
        "allowed_changed_bundle_entries": sorted(p.REQUIRED_CHANGED),
        "allowed_changed_standalone_entries": sorted(p.REQUIRED_CHANGED),
        "allowed_added_bundle_entries": sorted(p.ADDED_JVM), "allowed_added_standalone_entries": sorted(p.ADDED_JVM),
        "artifacts": {name: {"bytes": 1, "sha256": "1" * 64} for name in p.ASSETS},
    }


class PublicationTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        self.addCleanup(self.temp.cleanup)

    def load(self, value):
        path = self.root / "manifest.json"
        path.write_text(json.dumps(value))
        return p.load_expected(path)

    def state(self):
        return {"manifest": {
            "version": p.PREVIOUS, "created_at": "2026-10-26T06:06:38",
            "description": "ULike history\nSwiftKey Beta：v1.8.14（9.13.16.4／1236271168）\nother history",
            "download_url": p.BASE_URL, "extra_preserved": {"a": 1},
        }, "log": "# 1.0.297 (2026-10-25)\n\n* **ULike:** earlier change\n"}

    def test_metadata_preserves_history_and_only_marks_swiftkey(self):
        old = self.state()
        snapshot = copy.deepcopy(old)
        feed, log = p.metadata(old, "https://example.test/new.mpp", "2026-10-27T01:00:00", "候補スクロールの追加更新。上フリック診断を機能ごと削除。実機未確認。")
        self.assertEqual(old, snapshot)
        self.assertEqual(feed["extra_preserved"], old["manifest"]["extra_preserved"])
        self.assertIn("SwiftKey Beta：v1.8.15（9.13.16.4／1236271168）", feed["description"])
        self.assertTrue(log.endswith(old["log"]))
        for app in p.APP_NAMES:
            self.assertTrue(p.has_changes_for(log, p.PREVIOUS, [app]))
            self.assertFalse(p.has_changes_for(log, p.BUNDLE, [app]))
        for app in p.OTHER_APPS:
            self.assertFalse(p.has_changes_for(log, p.PREVIOUS, [app]))

    def test_metadata_rejects_stale_or_ambiguous_inputs(self):
        for mutate in (
            lambda s: s["manifest"].update(version="1.0.298"),
            lambda s: s["manifest"].update(description="no SwiftKey inventory"),
            lambda s: s["manifest"].update(description=s["manifest"]["description"] + "\nSwiftKey Beta：v1.8.14（9.13.16.4）"),
            lambda s: s["manifest"].update(created_at="2026-10-28T00:00:00"),
        ):
            state = self.state()
            mutate(state)
            with self.assertRaises(RuntimeError):
                p.metadata(state, "new", "2026-10-27T01:00:00", "notes")
        with self.assertRaises(RuntimeError):
            p.metadata(self.state(), "new", "2026-10-27T01:00:00", "# 9.0.0 (2026-10-26)\n* **ULike:** wrong")

    def test_expected_contract_rejects_missing_assertions_and_unrelated_entries(self):
        self.load(expected())
        for key, value in (
            ("baseline_bundle_sha256", "0" * 64),
            ("change_plan_reviewed", False), ("qa_contract_reviewed", False),
            ("allowed_changed_bundle_entries", sorted(p.REQUIRED_CHANGED | {"other/app"})),
            ("allowed_added_standalone_entries", ["app/hiro/swiftkey/patches/Extra.class"]),
            ("allowed_added_bundle_entries", sorted(p.ADDED_JVM | {"app/hiro/swiftkey/patches/Extra.class"})),
            ("android_device_tested", True),
        ):
            e = expected()
            e[key] = value
            with self.assertRaises(RuntimeError, msg=key):
                self.load(e)
        for key in p.REQUIRED_QA:
            e = expected()
            del e["qa_required_values"][key]
            with self.assertRaises(RuntimeError, msg=key):
                self.load(e)

    def loader_archives(self):
        old = {p.MF: b"old", p.EXTENSION: b"dex\nold", "classes.dex": b"dex\nloader",
               "swiftkey/dict": b"dict", "other/app": b"other",
               **{name:b"\xca\xfe\xba\xbeold-" + name.encode() for name in p.JVM_CLASSES}}
        new = dict(old)
        for name in p.REQUIRED_CHANGED:
            new[name] = (b"\xca\xfe\xba\xbe" if p.is_swiftkey_jvm(name) else b"dex\n") + b"changed"
        for name in p.ADDED_JVM:
            new[name] = b"\xca\xfe\xba\xbeadded"
        return old, new

    def test_entry_plan_preserves_every_other_byte(self):
        old, new = self.loader_archives()
        p.validate_entry_delta(old, new, expected(), "bundle")
        for name in set(old) - p.REQUIRED_CHANGED:
            damaged = {**new, name: b"changed"}
            with self.assertRaises(RuntimeError, msg=name):
                p.validate_entry_delta(old, damaged, expected(), "bundle")
        with self.assertRaises(RuntimeError):
            p.validate_entry_delta(old, {k: v for k, v in new.items() if k != "other/app"}, expected(), "bundle")

    def test_missing_or_corrupt_loader_is_rejected(self):
        old, new = self.loader_archives()
        p.validate_entry_delta(old, new, expected(), "bundle")
        for name in p.JVM_CLASSES | p.ADDED_JVM:
            with self.assertRaises(RuntimeError, msg=name):
                p.validate_entry_delta(old, {n:b for n,b in new.items() if n != name}, expected(), "bundle")
            with self.assertRaises(RuntimeError, msg=name):
                p.validate_entry_delta(old, {**new, name:b"not a class"}, expected(), "bundle")

    def test_archive_rejects_unsafe_and_duplicate_paths(self):
        for path in ("../app", "/app", "app\\bad"):
            with self.assertRaises(RuntimeError, msg=path):
                p.archive(zipped({path: b"x"}))
        stream = io.BytesIO()
        with zipfile.ZipFile(stream, "w") as z:
            z.writestr("same", b"a")
            with self.assertWarns(UserWarning):
                z.writestr("same", b"b")
        with self.assertRaises(RuntimeError):
            p.archive(stream.getvalue())

    def test_sources_must_match_and_extra_java_cannot_enter_build(self):
        files = {"build1815.py": b"build", "publish1815.py": b"publish", "publisher_tests.py": b"test", "src/Foo.java": b"Foo"}
        pins = {}
        for name, raw in files.items():
            path = self.root / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(raw)
            pins[name] = {"bytes": len(raw), "sha256": p.sha(raw)}
        e = {"source_files": pins}
        self.assertEqual(p.verify_source_files(self.root, e), len(files))
        (self.root / "src/Foo.java").write_bytes(b"changed")
        with self.assertRaises(RuntimeError):
            p.verify_source_files(self.root, e)
        (self.root / "src/Foo.java").write_bytes(b"Foo")
        for directory in ("src", "patch_java"):
            unreviewed = self.root / directory / "Unreviewed.java"
            unreviewed.parent.mkdir(parents=True, exist_ok=True)
            unreviewed.write_bytes(b"extra")
            with self.assertRaises(RuntimeError, msg=directory):
                p.verify_source_files(self.root, e)
            unreviewed.unlink()

    def test_full_artifact_validation_detects_tampering(self):
        def mf(version):
            return ("Manifest-Version: 1.0\r\nName: test\r\nVersion: " + version + "\r\n\r\n").encode()
        common, newcommon = self.loader_archives()
        for payload in (common, newcommon):
            payload.pop("other/app")
        baseline = zipped({**common, p.MF: mf(p.PREVIOUS), "other/app": b"preserved"})
        standalone = zipped({**common, p.MF: mf("1.8.14")})
        payloads = {
            p.SINGLE: zipped({**newcommon, p.MF: mf(p.VERSION)}),
            p.COMBINED: zipped({**newcommon, p.MF: mf(p.BUNDLE), "other/app": b"preserved"}),
            p.QA_NAME: json.dumps(p.REQUIRED_QA).encode(),
            p.SOURCE_ZIP: zipped({"src/build1815.py": b"reviewed build"}),
            "RELEASE_NOTES.txt": (p.VERSION + " / " + p.BUNDLE + " 候補スクロールの追加更新。上フリック診断を機能ごと削除。実機未確認。").encode(),
        }
        payloads["SHA256SUMS.txt"] = "".join(p.sha(raw) + "  " + name + "\n" for name, raw in payloads.items()).encode()
        e = expected()
        e["source_files"] = {"build1815.py": {"bytes": len(b"reviewed build"), "sha256": p.sha(b"reviewed build")}}
        e["artifacts"] = {name: {"bytes": len(raw), "sha256": p.sha(raw)} for name, raw in payloads.items()}
        for name, raw in payloads.items():
            (self.root / name).write_bytes(raw)
        with mock.patch.object(p, "BASE_SHA256", p.sha(baseline)), mock.patch.object(p, "SINGLE_BASE_SHA256", p.sha(standalone)):
            _, _, report = p.validate_local(self.root, e, baseline, standalone)
            self.assertEqual(report["integrated_classes_dex_unchanged"], "classes.dex" not in p.REQUIRED_CHANGED)
            self.assertEqual(report["standalone_classes_dex_unchanged"], "classes.dex" not in p.REQUIRED_CHANGED)
            (self.root / p.COMBINED).write_bytes(payloads[p.COMBINED] + b"unreviewed bytes")
            with self.assertRaisesRegex(RuntimeError, "Artifact differs"):
                p.validate_local(self.root, e, baseline, standalone)
            (self.root / p.COMBINED).write_bytes(payloads[p.COMBINED])
            e["source_files"]["build1815.py"]["sha256"] = "0" * 64
            with self.assertRaisesRegex(RuntimeError, "Published source archive differs"):
                p.validate_local(self.root, e, baseline, standalone)

    def test_feed_update_uses_both_leases_and_atomic_push(self):
        states = {b: {"head": b + "-old", "tree": b + "-tree"} for b in ("main", "dev")}
        changes = {b: {"patches-bundle.json": b"new"} for b in states}
        helper = mock.Mock()
        def prepare(repo, state, updates, message):
            return {**state, "head": state["head"].replace("old", "new")}
        with mock.patch.object(p, "git", return_value="") as git, mock.patch.object(p, "prepare_commit", side_effect=prepare):
            result = p.commit_feeds_atomically(helper, self.root, states, changes, "release")
        calls = [c.args for c in git.call_args_list]
        push = [c for c in calls if "push" in c][0]
        self.assertIn("--atomic", push)
        for branch in states:
            self.assertIn(f"--force-with-lease=refs/heads/{branch}:{branch}-old", push)
            self.assertIn(f"{branch}-new:refs/heads/{branch}", push)
        self.assertEqual(helper.check_heads.call_args_list, [mock.call(states), mock.call(states), mock.call(result)])
        helper.check_heads.side_effect = RuntimeError("moved head")
        with mock.patch.object(p, "git") as git:
            with self.assertRaises(RuntimeError):
                p.commit_feeds_atomically(helper, self.root, states, changes, "release")
            git.assert_not_called()


if __name__ == "__main__":
    unittest.main(verbosity=2)
