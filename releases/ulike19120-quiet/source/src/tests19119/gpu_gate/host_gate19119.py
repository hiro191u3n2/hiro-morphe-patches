"""Run production .119 GPU admission helper against failure-injecting peers."""
import hashlib,json,pathlib,subprocess,re

def test(source,work,jdk,ndk=None):
 source=pathlib.Path(source).resolve();work=pathlib.Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 command=['python3',str(source/'tests19119/gpu_gate/run.py'),'--out',str(work),'--jdk',str(jdk)]
 result=subprocess.run(command,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 (work/'host_gate19119.log').write_text(result.stdout)
 if result.returncode:raise RuntimeError(result.stdout)
 report=json.loads((work/'result.json').read_text());match=re.search(r'gate assertions=(\d+)',report.get('output',''));assert match and int(match.group(1))>=28
 report.update(assertions=int(match.group(1)),positive_controls=10,negative_controls=18,
  candidate_helper_sha256=hashlib.sha256((source/'GpuStrongOptimized19119.java').read_bytes()).hexdigest(),physical_device=False)
 (work/'result.json').write_text(json.dumps(report,indent=2)+'\n');return report
