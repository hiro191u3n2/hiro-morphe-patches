#!/usr/bin/env python3
"""Independent observer tests and semantic comparison of the private DEX delta.
Requires caller-owned original APK and the author's generated private delta.
"""
import argparse,hashlib,json,os,re,subprocess,tempfile,zipfile
from pathlib import Path
HERE=Path(__file__).resolve().parent
MODULE=HERE.parent
CORE=MODULE.parent
WORK=CORE.parents[1]
CHECKS=0
def check(ok,why):
 global CHECKS
 CHECKS+=1
 if not ok:raise AssertionError(why)
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(args):
 p=subprocess.run(list(map(str,args)),capture_output=True,text=True,env={**os.environ,'ORT_DISABLE_TELEMETRY':'1'})
 if p.returncode:raise RuntimeError(p.stdout+'\n'+p.stderr)
 return p.stdout.strip()
OWNER='Lcom/ss/android/medialib/RecordInvoker;'
HOOK='Lcom/hiro/ulike/composer/NativeComposerHooks;'
ENTRIES={'initBeautyPlay':'beforeInit','initBeautyPlayOnlyPreview':'beforeUnsupportedInit','uninitBeautyPlay':'beforeUninit',
 'setComposerMode':'beforeMode','setComposerResourcePath':'beforeResource','setComposerNodes':'beforeSet','appendComposerNodes':'beforeAppend',
 'removeComposerNodes':'beforeRemove','reloadComposerNodes':'beforeReload','replaceComposerNodes':'beforeReplace',
 'updateComposerNode':'beforeUpdate','updateMultiComposerNodes':'beforeUpdates','setVEEffectParams':'beforeEffectParams'}
def method_id(m):return m.get_class_name()+'->'+m.get_name()+m.get_descriptor().replace(' ','')
def listing(m):
 out=[];address=0
 for ins in m.get_instructions():out.append((address,ins));address+=ins.get_length()//2
 return out,address
def hook(ins):
 for op in ins.get_operands():
  if len(op)>=3 and isinstance(op[2],str) and op[2].startswith(HOOK+'->'):return op[2].split('->',1)[1].split('(',1)[0]
 return None
def registers(ins):return [op[1] for op in ins.get_operands() if int(op[0])==0]
def dex_review(apk,delta):
 from loguru import logger
 logger.disable('androguard')
 from androguard.core.dex import DEX
 originals={}
 with zipfile.ZipFile(apk) as z:
  for name in z.namelist():
   if not re.fullmatch(r'classes\d*\.dex',name):continue
   d=DEX(z.read(name))
   for c in d.get_classes():
    if c.get_name()==OWNER:
     for m in c.get_methods():originals[method_id(m)]=m
 edited=[]
 for c in DEX(delta.read_bytes()).get_classes():
  check(c.get_name()==OWNER,'delta contains only original recorder holder')
  edited.extend(c.get_methods())
 check(len(edited)==13,'exact thirteen transformed methods')
 total_returns=total_throws=total_instructions=0;fingerprints={}
 for m in edited:
  key=method_id(m);check(key in originals,'transformed method exists in original APK');old=originals[key]
  oi,oe=listing(old);ni,ne=listing(m)
  check(m.get_access_flags()==old.get_access_flags(),'access flags preserved')
  check(m.get_code().get_registers_size()==old.get_code().get_registers_size(),'register count preserved')
  check(m.get_code().get_ins_size()==old.get_code().get_ins_size(),'parameter words preserved')
  selfreg=m.get_code().get_registers_size()-m.get_code().get_ins_size()
  check(hook(ni[0][1])==ENTRIES[m.get_name()],'correct typed entry observer')
  check(registers(ni[0][1])==list(range(selfreg,m.get_code().get_registers_size())),'entry receives actual original argument registers')
  check([ins.get_name() for _,ins in ni[-3:]]==['move-exception','invoke-static/range','throw'],'appended catch handler shape')
  check(hook(ni[-2][1])=='failed' and registers(ni[-2][1])==[selfreg],'exception hook receives same original recorder')
  check(registers(ni[-3][1])==[0] and registers(ni[-1][1])==[0],'original Throwable register rethrown unchanged')
  kept=[(a,i) for a,i in ni[:-3] if hook(i) is None]
  check(len(kept)==len(oi),'no original instruction inserted/removed/replaced beyond observer hooks')
  old_index={a:k for k,(a,i) in enumerate(oi)};old_index[oe]=len(oi)
  new_index={a:k for k,(a,i) in enumerate(kept)};new_index[ni[-3][0]]=len(kept)
  # A branch originally targeting RETURN must target the new observer first.
  for idx,(a,ins) in enumerate(ni[:-3]):
   if hook(ins)=='returned':
    check(idx+1<len(ni) and ni[idx+1][1].get_name()=='return','completion immediately precedes original return')
    check(registers(ins)==registers(ni[idx+1][1]),'completion gets actual return value register')
    new_index[a]=new_index[ni[idx+1][0]];total_returns+=1
  def canonical(a,ins,index):
   operands=[]
   for op in ins.get_operands():
    kind=int(op[0])
    if kind==3:operands.append(('target',index[a+op[1]]))
    elif len(op)>=3:operands.append(('reference',kind,op[2]))
    else:operands.append((kind,op[1]))
   return ins.get_name(),operands
  for (oa,original),(na,changed) in zip(oi,kept):
   check(canonical(oa,original,old_index)==canonical(na,changed,new_index),'independent opcode/register/literal/reference/branch preservation: '+key)
   total_instructions+=1
  tail=ni[-3][0];handlers=m.get_code().get_handlers();outer=[]
  if handlers:
   hm={h.get_off()-handlers.get_off():h for h in handlers.get_list()}
   for t in m.get_code().get_tries():
    h=hm[t.get_handler_off()]
    check(t.get_start_addr()>=3,'entry observer not swallowed by original/outer catch')
    if h.get_size()<=0 and h.get_catch_all_addr()==tail:outer.append((t.get_start_addr(),t.get_start_addr()+t.get_insn_count()))
  check(bool(outer),'outer exception cleanup exists')
  for a,ins in ni[:-3]:
   if ins.get_name()=='throw':check(any(lo<=a<hi for lo,hi in outer),'original rethrow reaches observation failure handler');total_throws+=1
  fingerprints[key]={'stock_code_sha256':hashlib.sha256(old.get_code().get_raw()).hexdigest(),'delta_code_sha256':hashlib.sha256(m.get_code().get_raw()).hexdigest()}
 check(total_returns==26,'all26 original return sites observed')
 return {'checks':CHECKS,'methods':13,'normal_return_sites':total_returns,'original_rethrows_checked':total_throws,'original_instructions_compared':total_instructions,'method_code_sha256':fingerprints,'art_execution':False}
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--report',type=Path,default=HERE/'INDEPENDENT_REVIEW.json');args=p.parse_args()
 tools=WORK/'models168/tools';jdk=tools/'jdk21/jdk-21.0.12.1+1/bin';sdk=tools/'android.jar';r8=tools/'r8-8.3.37.jar'
 check(sha(sdk)=='d9eb9da824d9e247a352f570f01e1169e725b2954bca9e283a71786c59b59f9a','pinned SDK36')
 check(sha(r8)=='900dfbc649519969fc5a4c7520d6b7355338e565fa1249874e0190b8d61b1199','pinned D8')
 app=WORK/'complete_integration169';apk=WORK/'models168/inputs/base.apk';delta=app/'lifecycle/private_build/composer-observation-partial.dex'
 prod=sorted((MODULE/'src').rglob('*.java'));stubs=sorted((MODULE/'test/com/ss').rglob('*.java'))
 inventory=prod+stubs+[HERE/'BoundaryReview.java',Path(__file__).resolve(),app/'lifecycle/PrepareComposerObservation171.java',app/'lifecycle/VerifyComposerObservation171.java']
 before={str(f.relative_to(WORK)):sha(f) for f in inventory}
 with tempfile.TemporaryDirectory(prefix='ulike-composer-boundary-review-') as temp:
  w=Path(temp);host=w/'host';host.mkdir();android=w/'android';android.mkdir();dex=w/'dex';dex.mkdir()
  run([jdk/'javac','--release','8','-d',host,*prod,*stubs,HERE/'BoundaryReview.java'])
  host_result=json.loads(run([jdk/'java','-Xmx128m','-cp',host,'com.hiro.ulike.composer.BoundaryReview']))
  run([jdk/'javac','-source','8','-target','8','-bootclasspath',sdk,'-d',android,*prod])
  run([jdk/'java','-cp',r8,'com.android.tools.r8.D8','--min-api','26','--lib',sdk,'--output',dex,*sorted(android.rglob('*.class'))])
  dex_bytes=(dex/'classes.dex').stat().st_size
 independent_dex=dex_review(apk,delta)
 after={str(f.relative_to(WORK)):sha(f) for f in inventory}
 if before!=after:raise RuntimeError('Reviewed source changed during review')
 report={'status':'PASS_INDEPENDENT_NATIVE_COMPOSER_BOUNDARY_REVIEW','host':host_result,'sdk36_compile':True,'d8_min_api':26,'dex_bytes':dex_bytes,'independent_dex':independent_dex,
  'reviewed_file_sha256':before,'stock_apk_sha256':sha(apk),'private_delta_sha256':sha(delta),'tools_sha256':{'sdk36':sha(sdk),'r8':sha(r8)},
  'limits':['Entry snapshots own mutable arguments but do not prove the original native call consumed the same values if caller data changes concurrently.',
   'Native return values and handle snapshots do not prove callback correlation, queued composer completion, source restoration, or all-state mutation coverage.',
   'Observer hooks remain compile-time disabled and private delta is not installed; production ReplayPlan.UNAVAILABLE rejects authorization.',
   'DEX structure and original instruction semantics were inspected with an independent parser; ART and Samsung handset were not executed.',
   'No completed app release or style appearance equivalence is established.']}
 args.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps({'status':report['status'],'host':host_result,'independent_dex_checks':CHECKS,'report':str(args.report)}))
if __name__=='__main__':main()
