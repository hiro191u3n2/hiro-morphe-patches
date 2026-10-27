#!/usr/bin/env python3
"""Verify the completed 1.8.16 publication and save its missing receipt only."""
from __future__ import annotations

import argparse
import importlib.util
import json
import os
from pathlib import Path
import time

ROOT = Path(__file__).resolve().parent
SOURCE_COMMIT = "7cfa6041c54f823ad11a9cd731a17336bda5a0fc"
BASELINES = {"main": SOURCE_COMMIT, "dev": "4e64df32623c3b02d462823279a7b84800b5b839"}
FEED_COMMITS = {"main": "191f658329fda0c10dd06326ff0c2d6996ce99ba",
                "dev": "3e86eb0d115bce5e2485b69af5e738474ef765f1"}
RAW_COMMIT = "e52f3099ad3d312bdd7350a3b74e897193783588"
CREATED = "2026-10-27T11:23:51"
RELEASE_ID = 425362925
RUN_ID, JOB_ID = 40495662524, 122160335763
WORKFLOW = ".github/workflows/swiftkey1816-299-finish-publication.yml"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--preflight-only", action="store_true")
    args = parser.parse_args()
    repo, output = args.repo.resolve(), args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    spec = importlib.util.spec_from_file_location("publish1816", ROOT / "src/publish1816.py")
    p = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(p)
    require = p.require
    require(ROOT == repo / p.RELEASE_ROOT, "Recovery script must use the checked-out release tree")
    expected = p.load_expected(ROOT / "manifest.json")
    p.verify_source_files(ROOT / "src", expected)
    helper = p.load_helper(repo)
    require(helper.content(p.RELEASE_ROOT + "/manifest.json", SOURCE_COMMIT)
            == (ROOT / "manifest.json").read_bytes(), "Original reviewed manifest changed")
    require((repo / WORKFLOW).read_bytes() == (ROOT / "finish_publication.yml").read_bytes(),
            "Recovery workflow differs from reviewed copy")

    states = {branch: helper.snapshot(branch) for branch in ("main", "dev")}
    helper.check_heads(states)
    workflow_sha = os.environ.get("GITHUB_SHA")
    require(workflow_sha and states["main"]["head"] == workflow_sha,
            "main moved after this recovery workflow checkout")
    require(p.git(repo, "rev-parse", "HEAD") == workflow_sha, "Checkout differs from recovery source commit")
    require(states["dev"]["head"] == FEED_COMMITS["dev"], "dev moved after the completed feed publication")
    recovery = helper.api(f"repos/{p.REPO}/commits/{workflow_sha}")
    require([row["sha"] for row in recovery["parents"]] == [FEED_COMMITS["main"]],
            "Recovery commit must directly follow the known main feed commit")
    allowed = {p.RELEASE_ROOT + "/finish_publication.py", p.RELEASE_ROOT + "/finish_publication.yml", WORKFLOW}
    require({row["filename"] for row in recovery["files"]} == allowed,
            "Recovery commit must add only the two sidecars and workflow")
    require(all(row["status"] == "added" for row in recovery["files"]), "Recovery commit rewrites an existing file")

    run = helper.api(f"repos/{p.REPO}/actions/runs/{RUN_ID}")
    job = helper.api(f"repos/{p.REPO}/actions/jobs/{JOB_ID}")
    require(run["head_sha"] == SOURCE_COMMIT and run["status"] == "completed"
            and run["conclusion"] == "failure" and job["run_id"] == RUN_ID,
            "Original publication run identity or status differs")
    steps = {row["name"]: row["conclusion"] for row in job["steps"]}
    verified_steps = (
        "Verify source pins and unchanged active baseline",
        "Build first MPPs from pinned inputs and run regression tests",
        "Independently rebuild and rerun regression tests",
        "Match both builds to reviewed artifacts and preservation contract",
    )
    require(all(steps.get(name) == "success" for name in verified_steps), "Original build/hash gates did not pass")
    require(steps.get("Publish verified MPPs and atomically advance both Manager feeds") == "failure",
            "Original failure is not the known publication step")

    release = helper.api(f"repos/{p.REPO}/releases/{RELEASE_ID}")
    require(release["tag_name"] == p.TAG and release["draft"] is False
            and release["target_commitish"] == SOURCE_COMMIT, "Published release identity differs")
    assets = {row["name"]: row for row in release["assets"]}
    require(set(assets) == set(p.ASSETS), "Expected the six original assets and no receipt yet")
    for name, pin in expected["artifacts"].items():
        asset = assets[name]
        require(asset["size"] == pin["bytes"] and asset.get("digest") == "sha256:" + pin["sha256"],
                "Published asset metadata differs: " + name)
        raw = helper.fetch(asset["browser_download_url"])
        require(len(raw) == pin["bytes"] and p.sha(raw) == pin["sha256"], "Published asset bytes differ: " + name)
        (output / name).write_bytes(raw)
    payloads, notes, local_report = p.validate_local(output, expected, helper.fetch(p.BASE_URL), helper.fetch(p.SINGLE_BASE_URL))
    require(release["body"] == notes, "Published release notes changed")
    require(helper.head(p.RAW_BRANCH) == RAW_COMMIT, "Immutable distribution branch moved")
    urls = {name: f"https://raw.githubusercontent.com/{p.REPO}/{RAW_COMMIT}/downloads/{name}"
            for name in (p.SINGLE, p.COMBINED)}
    for name, url in urls.items():
        require(helper.fetch(url) == payloads[name], "Immutable MPP bytes differ: " + name)

    policies, public_targets, operations = {}, {}, []
    for branch, state in states.items():
        original = helper.api(f"repos/{p.REPO}/git/commits/{FEED_COMMITS[branch]}")
        require([row["sha"] for row in original["parents"]] == [BASELINES[branch]],
                "Published feed commit has an unexpected parent: " + branch)
        baseline = {"manifest": json.loads(helper.content("patches-bundle.json", BASELINES[branch])),
                    "log": helper.content("CHANGELOG.md", BASELINES[branch]).decode()}
        feed, changelog = p.metadata(baseline, urls[p.COMBINED], CREATED, notes)
        for path, raw in (("patches-bundle.json", p.json_bytes(feed)), ("CHANGELOG.md", changelog.encode())):
            require(helper.content(path, FEED_COMMITS[branch]) == raw, "Original published metadata differs: " + branch + "/" + path)
            require(helper.content(path, state["head"]) == raw, "Current metadata changed: " + branch + "/" + path)
            public_targets[f"https://raw.githubusercontent.com/{p.REPO}/{branch}/{path}"] = raw
        policies[branch] = helper.content("releases/ULike_ACTIVE.json", BASELINES[branch])
        require(helper.content("releases/ULike_ACTIVE.json", state["head"]) == policies[branch],
                "ULike policy changed: " + branch)
        receipt_path = p.RELEASE_ROOT + "/" + p.RECEIPT_NAME
        require(helper.api(f"repos/{p.REPO}/contents/{receipt_path}?ref={state['head']}", absent=True) is None,
                "Publication receipt already exists; inspect before retry")
        operations.append({"branch": branch, "commit": FEED_COMMITS[branch], "verified_at_head": state["head"],
                           "feed_verified": True, "changelog_verified": True,
                           "ulike_policy_byte_identical": True, "manager_download_verified": True})

    # Retry only public branch freshness; immutable bytes and all semantic gates
    # above fail immediately. Check the ordinary Manager URL and a fresh query.
    public_attempt = 0
    for public_attempt in range(1, 9):
        helper.check_heads(states)
        pending = []
        for url, raw in public_targets.items():
            fresh = url + f"?swiftkey1816finish={workflow_sha}&attempt={public_attempt}"
            if helper.fetch(url) != raw or helper.fetch(fresh) != raw:
                pending.append(url)
        if not pending:
            break
        print(f"Public branch freshness pending, attempt {public_attempt}/8: {pending}", flush=True)
        require(public_attempt < 8, "Public branch freshness did not converge; no receipt written")
        time.sleep(6)
    helper.check_heads(states)
    receipt = {
        "schema": "swiftkey1816-publication-receipt-v1", "status": "published_and_verified", "published": True,
        "swiftkey_version": p.VERSION, "bundle_version": p.BUNDLE,
        "baseline_bundle_version": p.PREVIOUS, "baseline_bundle_sha256": p.BASE_SHA256,
        "baseline_swiftkey_version": "1.8.15", "baseline_swiftkey_sha256": p.SINGLE_BASE_SHA256,
        "assets": expected["artifacts"], "local_validation": local_report,
        "release_id": RELEASE_ID, "release_url": p.PAGE, "raw_commit": RAW_COMMIT, "download_urls": urls,
        "android_device_tested": False, "original_apk_apply_tested": False,
        "manager_parser_source": p.MANAGER_SOURCE, "manager_app_names": list(p.APP_NAMES), "operations": operations,
        "manager_main_dev_updated_atomically": True, "manager_update_eligible_for_both_app_names": True,
        "manager_device_update_badge_observed": False, "other_apps_false_updates": False,
        "fixed_chatgpt_site_updated": False,
        "recovery": {"source_commit": workflow_sha, "original_source_commit": SOURCE_COMMIT,
                     "original_run_id": RUN_ID, "original_job_id": JOB_ID,
                     "original_successful_gates": list(verified_steps), "public_freshness_attempts": public_attempt,
                     "reason": "Complete verification and receipt after the original post-publication public-branch freshness check failed.",
                     "existing_release_assets_and_feeds_unchanged": True},
    }
    receipt_raw = p.json_bytes(receipt)
    local_receipt = output / p.RECEIPT_NAME
    local_receipt.write_bytes(receipt_raw)
    if args.preflight_only:
        print("PASS recovery preflight; existing publication verified, no remote writes", flush=True)
        return

    receipt_updates = {branch: {receipt_path: receipt_raw} for branch in states}
    saved = p.commit_feeds_atomically(helper, repo, states, receipt_updates,
                                     "docs: verify existing SwiftKey1816 publication and save receipt")
    for branch, state in saved.items():
        require(helper.content(receipt_path, state["head"]) == receipt_raw, "Saved receipt differs: " + branch)
        for path in ("patches-bundle.json", "CHANGELOG.md"):
            require(helper.content(path, state["head"]) == helper.content(path, states[branch]["head"]),
                    "Receipt commit changed published metadata: " + branch)
        require(helper.content("releases/ULike_ACTIVE.json", state["head"]) == policies[branch], "Receipt commit changed ULike policy")
    helper.check_heads(saved)
    helper.run(["gh", "release", "upload", p.TAG, "--repo", p.REPO, local_receipt])
    helper.verify_assets(RELEASE_ID, {**expected["artifacts"], p.RECEIPT_NAME:
                         {"bytes": len(receipt_raw), "sha256": p.sha(receipt_raw)}}, True)
    helper.check_heads(saved)
    print("PASS existing public MPPs and both Manager feeds verified; receipt saved to both branches and release", flush=True)


if __name__ == "__main__":
    main()
