"""Run the new patch alone and together with the user's previous NewX selection."""
import argparse, importlib.util, json, subprocess
from pathlib import Path
p=argparse.ArgumentParser()
p.add_argument("workspace", type=Path)
p.add_argument("case", choices=["single","combined","clone"])
a=p.parse_args()
work=a.workspace.resolve()
prior=Path(__file__).resolve().parents[1]/"piko-ja383/source/verification/verify_packages.py"
spec=importlib.util.spec_from_file_location("previous",prior)
previous=importlib.util.module_from_spec(spec)
spec.loader.exec_module(previous)
name="NewX: 動画リンクで元の投稿を開く"
cmd=["java","-XX:ActiveProcessorCount=4","-Xmx2500m","-jar",str(work/"tools/morphe.jar"),"patch",str(work/"X.apkm"),
     "--exclusive","--unsigned","--bytecode-mode","FULL","-o",str(work/(a.case+".apk")),"-r",str(work/(a.case+".json")),"-t",str(work/(a.case+"-temp"))]
if a.case=="single":
    configs=[("dist/NewX_Shared_Video_v1.0.0.mpp",[name])]
else:
    piko=previous.PIKO+(["NewX: Clone"] if a.case=="clone" else [])
    configs=[("dist/Hiro_Morphe_Patches_v1.0.387.mpp",previous.HIRO+[name]),("tools/patches-3.12.0-dev.3.mpp",piko)]
for path, names in configs:
    cmd+=["-p",str(work/path)]
    for item in names:cmd+=["-e",item]
with (work/(a.case+".log")).open("w") as f:subprocess.run(cmd,check=True,stdout=f,stderr=subprocess.STDOUT)
report=json.loads((work/(a.case+".json")).read_bytes())
assert report["success"] and not report["failedPatches"] and len(report["appliedPatches"])==sum(len(x) for _,x in configs)
print(a.case,len(report["appliedPatches"]))
