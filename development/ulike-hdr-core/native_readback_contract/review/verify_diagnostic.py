#!/usr/bin/env python3
import argparse,hashlib,json,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1];CORE=ROOT.parent

def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def run(a):
 p=subprocess.run(list(map(str,a)),capture_output=True,text=True)
 if p.returncode:raise RuntimeError(p.stdout+'\n'+p.stderr)
 return p.stdout.strip()
def main():
 p=argparse.ArgumentParser()
 for key in ('jdk','android-jar','natural-zip','purity-zip','report'):p.add_argument('--'+key,type=Path,required=True)
 a=p.parse_args();sources=sorted((CORE/'android_style_binding/src').rglob('*.java'))+[CORE/'style_pipeline/src/main/java/com/hiro/ulike/style/SampledMakeupPipeline.java',CORE/'android_still_analysis/src/main/java/com/hiro/ulike/hdr/stillanalysis/StillMessageCollector.java'];before={str(f.relative_to(CORE)):sha(f) for f in sources}
 with tempfile.TemporaryDirectory(prefix='ulike-mask-independent-') as t:
  t=Path(t);classes=t/'classes';classes.mkdir();private=t/'private';private.mkdir()
  run([a.jdk/'javac','-source','8','-target','8','-bootclasspath',a.android_jar,'-d',classes,*sources])
  run([a.jdk/'javac','--release','8','-cp',classes,'-d',classes,ROOT/'review/ReviewDiagnosticMask.java'])
  result=json.loads(run([a.jdk/'java','-Xmx128m','-cp',classes,'review.ReviewDiagnosticMask',a.natural_zip,a.purity_zip,private]))
 assert before=={str(f.relative_to(CORE)):sha(f) for f in sources}
 report={'status':'PASS_INDEPENDENT_DIAGNOSTIC_MASK_INSTALLER_AND_PARSER_REVIEW','results':result,'sdk36_compile':True,'reviewed_module_files':['android_style_binding/src/com/hiro/ulike/binding/SkinMaskProbeInstaller.java','android_style_binding/src/com/hiro/ulike/binding/DiagnosticSkinMask.java'],'compiled_dependency_sha256':before,'review_sha256':{str(f.relative_to(ROOT)):sha(f) for f in [ROOT/'review/ReviewDiagnosticMask.java',Path(__file__)]},'scope':{'actual_native_mask_callback_executed':False,'same_shot_mask_calibrated':False,'native_fullprecision_mask':False,'final_2d_geometry_available':False,'shader_only_replacement_of_exact_pinned_final_passes':True,'no_automatic_channel_orientation_or_gamma_inference':True}}
 a.report.write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(result))
if __name__=='__main__':main()
