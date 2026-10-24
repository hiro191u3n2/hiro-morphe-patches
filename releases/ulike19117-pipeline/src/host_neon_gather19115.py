#!/usr/bin/env python3
"""Actual ARM64 exact proof for cached four-lane Color loads, no speed claim."""
from pathlib import Path
import argparse,hashlib,json,os,re,shutil,subprocess,sys
BASELINE_HEADER_SHA='0f5f589a2d641e0929541a0c509af0212ddb8ec15e2a8b87b2f11b6d9d917b04'
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ['JAVA_HOME']);ndk=Path(ndk or os.environ['ULIKE_NDK_HOME'])
 cc=shutil.which('aarch64-linux-gnu-gcc');qemu=shutil.which('qemu-aarch64');assert cc and qemu,'Actual ARM64 compiler and emulator required'
 sys.path.insert(0,str(source));from host_speed1959 import baseline_pins
 historical=baseline_pins() # Independently pinned, immutable published scalar oracle.
 names=['native1958/smooth_noise1958.c','native1958/scratch1959.h','native1958/neon1959.h','tests1959/native-arm1959.c','tests1959/native-oracle1959.c','tests1959/published1958-reference/native1958/smooth_noise1958.c','tests19115/neon_gather/NeonGather19115Test.c','host_neon_gather19115.py']
 pins={n:sha(source/n) for n in names};commands=[]
 def run(label,args,success=True,timeout=180):
  args=list(map(str,args));commands.append({'label':label,'argv':args});r=subprocess.run(args,text=True,capture_output=True,timeout=timeout);out=r.stdout+r.stderr;(work/(label+'.log')).write_text(out)
  if success and r.returncode:raise AssertionError(label+'\n'+out[-12000:])
  if not success and (r.returncode==0 or 'Actual ARM .58 equivalence failed:' not in out):raise AssertionError('Mutation escaped or failed for unrelated reason: '+label+'\n'+out[-12000:])
  return r,out
 flags=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-pthread','-static']
 includes=['-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux')]
 driver='tests19115/neon_gather/NeonGather19115Test.c';oracle='tests1959/native-oracle1959.c'
 run('arm-compile',[cc,*flags,*includes,source/driver,source/oracle,'-lm','-o',work/'actual-arm'])
 _,out=run('arm-execute',[qemu,work/'actual-arm'],timeout=900)
 result=json.loads(out.split('GATHER_RESULT ',1)[1].strip());assert result['status']=='passed' and result['interior_cases']>0 and result['edge_cases']>0 and result['uncached_cases']>0
 header=(source/'native1958/neon1959.h').read_text();mutants=[('channels','VC1959 value={c.val[0],c.val[1],c.val[2],','VC1959 value={c.val[0],c.val[2],c.val[1],'),('alpha','vceqq_u32(vshrq_n_u32(pixels,24),vdupq_n_u32(255))','vorrq_u32(vceqq_u32(vshrq_n_u32(pixels,24),vdupq_n_u32(255)),vdupq_n_u32(UINT32_MAX))')]
 for label,before,after in mutants:
  assert header.count(before)==1,'Mutation anchor changed: '+label
  root=work/('mutant-'+label)
  for name in names:
   if name.endswith('.py'):continue
   target=root/name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(source/name,target)
  (root/'native1958/neon1959.h').write_text(header.replace(before,after))
  run(label+'-compile',[cc,*flags,*includes,root/driver,root/oracle,'-lm','-o',root/'proof'])
  run(label+'-execute',[qemu,root/'proof'],success=False,timeout=900)
 baseline=source/'tests19115/provenance/reference114/native1958/neon1959.h';assert sha(baseline)==BASELINE_HEADER_SHA,'Published baseline vector loader changed'
 android=ndk/'toolchains/llvm/prebuilt/linux-x86_64/bin/aarch64-linux-android26-clang'
 assembly_flags=['-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-pthread','-fPIC','-S']
 paths={}
 for label in ('baseline','candidate'):
  root=work/label/'native1958';root.mkdir(parents=True,exist_ok=True)
  for name in ('smooth_noise1958.c','scratch1959.h','neon1959.h'):shutil.copy2(source/'native1958'/name,root/name)
  if label=='baseline':shutil.copy2(baseline,root/'neon1959.h')
  assembly=work/(label+'-android.s');run(label+'-android-assembly',[android,*assembly_flags,root/'smooth_noise1958.c','-o',assembly])
  body=assembly.read_text().split('vgather1959:',1)[1].split('\tret',1)[0]
  instructions=[line for line in body.splitlines() if line.startswith('\t') and not line.lstrip().startswith('.')]
  paths[label]={'cached_hit_path_instructions':len(instructions)+1,'ld3_count':sum('\tld3\t' in line for line in instructions),'assembly_sha256':sha(assembly)}
 assert paths['candidate']['ld3_count']==1 and paths['candidate']['cached_hit_path_instructions']<paths['baseline']['cached_hit_path_instructions'],'Android cached path must contain the vector load and remove instructions'
 assert pins=={n:sha(source/n) for n in names},'ARM proof source changed during execution'
 result.update(assertions=result['assertions']+4,mutants_detected=2,actual_arm64_execution=True,independent_frozen_scalar_oracle=True,baseline_mpp_sha256=historical['baseline_mpp_sha256'],source_sha256=pins,android_assembly=paths,baseline_header_sha256=BASELINE_HEADER_SHA,production_arithmetic_unchanged=True,physical_android_tested=False,device_speedup_verified=False,save_2000ms_verified=False,scope='Actual AArch64/QEMU numerical proof plus official NDK Android cached-load code generation; instruction counts are one gather hit path, not whole-kernel time or device speed.')
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',required=True);p.add_argument('--work',required=True);p.add_argument('--jdk');p.add_argument('--ndk');a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
