#!/usr/bin/env python3
"""Append curated exact Japanese emoji aliases without removing/reordering baseline pairs."""
from __future__ import annotations
import argparse,hashlib,json,os,subprocess,unicodedata,zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
BASELINE_SHA='24ea97801a5756f9f5d7d5244a8e6eb094de8b7c10a9986e5f363fc0fc595e12'
def sha(b):return hashlib.sha256(b).hexdigest()
def require(ok,msg):
 if not ok:raise RuntimeError(msg)
def normalize(s):
 s=unicodedata.normalize('NFKC',s).lower().strip()
 return ''.join(chr(ord(c)-0x60) if '\u30a1'<=c<='\u30f6' else c for c in s)
def read_tsv(raw):
 d={}
 for line in raw.decode().splitlines():
  row=line.split('\t');require(len(row)>=2 and all(row),'Malformed dictionary row')
  require(row[0] not in d and len(row[1:])==len(set(row[1:])),'Duplicate baseline key/pair')
  d[row[0]]=row[1:]
 return d
def run(cmd,log):
 p=subprocess.run([str(x) for x in cmd],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);log.write_text(p.stdout)
 require(p.returncode==0,'Command failed: '+str(log)+'\n'+p.stdout[-3000:]);return p.stdout
def build(baseline,output,tools=None,runtime=None):
 raw=baseline.read_bytes();require(sha(raw)==BASELINE_SHA,'Baseline emoji dictionary differs')
 old=read_tsv(raw);new={k:list(v) for k,v in old.items()};known={e for es in old.values() for e in es};groups=0;aliases=[]
 for no,line in enumerate((ROOT/'related_terms.tsv').read_text().splitlines(),1):
  if not line or line.startswith('#'):continue
  cols=line.split('\t');require(len(cols)==2,'Malformed curated row '+str(no));emojis=cols[0].split();terms=cols[1].split('|')
  require(emojis and terms,'Empty curated row');require(all(e in known for e in emojis),'New glyph outside baseline inventory at row '+str(no)+': '+repr(set(emojis)-known))
  for term in terms:
   key=normalize(term);require(len(key)>=2 and not key.startswith('#'),'Short/comment alias');require('\t' not in key and '\n' not in key,'Invalid key')
   values=new.setdefault(key,[])
   for emoji in emojis:
    if emoji not in values:values.append(emoji)
   aliases.append((term,key,emojis))
  groups+=1
 require(all(new[k][:len(v)]==v for k,v in old.items()),'Baseline association/order changed')
 require(all(len(v)==len(set(v)) for v in new.values()),'Duplicate emoji pairs')
 output.mkdir(parents=True,exist_ok=True)
 data=''.join(k+'\t'+'\t'.join(new[k])+'\n' for k in sorted(new)).encode();(output/'emoji_ja.tsv').write_bytes(data)
 report={'status':'PASSED','baseline_sha256':sha(raw),'sha256':sha(data),'old_readings':len(old),'old_candidates':sum(map(len,old.values())),'readings':len(new),'candidates':sum(map(len,new.values())),'added_readings':len(new)-len(old),'added_candidates':sum(map(len,new.values()))-sum(map(len,old.values())),'maximum_candidates_per_reading':max(map(len,new.values())),'old_pairs_preserved':True,'order_preserved':True,'duplicates_absent':True,'sorted_unique_nonempty_keys':True,'curated_groups':groups,'curated_alias_occurrences':len(aliases),'curated_unique_keys':len({a[1] for a in aliases}),'new_single_character_aliases':0,'existing_emoji_inventory_only':True,'dictionary_bytes':len(data),'runtime_modified':False,'source_sha256':{p.relative_to(ROOT).as_posix():sha(p.read_bytes()) for p in sorted(ROOT.rglob('*')) if p.is_file() and '__pycache__' not in p.parts}}
 require(report['added_readings']>=500,'Expansion unexpectedly small')
 if tools or runtime:
  require(tools is not None and runtime is not None,'--tools and --runtime are required together')
  work=output/'host';work.mkdir(exist_ok=True);classes=work/'classes';classes.mkdir(exist_ok=True)
  run(['java','com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-cp',tools/'android.jar','-d',classes,ROOT/'EmojiCandidates.java',*sorted((ROOT/'stubs').rglob('*.java'))],work/'compile.log')
  with zipfile.ZipFile(work/'emoji.jar','w') as z,zipfile.ZipFile(work/'stubs.jar','w') as st:
   for p in sorted(classes.rglob('*.class')):
    name=p.relative_to(classes).as_posix();(z if name.startswith('hiro/swiftkey/EmojiCandidates') else st).writestr(name,p.read_bytes())
  dex=work/'dex';dex.mkdir(exist_ok=True)
  run(['java','-cp',tools/'r8.jar','com.android.tools.r8.D8','--debug','--min-api','26','--lib',tools/'android.jar','--classpath',work/'stubs.jar','--output',dex,work/'emoji.jar'],work/'d8.log')
  tc=work/'dumpclasses';tc.mkdir(exist_ok=True);run(['java','com.sun.tools.javac.Main','-cp',tools/'apktool.jar','-d',tc,ROOT/'Dump1814.java'],work/'dumpcompile.log')
  dump=['java','-cp',str(tc)+os.pathsep+str(tools/'apktool.jar'),'Dump1814']
  run(dump+[runtime,work/'published'],work/'published-dump.log');run(dump+[dex/'classes.dex',work/'rebuilt'],work/'rebuilt-dump.log')
  rebuilt={p.relative_to(work/'rebuilt').as_posix():p.read_bytes() for p in (work/'rebuilt').rglob('*.smali')}
  require(len(rebuilt)==2,'Unexpected emoji runtime family')
  require(all((work/'published'/name).read_bytes()==body for name,body in rebuilt.items()),'Archived EmojiCandidates source does not reconstruct published runtime')
  run(['java','com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-cp',str(classes)+os.pathsep+str(tools/'android.jar'),'-d',classes,*sorted((ROOT/'tests').rglob('*.java'))],work/'testcompile.log')
  stdout=run(['java','-Xmx512m','-cp',classes,'hiro.swiftkey.EmojiExpansionTest',output/'emoji_ja.tsv',ROOT/'related_terms.tsv'],work/'tests.log')
  result=json.loads(stdout.strip().splitlines()[-1]);require(result['status']=='PASSED','Host tests failed')
  report.update({'host_tests_passed':True,'baseline_java_matches_published_dex':True,'runtime_sha256':sha(runtime.read_bytes()),'host_report':result,'device_tested':False})
 (output/'emoji-report.json').write_text(json.dumps(report,ensure_ascii=False,indent=2,sort_keys=True)+'\n')
 print(json.dumps({k:report[k] for k in ['status','old_readings','readings','added_readings','added_candidates','sha256']},ensure_ascii=False));return report
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--baseline',type=Path,required=True);p.add_argument('--output',type=Path,required=True);p.add_argument('--tools',type=Path);p.add_argument('--runtime',type=Path);a=p.parse_args();build(a.baseline.resolve(),a.output.resolve(),a.tools.resolve() if a.tools else None,a.runtime.resolve() if a.runtime else None)
