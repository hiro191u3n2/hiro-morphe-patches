#!/usr/bin/env python3
"""Offline regression checks for exact publication contracts and atomic feed leases."""
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import types
sys.dont_write_bytecode = True
HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location("publication1930", HERE / "publish1930.py")
P = importlib.util.module_from_spec(spec)
spec.loader.exec_module(P)
checks = 0


def check(value, message):
    global checks
    if not value:
        raise AssertionError(message)
    checks += 1


def rejects(fn, message):
    try:
        fn()
    except RuntimeError:
        check(True, message)
    else:
        check(False, message)


def run(*args, env=None):
    return subprocess.check_output(list(map(str, args)), stderr=subprocess.PIPE, env=env).decode().strip()


old = {"version": "1.0.158", "created_at": "2026-10-06T17:25:01", "download_url": P.BASE_URL,
       "description": "Old history\nSwiftKey Beta：v1.8.3（9.13.16.4）\nULike：v1.9.29（5.6.2／740）\n", "signature_download_url": ""}
state = {"manifest": old, "log": "# 1.0.158 (2026-10-06)\n\n* **SwiftKey Beta:** prior deletion protection\n"}
notes = (HERE / "RELEASE_NOTES.template.txt").read_text()
feed, log = P.metadata(state, "https://example.invalid/test", "2026-10-06T17:25:02", notes)
check(feed["version"] == "1.0.159", "Bundle version advances")
check("ULike：v1.9.30（5.6.2／740）" in feed["description"], "ULike inventory advances")
check("SwiftKey Beta：v1.8.3（9.13.16.4）" in feed["description"], "SwiftKey183 inventory stays")
check(state["manifest"] == old and old["version"] == "1.0.158", "Input metadata is not mutated")
check(log.endswith(state["log"]), "Full prior SwiftKey change history is retained")
check(P.has_changes_for(log, "1.0.158", ["ULike"]), "ULike qualifies for Manager update")
check(P.has_changes_for(log, "1.0.158", ["ULIKE"]), "Case insensitive app name")
check(not P.has_changes_for(log, "1.0.159", ["ULike"]), "Newly installed bundle is not outdated")
for name in P.OTHER_APPS:
    check(not P.has_changes_for(log, "1.0.158", [name]), "Unrelated app does not qualify: " + name)
rejects(lambda: P.metadata(state, "unused", "2026-10-06T17:25:02", notes + "\n* **SwiftKey Beta:** accidental edit"), "Other app scope is refused")
rejects(lambda: P.metadata(state, "unused", "2026-10-06T17:25:01", notes), "Nonadvancing time is refused")
rejects(lambda: P.metadata({**state, "manifest": {**old, "version": "1.0.159"}}, "unused", "2026-10-06T17:25:02", notes), "Advanced baseline is refused")
rejects(lambda: P.metadata({**state, "manifest": {**old, "description": old["description"] + "ULike：v1.9.29（5.6.2／740）\n"}}, "unused", "2026-10-06T17:25:02", notes), "Ambiguous ULike inventory is refused")
rejects(lambda: P.load_expected(HERE / "manifest.template.json"), "Unfilled expected hashes are refused")

with tempfile.TemporaryDirectory(prefix="ulike1930-manifest-") as temp:
    path = Path(temp) / "manifest.json"
    valid = json.loads((HERE / "manifest.template.json").read_text())
    for row in valid["artifacts"].values():
        row.update(bytes=1, sha256="1" * 64)
    valid["change_plan_reviewed"] = True
    valid["qa_contract_reviewed"] = True
    def load_config(data):
        path.write_text(json.dumps(data))
        return P.load_expected(path)
    check(load_config(valid)["bundle_version"] == P.BUNDLE, "Completed manifest shape is accepted")
    for gate in ("change_plan_reviewed", "qa_contract_reviewed"):
        rejects(lambda: load_config({**valid, gate: False}), "Unresolved review gate: " + gate)
    for key in P.REQUIRED_QA:
        bad = {**valid, "qa_required_values": {k:v for k,v in valid["qa_required_values"].items() if k != key}}
        rejects(lambda: load_config(bad), "Missing required semantic guard: " + key)
    rejects(lambda: load_config({**valid, "allowed_changed_bundle_entries": valid["allowed_changed_bundle_entries"] + ["extensions/swiftkey_japanese.mpe"]}), "SwiftKey change refused even if newly declared")
    for native in P.IMMUTABLE_NATIVE:
        rejects(lambda: load_config({**valid, "allowed_changed_bundle_entries": valid["allowed_changed_bundle_entries"] + [native]}), "Native payload change refused even if declared")
    rejects(lambda: load_config({**valid, "allowed_added_bundle_entries": ["ulike/new-entry.bin"]}), "Unexpected top-level entry refused")
    rejects(lambda: load_config({**valid, "allowed_added_standalone_entries": ["../Escape.class"]}), "Unsafe class path refused")
    rejects(lambda: load_config({**valid, "baseline_bundle_sha256": "2" * 64}), "Rebased binary without review refused")
    bad_qa = {**valid["qa_required_values"], "changed_runtime_methods": valid["qa_required_values"]["changed_runtime_methods"] + ["Lcom/hiro/ulike/Image;->process()V"]}
    rejects(lambda: load_config({**valid, "qa_required_values": bad_qa}), "Fourth runtime method change refused")

old_entries = {P.MF:b"old", P.RUNTIME:b"old", "classes.dex":b"old", "ulike/methods.dex":b"native", "ulike/methods.tsv":b"inventory", "extensions/swiftkey_japanese.mpe":b"keep183"}
new_entries = {**old_entries, P.MF:b"new", P.RUNTIME:b"new", "classes.dex":b"new"}
plan = {"allowed_changed_bundle_entries": sorted(P.REQUIRED_CHANGED | {"classes.dex"}), "allowed_added_bundle_entries": []}
changed, added = P.validate_entry_delta(old_entries, new_entries, plan, "bundle")
check(changed == P.REQUIRED_CHANGED | {"classes.dex"} and not added, "Exact runtime/loader delta accepted")
rejects(lambda: P.validate_entry_delta(old_entries, {**new_entries, "extensions/swiftkey_japanese.mpe":b"old182"}, plan, "bundle"), "SwiftKey regression refused")
rejects(lambda: P.validate_entry_delta(old_entries, {**new_entries, "ulike/methods.dex":b"wrong"}, plan, "bundle"), "Native payload mutation refused")
rejects(lambda: P.validate_entry_delta(old_entries, {k:v for k,v in new_entries.items() if k != "ulike/methods.tsv"}, plan, "bundle"), "Entry removal refused")
policy = {"ulike_version":"1.9.29", "bundle_version":"1.0.156", "source_sha256":P.SINGLE_BASE_SHA256, "approved_lineage_version":"1.8.8", "withdrawn_versions":["1.9.17"], "prior_setting":"preserve"}
check(P.validate_policy(json.dumps(policy))["bundle_version"] == "1.0.156", "ULike policy's historical bundle156 is accepted against current bundle158")
new_policy = P.new_policy(policy, {"artifacts":{P.SINGLE:{"sha256":"1" * 64}}, "original_apk_apply_tested": True})
check(new_policy["bundle_version"] == "1.0.159" and new_policy["ulike_version"] == "1.9.30", "Active ULike policy advances")
check(new_policy["prior_setting"] == "preserve" and new_policy["withdrawn_versions"] == ["1.9.17"], "Prior policy restrictions survive")
check(new_policy["android_device_tested"] is False and new_policy["original_apk_apply_tested"] is True, "Desktop validation and device scope remain distinct")
rejects(lambda: P.validate_policy(json.dumps({**policy,"ulike_version":"1.9.30"})), "Concurrent ULike update refused")


with tempfile.TemporaryDirectory(prefix="ulike1930-offline-git-") as temp:
    temp = Path(temp)
    bare, work = temp / "remote.git", temp / "work"
    run("git", "init", "--bare", "--initial-branch=main", bare)
    run("git", "init", "--initial-branch=main", work)
    run("git", "-C", work, "config", "user.name", "Offline Test")
    run("git", "-C", work, "config", "user.email", "offline@example.invalid")
    (work / "feed.json").write_text("old")
    (work / "SwiftKey_current.txt").write_text("keep unchanged")
    run("git", "-C", work, "add", ".")
    run("git", "-C", work, "commit", "-m", "local fixture baseline")
    run("git", "-C", work, "branch", "dev")
    run("git", "-C", work, "push", bare, "main", "dev")
    # Match Actions checkout: divergent branch trees, main only, depth 1.
    (work / "main-only.txt").write_text("main preserved")
    run("git", "-C", work, "add", "main-only.txt")
    run("git", "-C", work, "commit", "-m", "main-only fixture")
    run("git", "-C", work, "checkout", "dev")
    (work / "dev-only.txt").write_text("dev preserved")
    run("git", "-C", work, "add", "dev-only.txt")
    run("git", "-C", work, "commit", "-m", "dev-only fixture")
    run("git", "-C", work, "push", bare, "main", "dev")
    shallow = temp / "shallow"
    run("git", "clone", "--depth", "1", "--single-branch", "--branch", "main", bare.as_uri(), shallow)
    work = shallow
    check(run("git", "-C", work, "rev-parse", "--is-shallow-repository") == "true", "Actual depth-1 clone")
    check(run("git", "-C", work, "rev-list", "--count", "HEAD") == "1", "Only main tip initially available")
    # An unrelated uncommitted file and the real index must survive publication.
    (work / "uncommitted.txt").write_text("retain")
    index_before = (work / ".git/index").read_bytes()
    status_before = run("git", "-C", work, "status", "--porcelain")
    states = {branch: {"branch": branch, "head": run("git", "--git-dir", bare, "rev-parse", branch)} for branch in ("main", "dev")}
    def check_heads(expected):
        for s in expected.values():
            P.require(run("git", "--git-dir", bare, "rev-parse", s["branch"]) == s["head"], "Offline lease changed")
    helper = types.SimpleNamespace(check_heads=check_heads)
    original_git = P.git
    inject_race = False
    concurrent_head = None
    def local_git(repo, *args, **kwargs):
        global inject_race, concurrent_head
        safe_args = [str(bare) if str(arg) == f"https://github.com/{P.REPO}.git" else str(arg) for arg in args]
        if inject_race and args and args[0] == "push":
            inject_race = False
            parent = run("git", "--git-dir", bare, "rev-parse", "dev")
            tree = run("git", "--git-dir", bare, "rev-parse", "dev^{tree}")
            env = {**os.environ, "GIT_AUTHOR_NAME": "Concurrent Test", "GIT_COMMITTER_NAME": "Concurrent Test",
                   "GIT_AUTHOR_EMAIL": "offline@example.invalid", "GIT_COMMITTER_EMAIL": "offline@example.invalid"}
            concurrent_head = run("git", "--git-dir", bare, "commit-tree", tree, "-p", parent, "-m", "concurrent fixture commit", env=env)
            run("git", "--git-dir", bare, "update-ref", "refs/heads/dev", concurrent_head, parent)
        return original_git(repo, *safe_args, **kwargs)
    P.git = local_git
    changes = {branch: {"feed.json": b"new " + branch.encode()} for branch in states}
    active = P.commit_feeds_atomically(helper, work, states, changes, "atomic fixture update")
    for branch in active:
        check(run("git", "--git-dir", bare, "rev-parse", branch) == active[branch]["head"], "Both remote branches advance")
        check(run("git", "--git-dir", bare, "show", branch + ":SwiftKey_current.txt") == "keep unchanged", "Unrelated file survives")
        check(run("git", "--git-dir", bare, "show", branch + ":" + branch + "-only.txt") == branch + " preserved", "Divergent branch contents survive")
        check(run("git", "--git-dir", bare, "show", "-s", "--format=%P", branch) == states[branch]["head"], "Each commit is a direct child")
    check((work / ".git/index").read_bytes() == index_before, "Private index preserves working index")
    check(run("git", "-C", work, "status", "--porcelain") == status_before, "Worktree is unchanged")
    check((work / "uncommitted.txt").read_text() == "retain", "Uncommitted file is unchanged")
    inject_race = True
    main_before = run("git", "--git-dir", bare, "rev-parse", "main")
    rejects(lambda: P.commit_feeds_atomically(helper, work, active, {b: {"feed.json": b"next"} for b in active}, "must refuse race"), "Concurrent dev update rejects the atomic push")
    check(run("git", "--git-dir", bare, "rev-parse", "main") == main_before, "Failed atomic push does not update main")
    check(run("git", "--git-dir", bare, "rev-parse", "dev") == concurrent_head, "Concurrent dev commit is retained")
    check((work / ".git/index").read_bytes() == index_before, "Failed push still preserves working index")
    P.git = original_git


print(f"PASS {checks} offline checks: exact ULike scope, SwiftKey retention, QA/delta gates, actual atomic Git publication, leases and race handling; no GitHub writes")
