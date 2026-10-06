#!/usr/bin/env python3
"""Regression checks for publication boundaries; all external operations mocked."""
import argparse
import contextlib
import copy
import importlib.util
import io
import json
import os
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
import zipfile

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("trip162_publish", HERE / "publish162.py")
pub = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pub)


def config():
    return {"new_trip_extensions": ["extensions/myplan_bundle_guard.mpe"], "summary": "時刻と不要欄を修正。実機未確認。"}


def state(branch="main"):
    return {"branch": branch, "head": branch + "-head", "tree": branch + "-tree", "ulike_policy": b"ULike1930",
            "manifest": {"version": pub.PREVIOUS, "created_at": "2026-10-06T19:03:39",
                         "download_url": pub.BASE_URL, "signature_download_url": "", "custom_field": "retained",
                         "description": "QuickSearch latest notes\r\n\r\nULike：v1.9.30（5.6.2／740）\r\n"
                         + pub.INVENTORY_OLD + "\r\n簡単検索くん：v1.0.1（0.4.5／36）\r\n"},
            "log": "# 1.0.161 (2026-10-06)\n\n* **簡単検索くん:** prior close/back fix\n\n"
                   "# 1.0.159 (2026-10-06)\n\n* **ULike:** prior front camera fix\n"}


def resources():
    old_single = {pub.MF: b"manifest", "classes.dex": b"dex\nold-loader",
                  "extensions/extension.mpe": b"dex\nextension", "extensions/search_landing_guard.mpe": b"dex\nsearch",
                  "extensions/myplan_preparation_guard.mpe": b"dex\npreparation",
                  pub.SCHEDULE_ARCHIVE: b"7z\xbc\xaf\x27\x1cold", pub.PATCH_PREFIX + "Old.class": b"\xca\xfe\xba\xbeold"}
    old_bundle = {**old_single, "extensions/quicksearch_recents.mpe": b"dex\nquicksearch101",
                  "extensions/swiftkey_japanese.mpe": b"dex\nswiftkey183", "ULike/data.json": b"ULike1930"}
    single = {**old_single, pub.MF: b"manifest-new", "classes.dex": b"dex\nnew-loader",
              pub.SCHEDULE_ARCHIVE: b"7z\xbc\xaf\x27\x1cnew", "extensions/myplan_bundle_guard.mpe": b"dex\nnew-hook",
              pub.PATCH_PREFIX + "New.class": b"\xca\xfe\xba\xbeadded"}
    new_bundle = {**old_bundle, **single}
    return old_bundle, new_bundle, single, old_single


class PublicationBoundaryTests(unittest.TestCase):
    def test_exact_inventory_change_below_recent_other_app_notes(self):
        old = state()
        notes = "Trip.com v1.10.15\n\n* **Trip.com:** 時刻表示修正。実機未確認。"
        feed, changelog = pub.metadata(old, config(), "https://example.invalid/exact-reviewed.mpp",
                                       "2026-10-07T00:00:00", notes)
        self.assertEqual(feed["custom_field"], "retained")
        history = feed["description"][len(notes) + 2:]
        self.assertEqual(history.replace(pub.INVENTORY_NEW, pub.INVENTORY_OLD), old["manifest"]["description"])
        self.assertTrue(changelog.endswith(old["log"]))
        self.assertTrue(pub.has_changes_for(changelog, "1.0.161", "Trip.com"))
        self.assertFalse(pub.has_changes_for(changelog, "1.0.162", "Trip.com"))
        for app in pub.OTHER_APPS:
            self.assertFalse(pub.has_changes_for(changelog, "1.0.161", app), app)

    def test_duplicate_inventory_is_rejected(self):
        old = state()
        old["manifest"]["description"] += pub.INVENTORY_OLD + "\n"
        with self.assertRaisesRegex(RuntimeError, "ambiguous"):
            pub.metadata(old, config(), "https://example.invalid/mpp", "2026-10-07T00:00:00", "Trip update")

    def test_changed_baseline_is_rejected(self):
        old = state()
        old["manifest"]["version"] = "1.0.162"
        with self.assertRaisesRegex(RuntimeError, "baseline161"):
            pub.metadata(old, config(), "https://example.invalid/mpp", "2026-10-07T00:00:00", "Trip update")

    def test_non_trip_update_scope_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "False update scope"):
            pub.metadata(state(), config(), "https://example.invalid/mpp", "2026-10-07T00:00:00",
                         "* **簡単検索くん:** accidental extra scope")

    def test_trip_delta_keeps_other_apps(self):
        result = pub.validate_entry_delta(*resources(), config())
        self.assertEqual(result["existing_non_trip_resource_entries_byte_identical"], 3)
        self.assertTrue(result["standalone_and_bundle_trip_resources_identical"])

    def test_quicksearch_resource_change_is_rejected(self):
        old, new, single, old_single = resources()
        new["extensions/quicksearch_recents.mpe"] = b"dex\nwrong-version"
        with self.assertRaisesRegex(RuntimeError, "unrelated"):
            pub.validate_entry_delta(old, new, single, old_single, config())

    def test_extension_allowlist_cannot_take_over_existing_resource(self):
        old, new, single, old_single = resources()
        cfg = config()
        cfg["new_trip_extensions"].append("extensions/quicksearch_recents.mpe")
        with self.assertRaisesRegex(RuntimeError, "already exists"):
            pub.validate_entry_delta(old, new, single, old_single, cfg)

    def test_standalone_bundle_mismatch_is_rejected(self):
        old, new, single, old_single = resources()
        new[pub.PATCH_PREFIX + "New.class"] = b"\xca\xfe\xba\xbeother"
        with self.assertRaisesRegex(RuntimeError, "resources differ"):
            pub.validate_entry_delta(old, new, single, old_single, config())

    def test_pending_manifest_is_rejected(self):
        with self.assertRaisesRegex(RuntimeError, "pending review"):
            pub.load_config(HERE / "manifest.template.json")

    def test_xml_build_fixture_is_required_and_accepted(self):
        with tempfile.TemporaryDirectory(prefix="trip162-source-test-", dir=HERE) as temporary:
            root = Path(temporary)
            files = {name: b"source\n" for name in pub.REQUIRED_SOURCES}
            files.update({"src/Helper.java": b"class Helper {}\n", "src/patch.js": b"void 0;\n",
                          "src/resource/fixtures/layout/p8.xml": b"<LinearLayout/>\n"})
            for name, data in files.items():
                path = root / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(data)
            def packed(omit_fixture=False):
                stream = io.BytesIO()
                with zipfile.ZipFile(stream, "w") as z:
                    for name, data in files.items():
                        if not omit_fixture or not name.endswith(".xml"):
                            z.writestr(name, data)
                return stream.getvalue()
            with patch.object(pub, "SOURCE_ROOT", root):
                self.assertEqual(pub.validate_sources(packed(), root / "publication/manifest.json"), len(files))
                with self.assertRaisesRegex(RuntimeError, "Missing or ambiguous archived source.*p8.xml"):
                    pub.validate_sources(packed(True), root / "publication/manifest.json")

    def test_local_mode_cannot_read_or_write_remote(self):
        args = argparse.Namespace(local_only=True, preflight_only=False)
        with patch.object(pub, "validate_local", return_value=(config(), {}, "notes", {}, {})), \
             patch.object(pub, "snapshot", side_effect=AssertionError("remote read")), \
             patch.object(pub, "api", side_effect=AssertionError("api")), \
             patch.object(pub, "git", side_effect=AssertionError("git")), contextlib.redirect_stdout(io.StringIO()) as out:
            pub.publication(args)
        self.assertEqual(json.loads(out.getvalue())["status"], "local_artifacts_verified")

    def test_preflight_stops_before_mutation(self):
        with tempfile.TemporaryDirectory(prefix="trip162-preflight-test-", dir=HERE) as temporary:
            args = argparse.Namespace(local_only=False, preflight_only=True, dist=Path(temporary), repo=Path(temporary))
            api_calls = []
            def readonly_api(path, data=None, method=None, absent=False):
                api_calls.append((path, data, method, absent))
                if data is not None or method is not None:
                    raise AssertionError("Mutating API called in preflight")
                return None
            def fetched(url):
                return {pub.BASE_URL: b"baseline", pub.SINGLE_BASE_URL: b"single-baseline"}[url]
            def digest(raw):
                return {b"baseline": pub.BASE_SHA256, b"single-baseline": pub.SINGLE_BASE_SHA256}[raw]
            with patch.dict(os.environ, {k: v for k, v in os.environ.items() if k != "GITHUB_SHA"}, clear=True), \
                 patch.object(pub, "validate_local", return_value=(config(), {}, "Trip.com notes", {}, {})), \
                 patch.object(pub, "snapshot", side_effect=state), patch.object(pub, "head", side_effect=lambda b: b + "-head"), \
                 patch.object(pub, "fetch", side_effect=fetched), patch.object(pub, "sha", side_effect=digest), \
                 patch.object(pub, "api", side_effect=readonly_api), \
                 patch.object(pub, "git", side_effect=AssertionError("Unexpected Git operation")), \
                 patch.object(pub, "commit_feeds", side_effect=AssertionError("Feed mutation")), \
                 contextlib.redirect_stdout(io.StringIO()) as out:
                pub.publication(args)
            self.assertEqual(json.loads(out.getvalue())["status"], "preflight_passed_no_remote_writes")
            self.assertEqual(len(api_calls), 3)
            self.assertEqual(list(Path(temporary).iterdir()), [])


if __name__ == "__main__":
    unittest.main()
