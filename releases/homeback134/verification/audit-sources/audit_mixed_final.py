from pathlib import Path
import hashlib,json,os,re,subprocess,sys,zipfile
R=Path(__file__).resolve().parent;W=R.parent
JAVA=W/'jdk134/jdk-21.0.8+9/bin/java';MPP=W/'dist134-first/Hiro_Morphe_Patches_v1.0.318.mpp'
BASE=R/'piko64-baseline/patched.apk'
CP=os.pathsep.join(map(str,[W/'build134-first/audit',W/'build134-first/patch',W/'tools134/tools/morphe.jar']))
RCP=os.pathsep.join(map(str,[R/'audit-resources',W/'tools134/tools/morphe.jar']))
def identity(p):
 with p.open('rb') as f:return {'bytes':p.stat().st_size,'sha256':hashlib.file_digest(f,'sha256').hexdigest()}
def inventory(p):
 result={}
 with zipfile.ZipFile(p) as z:
  assert len(z.namelist())==len(set(z.namelist()))
  for e in z.infolist():
   n=e.filename
   if e.is_dir() or n in ('AndroidManifest.xml','resources.arsc') or re.fullmatch(r'classes(?:[0-9]+)?\.dex',n) or re.fullmatch(r'META-INF/(?:MANIFEST\.MF|[^/]+\.(?:SF|RSA|DSA|EC))',n,re.I):continue
   result[n]=hashlib.sha256(z.read(e)).hexdigest()
 return result
for label in sys.argv[1:]:
 folder=R/label;out=folder/'patched.apk';proof=json.loads((folder/'proof.json').read_text())
 assert proof['success'] and identity(out)==proof['output']
 before=identity(out);base_id=identity(BASE);mpp_id=identity(MPP)
 runs=[('native-home-audit','2g',CP,'Instagram134NativeHomeAudit',[BASE,folder/'native-synthetic.dex',out]),
       ('manifest-audit','2g',RCP,'InstagramManifestAudit',['--preserve-baseline',BASE,out]),
       ('resources-audit','2g',RCP,'InstagramResourcesAudit',['--preserve-baseline',BASE,out]),
       ('profile-link-audit','2g',CP,'Instagram130ProfileLinkAudit',['output',BASE,out]),
       ('whole-apk-audit','3g',CP,'ActualInstagram452Audit',[BASE,out,MPP])]
 audits={}
 for name,heap,cp,clazz,args in runs:
  command=list(map(str,[JAVA,'-ea','-Xmx'+heap,'-cp',cp,clazz,*args]));log=folder/(name+'.txt')
  print('RUN',label,name,flush=True)
  with log.open('w') as f:p=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
  text=log.read_text();assert p.returncode==0,(label,name,p.returncode,text[-2000:])
  assert identity(out)==before and identity(BASE)==base_id and identity(MPP)==mpp_id,'Audit mutated input'
  passes=[l for l in text.splitlines() if l.startswith('PASS ')];assert passes,(label,name)
  audits[name]={'result':'PASS','summary':passes,'log':identity(log),'command':command}
 bi=inventory(BASE);oi=inventory(out);changes=[n for n in bi.keys()|oi.keys() if bi.get(n)!=oi.get(n)];assert not changes,changes
 result={'schema':'homeback134-piko-preservation-v1','result':'PASS','case':label,'order':proof['order'],'physical_android_tested':False,'input_piko_baseline':base_id,'output':before,'mpp':mpp_id,'manager_apply':proof,'audits':audits,'unchanged_zip_entries':len(bi),'native_libraries_byte_identical':True,'zip_entry_exclusions':['DEX','AndroidManifest.xml','resources.arsc','signature metadata'],'inputs_hash_unchanged_after_each_audit':True}
 (folder/'preservation-proof.json').write_text(json.dumps(result,ensure_ascii=False,indent=2,sort_keys=True)+'\n')
 print('PASS',label,json.dumps(before),flush=True)
