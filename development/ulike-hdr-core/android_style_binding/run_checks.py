#!/usr/bin/env python3
"""Reproduce source, caller-owned-asset, Lua-mock and Android SDK compile gates.
No copied vendor assets are written into this source module.
"""
import argparse,hashlib,json,pathlib,subprocess,tempfile,importlib.util
from PIL import Image
ROOT=pathlib.Path(__file__).resolve().parent
WORK=ROOT.parents[2]
def run(args):
 p=subprocess.run([str(x) for x in args],check=True,text=True,capture_output=True)
 return p.stdout.strip()
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
 ap=argparse.ArgumentParser();ap.add_argument('--assets',type=pathlib.Path,default=WORK/'hdr_rebuild167/model_audit168/LOCAL_ONLY_styles');ap.add_argument('--natural-zip',type=pathlib.Path,default=WORK.parent/'upload/ULike_Natural_blush_1790815043265.zip');ap.add_argument('--purity-zip',type=pathlib.Path,default=WORK.parent/'upload/ULike_Purity2_1790815028634.zip');a=ap.parse_args()
 jdk=WORK/'models168/tools/jdk21/jdk-21.0.12.1+1/bin';sdk=WORK/'models168/tools/android.jar'
 observer=WORK/'export_recheck166/recovered/source/style_export/src/com/hiro/ulike'
 collector=ROOT.parent/'android_still_analysis/src/main/java/com/hiro/ulike/hdr/stillanalysis/StillMessageCollector.java'
 pipeline=ROOT.parent/'style_pipeline/src/main/java/com/hiro/ulike/style/SampledMakeupPipeline.java'
 production=sorted((ROOT/'src').rglob('*.java'));tests=sorted((ROOT/'test').rglob('*.java'))
 with tempfile.TemporaryDirectory(prefix='ulike-binding-qa-') as tmp:
  temp=pathlib.Path(tmp);host=temp/'host';host.mkdir();android=temp/'android';android.mkdir()
  run([jdk/'javac','-d',host,*production,*tests,collector,pipeline,observer/'StyleSnapshot164.java',observer/'ComposerPath164.java'])
  pngs=[]
  for asset in json.loads((ROOT/'ASSET_PINS.json').read_text())['assets']:
   p=a.assets/asset['style']/asset['path'];raw=p.read_bytes();assert digest(p)==asset['sha256'] and len(raw)==asset['bytes']
   if raw.startswith(b'\x89PNG'):
    image=Image.open(p).convert('RGBA');pngs.append('\t'.join([asset['style']+'/'+asset['path'],asset['sha256'],hashlib.sha256(image.tobytes()).hexdigest(),str(image.width),str(image.height)]))
  fixtures=temp/'png.tsv';fixtures.write_text('\n'.join(pngs)+'\n')
  installer=temp/'installer';installer.mkdir()
  installer_result=run([jdk/'java','-cp',host,'com.hiro.ulike.binding.InstallerContracts',a.natural_zip,a.purity_zip,installer])
  spec=importlib.util.spec_from_file_location('instrument',ROOT/'instrument_style.py');instrument=importlib.util.module_from_spec(spec);spec.loader.exec_module(instrument)
  parity=0
  for style,archive in [('natural',a.natural_zip),('purity',a.purity_zip)]:
   pyout=temp/('python-'+style);instrument.instrument(archive,style,pyout,314)
   for path in sorted((installer/style).rglob('*')):
    if path.is_file():
     assert path.read_bytes()==(pyout/path.relative_to(installer/style)).read_bytes();parity+=1
  assert parity==188
  checks={
   'android_installer':installer_result,
   'skin_probe':run([jdk/'java','-cp',host,'com.hiro.ulike.binding.SkinProbeContracts',a.natural_zip,a.purity_zip,installer]),
   'java_python_installer_byte_identical_files':parity,
   'binding_contracts':run([jdk/'java','-cp',host,'com.hiro.ulike.binding.BindingContracts',a.assets]),
   'texture_geometry_contracts':run([jdk/'java','-cp',host,'com.hiro.ulike.binding.TextureGeometryContracts',a.assets,fixtures]),
   'lua_instrumentation':run(['python',ROOT/'test/test_instrumentation.py',a.natural_zip,a.purity_zip])
  }
  run([jdk/'javac','-source','8','-target','8','-bootclasspath',sdk,'-d',android,*production,collector,pipeline])
  report={'schema':'android-style-binding-qa-1','checks':checks,'sdk36_compile':True,'sdk_sha256':digest(sdk),
   'production_sources':{str(p.relative_to(ROOT)):digest(p) for p in production},
   'dependency_sources':{str(p.relative_to(WORK)):digest(p) for p in [collector,pipeline,observer/'StyleSnapshot164.java',observer/'ComposerPath164.java']},
   'real_png_byte_decodes_compared_with_pillow':len(pngs),'real_assets_hash_checked':30,'instrumented_original_scripts':12,
   'geometry_test_positions':'synthetic planar positions with real pinned UV/topology/texture',
   'actual_sdk_message_delivery_tested':False,'actual_device_3d_geometry_tested':False,'native_2d_geometry_available':False,
   'skin_mask_pixels_available':False,'full_binding_provider_implemented':False,'skin_mask_diagnostic_installer_implemented':True,'actual_skin_mask_callback_calibrated':False,'hdr_or_complete_style_claim':False}
  (ROOT/'QA.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
if __name__=='__main__':main()
