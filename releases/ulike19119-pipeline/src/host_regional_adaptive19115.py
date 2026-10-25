#!/usr/bin/env python3
"""Isolated actual-owner certificate and queue regression proof, no GPU timing proof."""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess, time
ROOT=Path(__file__).resolve().parent
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve()
 jdk=Path(jdk or os.environ.get('ULIKE_JDK_HOME') or os.environ.get('JAVA_HOME') or Path(shutil.which('javac')).resolve().parent.parent)
 if work.exists():raise AssertionError('Fresh work directory required')
 work.mkdir(parents=True)
 helper=source/'GpuRegionalDefault19115.java'
 gsd=source/'GpuStrongDefault1990.java';peers=source/'tests19115/adaptive_preserve/PreservePeers19115.java';testfile=source/'tests19115/adaptive_preserve/RegionalAdaptivePreserve19115Test.java'
 tracked=[helper,gsd,peers,testfile,source/'host_regional_adaptive19115.py']
 pins={str(p.relative_to(source)):sha(p) for p in tracked};commands=[];start=time.monotonic()
 def run(argv,name,expected=None):
  argv=list(map(str,argv));commands.append({'name':name,'argv':argv});p=subprocess.run(argv,text=True,capture_output=True,timeout=180);out=p.stdout+p.stderr
  (work/(name+'.log')).write_text(out)
  if expected:
   if not p.returncode or 'java.lang.AssertionError' not in out or expected not in out:raise AssertionError(name+' did not fail at contract:\n'+out[-7000:])
  elif p.returncode:raise AssertionError(name+' failed:\n'+out[-7000:])
  return out
 def compile(h,t,name,g=None):
  classes=work/name/'classes';classes.mkdir(parents=True)
  run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,h,g or gsd,peers,t],name+'-compile')
  return classes
 def execute(classes,name,expected=None,args=()):return run([jdk/'bin/java','-XX:ActiveProcessorCount=4','-Xmx768m','-cp',classes,'com.hiro.ulike.RegionalAdaptivePreserve19115Test',*args],name,expected)
 result=json.loads(execute(compile(helper,testfile,'integrated'),'integrated'))
 original=helper.read_text()
 guard='if(legacyAdaptiveCached19115()){GpuQualification1961.outcome1984("baseline_unavailable");return false;}'
 mutations=[
  ('heavy_reservation','if(GpuStrongDefault1990.adaptivePending19115()||heavy&&!GpuStrongDefault1990.regionalHeavyAllowed19115())return;','if(GpuStrongDefault1990.adaptivePending19115())return;',1,'unfinished legacy candidates refuse regional heavy reservation'),
  ('pending_preparation','if(GpuStrongDefault1990.adaptivePending19115()||heavy&&!GpuStrongDefault1990.regionalHeavyAllowed19115())return;','if(heavy&&!GpuStrongDefault1990.regionalHeavyAllowed19115())return;',1,'even cached regional preparation cannot coqueue an active adaptive owner'),
  ('direct_heavy_qualification','||heavy&&!cached()&&!GpuStrongDefault1990.regionalHeavyAllowed19115()','',1,'unfinished legacy boundary precedes optional GPU preparation'),
  ('final_terminal','            if(!GpuStrongDefault1990.regionalHeavyAllowed19115()){\n                GpuQualification1961.outcome1984("baseline_unavailable");return false;\n            }','',1,'final terminal loss cannot persist regional proof'),
  ('selection','&&!legacyAdaptiveCached19115()','',1,'regional cannot replace old independently faster adaptive route'),
  ('reservation','GpuCertificates1990.rejected(EXACT)||legacyAdaptiveCached19115()||faster()','GpuCertificates1990.rejected(EXACT)||faster()',1,'old adaptive pair prevents regional queue reservation'),
  ('early_qualification',guard,'',1,'legacy owner checked before compilation and proof writes'),
  ('final_qualification','check(cancellation);progress("strong_speed_compare",2,2);\n            '+guard,'check(cancellation);progress("strong_speed_compare",2,2);',1,'final-boundary race cannot persist a mismatched baseline proof'),
  ('prepared_required','return GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)!=null&&','return GpuNoise1960.prepared1994(48)&&GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)!=null&&',1,'old exact and speed pair blocks independently of preparation'),
  ('quality_only','&&\n                GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.FAST)!=null','',1,'partial or new-namespace proof is not an old adaptive pair'),
  ('speed_only','GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)!=null&&\n                ','',1,'partial or new-namespace proof is not an old adaptive pair'),
  ('new_namespace','GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.FAST)','GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.FAST+":regional115")',1,'old exact and speed pair blocks independently of preparation'),
 ]
 direct=original[original.index('        return GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)!=null&&'):original.index('\n    }',original.index('        return GpuCertificates1990.restore(GpuStrongDefault1990.Adaptive1994.EXACT)!=null&&'))]
 mutations.append(('recursive_cache',direct,'        return GpuStrongDefault1990.Adaptive1994.cached()||GpuStrongDefault1990.Adaptive1998.cached();',1,'certificate query recursion guard'))
 mutants=[]
 for name,before,after,n,expected in mutations:
  if before not in original:raise AssertionError('Missing mutation anchor '+name)
  changed=original.replace(before,after,n)
  folder=work/'mutations'/name;folder.mkdir(parents=True);m=folder/helper.name;m.write_text(changed)
  execute(compile(m,testfile,'mutation-'+name),'mutation-'+name,expected)
  mutants.append({'name':name,'rejected':True,'expected_assertion':expected})
 gsd_original=gsd.read_text()
 for name,before,after,expected in [
  ('both_legacy_terminal','return Adaptive1998.completed19115()&&Adaptive1994.completed19115();','return Adaptive1998.completed19115()||Adaptive1994.completed19115();','one completed legacy candidate cannot unlock regional heavy'),
  ('cancelled_legacy_terminal','if(requested)return false;if(attempted)return true;','if(requested)return false;if(!attempted)return true;if(attempted)return true;','cancelled legacy attempt is not a terminal candidate')]:
  if before not in gsd_original:raise AssertionError('Missing GSD mutation anchor '+name)
  folder=work/'mutations'/name;folder.mkdir(parents=True);m=folder/gsd.name;m.write_text(gsd_original.replace(before,after,1))
  execute(compile(helper,testfile,'mutation-'+name,m),'mutation-'+name,expected)
  mutants.append({'name':name,'rejected':True,'expected_assertion':expected})
 if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('Tracked input changed during proof')
 result.update(assertions=result['assertions']+len(mutants),mutation_controls=mutants,mutation_count=len(mutants),source_sha256=pins,host_elapsed_seconds=round(time.monotonic()-start,3),physical_android_tested=False,device_speedup_verified=False)
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n')
 return result
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk),indent=2))
