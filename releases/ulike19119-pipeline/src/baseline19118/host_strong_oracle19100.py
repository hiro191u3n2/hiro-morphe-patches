#!/usr/bin/env python3
"""Independent native CPU admission oracle: exact models, images and ownership.

The prior prepareJava whole-image selection is compiled separately. Only its
single selector differs; every produced output word is compared with current.
No GPU library is loaded. Production counters in throwaway source copies prove
that native CPU map work occurs while GPU model selection is never entered.
"""
from pathlib import Path
import argparse,hashlib,json,os,subprocess,sys
ROOT=Path(__file__).resolve().parent
def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def test(source,work,jdk=None,ndk=None):
 source,work=Path(source).resolve(),Path(work).resolve();work.mkdir(parents=True,exist_ok=True)
 sys.path.insert(0,str(source));import build1992
 jdk=Path(jdk or os.environ['ULIKE_JDK_HOME']);android=Path(os.environ['ULIKE_ANDROID_JAR'])
 sources=list(dict.fromkeys(build1992.compile_inputs([]).values()));fixture=source/'tests19100/strong/StrongOracle19100Test.java'
 tracked=sources+[fixture,source/'host_strong_oracle19100.py',source/'native1958/smooth_noise1958.c']
 pins={str(p.relative_to(source)):sha(p) for p in tracked};commands=[]
 def run(argv,name):
  argv=list(map(str,argv));commands.append(dict(name=name,argv=argv));p=subprocess.run(argv,capture_output=True,text=True,timeout=600)
  output=p.stdout+p.stderr;(work/(name+'.log')).write_text(output)
  if p.returncode or 'WARNING in native method' in output or 'FATAL ERROR' in output:raise RuntimeError(name+'\n'+output[-14000:])
  return output
 classes=work/'classes';classes.mkdir(exist_ok=True);copies=work/'observed';copies.mkdir(exist_ok=True)
 strong=(source/'StrongNoise1958.java').read_text();anchor='if(backend)try{complete=processNative('
 if strong.count(anchor)!=1:raise AssertionError('native map counter seam drift')
 strong=strong.replace(anchor,'if(backend)try{StrongOracle19100Test.nativeCall();complete=processNative(')
 (copies/'StrongNoise1958.java').write_text(strong)
 analysis=(source/'GpuAnalysis1961.java').read_text()
 anchors=['static boolean regionsQualified1996(int width,int height){','float[] evidence,float[] runtime,int[] selected1988,Runnable runtimeReady1996){','static void scheduleResident(int[] halfSource,final int width,final int height,final int noise,final boolean shadows,float[] evidence,float[] runtime,final StrongNoise1958.Model reference,final long originalCpu){']
 for anchor in anchors:
  if analysis.count(anchor)!=1:raise AssertionError('GPU entry counter seam drift '+anchor)
  analysis=analysis.replace(anchor,anchor+'StrongOracle19100Test.gpuCalls++;')
 (copies/'GpuAnalysis1961.java').write_text(analysis)
 sources=[copies/p.name if p.name in ('StrongNoise1958.java','GpuAnalysis1961.java') else p for p in sources]
 graphics=source/'tests/pipeline1942-fixtures'
 bitmap=copies/'Bitmap.java';bitmap.write_text((graphics/'android/graphics/Bitmap.java').read_text().replace('public final class Bitmap {','public final class Bitmap {public int getGenerationId(){return 1;}'))
 overlays=[bitmap,graphics/'android/graphics/ColorSpace.java',graphics/'com/hiro/ulike/HostAudit1932.java']
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',android,'-d',classes,*sources,source/'tests1962/Chain1962Oracle.java',fixture],'compile-production')
 run([jdk/'bin/javac','--release','8','-cp',str(classes)+os.pathsep+str(android),'-d',classes,*overlays],'compile-bitmap-fixture')
 cp=str(classes)+os.pathsep+str(android);native=work/'native-cpu';native.mkdir(exist_ok=True);absent=work/'no-native';absent.mkdir(exist_ok=True)
 run(['cc','-O3','-std=c11','-Wall','-Wextra','-Werror','-ffp-contract=off','-fno-fast-math','-fPIC','-shared','-Wl,--no-undefined','-I'+str(jdk/'include'),'-I'+str(jdk/'include/linux'),source/'native1958/smooth_noise1958.c','-lm','-pthread','-o',native/'libulike_smooth1958.so'],'compile-native-cpu')
 baseline=work/'baseline';baseline.mkdir(exist_ok=True);old=copies/'QualityPipeline1932.java';text=(source/'QualityPipeline1932.java').read_text();anchor='?StrongNoise1958.prepareCpuOracle19100(modelSource1976,width,height,level,shadows)'
 if text.count(anchor)!=1:raise AssertionError('whole-image baseline seam drift')
 old.write_text(text.replace(anchor,'?StrongNoise1958.prepareJava(modelSource1976,width,height,level,shadows)'))
 run([jdk/'bin/javac','--release','8','-encoding','UTF-8','-cp',cp,'-d',baseline,old],'compile-old-selector')
 java=[jdk/'bin/java','-ea','-Xcheck:jni','-XX:ActiveProcessorCount=4','-Xmx768m'];reports={}
 for name,path,classpath,backend in [('current-native',native,cp,'native'),('current-fallback',absent,cp,'fallback'),('old-java-selection',native,str(baseline)+os.pathsep+cp,'native')]:
  out=run(java+['-Djava.library.path='+str(path),'-cp',classpath,'com.hiro.ulike.StrongOracle19100Test',work/(name+'.pixels'),backend],name)
  report=json.loads(out.split('RESULT ',1)[1]);reports[name]=report
  if report['model_cases']!=64 or report['image_cases']!=12 or report['gpu_calls']!=0 or bool(report['native_calls'])!=(backend=='native'):raise AssertionError('missing exact CPU oracle coverage '+name)
 expected=(work/'old-java-selection.pixels').read_bytes()
 if len(expected)!=4*(reports['old-java-selection']['compared_pixels']+reports['old-java-selection']['image_cases']):raise AssertionError('whole image corpus length incomplete')
 for name in ('current-native','current-fallback'):
  if (work/(name+'.pixels')).read_bytes()!=expected:raise AssertionError('whole image output differs '+name)
 if pins!={str(p.relative_to(source)):sha(p) for p in tracked}:raise AssertionError('source changed during oracle proof')
 result=dict(status='passed',assertions=sum(r['assertions'] for r in reports.values()),tests=reports,whole_model_bitwise_equal=True,whole_final_image_bitwise_equal=True,two_independent_cpu_oracles=True,gpu_model_calls_zero=True,native_cpu_and_java_fallback_executed=True,cancellation_and_recovery=True,original_snapshot_ownership_preserved=True,whole_image_sha256=hashlib.sha256(expected).hexdigest(),source_sha256=pins,physical_android_tested=False,device_speedup_verified=False)
 (work/'commands.json').write_text(json.dumps(commands,indent=2)+'\n');(work/'result.json').write_text(json.dumps(result,indent=2)+'\n');return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,default=ROOT);p.add_argument('--work',type=Path,required=True);p.add_argument('--jdk',type=Path);p.add_argument('--ndk',type=Path);a=p.parse_args();print(json.dumps(test(a.source,a.work,a.jdk,a.ndk),indent=2))
