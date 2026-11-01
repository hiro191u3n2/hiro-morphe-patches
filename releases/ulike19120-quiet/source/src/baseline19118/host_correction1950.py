#!/usr/bin/env python3
"""Execute the production DEX correction before/after removal of an unused copy.

The general correction API is pinned and unmodified. The focused save-strip
clone omits only destination initialization because both production kernels
initialize every consumed output pixel themselves. Native ARM64 is statically
reviewed from its pinned actual binary; it is not executed on this host.
"""
from pathlib import Path
import argparse,hashlib,json,os,shutil,subprocess
ROOT=Path(__file__).resolve().parent
DEX_SHA256='e40b22ecbaa99911c40f67d2af9411247111bb719c6ab1796dfcbc8070bd01a6'
NATIVE_SHA256='0ac181fac2afbd9260b8c59d922843deead72d3184e78ce97346d57a76b169cb'
DISASM_SHA256='698a4b2180c843b9cdb2914ba6cc38fe66bcfdaee0e5b833839d6ddaa414b62b'
NATIVE_ENTRY='ulike186/runtime/0000.bin'
def digest(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()
def require(value,message):
 if not value:raise RuntimeError(message)
def test(root,work,android):
 root,work,android=Path(root),Path(work),Path(android)
 out=work/'host-correction1950';out.mkdir(parents=True,exist_ok=True)
 # During a reproducible release build, MethodContract and the old native
 # artifact are extracted into the current build's baseline directory.
 candidates=[p/'baseline' for p in [work,*work.parents]]
 if os.environ.get('ULIKE_CORRECTION_BASELINE'):candidates.insert(0,Path(os.environ['ULIKE_CORRECTION_BASELINE']))
 baseline=next((p for p in candidates if (p/'app/hiro/ulike/patches/MethodContract.class').is_file() and (p/NATIVE_ENTRY).is_file()),None)
 require(baseline is not None,'Pinned extracted build baseline required for production correction audit')
 morphe=android.parent/'morphe.jar'
 if os.environ.get('ULIKE_MORPHE_JAR'):morphe=Path(os.environ['ULIKE_MORPHE_JAR'])
 require(morphe.is_file(),'Morphe dexlib tooling required for direct production bytecode execution')
 dex=root/'tests/correction1950-baseline.dex'
 native=baseline/NATIVE_ENTRY
 disasm=root/'tests/correction1950-native.disasm.txt'
 require(digest(dex)==DEX_SHA256,'Unreviewed production Chroma186 baseline DEX')
 require(digest(native)==NATIVE_SHA256,'Production correction native binary differs from audited ARM64 artifact')
 require(digest(disasm)==DISASM_SHA256,'Native destination-access disassembly changed')
 text=disasm.read_text()
 # These exact instructions initialize the current output pixel before all
 # subsequent correction branches. Output pointer advances by width per row;
 # row loop is [begin,end) and column loop [0,width).
 for witness in [
  '49ac: b8716b70', # ldr w16,[filtered-row,x17]
  '49b8: b8316b90', # str w16,[output-row,x17]
  '48a8: 6b0801bf', # compare current row with end
  '48b4: 8b08039c', # advance output row by width*4
  '4994: eb0a027f', # compare column with width
  '4fd4: b82a7b8b', # output[current] corrected pixel store
  '511c: b82a7b90', # output[current] alternative corrected pixel store
 ]:require(witness in text,'Pinned native output initialization/boundary witness missing: '+witness)
 javac=os.environ.get('ULIKE_JAVAC') or shutil.which('javac')
 java=os.environ.get('ULIKE_JAVA') or (str(Path(javac).with_name('java')) if javac else shutil.which('java'))
 require(javac is not None and java is not None,'JDK compiler required for production DEX oracle')
 cp=os.pathsep.join([str(morphe),str(baseline)])
 target=out/'classes';target.mkdir(exist_ok=True)
 built=subprocess.run([javac,'-encoding','UTF-8','-cp',cp,'-d',str(target),str(root/'MergePayloads.java'),str(root/'CorrectionHooks1950.java'),str(root/'tests/CorrectionDex1950Test.java')],capture_output=True,text=True,timeout=120)
 (out/'compile.log').write_text(built.stdout+built.stderr)
 require(built.returncode==0,'Production correction oracle failed compilation: '+built.stderr[-6000:])
 executed=subprocess.run([java,'-XX:ActiveProcessorCount=4','-cp',cp+os.pathsep+str(target),'CorrectionDex1950Test',str(dex)],capture_output=True,text=True,timeout=180)
 (out/'test.log').write_text(executed.stdout+executed.stderr)
 require(executed.returncode==0,'Production correction oracle failed: '+executed.stderr[-8000:])
 result=json.loads(executed.stdout)
 require(result.get('status')=='passed' and result.get('assertions',0)>0 and result.get('pixel_comparisons',0)>0,'Executed exact correction assertions missing')
 pipeline=(root/'QualityPipeline1932.java').read_text()
 require(pipeline.count('Chroma186.finishConsumed1950(')==2 and 'Chroma186.finishWorkspace(' not in pipeline,'Exact focused saved-image correction consumers required')
 require('System.arraycopy(result,lo*state.width,w.denoised,lo*state.width,(hi-lo)*state.width);' in pipeline and 'state.write(result,start,first,count);' in pipeline,'Residual/publish consumers must retain bounded corrected row range')
 result.update({
  'baseline_dex_sha256':DEX_SHA256,
  'actual_native_binary_pinned':True,
  'native_binary_sha256':NATIVE_SHA256,
  'native_disassembly_sha256':DISASM_SHA256,
  'native_correction_executed_on_host':False,
  'native_correction_proof':'Pinned ARM64 disassembly: current filtered pixel is stored to destination at 0x49ac/0x49b8 before branches, destination pointers stay in requested rows; subsequent stores are same current pixel. No destination halo reads.',
  'production_copy_only_inverse_verified':True,
  'removed_copy_bytes_per_consumed_pass':result['copy_width']*result['copy_rows']*4,
  'copy_example_shape':{'width':result['copy_width'],'strip_rows':result['copy_rows']},
  'copy_benchmark_scope':'Actual Java System.arraycopy cost only; does not predict Android correction or total camera/save speed.',
  'copy_benchmark_mean_ms':result['host_copy_elapsed_nanos']/result['host_copy_iterations']/1_000_000,
  'physical_android_tested':False,
  'device_speedup_verified':False,
 })
 (out/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2,sort_keys=True)+'\n')
 return result
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);p.add_argument('--android',type=Path,required=True);p.add_argument('--baseline',type=Path);a=p.parse_args()
 if a.baseline:os.environ['ULIKE_CORRECTION_BASELINE']=str(a.baseline)
 print(json.dumps(test(ROOT,a.out,a.android),ensure_ascii=False))
