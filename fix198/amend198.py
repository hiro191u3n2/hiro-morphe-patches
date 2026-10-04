#!/usr/bin/env python3
"""Bounded build-only amendment; preserve native source and all release gates."""
import hashlib,json,sys
from pathlib import Path
root=Path(sys.argv[1]).resolve()
p=root/'fix198/package198.py'
old_hash='97cc642dad2253657439dfbc44bbf310e0c2f688c886db08191ada3b39fe82c2'
new_hash='4b005e4be5efe6f49d15cf622f2a324de5dfbee9ca526195405c2c447fa2ebff'
old="tools=work/'classes';tools.mkdir(exist_ok=True)\n run([javac,'-encoding','UTF-8','-cp',jar,'-d',tools,R/'fix197/build/MergePayloads.java',R/'fix197/build/Merge197.java',R/'fix198/Patch198.java'],work/'javac.txt')"
new="tools=work/'classes';tools.mkdir(exist_ok=True)\n # Reuse the exact contract helper from the checksum-pinned baseline MPP.\n contract=tools/'app/hiro/ulike/patches/MethodContract.class';contract.parent.mkdir(parents=True,exist_ok=True);contract.write_bytes(base['app/hiro/ulike/patches/MethodContract.class'])\n run([javac,'-encoding','UTF-8','-cp',str(tools)+os.pathsep+str(jar),'-d',tools,R/'fix197/build/MergePayloads.java',R/'fix197/build/Merge197.java',R/'fix198/Patch198.java'],work/'javac.txt')"
raw=p.read_bytes();digest=hashlib.sha256(raw).hexdigest()
assert digest in (old_hash,new_hash),'Unexpected packaging source revision'
if digest==old_hash:
 text=raw.decode();assert text.count(old)==1
 updated=text.replace(old,new).encode();assert hashlib.sha256(updated).hexdigest()==new_hash
 p.write_bytes(updated)
source=Path(__file__).read_bytes();(root/'fix198/amend198.py').write_bytes(source)
m=root/'fix198/overlay-files.json';data=json.loads(m.read_text())
data['fix198/package198.py']=new_hash
data['fix198/amend198.py']=hashlib.sha256(source).hexdigest()
m.write_text(json.dumps(data,indent=2,sort_keys=True)+'\n')
for name,h in data.items():assert hashlib.sha256((root/name).read_bytes()).hexdigest()==h,name
print('PASS bounded build-only amendment and complete source closure')
