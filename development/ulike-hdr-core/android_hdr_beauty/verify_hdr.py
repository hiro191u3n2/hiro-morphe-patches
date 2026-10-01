#!/usr/bin/env python3
"""SDK36 compile + actual-model synthetic P010 HDR appearance integration.
Original model/library/mask payloads remain local; no real device claim.
"""
import argparse,hashlib,json,os,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parent
CORE=ROOT.parent
os.environ['ORT_DISABLE_TELEMETRY']='1'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(cmd):
 p=subprocess.run(list(map(str,cmd)),text=True,stdout=subprocess.PIPE,stderr=subprocess.PIPE)
 if p.returncode:raise RuntimeError(p.stdout+'\n'+p.stderr)
 return p.stdout.strip()
def main():
 p=argparse.ArgumentParser()
 for n in ('jdk','android-jar','ort-android-classes','ort-host-jar','r8','baoman','goodlike','bytenn','effect','natural-zip','purity-zip'):p.add_argument('--'+n,type=Path,required=True)
 p.add_argument('--report',type=Path,required=True);a=p.parse_args()
 sources=[]
 for folder in ('hdr_input/src/main/java','android_model_runtime/src','android_beauty_image/src','android_hdr_color/src/main/java','face_analysis/src/main/java','style_pipeline/src/main/java','android_hdr_beauty/src'):sources+=sorted((CORE/folder).rglob('*.java'))
 before={str(f.relative_to(CORE)):sha(f) for f in sources}
 with tempfile.TemporaryDirectory(prefix='ulike-hdr-beauty-') as t:
  t=Path(t);classes=t/'classes';classes.mkdir()
  run([a.jdk/'javac','-source','8','-target','8','-bootclasspath',a.android_jar,'-cp',a.ort_android_classes,'-d',classes,*sources])
  prod=t/'prod.jar';run([a.jdk/'jar','--create','--file',prod,'-C',classes,'.'])
  dex=t/'dex';dex.mkdir();run([a.jdk/'java','-cp',a.r8,'com.android.tools.r8.D8','--min-api','33','--lib',a.android_jar,'--classpath',a.ort_android_classes,'--output',dex,prod])
  run([a.jdk/'javac','--release','8','-cp',str(classes)+os.pathsep+str(a.ort_android_classes),'-d',classes,*sorted((ROOT/'test').rglob('*.java'))])
  ordered=json.loads(run([a.jdk/'java','-Xmx128m','-cp',str(classes)+os.pathsep+str(a.ort_android_classes),'hiro.ulike.beauty.OrderedFacesTest']));print(json.dumps({'ordered_faces':ordered}),flush=True)
  cases=[]
  for style,model,archive in [('NATURAL_BLUSH',a.baoman,a.natural_zip),('PURITY2',a.goodlike,a.purity_zip)]:
   line=run([a.jdk/'java','-Xmx384m','-cp',str(classes)+os.pathsep+str(a.ort_host_jar),'com.hiro.ulike.hdr.input.HdrBeautyIntegration',style,model,a.bytenn,a.effect,archive]);print(line,flush=True);cases.append(json.loads(line))
 assert before=={str(f.relative_to(CORE)):sha(f) for f in sources}
 report={'status':'PASS_SDK36_D8_HOST_ACTUAL_MODEL_HDR_APPEARANCE','cases':cases,'ordered_faces_author_tests':ordered,'source_sha256':before,'test_sha256':{str(f.relative_to(ROOT)):sha(f) for f in sorted((ROOT/'test').rglob('*.java'))},'script_sha256':sha(Path(__file__)),'sdk36_compile_d8_min33':True,'input_output_dimensions':[512,384],'model_crop_dimensions':[256,256],'actual_models_and_masks':True,'synthetic_p010_and_explicit_synthetic_style_bindings':True,'whole_captured_hdr_information_preserved':False,'native_hdr_style_equivalent':False,'android_device_execution':False,'published_photo':False,'telemetry_opt_out_before_runtime':True,'input_sha256':{n:sha(getattr(a,n)) for n in ('baoman','goodlike','bytenn','effect','natural_zip','purity_zip')}}
 a.report.write_text(json.dumps(report,indent=2)+'\n')
if __name__=='__main__':main()
