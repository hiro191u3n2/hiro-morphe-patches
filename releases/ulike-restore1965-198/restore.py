#!/usr/bin/env python3
"""Restore exact published .65/.198 bytes; preserve Git history and unrelated apps."""
import argparse
import datetime as dt
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess

REPO = "hiro191u3n2/hiro-morphe-patches"
ROOT = "releases/ulike-restore1965-198"
RECEIPT_PATH = ROOT + "/restoration_receipt.json"
SOURCE_COMMIT = "f57155718a4b56dcf3b2317c821fca97faba2e0f"
RAW_COMMIT = "23de1780f587bc280aa9150d55a305e8b86f914b"
BAD_RAW_COMMIT = "1cc50cdda1dba7f6bb007931208c2683b1e539fc"
HELPER_PATH = "releases/ulike1966-gpu-required/publication/publish1966.py"
HELPER_SHA = "5f25ebd4dc33bb22241147e9877d4a22287451e08c273cf2481f6186d9af2164"
TARGET_TAG, BAD_TAG = "ulike-v1.9.65", "ulike-v1.9.66"
TARGET_RELEASE_ID, BAD_RELEASE_ID, BAD_WORKFLOW_ID = 407436100, 407468187, 379231328
BAD_WORKFLOW_PATH = ".github/workflows/ulike1966-gpu-required-publish.yml"
BAD_BRANCH = "release/ulike1966-199"
SINGLE, BUNDLE = "ULike_HQ_Texture_Online_v1.9.65.mpp", "Hiro_Morphe_Patches_v1.0.198.mpp"
BAD_SINGLE, BAD_BUNDLE = "ULike_HQ_Texture_Online_v1.9.66.mpp", "Hiro_Morphe_Patches_v1.0.199.mpp"
PINS = {
    SINGLE: {"bytes": 1180426, "sha256": "4b4ea78a8f3ee6d959d6f89585296061bc87454db67debeae9b1541a60a6ecfa"},
    BUNDLE: {"bytes": 17827397, "sha256": "b7853baeb38b435ff68cfb0fd249f903401fc354d473e98790b52a459c176c81"},
    BAD_SINGLE: {"bytes": 1286915, "sha256": "4ef193d460ce2605fca5a13ba86dfc0b1d090b9d95c63cf692ff971d53d0bd4f"},
    BAD_BUNDLE: {"bytes": 17933951, "sha256": "acf6174e39e101b8f0ff27976b817aa6259ee0f70210ca3ed92f50b262a2d8d8"},
}
PAGE = f"https://github.com/{REPO}/releases/tag/{TARGET_TAG}"
NOTE = (
    "【復元・最新版撤回】ULike v1.9.66／総合版 v1.0.199は、ユーザーからの不具合報告により採用を取り消しました。\n"
    "指定された公開済みULike v1.9.65／総合版 v1.0.198を、配布当時のファイルと完全に同じバイトで復元しました。\n"
    "GPU必須化の改造は撤回し、今後の基準もv1.9.65／v1.0.198とします。\n"
    "Morphe Managerでソースを手動更新し、総合版v1.0.198を選んで未改造ULike 5.6.2（740）へ再適用してください。\n"
    "版番号を下げる復元のため、v1.0.199からの更新マークは表示されない場合があります。ソース更新だけではインストール済みアプリは戻りません。\n"
    "今回、新しい画素処理や圧縮設定を追加・変更していません。他アプリの配布内容も維持します。\n"
    "Galaxy実機の動作は今回も未確認です。元APKSや再構築APKは配布しません。"
)
WARNING = "【撤回済み・配信停止】ULike v1.9.66／総合版 v1.0.199は不具合報告により撤回しました。現在の指定版はULike v1.9.65／総合版 v1.0.198です。 " + PAGE + "\n\n"


def require(value, reason):
    if not value:
        raise RuntimeError(reason)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def jb(value):
    return (json.dumps(value, ensure_ascii=False, indent=2) + "\n").encode()


def helper(repo):
    path = repo / HELPER_PATH
    require(path.is_file() and sha(path.read_bytes()) == HELPER_SHA, "Publisher helper differs from approved source")
    spec = importlib.util.spec_from_file_location("pinned_publication_helper", path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    require(module.REPO == REPO, "Publisher repository differs")
    return module


def raw_url(name, bad=False):
    return f"https://raw.githubusercontent.com/{REPO}/{BAD_RAW_COMMIT if bad else RAW_COMMIT}/downloads/{name}"


def bytes_checked(raw, name):
    require(len(raw) == PINS[name]["bytes"] and sha(raw) == PINS[name]["sha256"], "Pinned MPP bytes differ: " + name)
    return raw


def load_bytes(pub, args, name, supplied=None, bad=False):
    if supplied:
        return bytes_checked(supplied.read_bytes(), name)
    path = args.work_dir / name
    if path.exists():
        return bytes_checked(path.read_bytes(), name)
    raw = bytes_checked(pub.fetch(raw_url(name, bad)), name)
    path.write_bytes(raw)
    return raw


def baseline(pub, args, path):
    # Local-only verification can use the immutable object already fetched into Git.
    if args.local_only:
        proc = subprocess.run(["git", "-C", str(args.repo), "show", SOURCE_COMMIT + ":" + path], capture_output=True)
        require(proc.returncode == 0, "Pinned metadata source is unavailable locally")
        return proc.stdout
    return pub.content(path, SOURCE_COMMIT)


def exact_archives(pub, args):
    single = load_bytes(pub, args, SINGLE, args.standalone)
    bundle = load_bytes(pub, args, BUNDLE, args.bundle)
    rejected = load_bytes(pub, args, BAD_BUNDLE, args.current_bundle, True)
    s, b, c = map(pub.archive, (single, bundle, rejected))
    for archive, version in ((s, "1.9.65"), (b, "1.0.198"), (c, "1.0.199")):
        require(pub.headers(archive["META-INF/MANIFEST.MF"])["Version"] == version, "MPP manifest version differs")
        require(archive.get("classes.dex", b"").startswith(b"dex\n") and archive.get("ulike/runtime.dex", b"").startswith(b"dex\n"), "MPP DEX is missing")
    own = lambda entries: {n: v for n, v in entries.items() if pub.is_ulike(n)}
    other = lambda entries: {n: v for n, v in entries.items() if not pub.is_ulike(n) and n not in ("META-INF/MANIFEST.MF", "classes.dex")}
    require(own(s) == own(b), "Restored standalone and bundle ULike resources differ")
    require(other(b) == other(c) and len(other(b)) == 50, "Unrelated app resources changed")
    require(all(pub.is_ulike(n) or n in ("META-INF/MANIFEST.MF", "classes.dex") for n in s), "Standalone contains foreign application resources")
    return {"target_bytes_verified": True, "single_bundle_ulike_resources_identical": True,
            "ulike_resource_count": len(own(b)), "non_ulike_resources_byte_identical_to_rejected_bundle": True,
            "non_ulike_resources_preserved": len(other(b)), "artifacts": {n: PINS[n] for n in (SINGLE, BUNDLE)}}, {SINGLE: single, BUNDLE: bundle}


def release_fingerprint(release):
    return {"id": release["id"], "tag_name": release["tag_name"], "draft": release["draft"], "prerelease": release["prerelease"],
            "assets": sorted(({"id": a["id"], "name": a["name"], "bytes": a["size"], "digest": a.get("digest")} for a in release["assets"]), key=lambda a: a["name"])}


def release_pins(release, names):
    assets = {a["name"]: a for a in release["assets"]}
    require(len(assets) == len(release["assets"]), "Duplicate release asset names")
    for name in names:
        require(name in assets and assets[name]["size"] == PINS[name]["bytes"] and assets[name].get("digest") == "sha256:" + PINS[name]["sha256"], "Release asset differs: " + name)
    return assets


def target_release(pub, payloads, fingerprint=None, download=False):
    release = pub.api(f"repos/{REPO}/releases/{TARGET_RELEASE_ID}")
    require(release["id"] == TARGET_RELEASE_ID and release["tag_name"] == TARGET_TAG and release["draft"] is False, "Target release is unavailable")
    assets = release_pins(release, (SINGLE, BUNDLE))
    if fingerprint:
        require(release_fingerprint(release) == fingerprint, "Target release changed during restoration")
    if download:
        for name in (SINGLE, BUNDLE):
            require(bytes_checked(pub.fetch(assets[name]["browser_download_url"]), name) == payloads[name], "Public target release download differs")
    return release_fingerprint(release)


def prepare_metadata(pub, args, states, created):
    feed = json.loads(baseline(pub, args, "patches-bundle.json"))
    policy = json.loads(baseline(pub, args, "releases/ULike_ACTIVE.json"))
    log = baseline(pub, args, "CHANGELOG.md").decode()
    require(feed["version"] == "1.0.198" and feed["download_url"] == raw_url(BUNDLE) and feed["page_url"] == PAGE, "Pinned .198 metadata differs")
    require(policy["ulike_version"] == "1.9.65" and policy["bundle_version"] == "1.0.198" and policy["source_sha256"] == PINS[SINGLE]["sha256"], "Pinned .65 active policy differs")
    require(policy.get("selected_candidates") == ["CAMERA_TRACE"] and not any(policy.get(k) is True for k in ("gpu_required_noise", "gpu_required_protection", "gpu_required_correction")), "Pinned policy still requires rejected GPU work")
    require(log.startswith("# 1.0.198 ") and not any(e["version"] == "1.0.199" for e in pub.parsed_entries(log)), "Baseline changelog advertises rejected bundle")
    feed = {**feed, "created_at": created, "description": NOTE + "\n\n" + feed["description"]}
    policy = {**policy, "withdrawn_versions": list(dict.fromkeys(policy.get("withdrawn_versions", []) + ["1.9.66"])),
              "withdrawn_bundle_versions": list(dict.fromkeys(policy.get("withdrawn_bundle_versions", []) + ["1.0.199"])),
              "reason": "User rejected buggy .66/.199 and requested restoration of exact published .65/.198 files.",
              "future_ulike_base": "Use exact published ULike .65 and bundle .198. Do not restore rejected .66/.199 or its mandatory GPU processing without a new user instruction.",
              "restored_from_ulike_version": "1.9.66", "restored_from_bundle_version": "1.0.199",
              "restoration_exact_published_bytes": True, "restoration_metadata_source_commit": SOURCE_COMMIT,
              "restoration_created_at": created, "release_status": "restored_exact_1965_198_rejected_1966_199",
              "manager_downgrade_update_badge_guaranteed": False}
    lines = log.splitlines()
    lines[2:2] = ["* **ULike:** v1.9.66／総合版v1.0.199を撤回し、公開済みv1.9.65／v1.0.198を完全一致で復元。手動ソース更新・再適用が必要です。", "", NOTE, ""]
    log = "\n".join(lines) + "\n"
    require(not pub.has_changes_for(log, "1.0.199", ["ULike"]), "Rollback incorrectly promises an automatic newer-version badge")
    require(pub.parsed_entries(log)[0]["version"] == "1.0.198", "Restored changelog version differs")
    updates = {branch: {"patches-bundle.json": jb(feed), "CHANGELOG.md": log.encode(), "releases/ULike_ACTIVE.json": jb(policy)} for branch in states}
    return updates, feed, policy, log


def verify_feeds(pub, states, feed, policy, log, payloads):
    for branch, state in states.items():
        require(json.loads(pub.content("patches-bundle.json", state["head"])) == feed, "Restored feed differs: " + branch)
        require(json.loads(pub.content("releases/ULike_ACTIVE.json", state["head"])) == policy, "Restored active policy differs: " + branch)
        require(pub.content("CHANGELOG.md", state["head"]).decode() == log, "Restored changelog differs: " + branch)
        require(bytes_checked(pub.fetch(feed["download_url"]), BUNDLE) == payloads[BUNDLE], "Manager serves wrong MPP")
        public_feed = json.loads(pub.fetch(f"https://raw.githubusercontent.com/{REPO}/{branch}/patches-bundle.json?restore1965={state['head']}"))
        require(public_feed == feed, "Public Manager feed differs: " + branch)
    pub.check_heads(states)


def verify_withdrawn(pub, receipt):
    bad = pub.api(f"repos/{REPO}/releases/{BAD_RELEASE_ID}")
    require(bad["tag_name"] == BAD_TAG and bad["draft"] is True and bad["body"].startswith(WARNING), "Rejected release is still publicly distributed")
    release_pins(bad, (BAD_SINGLE, BAD_BUNDLE))
    require(pub.api(f"repos/{REPO}/git/ref/tags/{BAD_TAG}")["object"]["sha"] == receipt["rejected_tag_sha"], "Rejected tag tombstone changed")
    require(pub.head(BAD_BRANCH) == receipt["rejected_raw_branch_sha"] == BAD_RAW_COMMIT, "Rejected raw branch tombstone changed")
    workflow = workflow_state(pub)
    require(workflow["state"] in ("disabled_manually", "deleted", "removed_from_main"), "Rejected publication workflow is not disabled")


def workflow_state(pub):
    workflow = pub.api(f"repos/{REPO}/actions/workflows/{BAD_WORKFLOW_ID}", absent=True)
    if workflow is None or workflow.get("state") == "deleted":
        missing = pub.api(f"repos/{REPO}/contents/{BAD_WORKFLOW_PATH}?ref={pub.head('main')}", absent=True)
        require(missing is None, "Rejected workflow is still present on main")
        return {"id": BAD_WORKFLOW_ID, "path": BAD_WORKFLOW_PATH, "state": "removed_from_main" if workflow is None else "deleted"}
    require(workflow["id"] == BAD_WORKFLOW_ID and workflow["path"] == BAD_WORKFLOW_PATH, "Rejected workflow identity differs")
    return workflow


def disable_workflow(pub):
    workflow = workflow_state(pub)
    if workflow["state"] not in ("disabled_manually", "deleted", "removed_from_main"):
        try:
            pub.api(f"repos/{REPO}/actions/workflows/{BAD_WORKFLOW_ID}/disable", method="PUT")
        except RuntimeError as error:
            if "(HTTP 404)" not in str(error):
                raise
            # GitHub may retire a workflow after its file is removed. A missing
            # workflow is accepted only when the main file is also proved absent.
            missing = pub.api(f"repos/{REPO}/contents/{BAD_WORKFLOW_PATH}?ref={pub.head('main')}", absent=True)
            require(missing is None, "Rejected workflow disable failed while its source is still present")
    workflow = workflow_state(pub)
    require(workflow["state"] in ("disabled_manually", "deleted", "removed_from_main"), "Rejected workflow is still active")
    return workflow["state"]


def completed(pub, args, states, payloads, report):
    if not all(s["manifest"].get("version") == "1.0.198" for s in states.values()):
        return False
    receipts = {b: pub.content(RECEIPT_PATH, s["head"]) for b, s in states.items()}
    require(receipts["main"] == receipts["dev"], "Restoration receipts differ; inspect partial rollback")
    receipt = json.loads(receipts["main"])
    require(receipt.get("status") == "restored_verified_rejected_release_withdrawn" and receipt.get("ulike_version") == "1.9.65"
            and receipt.get("bundle_version") == "1.0.198" and receipt.get("validation") == report, "Restoration is incomplete or differs")
    created = states["main"]["manifest"]["created_at"]
    _, feed, policy, log = prepare_metadata(pub, args, states, created)
    verify_feeds(pub, states, feed, policy, log, payloads)
    target_release(pub, payloads, receipt["target_release_fingerprint"], download=True)
    verify_withdrawn(pub, receipt)
    pub.check_heads(states)
    print(json.dumps({"status": "already_restored_verified_no_remote_writes", "ulike_version": "1.9.65", "bundle_version": "1.0.198"}))
    return True


def run(args):
    args.work_dir.mkdir(parents=True, exist_ok=True)
    pub = helper(args.repo)
    report, payloads = exact_archives(pub, args)
    if args.local_only:
        # A read-only preflight of archive/math-independent metadata invariants.
        _, feed, policy, _ = prepare_metadata(pub, args, {"main": {}, "dev": {}}, "2026-10-09T23:59:59")
        require(feed["version"] == "1.0.198" and "1.9.66" in policy["withdrawn_versions"], "Local rollback metadata differs")
        (args.work_dir / "local_validation.json").write_bytes(jb(report))
        print(json.dumps({"status": "local_readonly_preflight_passed", **report}))
        return
    states = {b: pub.snapshot(b) for b in ("main", "dev")}
    pub.check_heads(states)
    if completed(pub, args, states, payloads, report):
        return
    require(states["main"]["manifest"] == states["dev"]["manifest"], "Current main/dev feeds differ")
    require(all(s["manifest"].get("version") == "1.0.199" for s in states.values()), "Rollback baseline advanced")
    require(os.environ.get("GITHUB_SHA") == states["main"]["head"], "Main moved after workflow checkout")
    require(subprocess.check_output(["git", "-C", str(args.repo), "rev-parse", "HEAD"]).decode().strip() == os.environ["GITHUB_SHA"], "Checkout differs from workflow source")
    for state in states.values():
        policy = json.loads(state["ulike_policy"])
        require(policy.get("ulike_version") == "1.9.66" and policy.get("bundle_version") == "1.0.199" and policy.get("source_sha256") == PINS[BAD_SINGLE]["sha256"], "Active rejected ULike differs")
        require(bytes_checked(pub.fetch(state["manifest"]["download_url"]), BAD_BUNDLE), "Active bundle differs")
    fingerprint = target_release(pub, payloads, download=True)
    bad = pub.api(f"repos/{REPO}/releases/{BAD_RELEASE_ID}")
    require(bad["tag_name"] == BAD_TAG and bad["draft"] is False, "Rejected release identity changed")
    release_pins(bad, (BAD_SINGLE, BAD_BUNDLE))
    tag_sha = pub.api(f"repos/{REPO}/git/ref/tags/{BAD_TAG}")["object"]["sha"]
    require(pub.head(BAD_BRANCH) == BAD_RAW_COMMIT, "Rejected raw branch advanced")
    workflow_state(pub)
    created = pub.utc_created(states)
    updates, feed, policy, log = prepare_metadata(pub, args, states, created)
    pub.check_heads(states)
    if args.preflight_only:
        print(json.dumps({"status": "remote_readonly_preflight_passed", "validation": report, "heads": {b: s["head"] for b, s in states.items()}}))
        return
    receipt = {"schema": "ulike-exact-restore1965-198-v1", "status": "restored_feeds_pending_withdrawal",
               "ulike_version": "1.9.65", "bundle_version": "1.0.198", "withdrawn_ulike_version": "1.9.66", "withdrawn_bundle_version": "1.0.199",
               "created_at": created, "source_commit": os.environ["GITHUB_SHA"], "metadata_source_commit": SOURCE_COMMIT,
               "publisher_helper_sha256": HELPER_SHA, "validation": report, "target_release_fingerprint": fingerprint,
               "rejected_release_id": BAD_RELEASE_ID, "rejected_tag_sha": tag_sha, "rejected_raw_branch_sha": BAD_RAW_COMMIT,
               "rejected_workflow_id": BAD_WORKFLOW_ID, "restored_download_urls": {n: raw_url(n) for n in (SINGLE, BUNDLE)},
               "manager_main_dev_updated_atomically": True, "manager_downgrade_update_badge_guaranteed": False,
               "manual_source_refresh_and_repatch_required": True, "android_device_tested": False, "original_apk_apply_tested": False,
               "git_history_force_rewritten": False, "target_release_modified": False, "other_apps_false_updates": False}
    receipt_local = args.work_dir / "restoration_receipt.json"
    receipt_local.write_bytes(jb(receipt))
    for branch in updates:
        updates[branch][RECEIPT_PATH] = jb(receipt)
    active = pub.commit_feeds_atomically(args.repo, states, updates, "release: restore exact ULike1.9.65 and bundle1.0.198; withdraw buggy .66/.199")
    verify_feeds(pub, active, feed, policy, log, payloads)
    target_release(pub, payloads, fingerprint)
    pub.check_heads(active)
    pub.api(f"repos/{REPO}/releases/{BAD_RELEASE_ID}", {"draft": True, "prerelease": True,
            "name": "撤回済み：ULike v1.9.66 / Hiro Morphe v1.0.199", "body": WARNING + bad["body"], "make_latest": "false"}, "PATCH")
    disabled_state = disable_workflow(pub)
    verify_withdrawn(pub, receipt)
    target_release(pub, payloads, fingerprint)
    pub.check_heads(active)
    receipt.update(status="restored_verified_rejected_release_withdrawn", restored=True,
                   rejected_release_draft=True, rejected_publication_workflow_state=disabled_state,
                   operations=[{"branch": b, "commit": s["head"], "feed_verified": True, "changelog_verified": True,
                                "policy_verified": True, "manager_download_verified": True} for b, s in active.items()])
    receipt_local.write_bytes(jb(receipt))
    saved = pub.commit_feeds_atomically(args.repo, active, {b: {RECEIPT_PATH: jb(receipt)} for b in active}, "docs: verified exact .65/.198 restoration and .66/.199 withdrawal")
    for state in saved.values():
        require(pub.content(RECEIPT_PATH, state["head"]) == jb(receipt), "Remote final restoration receipt differs")
    verify_feeds(pub, saved, feed, policy, log, payloads)
    verify_withdrawn(pub, receipt)
    target_release(pub, payloads, fingerprint)
    pub.check_heads(saved)
    print(json.dumps({"status": receipt["status"], "ulike_version": "1.9.65", "bundle_version": "1.0.198", "heads": {b: s["head"] for b, s in saved.items()}}))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--work-dir", type=Path, required=True)
    parser.add_argument("--standalone", type=Path)
    parser.add_argument("--bundle", type=Path)
    parser.add_argument("--current-bundle", type=Path)
    modes = parser.add_mutually_exclusive_group()
    modes.add_argument("--local-only", action="store_true")
    modes.add_argument("--preflight-only", action="store_true")
    args = parser.parse_args()
    for key in ("repo", "work_dir", "standalone", "bundle", "current_bundle"):
        if getattr(args, key) is not None:
            setattr(args, key, getattr(args, key).resolve())
    run(args)
