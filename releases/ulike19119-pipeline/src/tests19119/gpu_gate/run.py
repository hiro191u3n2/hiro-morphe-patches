import argparse,pathlib,subprocess,hashlib,json,shutil
p=argparse.ArgumentParser();p.add_argument('--out',required=True);p.add_argument('--jdk',required=True);a=p.parse_args();here=pathlib.Path(__file__).resolve().parent;root=here.parent.parent;out=pathlib.Path(a.out);out.mkdir(parents=True,exist_ok=True);classes=out/'classes';classes.mkdir(exist_ok=True)
sources=[root/'GpuStrongOptimized19119.java',here/'GpuGatePeers19119.java',here/'GpuGate19119Test.java']
cmd=[str(pathlib.Path(a.jdk)/'bin/javac'),'-source','8','-target','8','-d',str(classes),*map(str,sources)]
r=subprocess.run(cmd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(out/'javac.log').write_text(r.stdout);assert r.returncode==0,r.stdout
r=subprocess.run([str(pathlib.Path(a.jdk)/'bin/java'),'-cp',str(classes),'com.hiro.ulike.GpuGate19119Test'],text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT);(out/'run.log').write_text(r.stdout);assert r.returncode==0,r.stdout
result={'status':'passed','scope':'actual production candidate helper with independent fake native/ledger peers; host admission only','sources':{str(f.relative_to(root)):hashlib.sha256(f.read_bytes()).hexdigest() for f in sources},'output':r.stdout.strip()};(out/'result.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
