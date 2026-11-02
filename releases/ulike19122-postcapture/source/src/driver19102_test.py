#!/usr/bin/env python3
"""Execute focused tests, never substitute static assertions for device tests."""
from pathlib import Path
import argparse,hashlib,importlib.util,json,os,subprocess,sys
HERE=Path(__file__).resolve().parent
SOURCE=HERE/'generated/src'
def load(name,path):
 spec=importlib.util.spec_from_file_location(name,path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module
def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def command(args,log):
 p=subprocess.run([str(x) for x in args],stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=180)
 log.write_text(p.stdout)
 if p.returncode:raise RuntimeError('Command failed: '+str(args[0])+'\n'+p.stdout[-12000:])
 return p.stdout

def queue(args):
 sys.path.insert(0,str(SOURCE));from host_qualification1981 import FIXTURES
 work=args.work;fixtures=work/'fixtures';classes=work/'classes';fixtures.mkdir(parents=True);classes.mkdir();sources=[]
 for name,body in FIXTURES.items():
  if name.endswith('/QueueHost1967.java'):continue
  if name.endswith('/QueueFixtures1967.java'):
   old='static long captureEpoch1953(){return epoch;}'
   assert body.count(old)==1
   body=body.replace(old,old+'static long captureId1989(long value){return value==epoch?1:0;}')
  path=fixtures/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(body);sources.append(path)
 sources.extend([SOURCE/'GpuQualification1961.java',SOURCE/'tests1992/failure/Qualification1992Test.java',SOURCE/'tests1992/failure/TypedFailure1992Test.java',SOURCE/'tests19102/TerminalFailure19102Test.java'])
 command([args.jdk/'bin/javac','--release','8','-encoding','UTF-8','-d',classes,*sources],work/'javac.log')
 reports={}
 for name in ('Qualification1992Test','TypedFailure1992Test','TerminalFailure19102Test'):
  stdout=command([args.jdk/'bin/java','-ea','-cp',classes,'com.hiro.ulike.'+name],work/(name+'.log'))
  reports[name]=json.loads(stdout.strip().splitlines()[-1]);assert reports[name]['status']=='passed'
 original=load('fingerprint19102_retained',SOURCE/'host1990_fingerprint.py')
 reports['retained_fingerprint']=original.test(SOURCE,work/'fingerprint')
 private=work/'fingerprint/source';builder=load('fingerprint19102_extended',private/'native1960/build_native1960.py');native=private/'native1960'
 base=builder.compute_fingerprint1990(native)[0];path=native/'compute_dependencies1990.json';manifest=json.loads(path.read_text());count=0
 manifest['native_dependencies'].reverse();manifest['numerical_java'].reverse();path.write_text(json.dumps(manifest,indent=4));assert builder.compute_fingerprint1990(native)[0]==base;count+=1
 name=next(iter(manifest['excluded_runtime_java']));manifest['excluded_runtime_java'][name]+=' Reviewed diagnostic explanation only.';path.write_text(json.dumps(manifest));assert builder.compute_fingerprint1990(native)[0]==base;count+=1
 name='DiagnosticOnly19102.java';(private/name).write_text('package com.hiro.ulike; final class DiagnosticOnly19102 {}\n');manifest['excluded_runtime_java'][name]='Explicitly reviewed test-only diagnostic, no numeric or admission logic.';path.write_text(json.dumps(manifest));assert builder.compute_fingerprint1990(native)[0]==base;count+=1
 (private/name).write_text((private/name).read_text()+'// presentation-only change\n');assert builder.compute_fingerprint1990(native)[0]==base;count+=1
 reports['explicit_diagnostic_identity']={'status':'passed','assertions':count,'arbitrary_runtime_exemptions_allowed':False,'old_release_certificates_migrated':False}
 result={'schema':'ulike19102-focused-queue-evidence-v1','status':'passed','tests':reports,'assertions':sum(r['assertions'] for r in reports.values()),'production_source_sha256':sha(SOURCE/'GpuQualification1961.java'),'fingerprint_builder_sha256':sha(SOURCE/'native1960/build_native1960.py'),'physical_android_tested':False,'gpu_pixels_tested':False,'fixture_classes_in_runtime':False}
 return result

def production(args):
 if args.helpers is None or args.tools is None:raise ValueError('--helpers and --tools required')
 classes=args.work/'classes';classes.mkdir(parents=True)
 cp=os.pathsep.join(map(str,(args.helpers,args.tools/'android.jar')))
 command([args.jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',classes,SOURCE/'tests19102/ResidentPolicy19102Test.java'],args.work/'javac.log')
 stdout=command([args.jdk/'bin/java','-ea','-cp',str(classes)+os.pathsep+cp,'com.hiro.ulike.ResidentPolicy19102Test'],args.work/'ResidentPolicy19102Test.log')
 result=json.loads(stdout.strip().splitlines()[-1]);assert result['status']=='passed'
 result['schema']='ulike19102-production-policy-evidence-v1'
 result['production_class_sha256']={name:sha(args.helpers/'com/hiro/ulike'/(name+'.class')) for name in ('GpuStrong1960','GpuResident1976')}
 return result

def main():
 parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--phase',choices=('queue','production'),required=True)
 for name in ('work','jdk'):parser.add_argument('--'+name,type=Path,required=True)
 for name in ('helpers','tools'):parser.add_argument('--'+name,type=Path)
 args=parser.parse_args()
 for name in ('work','jdk','helpers','tools'):
  value=getattr(args,name)
  if value is not None:setattr(args,name,value.resolve())
 args.work.mkdir(parents=True,exist_ok=True)
 result=queue(args) if args.phase=='queue' else production(args)
 (args.work/'result.json').write_text(json.dumps(result,ensure_ascii=False,sort_keys=True,indent=2)+'\n');print(json.dumps(result,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
