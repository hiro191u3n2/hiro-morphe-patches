"""Build the link fix from a verified v386 baseline and check existing ZIP entries."""
from pathlib import Path
import argparse, importlib.util, json, subprocess, tempfile, zipfile

ROOT = Path(__file__).resolve().parents[2]
SRC = Path(__file__).resolve().parent
SPEC = importlib.util.spec_from_file_location("bundle", ROOT / "scripts/newx1243-377/source/build/integrate.py")
bundle = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(bundle)
BASE_HASH = "5f910ad5d66a50f11aec6d3f9952e1034892a7a748ebd9aff97f1cdd201aa845"
VERSION = "1.0.387"
STAMP = "2026-11-19T11:20:00"
COMBINED = f"Hiro_Morphe_Patches_v{VERSION}.mpp"
SINGLE = "NewX_Shared_Video_v1.0.0.mpp"

def run(*args):
    subprocess.run([str(x) for x in args],check=True)

def build(tools, out):
    basefile = ROOT / "public/Hiro_Morphe_Patches_v1.0.386.mpp"
    assert bundle.sha(basefile)==BASE_HASH
    tools, out = tools.resolve(), out.resolve()
    assert bundle.sha(tools/"d8.jar")=="38a9ffaaebbd17d1514e3ecbff5a85b989bb13c551b9e224c7409514eea84ff5"
    assert bundle.sha(tools/"morphe.jar")=="07992da468446028f54cfd7854fee1e63d883dd5babb2b208979e13ba253bf10"
    out.mkdir(exist_ok=True,parents=True)
    base=bundle.read_archive(basefile)
    fields=bundle.headers(base[bundle.MF])
    fields.update(Version=VERSION,Timestamp=STAMP,Description="Open the linked X post for shared first-video URLs.")
    with tempfile.TemporaryDirectory() as t:
        tmp=Path(t)
        for name in ("jvm","dex","merger"): (tmp/name).mkdir()
        javac=["java","-m","jdk.compiler/com.sun.tools.javac.Main","-proc:none","-cp",tools/"morphe.jar"]
        run(*javac,"-source","8","-target","8","-d",tmp/"jvm",SRC/"SharedVideoPatch.java")
        providers={p.relative_to(tmp/"jvm").as_posix():p.read_bytes() for p in (tmp/"jvm").rglob("*.class")}
        assert not providers.keys() & base.keys()
        bundle.write_archive(tmp/"providers.jar",providers)
        run("java","-cp",tools/"d8.jar","com.android.tools.r8.D8","--min-api","26","--classpath",tools/"morphe.jar",
            "--output",tmp/"dex",tmp/"providers.jar")
        solo=(tmp/"dex/classes.dex").read_bytes()
        (tmp/"base.dex").write_bytes(base["classes.dex"])
        run(*javac,"-d",tmp/"merger",SRC/"MergeVideo.java")
        run("java","-Xmx1g","-cp",str(tmp/"merger")+":"+str(tools/"morphe.jar"),"MergeVideo",
            tmp/"base.dex",tmp/"dex/classes.dex",tmp/"merged.dex")
        for name,content in [(SINGLE,{**providers,bundle.MF:bundle.manifest(dict(fields,Name="NewX Shared Video",Version="1.0.0")),"classes.dex":solo}),
                             (COMBINED,{**base,**providers,bundle.MF:bundle.manifest(fields),"classes.dex":(tmp/"merged.dex").read_bytes()})]:
            bundle.write_archive(out/name,content)
            assert bundle.read_archive(out/name)==content
    files={n:{"bytes":(out/n).stat().st_size,"sha256":bundle.sha(out/n)} for n in (SINGLE,COMBINED)}
    report={"artifacts":files,"baseline":BASE_HASH,"existing_zip_entries_verified":len(base)-2}
    (out/"build.json").write_text(json.dumps(report,indent=2)+"\n")
    print(json.dumps(report,indent=2))
if __name__=="__main__":
    p=argparse.ArgumentParser()
    p.add_argument("tools",type=Path)
    p.add_argument("out",type=Path)
    a=p.parse_args()
    build(a.tools,a.out)
