"""Publish the exact MPP recorded in the reviewed Hiro release candidate."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
from zipfile import ZipFile

repo = os.environ["GITHUB_REPOSITORY"]
def gh(*arguments, capture=False):
    result = subprocess.run(["gh", *arguments], check=True, text=True, stdout=subprocess.PIPE if capture else None)
    return result.stdout if capture else None
def checksum(path):
    with path.open("rb") as handle:
        return hashlib.file_digest(handle, "sha256").hexdigest()
def require(condition, message):
    if not condition:
        raise ValueError(message)

candidate = json.loads(Path("releases/hiro-release.json").read_text())
version, base = candidate["version"], candidate["base"]
for key in (version, base):
    require(re.fullmatch(r"[0-9]+\.[0-9]+\.[0-9]+", key), "Invalid bundle version")
for key in ("sha256","base_sha256"):
    require(re.fullmatch(r"[0-9a-f]{64}", candidate[key]), "Invalid release checksum")
require(tuple(map(int,version.split("."))) > tuple(map(int,base.split("."))), "Bundle version must increase")
tag = "hiro-v" + version
name = f"Hiro_Morphe_Patches_v{version}.mpp"
asset = lambda v: f"Hiro_Morphe_Patches_v{v}.mpp"
feed = json.loads(Path("patches-bundle.json").read_text())
with tempfile.TemporaryDirectory() as work:
    tmp = Path(work)
    old, final = tmp/"base.mpp", tmp/name
    gh("release","download","hiro-v"+base,"--repo",repo,"-p",asset(base),"-O",str(old))
    require(checksum(old)==candidate["base_sha256"], "Previous release has different bytes")
    subprocess.run(["zstd","-q","-d","--long=26","--patch-from="+str(old),
                    str(Path("releases/hiro-bundles")/version/"delta.zst"),"-o",str(final)],check=True)
    require(checksum(final)==candidate["sha256"],"Reconstructed MPP differs from the verified build")
    with ZipFile(final) as archive:
        manifest = archive.read("META-INF/MANIFEST.MF").decode().splitlines()
        require("Version: "+version in manifest,"Wrong MPP manifest version")
    present=subprocess.run(["gh","release","view",tag,"--repo",repo],stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
    if present.returncode:
        require(feed["version"]==base,"Unexpected version on the update channel")
        notes=tmp/"notes.md"; notes.write_text(candidate["notes"]+"\n")
        gh("release","create",tag,str(final),"--repo",repo,"--target",os.environ["GITHUB_SHA"],"--latest=false","--title","Hiro Morphe Patches "+version,"--notes-file",str(notes))
    downloaded=tmp/"verified.mpp"
    gh("release","download",tag,"--repo",repo,"-p",name,"-O",str(downloaded))
    require(checksum(downloaded)==candidate["sha256"],"Published release asset differs")
    if feed["version"]==version:
        require(feed["download_url"].endswith("/"+name),"Manager URL does not name the verified MPP")
        gh("release","edit",tag,"--repo",repo,"--latest")
